package com.cbxsoftware.rest.util;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public static String toJson(final Object object) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(object);
    }

    public static <T> T parse(final String json, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(json, clazz);
    }

    public static <T> T parse(final byte[] data, final Class<T> clazz) throws IOException {
        return OBJECT_MAPPER.readValue(data, clazz);
    }

    public static <T> T parse(final Object from, final Class<T> clazz) throws Exception {
        return OBJECT_MAPPER.convertValue(from, clazz);
    }

    public static JsonNode parse(final Object from) {
        return OBJECT_MAPPER.valueToTree(from);
    }

    public static JsonNode parse(final byte[] data) throws IOException {
        return OBJECT_MAPPER.readTree(data);
    }

    public static <T> T parse(final InputStream from, final TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(from, typeReference);
    }

    public static <T> T parse(final String from, final TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(from, typeReference);
    }

}
