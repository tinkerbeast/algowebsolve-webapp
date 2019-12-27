package com.algowebsolve.webapp.reactivemq;

import java.util.stream.Stream;

public interface MqIoLoopable extends Runnable {

    void start();
    Stream<byte[]> stream();
    NativeMq getOrCreateMq(String mqName);
    void run();
}
