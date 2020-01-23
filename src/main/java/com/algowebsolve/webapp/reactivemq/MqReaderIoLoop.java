package com.algowebsolve.webapp.reactivemq;

import com.algowebsolve.webapp.nsystem.linux.NativeIo;
import com.algowebsolve.webapp.nsystem.linux.MqIo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Native;
import io.dvlopt.linux.epoll.Epoll;
import io.dvlopt.linux.epoll.EpollEvent;
import io.dvlopt.linux.epoll.EpollEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class MqReaderIoLoop implements MqIoLoopable {

    @Autowired
    private TaskExecutor executor;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqReaderIoLoop.class);

    private Map<String, MqIo> mqMap = null;
    private Map<Integer, MqIo> fdMap = null;
    private Map<Long, byte[]> recvQ = null;
    private Epoll poller = null;
    private String defaultMq = null;
    private int millis = -1;
    private int batchSize = -1;
    private int stock = -1;

    public MqReaderIoLoop() throws IOException {
        this(10, 3, 10240, "rishin_in");
    }

    public MqReaderIoLoop(int millisLatency, int batchSize, int stock, String defaultMq) throws IOException {
        //this.millis = 10; // latency in terms ioloop blocking
        //this.batchSize = 3; // maximum messages wait for ioloop
        //this.stock = 10240; // store upto 10240 responses before they are deleted
        this.millis = millisLatency;
        this.batchSize = batchSize;
        this.stock = stock;
        this.mqMap = new ConcurrentHashMap<>();
        this.fdMap = new ConcurrentHashMap<>();
        this.recvQ = Collections.synchronizedMap(new FixedMap<Long, byte[]>(new TreeMap<Long, byte[]>(), this.stock));
        this.poller = new Epoll();

        this.defaultMq = defaultMq;
        this.getOrCreateMq(defaultMq);
    }

    @Bean(name = "MqReaderIoLoop.start")
    @Override
    public void start() {
        executor.execute(this);
    }

    @Override
    public Stream<byte[]> stream() {
        return this.recvQ.values().stream();
    }

    @Override
    public long postJob(String mqName, byte[] data) {
        throw new UnsupportedOperationException("Reader does not support post");
    }

    @Override
    public byte[] getJob(long id) {
        return this.recvQ.remove(id);
    }

    @Override
    public MqIo getOrCreateMq(String mqName) throws IOException {
        MqIo mq = null;
        mq = this.mqMap.get(mqName);
        if (mq == null) {
            mq = new MqIo(mqName, NativeIo.O_RDWR);
            this.mqMap.put(mqName, mq);
            this.fdMap.put(mq.getFd(), mq);
            this.monitorMq(mq);
            log.info(String.format("Reader started monitoring new queue, queue=%s fd=%d instance=%s", mqName, mq.getFd(), mq));
        }
        return mq;
    }

    @Override
    public MqIo getMq(String mqName) {
        return this.mqMap.get(mqName);
    }

    private void monitorMq(MqIo mq) throws IOException {
        // TODO: move this code block to single place instead of recreating every time
        EpollEvent.Flags toMonitorFlags = new EpollEvent.Flags();
        toMonitorFlags.set(EpollEvent.Flag.EPOLLIN);
        //toMonitorFlags.set(EpollEvent.Flag.EPOLLOUT);
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
                log.error("IO error on poller wait", e);
            }
            // exit if interrupted
            if (Thread.interrupted()) {
                log.info("Service stopping");
                break;
            }
            // check if epoll returned a valid state
            if (numEvents < 0) {
                int errno = Native.getLastError();
                log.error("Native call 'epoll_wait' failed, errno=" + errno);
                continue;
            }
            // process events
            for (int i = 0; i < numEvents; i++) {
                EpollEvent event = incomingEvents.getEpollEvent(i);
                if (event.getFlags().isSet(EpollEvent.Flag.EPOLLIN)) {
                    long userData = event.getUserData(); // TODO: hate casting (even when ok)
                    //log.info("userData=" + userData); // TODO ERROR in underlying lib - Why is is this zero?
                    try {
                        // TODO: HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE HORRIBLE
                        // TODO: MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX MUSTFIX
                        // TODO: Only working since we're listening on one queue
                        byte[] data = mqMap.get(this.defaultMq).recv();
                        // byte[] data = fdMap.get((int)userData).recv();
                        MqPacket packet = mapper.readValue(data, MqPacket.class);
                        recvQ.put(packet.id, packet.data);
                    } catch (JsonParseException | JsonMappingException e) {
                        log.error("Failed to deserialise packet", e);
                    } catch (IOException e) {
                        log.error("Message queue receive had IO error", e);
                    }
                } else {
                    // TODO: handle all events
                    log.error("TODO: Could not handle event event=" + event.getFlags());
                }
            }
        }
    }
}
