package com.algowebsolve.tool;


import com.algowebsolve.webapp.apiv1.Problems;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@SpringBootApplication
public class ModelGenrator implements CommandLineRunner {

    private static final ObjectMapper jsonMapper = new ObjectMapper();


    public static void main(String[] args) {
        SpringApplication.run(ModelGenrator.class, args);
    }

    // TODO TODO TODO TODO TODO TODO TODO TODO TODO TODO
    // TODO: overall bad code written
    @Override
    public void run(String... args) throws Exception {
        String directory = "/home/rishin/workspace/algowebsolve-processor/src/web.api/"; // TODO: Sooooo baaddd

        System.out.println("Started cli application.");
        for(String param: args) {
            System.out.println(param);
        }
        System.out.println("<<args");

        Problems problemsApi = new Problems();
        problemsApi.problemsInit();

        List<JavaType> models = problemsApi.getModels();
        JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(jsonMapper);
        for(JavaType type: models) {
            String canonicalName = type.getRawClass().getCanonicalName();
            System.out.println("Generating schema for " + canonicalName);
            String fileName = directory + canonicalName.replace('.', '_') + ".schema.json";
            try (FileWriter out = new FileWriter(fileName)) {
                JsonSchema schema = schemaGen.generateSchema(type);
                jsonMapper.writerWithDefaultPrettyPrinter().writeValue(out, schema); //.writeValueAsString(schema);
                //System.out.println(schemaString);
            } catch (IOException e) {
                System.err.println("Failed to generate: " + canonicalName);
            }
        }
        System.out.println("DONE cli application.");
    }
}
