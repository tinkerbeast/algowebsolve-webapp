package com.algowebsolve.webapp;

import com.algowebsolve.webapp.reactivemq.MqIoLoop;

import com.algowebsolve.webapp.reactivemq.MqPacket;
import com.algowebsolve.webapp.reactivemq.MqPacketIdFactory;
import com.algowebsolve.webapp.reactivemq.NativeMq;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@RestController
public class MqController {

    @Autowired
    MqIoLoop mqIoLoop;

    @Autowired
    MqPacketIdFactory packetIdFactory;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MqController.class);

    @PostMapping("/readermq/{mqname}")
    public ResponseEntity<Void> createReaderMq(@PathVariable String mqname) {
        try {
            mqIoLoop.getOrCreateMq(mqname);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.info("Failed to create or get native mq, mq=" + mqname, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/readermq/{mqname}")
    public ResponseEntity<Void> sendReaderMsg(@PathVariable String mqname, @RequestParam(value="msg") String msg) {
        try {
            NativeMq mq = mqIoLoop.getOrCreateMq(mqname);
            // TODO: is there shorter code to do this?
            MqPacket packet = new MqPacket();
            packet.setId(packetIdFactory.produceId());
            packet.setData(msg.getBytes(StandardCharsets.UTF_8));
            // TODO: so many levels of conversion cant be good. Use sendfile?
            //
            byte[] data = mapper.writeValueAsBytes(packet);
            mq.send(data, NativeMq.MSG_PRIORITY_DEFAULT);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.info("Failed to do send operations on native mq, mq=" + mqname, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/readermq/{mqname}")
    public Flux<String> recvReaderMsg(@PathVariable String mqname) {
        WebClient webClient = null;
        webClient.post().uri("").exchange().fl
        return Flux.fromStream(mqIoLoop.stream()
                .map((byteArr) -> {
                    return new String(byteArr, StandardCharsets.UTF_8);
                }));
    }
}
