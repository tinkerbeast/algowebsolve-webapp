package com.algowebsolve.webapp;


import io.dvlopt.linux.epoll.Epoll;
import io.dvlopt.linux.epoll.EpollEvent;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;


public class MyEpollFlux<T> implements Publisher<T>, Runnable {

    private ExecutorService executor= Executors.newSingleThreadExecutor();
    Future ioLoopFuture;
    String fifoPath;
    Epoll poller;
    SynchronousQueue<T> queue;
    boolean finished = false;

    MyEpollFlux(String fifoPath) {
        System.out.println("RISHIN: In constructor ...");
        try {
            this.fifoPath = fifoPath;
            this.poller = new Epoll();
            this.queue = new SynchronousQueue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.ioLoopFuture = executor.submit(this);
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

        try (NativeIo io = new NativeIo(this.fifoPath)) {
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
                    this.queue.offer((T)io.read()); // TODO: typecast fix
                } else {
                    this.setFinished();
                }
            }
        } catch (IOException e) {
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
            downstream.onNext(this.upstream.getItem());
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