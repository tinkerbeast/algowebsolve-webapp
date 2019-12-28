package com.algowebsolve.webapp.reactivemq;

import java.io.IOException;
import java.util.stream.Stream;

public interface MqIoLoopable extends Runnable {

    void start();
    Stream<byte[]> stream();
    NativeMq getOrCreateMq(String mqName) throws IOException;
    NativeMq getMq(String mqName);
    void run();
    long postJob(String mqName, byte[] data);
    byte[] getJob(long id);
}
