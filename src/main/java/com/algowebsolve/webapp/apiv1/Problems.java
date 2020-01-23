package com.algowebsolve.webapp.apiv1;

import com.algowebsolve.webapp.model.BackpackProblem;
import com.algowebsolve.webapp.model.ProblemRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.json.JsonParseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class Problems {
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Problems.class);
    private static final Map<String, JavaType> modelMap = new HashMap<>();
    static {
        modelMap.put("dynamic-backpack1d", jsonMapper.getTypeFactory().constructType(BackpackProblem.class));
    }

    private static final String API_PROBLEMS = "/v1/problems";

    // DEVNOTE: Why use PUT?
    // 1. If we use POST, the job-id created is for this specific host. There is not guarantee that a subsequent request
    //    will come to this host. This will involve some distributed arbitration which I want to avoid.
    // 2. GET is out of the question since this call mutates state.
    // TODO: For a general case of producing jobId, how to make it session safe (eg. a hacker won't use DELETE on someone elses jobid)?
    @PutMapping(path=API_PROBLEMS, consumes="application/json", produces="application/json")
    @ResponseBody
    public Mono<String> fluxB(@RequestBody ProblemRequest problemRequest) {
        try {
            // Parse model
            if (problemRequest.type == null || problemRequest.details == null) { // TODO: Why is the conversion allowing null?
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Json type was invalid"));
            }
            JavaType modelType = modelMap.get(problemRequest.type);
            Object model = jsonMapper.treeToValue(problemRequest.details, modelType.getRawClass());
            byte[] data = jsonMapper.writeValueAsBytes(model);
            // Send to message-queue




            return Mono.just("abd");
        } catch (JsonParseException | JsonProcessingException e) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Json format was invalid"));
        }
        // TODO: What is the preferred way to handle general exceptions?
        //catch (Exception e) {
        //    log.error(API_PROBLEMS, e);
        //    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, API_PROBLEMS));
        //}
    }

}
