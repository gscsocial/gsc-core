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

package org.gsc.net.peer.p2p;

import static org.gsc.core.exception.P2pException.TypeEnum.PROTOBUF_ERROR;

import com.google.protobuf.CodedInputStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Field;
import java.util.Arrays;

import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.gsc.utils.Sha256Hash;
import org.gsc.db.Manager;
import org.gsc.core.exception.P2pException;
import org.gsc.net.peer.message.MessageTypes;

public abstract class Message {

    protected static final Logger logger = LoggerFactory.getLogger("Message");

    protected byte[] data;
    protected byte type;
    @Setter
    private static Manager manager;

    public Message() {
    }

    public Message(byte[] packed) {
        this.data = packed;
    }

    public Message(byte type, byte[] packed) {
        this.type = type;
        this.data = packed;
    }

    public ByteBuf getSendData() {
        return Unpooled.wrappedBuffer(ArrayUtils.add(this.getData(), 0, type));
    }

    public Sha256Hash getMessageId() {
        return Sha256Hash.of(getData());
    }

    public byte[] getData() {
        return this.data;
    }

    public MessageTypes getType() {
        return MessageTypes.fromByte(this.type);
    }

    public abstract Class<?> getAnswerMessage();

    @Override
    public String toString() {
        return "type: " + getType() + "\n";
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        Message message = (Message) o;
        return Arrays.equals(data, message.data);
    }

    public static void compareBytes(byte[] src, byte[] dest) throws P2pException {
        if (src.length != dest.length) {
            throw new P2pException(PROTOBUF_ERROR, PROTOBUF_ERROR.getDesc());
        }
    }

    private static final Field field = ReflectionUtils
            .findField(CodedInputStream.class, "explicitDiscardUnknownFields");

    static {
        ReflectionUtils.makeAccessible(field);
    }

    public static CodedInputStream getCodedInputStream(byte[] data) {
        CodedInputStream codedInputStream = CodedInputStream.newInstance(data);
        if (isFilter()) {
            ReflectionUtils.setField(field, codedInputStream, true);
        }
        return codedInputStream;
    }

    public static boolean isFilter() {
        return manager.getDynamicPropertiesStore().getAllowProtoFilterNum() == 1;
    }

}