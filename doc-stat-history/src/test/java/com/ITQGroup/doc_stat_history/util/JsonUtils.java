package com.ITQGroup.doc_stat_history.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

    private static final ObjectReader READER = MAPPER.reader();
    private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    public static <T> T convertJsonFromFileToObject(String filePath, Class<T> clazz) throws IOException {
        return MAPPER.readValue(new File(filePath), clazz);
    }

    public static <T> List<T> convertJsonFromFileToList(String filePath, Class<T> clazz) throws IOException {
        JavaType type = MAPPER.getTypeFactory()
                .constructCollectionType(List.class, clazz);

        return MAPPER.readValue(new File(filePath), type);
    }

    public static <T> String convertJsonFromObjectToString(T object) throws IOException {
        return WRITER.writeValueAsString(object);
    }

    public static <T> T convertJsonStringToObject(String json, Class<T> clazz) throws JsonProcessingException {
        return MAPPER.readValue(json, clazz);
    }

    public static <T> String convertListToJson(List<T> list) throws JsonProcessingException {
        return MAPPER.writeValueAsString(list);
    }

    public static String readFile(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }
}