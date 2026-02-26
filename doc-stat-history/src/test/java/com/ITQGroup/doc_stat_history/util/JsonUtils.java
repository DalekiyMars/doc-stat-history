package com.ITQGroup.doc_stat_history.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonUtils {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);

    public static <T> T convertJsonFromFileToObject(String filePath, Class<T> clazz) throws IOException {
        return MAPPER.readValue(new File(filePath), clazz);
    }

    public static <T> List<T> convertJsonFromFileToList(String filePath, Class<T> clazz) throws IOException {
        JavaType type = MAPPER.getTypeFactory()
                .constructCollectionType(List.class, clazz);

        return MAPPER.readValue(new File(filePath), type);
    }
}