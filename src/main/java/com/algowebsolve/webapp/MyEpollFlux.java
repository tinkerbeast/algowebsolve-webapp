package com.algowebsolve.webapp;


import io.dvlopt.linux.epoll.Epoll;
import io.dvlopt.linux.epoll.EpollEvent;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.*;

@Component
public class MyEpollFlux<T> implements Publisher<T>, Runnable {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TaskExecutor executor;

    String fifoPath;
    Epoll poller;
    Queue<T> queue;
    Logger logger = LoggerFactory.getLogger(MyEpollFlux.class);
    boolean finished = false;

    MyEpollFlux() {
        logger.info("RISHIN: In MyEpollFlux constructor ...");
        this.fifoPath = "/tmp/rishinfifo";
        try {
            this.poller = new Epoll();
            this.queue = new ConcurrentLinkedQueue<>();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Bean
    void startPublisher() {
        executor.execute(this);
    }

    synchronized boolean isRunning() {
        return !finished;
    }

    synchronized void setFinished() {
        this.finished = true;
    }

    T getItem() {
        return this.queue.poll();
    }

    @Override
    public void run() {

        try (NativeIo io = new NativeIo(this.fifoPath, NativeIo.O_RDONLY)) {
            int fd = io.getFd();

            EpollEvent.Flags toMonitorFlags = new EpollEvent.Flags();
            toMonitorFlags.set(EpollEvent.Flag.EPOLLIN);
            //toMonitorFlags.set(EpollEvent.Flag.EPOLLOUT);
            // TODO: Error and other flags

            EpollEvent toMonitorEvents = new EpollEvent();
            toMonitorEvents.setFlags(toMonitorFlags);

            poller.add(fd, toMonitorEvents);

            while (true) {
                EpollEvent incomingEvent = new EpollEvent();
                poller.wait(incomingEvent);
                if (incomingEvent.getFlags().isSet(EpollEvent.Flag.EPOLLIN)) {
                    String readItem = io.read();
                    // TODO: return value check
                    this.queue.offer((T)readItem); // TODO: typecast fix
                    logger.info("Readitem: " + readItem + " queue_size=" + this.queue.size());
                } else {
                    this.setFinished();
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            this.setFinished();
        }
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        if (subscriber == null) throw null;
        subscriber.onSubscribe(new MyEpollSubscription<T>(subscriber, this));
    }
}


class MyEpollSubscription<T> implements Subscription {



    Subscriber<? super T> downstream;
    MyEpollFlux<T> upstream;

    MyEpollSubscription(Subscriber<? super T> subscriber, MyEpollFlux<T> publisher) {
        this.downstream = subscriber;
        this.upstream = publisher;
    }

    @Override
    public void request(long l) {
        for (int i = 0; i < l; i++) {
            if (!this.upstream.isRunning()) {
                break;
            }
            T item = this.upstream.getItem();
            while (item == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    downstream.onError(e);
                }
                item = this.upstream.getItem();
            }
            downstream.onNext(item);
        }

        if (!this.upstream.isRunning()) {
            downstream.onComplete();
        }
    }

    @Override
    public void cancel() {
        this.upstream.setFinished();
    }
}