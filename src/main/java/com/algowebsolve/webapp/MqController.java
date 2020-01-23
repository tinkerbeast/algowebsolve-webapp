package com.algowebsolve.webapp;

import com.algowebsolve.webapp.nsystem.linux.MqIo;
import com.algowebsolve.webapp.reactivemq.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;



public class MqController {

    @Autowired
    MqReaderIoLoop reader;

    //@Autowired
    //MqWriterIoLoop writer;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqController.class);

    private void sendDumyyPacket(MqIo mq, String msg) throws IOException {
        // TODO: is there shorter code to do this?
        MqPacket packet = new MqPacket();
        packet.id = -1L;
        packet.data = msg.getBytes(StandardCharsets.UTF_8);
        // send the data
        byte[] data = mapper.writeValueAsBytes(packet);
        mq.send(data, MqIo.MSG_PRIORITY_DEFAULT);
    }

    @PostMapping("/mq/reader/{mqname}")
    public ResponseEntity<Void> createReaderMq(@PathVariable String mqname, @RequestParam(value="msg", required=false) String msg) {
        try {
            MqIo mq = reader.getOrCreateMq(mqname);
            if (msg != null) {
                sendDumyyPacket(mq, msg);
            }
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.info("Failed to create or get native mq, mq=" + mqname, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/mq/reader/{mqname}")
    public ResponseEntity<Void> sendReaderMsg(@PathVariable String mqname, @RequestParam(value="msg") String msg) {
        try {
            MqIo mq = reader.getMq(mqname);
            if (mq == null) {
                return ResponseEntity.notFound().build();
            }
            sendDumyyPacket(mq, msg);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.info("Failed to do send operations on native mq, mq=" + mqname, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value="/mq/reader/{mqname}/live", produces="text/event-stream")
    public Flux<String> recvReaderMsg(@PathVariable String mqname) {
        return Flux.fromStream(reader.stream()
                .map((byteArr) -> {
                    return new String(byteArr, StandardCharsets.UTF_8);
                }));
    }

}
