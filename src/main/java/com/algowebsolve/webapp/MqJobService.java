package com.algowebsolve.webapp;

import com.algowebsolve.webapp.apiv1.Problems;
import com.algowebsolve.webapp.nsystem.linux.MqIo;
import com.algowebsolve.webapp.reactivemq.MqReaderIoLoop;
import com.algowebsolve.webapp.reactivemq.MqWriterIoLoop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;

@Service
public class MqJobService {
    @Autowired
    private static MqReaderIoLoop mqReader;

    @Autowired
    private static MqWriterIoLoop mqWriter;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqJobService.class);

    byte[] singleJob;

    public long startJob(byte[] data) {

        //return mqWriter.postJob("rishin_out", data);
        singleJob = data;
        return  -1;
    }

    public boolean isDone(long jobId) {
        //return mqReader.isDone(jobId);
        return true;
    }

    public byte[] getResult(long jobId) {
        //return mqReader.getJob(jobId);
        return singleJob;
    }
}
