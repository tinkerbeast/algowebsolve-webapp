package com.algowebsolve.webapp;

import com.algowebsolve.webapp.apiv1.Problems;
import com.algowebsolve.webapp.model.TestPrimitiveArrays;
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
		Assertions.assertThat(respJson).isEqualTo(jsonNode);
		/*
		Assertions.assertThat(obj.aShort).isEqualTo(respObj.aShort);
		Assertions.assertThat(obj.anInt).isEqualTo(respObj.anInt);
		Assertions.assertThat(obj.aLong).isEqualTo(respObj.aLong);
		Assertions.assertThat(obj.aFloat).isEqualTo(respObj.aFloat);
		Assertions.assertThat(obj.aDouble).isEqualTo(respObj.aDouble);
		Assertions.assertThat(obj.aBoolean).isEqualTo(respObj.aBoolean);
		Assertions.assertThat(obj.aChar).isEqualTo(respObj.aChar);
		*/
	}

	@Test
	void primitiveArrays() throws JsonProcessingException, JsonMappingException {
		// fill obj value
		TestPrimitiveArrays obj = new TestPrimitiveArrays();
		//obj.aByteArray = new byte[]{13, 0, 1, -1, Byte.MAX_VALUE, Byte.MIN_VALUE};
		obj.aShortArray = new short[]{133, 0, 1, -1, Short.MAX_VALUE, Short.MIN_VALUE};
		obj.anIntArray = new int[]{1333, 0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
		obj.aLongArray = new long[]{13333, 0, 1, -1, Long.MAX_VALUE, Long.MIN_VALUE};
		obj.aFloatArray = new float[]{13.5f, 0, 1f, -1f, Float.MAX_VALUE, Float.MIN_VALUE, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
		obj.aDoubleArray = new double[]{13.55, 0.0, 1.0, -1.0, Double.MAX_VALUE, Double.MIN_VALUE, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
		obj.aBooleanArray = new boolean[]{true, true, false, true};
		obj.aCharArray = "hello world".toCharArray();
		// convert obj to json
		String jsonString = jsonMapper.writeValueAsString(obj);
		JsonNode jsonNode = jsonMapper.readTree(jsonString);
		// request to response
		Mono<JsonNode> response = controller.problemRequest("test-echo-primitivearrays", jsonNode);
		JsonNode respJson = response.block();
		TestPrimitiveArrays respObj = jsonMapper.convertValue(respJson, TestPrimitiveArrays.class);
		// assertions
		Assertions.assertThat(respJson).isEqualTo(jsonNode);
	}

}
