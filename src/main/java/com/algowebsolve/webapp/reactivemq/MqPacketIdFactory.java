package com.algowebsolve.webapp.reactivemq;


import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MqPacketIdFactory {

    private AtomicLong idCounter = null;

    public MqPacketIdFactory() {
        this.idCounter = new AtomicLong(0);
    }

    public long produceId() {
        return idCounter.incrementAndGet();
    }
}
