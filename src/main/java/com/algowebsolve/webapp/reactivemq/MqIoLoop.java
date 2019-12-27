package com.algowebsolve.webapp.reactivemq;

import com.algowebsolve.webapp.NativeIo;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class MqIoLoop {

    Map<String, NativeMq> mqMap = null;

    public NativeMq getOrCreateMq(String mqName) throws IOException {
        NativeMq mq = null;
        mq = this.mqMap.get(mqName);
        if (mq == null) {
            mq = new NativeMq(mqName, NativeIo.O_RDONLY);
            this.mqMap.put()
        }

    }
}
