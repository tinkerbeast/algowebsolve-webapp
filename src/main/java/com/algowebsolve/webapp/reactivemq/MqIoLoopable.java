package com.algowebsolve.webapp.reactivemq;

import com.algowebsolve.webapp.nsystem.linux.MqIo;

import java.io.IOException;
import java.util.stream.Stream;

public interface MqIoLoopable extends Runnable {

    void start();
    Stream<byte[]> stream();
    MqIo getOrCreateMq(String mqName) throws IOException;
    MqIo getMq(String mqName);
    void run();
    long postJob(String mqName, byte[] data);
    byte[] getJob(long id);
}
