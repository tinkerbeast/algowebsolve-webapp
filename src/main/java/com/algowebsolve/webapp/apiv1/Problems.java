package com.algowebsolve.webapp.apiv1;

import com.algowebsolve.webapp.MqJobService;
import com.algowebsolve.webapp.model.BackpackProblem;
import com.algowebsolve.webapp.model.BackpackSolution;
import com.algowebsolve.webapp.model.ProblemRequest;
import com.algowebsolve.webapp.reactivemq.SimpleMqIoLoop;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
public class Problems {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Problems.class);
    private static final Map<String, JavaType> srcModelMap = new HashMap<>();
    static {
        srcModelMap.put("dynamic-backpack1d", jsonMapper.getTypeFactory().constructType(BackpackProblem.class));
    }
    private static final Map<String, JavaType> dstModelMap = new HashMap<>();
    static {
        dstModelMap.put("dynamic-backpack1d", jsonMapper.getTypeFactory().constructType(BackpackSolution.class));
    }

    private static final String API_PROBLEMS = "/v1/problems";
    private static final long TIMEOUT_DEFAULT_MS = 1000;
    private static final long RETRY_INTERVAL_MS = 500;

    @Autowired
    SimpleMqIoLoop jobService;


    // DEVNOTE: Why use PUT?
    // 1. If we use POST, the job-id created is for this specific host. There is not guarantee that a subsequent request
    //    will come to this host. This will involve some distributed arbitration which I want to avoid.
    // 2. GET is out of the question since this call mutates state.
    // TODO: For a general case of producing jobId, how to make it session safe (eg. a hacker won't use DELETE on someone elses jobid)?
    @PutMapping(path=API_PROBLEMS, consumes="application/json", produces="application/json")
    @ResponseBody
    public Mono<JsonNode> fluxB(@RequestBody ProblemRequest problemRequest) {
        // Parse input and start job
        final byte[] data;
        final JavaType dstModelType;
        final long jobId;
        try {
            // parse model
            if (problemRequest.type == null || problemRequest.details == null) { // TODO: Why is the conversion allowing null?
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem type or detail was null"));
            }
            JavaType modelType = srcModelMap.get(problemRequest.type);
            dstModelType = dstModelMap.get(problemRequest.type);
            if (modelType == null || dstModelType == null) {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem type is invalid"));
            }
            Object model = jsonMapper.treeToValue(problemRequest.details, modelType.getRawClass()); // TODO: There has to be a better way to validate
            // start job
            jobId = jobService.addJob(problemRequest);
            if (jobId == -1L) {
                String errStr = "Problem input queue is overloaded";
                log.error(errStr);
                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errStr));
            }
        } catch (JsonParseException | JsonProcessingException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Json format was invalid"));
        } catch (Exception e) { // TODO: What is the preferred way to handle general exceptions?
            log.error(API_PROBLEMS, e);
            return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, API_PROBLEMS));
        }

        // Get job result
        return Flux.interval(Duration.ofMillis(RETRY_INTERVAL_MS)) // TODO: This should hit performance in a really bad way
                // TODO: Add another filter before this to check for queue overload (maybe with jobService.isStarted)
                .filter(retryCount -> {
                    if (jobService.isDone(jobId)) {
                        return true;
                    } else if((retryCount + 1) * RETRY_INTERVAL_MS >= TIMEOUT_DEFAULT_MS) { // retry timeout exceeded
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request took too long to process");
                    } else {
                        return false;
                    }
                })
                .map(retryCount -> {
                    try {
                        ProblemRequest resultData = jobService.getResult(jobId);
                        Object dstModel = jsonMapper.treeToValue(resultData.details, dstModelType.getRawClass()); // TODO: There has to be a non exception throwing way of validating the result
                        return resultData.details;
                    } catch (JsonParseException | JsonProcessingException e) {
                        String errStr = "Failed to parse job results";
                        log.error(errStr, e);
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errStr);
                    }
                })
                .next();
    }

}
