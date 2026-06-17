package com.cintara.workflow.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpaConverters {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Converter
    public static class StepDefinitionsConverter implements AttributeConverter<List<StepDefinition>, String> {
        @Override
        public String convertToDatabaseColumn(List<StepDefinition> attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting List<StepDefinition> to JSON", e);
            }
        }

        @Override
        public List<StepDefinition> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<StepDefinition>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to List<StepDefinition>", e);
            }
        }
    }

    @Converter
    public static class StepExecutionsConverter implements AttributeConverter<List<StepExecution>, String> {
        @Override
        public String convertToDatabaseColumn(List<StepExecution> attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting List<StepExecution> to JSON", e);
            }
        }

        @Override
        public List<StepExecution> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return new ArrayList<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<List<StepExecution>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to List<StepExecution>", e);
            }
        }
    }

    @Converter
    public static class MapConverter implements AttributeConverter<Map<String, Object>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, Object> attribute) {
            if (attribute == null) {
                return null;
            }
            try {
                return objectMapper.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error converting Map<String, Object> to JSON", e);
            }
        }

        @Override
        public Map<String, Object> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) {
                return new HashMap<>();
            }
            try {
                return objectMapper.readValue(dbData, new TypeReference<Map<String, Object>>() {});
            } catch (IOException e) {
                throw new IllegalArgumentException("Error converting JSON to Map<String, Object>", e);
            }
        }
    }
}
