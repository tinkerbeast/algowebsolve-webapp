package com.algowebsolve.webapp.reactivemq;

import lombok.Data;
import java.util.concurrent.atomic.AtomicLong;


@Data
public class MqPacket {
    long id;
    byte[] data;
}

class MqJobIdFactory {

    private static MqJobIdFactory instance = null;
    private AtomicLong idCounter = null;

    private MqJobIdFactory() {
        idCounter = new AtomicLong(0);
    }

    public static synchronized MqJobIdFactory getInstance() {
        if (instance == null) {
            instance = new MqJobIdFactory();
        }
        return instance;
    }

    public long produceId() {
        return idCounter.incrementAndGet();
    }
}