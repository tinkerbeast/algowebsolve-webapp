package com.algowebsolve.webapp;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.algowebsolve.webapp.reactivemq.NativeMq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
public class MyController {

    private static final String HELLO_FORMAT = "Hello %s (%d)";
    private final AtomicLong counter = new AtomicLong(); // RISHIN: This is inited once for the entire lifetime
    Logger logger = LoggerFactory.getLogger(MyController.class);
    Map<Integer, MyJediModel> jediMap  = new HashMap<>() {{
        put(1, new MyJediModel("Luke"));
        put(2, new MyJediModel("Yoda"));
    }};

    // TODO: Is this a singleton?
    @Autowired
    MyEpollFlux<String> myFlux;


    @Autowired
    private MyFormatterService formatterService;

    @GetMapping("/hello")
    public String hello(@RequestParam(value="name", defaultValue="World") String name) {
        counter.incrementAndGet();
        return String.format(HELLO_FORMAT, name, counter.longValue());        
    }

    @GetMapping("/hellonew")
    public ResponseEntity<String> hellonew(@RequestParam(value="name", defaultValue="World") String name) {
        logger.info("hellonew called");
        counter.incrementAndGet();
        String response = formatterService.formatString(HELLO_FORMAT, name, counter.longValue());
        return ResponseEntity.ok(response); // RISHIN: Better way of service http responses        
    }

    // FLUX RELATED

    @GetMapping("/jedi")
    public Flux<MyJediModel> jedi() {
        return Flux.fromIterable(jediMap.values());
    }

    @GetMapping("/jedi/{id}")
    public Mono<MyJediModel> jediById(@PathVariable Integer id) {
        return Mono.justOrEmpty(jediMap.get(id));
    }

    @PostMapping("/jedi")
    public ResponseEntity<MyJediModel> hello(@RequestParam(value="id") Integer id, @RequestParam(value="name") String name) {
        MyJediModel m  = new MyJediModel(name);
        jediMap.put(id, m);
        return ResponseEntity.ok(m);
    }

    // MYFLUX
    @GetMapping(value="/myflux/live", produces="text/event-stream")
    public Flux<String> jediLive() {
        return Flux.from(myFlux);
    }

    @GetMapping("/echo/nativemq")
    public ResponseEntity<String> echoMq(@RequestParam(value="mq") String mq, @RequestParam(value="msg") String msg) {
        try (NativeMq nativeMq = new NativeMq(mq, NativeIo.O_RDWR)) {
            logger.info("IN: " + msg);
            byte[] inData = msg.getBytes(StandardCharsets.UTF_8);
            nativeMq.send(inData, 1);
            byte[] outData = nativeMq.recv();
            String out = new String(outData, StandardCharsets.UTF_8);
            logger.info("OUT: " + out);
            return ResponseEntity.ok(out);
        } catch (IOException e ) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()); // TODO: dangerous exposing internat error to external
        }
    }

    // JNA
    @GetMapping("/myjna")
    public ResponseEntity<String> myjna(@RequestParam(value="filename") String name) {
        logger.info("Param to myjna:" + name);
        String content = null;
        try (NativeIo io = new NativeIo(name, NativeIo.O_RDONLY)) {
            content = io.read();
        } catch (IOException e) {
            logger.error(e.toString());
        }

        if (content != null) {
            return ResponseEntity.ok(content);
        } else {
            return null;
        }
    }
}
