package com.algowebsolve.webapp;

import com.algowebsolve.webapp.apiv1.Problems;
import com.algowebsolve.webapp.model.TestPrimitives;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;

@SpringBootTest
class WebappApplicationTests { // TODO: rename to echo test
// TODO: convert this whole test to modern web flux
	@Autowired
	private Problems controller;

	private static final ObjectMapper jsonMapper = new ObjectMapper();

	@Test
	void primitives() throws JsonProcessingException, JsonMappingException {
		// fill obj value
		TestPrimitives obj = new TestPrimitives();
		obj.aByte = 13;
		obj.aShort = 0xfff;
		obj.anInt = 12345678;
		obj.aLong = 0L;
		obj.aFloat = 35.5f;
		obj.aDouble = 42.0;
		obj.aBoolean = true;
		obj.aChar = 'r';
		// convert obj to json
		String jsonString = jsonMapper.writeValueAsString(obj);
		JsonNode jsonNode = jsonMapper.readTree(jsonString);
		// request to response
		Mono<JsonNode> response = controller.problemRequest("test-echo-primitives", jsonNode);
		JsonNode respJson = response.block();
		TestPrimitives respObj = jsonMapper.convertValue(respJson, TestPrimitives.class);
		// assertions
		Assertions.assertThat(obj.aByte).isEqualTo(respObj.aByte);
		Assertions.assertThat(obj.aShort).isEqualTo(respObj.aShort);
		Assertions.assertThat(obj.anInt).isEqualTo(respObj.anInt);
		Assertions.assertThat(obj.aLong).isEqualTo(respObj.aLong);
		Assertions.assertThat(obj.aFloat).isEqualTo(respObj.aFloat);
		Assertions.assertThat(obj.aDouble).isEqualTo(respObj.aDouble);
		Assertions.assertThat(obj.aBoolean).isEqualTo(respObj.aBoolean);
		Assertions.assertThat(obj.aChar).isEqualTo(respObj.aChar);
	}

}
