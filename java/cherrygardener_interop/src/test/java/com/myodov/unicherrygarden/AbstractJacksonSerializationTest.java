package com.myodov.unicherrygarden;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractJacksonSerializationTest {
    protected static String makeJson(Object obj) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        objectMapper.configOverride(BigInteger.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        return objectMapper.writeValueAsString(obj);
    }

    protected static <T> T readJson(String serialized, Class<T> valueType) throws JsonProcessingException {
        return new ObjectMapper().readValue(serialized, valueType);
    }

    protected static <T> void assertJsonDeserialization(Object obj, String serialized, Class<T> valueType) throws JsonProcessingException {
        assertEquals(
                "Serialization works well",
                serialized,
                makeJson(obj)
        );

//        assertNotNull(
//                "Deserialization doesn't fail",
//                readJson(serialized, valueType)
//        );
    }
}
