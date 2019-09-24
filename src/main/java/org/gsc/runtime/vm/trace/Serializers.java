/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.runtime.vm.trace;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.gsc.runtime.vm.DataWord;
import org.gsc.runtime.vm.OpCode;

@Slf4j(topic = "VM")
public final class Serializers {

    public static class DataWordSerializer extends JsonSerializer<DataWord> {

        @Override
        public void serialize(DataWord cpu, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeString(cpu.value().toString());
        }
    }

    public static class ByteArraySerializer extends JsonSerializer<byte[]> {

        @Override
        public void serialize(byte[] memory, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeString(Hex.toHexString(memory));
        }
    }

    public static class OpCodeSerializer extends JsonSerializer<Byte> {

        @Override
        public void serialize(Byte op, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeString(OpCode.code(op).name());
        }
    }


    public static String serializeFieldsOnly(Object value, boolean pretty) {
        try {
            ObjectMapper mapper = createMapper(pretty);
            mapper.setVisibilityChecker(fieldsOnlyVisibilityChecker(mapper));

            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            logger.error("JSON serialization error: ", e);
            return "{}";
        }
    }

    private static VisibilityChecker<?> fieldsOnlyVisibilityChecker(ObjectMapper mapper) {
        return mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
    }

    public static ObjectMapper createMapper(boolean pretty) {
        ObjectMapper mapper = new ObjectMapper();
        if (pretty) {
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        }
        return mapper;
    }
}
