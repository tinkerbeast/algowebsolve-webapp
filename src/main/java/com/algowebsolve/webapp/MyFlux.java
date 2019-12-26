package com.algowebsolve.webapp;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import org.springframework.stereotype.Component;


@Component
public class MyFlux implements Publisher<Integer> {


    @Override
    public void subscribe(Subscriber<? super Integer> subscriber) {
        if (subscriber == null) throw null;
        subscriber.onSubscribe(new MyIncrementSubscription(subscriber));
    }
}


class MyIncrementSubscription implements Subscription {

    boolean isRunning = true;
    int current = 0;
    Subscriber<? super Integer> downstream;

    MyIncrementSubscription(Subscriber<? super Integer> subscriber) {
        this.downstream = subscriber;
    }

    @Override
    public void request(long l) {
        for (int i = 0; i < l; i++) {
            if (!isRunning) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                downstream.onError(e);
            }

            downstream.onNext(current);
            current += 1;
        }
        downstream.onComplete();
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}

