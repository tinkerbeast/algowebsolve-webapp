package com.algowebsolve.webapp;

import com.algowebsolve.webapp.reactivemq.NativeMq;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;


@RestController
public class MyControllerFlux {

    Logger logger = LoggerFactory.getLogger(MyControllerFlux.class);

    // TODO: Is this a singleton?
    //@Autowired
    //MyEpollFlux<String> myFlux;


    @Autowired
    private MyFormatterService formatterService;

    @GetMapping("/flux-singlethread-multiconsume")
    public String fluxA(@RequestParam(value="name") String name) {
        System.out.println("Request thread: " + Thread.currentThread().getName());
        Flux<Integer> myFlux = Flux.range(1, 10);
        IntStream.range(1, 5).forEach(value -> {
            myFlux.subscribe(integer -> {
                System.out.println(String.format("Subscription:" + value
                        + " Flux-stream-val:" + integer
                        + " Thread-id:" + Thread.currentThread().getName()
                ));
            });
        });
        // Learnings:
        // 1. A copy of the Flux object is created for the each functor
        // 2. Flux.subscribe calls the Consumer function multiple times
        return "ok";
    }

    @GetMapping("/flux-requestthread-clientconsume")
    public Flux<String> fluxB(@RequestParam(value="name") String name) {
        System.out.println("Request thread: " + Thread.currentThread().getName());
        Flux<String> myFlux = Flux.range(1, 10).map(intVal -> {
            return String.format("Subscription:CLIENT Flux-stream-val:%d Thread-id:%s %n",
                    intVal, Thread.currentThread().getName());
        });
        return myFlux;
        // Learnings:
        // 3. `subsribe()` happens from Spring framework and the function is also spring internal
        // 4. Request thread and subscribe thread is same by default
    }

    @GetMapping("/flux-deferthread-clientconsume")
    public Flux<String> fluxC(@RequestParam(value="name") String name) {
        System.out.println("Request thread: " + Thread.currentThread().getName());
        Stream<Integer> stream = IntStream.range(1, 100).boxed();
        return  Flux.defer(() -> {
            return Flux.fromStream(stream).map(intVal -> {
                    return String.format("Subscription:CLIENT Flux-stream-val:%d Thread-id:%s %n",
                    intVal, Thread.currentThread().getName());
            });
        });

        // Learnings:
        // 5. ???? Is the request and defer thread the same ????
        // 6. The subription on the deffered thread happens in one continuous thread
        // 7. Objects are copied to the deffered thread
    }

    @GetMapping("/flux-simpledefer-clientconsume")
    public Flux<String> fluxD(@RequestParam(value="name") String name) {
        System.out.println("Request thread: " + Thread.currentThread().getName());
        Flux<String> myFlux = Flux.range(1, 10).map(intVal -> {
            return String.format("Subscription:CLIENT Flux-stream-val:%d Thread-id:%s %n",
                    intVal, Thread.currentThread().getName());
        });
        return  Flux.defer(() -> {
                return myFlux;
            });

        // Learnings:
        // 8. ??? Flux transformation happen in requst or defered thread ????
    }

    @GetMapping("/flux-doubledefer-clientconsume")
    public Flux<String> fluxE(@RequestParam(value="name") String name) {
        System.out.println("Request thread: " + Thread.currentThread().getName());
        Flux<String> myFlux1 = Flux.range(1, 5).map(intVal -> {
            return String.format("Subscription:CLIENT Flux-stream-val:%d Thread-id:%s %n",
                    intVal, Thread.currentThread().getName());
        });
        Flux<String> myFlux2 = Flux.range(5, 10).map(intVal -> {
            return String.format("Subscription:CLIENT Flux-stream-val:%d Thread-id:%s %n",
                    intVal, Thread.currentThread().getName());
        });
        return  Flux.defer(() -> {
            Random rand = new Random();
            if (rand.nextDouble() < 0.5) {
                return myFlux1;
            } else {
                return Flux.defer(() -> {
                    return myFlux2;
                });
            }
        });

        // Learnings:
        // 9. You can double defer
    }



}
