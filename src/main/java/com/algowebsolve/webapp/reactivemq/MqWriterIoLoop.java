package com.algowebsolve.webapp.reactivemq;

import com.algowebsolve.webapp.NativeIo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Native;
import io.dvlopt.linux.epoll.Epoll;
import io.dvlopt.linux.epoll.EpollEvent;
import io.dvlopt.linux.epoll.EpollEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;


public class MqWriterIoLoop implements MqIoLoopable {

    @Autowired
    MqPacketIdFactory packetIdFactory;

    private static final ObjectMapper mapper = new ObjectMapper();

    private Logger logger = LoggerFactory.getLogger(MqWriterIoLoop.class);

    private Map<String, NativeMq> mqMap = null;
    private Map<Integer, NativeMq> fdMap = null;
    private Map<String, Queue<MqPacket>> sendQ = null;
    private Epoll poller = null;
    private String defaultMq = null;
    private TaskExecutor executor = null;
    private int millis = -1;
    private int batchSize = -1;
    private int stock = -1;

    public MqWriterIoLoop(int millisLatency, int batchSize, int stock, String defaultMq, TaskExecutor executor) throws IOException {
        //this.millis = 10; // latency in terms ioloop blocking
        //this.batchSize = 3; // maximum messages wait for ioloop
        //this.stock = 10240; // store upto 10240 responses before they are deleted
        this.millis = millisLatency;
        this.batchSize = batchSize;
        this.stock = stock;
        this.mqMap = new ConcurrentHashMap<>();
        this.fdMap = new ConcurrentHashMap<>();
        this.sendQ = new ConcurrentHashMap<>();
        this.poller = new Epoll();
        this.executor = executor;

        this.defaultMq = defaultMq;
        this.getOrCreateMq(defaultMq);
    }

    @Override
    public void start() {
        this.executor.execute(this);
    }

    @Override
    public Stream<byte[]> stream() {
        throw new UnsupportedOperationException("MqWriterIoLoop does not support stream api");
    }

    long post(String mqName, byte[] data) {
        // create a packet around the data
        long jobId = packetIdFactory.produceId();
        MqPacket packet = new MqPacket();
        packet.setId(jobId);
        packet.setData(data));
        // add the packet to processing queue
        Queue<MqPacket> inQueue = this.sendQ.get(mqName);
        synchronized (inQueue) { // TODO: RISHIN_A1: Used synchronised here since I dind't make the queue synchronous
            inQueue.add(packet);
        }
        return jobId;
    }

    @Override
    public NativeMq getOrCreateMq(String mqName) throws IOException {
        NativeMq mq = null;
        mq = this.mqMap.get(mqName);
        if (mq == null) {
            mq = new NativeMq(mqName, NativeIo.O_WRONLY);
            this.mqMap.put(mqName, mq);
            this.fdMap.put(mq.getFd(), mq);
            this.sendQ.put(mqName, new FixedQueue<>(new LinkedList<>(), this.batchSize)); // TODO: Synchronisation is must for the queue See RISHIN_A1 marker
            this.monitorMq(mq);
            logger.info(String.format("Started monitoring new queue, queue=%s fd=%d instance=%s", mqName, mq.getFd(), mq));
        }
        return mq;
    }

    private void monitorMq(NativeMq mq) throws IOException {
        // TODO: move this code block to single place instead of recreating every time
        EpollEvent.Flags toMonitorFlags = new EpollEvent.Flags();
        //toMonitorFlags.set(EpollEvent.Flag.EPOLLIN);
        toMonitorFlags.set(EpollEvent.Flag.EPOLLOUT);
        // TODO: Error and other flags
        EpollEvent toMonitorEvents = new EpollEvent();
        toMonitorEvents.setFlags(toMonitorFlags);

        poller.add(mq.getFd(), toMonitorEvents);
    }

    @Override
    public void run() {

        while (true) {
            // create epoll events storage and wait for events
            EpollEvents incomingEvents = new EpollEvents(batchSize);
            int numEvents = 0;
            try {
                numEvents = poller.wait(incomingEvents, millis);
            } catch (IOException e) {
                logger.error("IO error on poller wait", e);
            }
            // exit if interrupted
            if (Thread.interrupted()) {
                logger.info("Service stopping");
                break;
            }
            // check if epoll returned a valid state
            if (numEvents < 0) {
                int errno = Native.getLastError();
                logger.error("Native call 'epoll_wait' failed, errno=" + errno);
                continue;
            }
            // process events
            for (int i = 0; i < numEvents; i++) {
                EpollEvent event = incomingEvents.getEpollEvent(i);
                if (event.getFlags().isSet(EpollEvent.Flag.EPOLLOUT)) {
                    long userData = event.getUserData(); // TODO: hate casting (even when ok)
                    logger.info("userData=" + userData); // TODO ERROR in underlying lib - Why is is this zero?
                    try {
                        // TODO: HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE
                        // TODO: MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX
                        // TODO: Only working since we're listening on one queue
                        MqPacket packet = this.sendQ.get(this.defaultMq).remove();
                        byte[] data = mapper.writeValueAsBytes(packet);
                        mqMap.get(this.defaultMq).send(data, NativeMq.MSG_PRIORITY_DEFAULT);
                    } catch (JsonParseException | JsonMappingException e) {
                        logger.error("Failed to deserialise packet", e);
                    } catch (IOException e) {
                        logger.error("Message queue receive had IO error", e);
                    }
                } else {
                    // TODO: handle all events
                    logger.error("TODO: Could not handle event event=" + event.getFlags());
                }
            }
        }
    }
}
