package com.algowebsolve.webapp;

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


@RestController
public class MqController {

    @Autowired
    MqReaderIoLoop reader;

    //@Autowired
    //MqWriterIoLoop writer;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqController.class);

    private void sendDumyyPacket(NativeMq mq, String msg) throws IOException {
        // TODO: is there shorter code to do this?
        MqPacket packet = new MqPacket();
        packet.setId(-1L);
        packet.setData(msg.getBytes(StandardCharsets.UTF_8));
        // send the data
        byte[] data = mapper.writeValueAsBytes(packet);
        mq.send(data, NativeMq.MSG_PRIORITY_DEFAULT);
    }

    @PostMapping("/mq/reader/{mqname}")
    public ResponseEntity<Void> createReaderMq(@PathVariable String mqname, @RequestParam(value="msg", required=false) String msg) {
        try {
            NativeMq mq = reader.getOrCreateMq(mqname);
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
            NativeMq mq = reader.getMq(mqname);
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
