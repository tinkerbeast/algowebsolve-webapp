package com.algowebsolve.webapp.reactivemq;

import com.algowebsolve.webapp.model.ProblemRequest;
import com.algowebsolve.webapp.nsystem.PacketIoInterface;
import com.algowebsolve.webapp.nsystem.linux.MqIo;
import com.algowebsolve.webapp.nsystem.linux.NativeIo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Service
public class SimpleMqIoLoop {

    @Autowired
    MqPacketIdFactory packetIdFactory;

    @Autowired
    private TaskExecutor executor;

    private static class MqPacket {
        public long id;
        public ProblemRequest data;
    }

    // service parameters
    private static final int SENDER_PENDING_LIMIT = 128;
    private static final int RECEIVER_PENDING_LIMIT = 512;
    private static final String SENDER_QNAME = "rishin_out"; // TODO: parameterise mq name
    private static final String RECEIVER_QNAME = "rishin_in"; // TODO: parameterise mq name
    // statics
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqWriterIoLoop.class);
    // members
    private BlockingQueue<MqPacket> sendingQ;
    private Map<Long, ProblemRequest> receivingSet;
    private PacketIoInterface sendingMq;
    private PacketIoInterface receiverMq;

    SimpleMqIoLoop() throws IOException {
        sendingQ = new ArrayBlockingQueue<>(SENDER_PENDING_LIMIT);
        receivingSet = Collections.synchronizedMap(new FixedMap<Long, ProblemRequest>(new TreeMap<Long, ProblemRequest>(), RECEIVER_PENDING_LIMIT));
        sendingMq = new MqIo(SENDER_QNAME, NativeIo.O_WRONLY);
        receiverMq = new MqIo(RECEIVER_QNAME, NativeIo.O_RDONLY);
    }

    public long addJob(ProblemRequest data) {
        long id = packetIdFactory.produceId();
        MqPacket packet = new MqPacket();
        packet.id = id;
        packet.data = data;
        boolean accepted = sendingQ.offer(packet);
        return accepted? id: -1L;
    }

    public boolean isDone(long jobId) {
        return receivingSet.get(jobId) != null;
    }

    public ProblemRequest getResult(long jobId) {
        return receivingSet.remove(jobId);
    }

    @Bean(name = "SimpleMqIoLoop.start")
    public void start() {
        executor.execute(this::readerLoop);
        executor.execute(this::writerLoop);
    }

    private void writerLoop() {
        while(true) {
            try {
                MqPacket packet = sendingQ.take();
                byte[] data = mapper.writeValueAsBytes(packet);
                sendingMq.send(data, MqIo.MSG_PRIORITY_DEFAULT);
            } catch (JsonParseException | JsonMappingException e) {
                log.error("Error in sending to processing", e);
            } catch (IOException e) {
                log.error("MqIo write error", e);
                System.exit(1); // TODO: IOException over mq is currently unhandled
            } catch (InterruptedException e) {
                log.info("Interruption probably for exit sequence");
            }
        }
    }

    private void readerLoop() {
        while(true) {
            try {
                byte[] data = receiverMq.recv();
                MqPacket packet = mapper.readValue(data, MqPacket.class);
                receivingSet.put(packet.id, packet.data);
            } catch (JsonParseException | JsonMappingException e) {
                log.error("Error in receiving from processing", e);
            } catch (IOException e) {
                log.error("MqIo read error", e);
                System.exit(1); // TODO: IOException over mq is currently unhandled
            }
        }
    }

}
