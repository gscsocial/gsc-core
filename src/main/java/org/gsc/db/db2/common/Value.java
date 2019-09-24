package org.gsc.db.db2.common;

import java.util.Arrays;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.gsc.db.common.WrappedByteArray;

@EqualsAndHashCode(exclude = "operator")
public final class Value {

    public byte[] encode() {
        if (data.getBytes() == null) {
            return new byte[]{operator.getValue()};
        }

        byte[] r = new byte[1 + data.getBytes().length];
        r[0] = operator.getValue();
        System.arraycopy(data.getBytes(), 0, r, 1, data.getBytes().length);
        return r;
    }

    public static Value decode(byte[] bytes) {
        Operator operator = Operator.valueOf(bytes[0]);
        byte[] value = null;
        if (bytes.length > 1) {
            value = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Value.of(operator, value);
    }

    public enum Operator {
        CREATE((byte) 0),
        MODIFY((byte) 1),
        DELETE((byte) 2),
        PUT((byte) 3);

        @Getter
        private byte value;

        Operator(byte value) {
            this.value = value;
        }

        static Operator valueOf(byte b) {
            switch (b) {
                case 0:
                    return Operator.CREATE;
                case 1:
                    return Operator.MODIFY;
                case 2:
                    return Operator.DELETE;
                case 3:
                    return Operator.PUT;
                default:
                    return null;
            }
        }
    }

    @Getter
    final private Operator operator;
    final private WrappedByteArray data;

    private Value(Operator operator, WrappedByteArray data) {
        this.operator = operator;
        this.data = data;
    }

    public static Value copyOf(Operator operator, byte[] data) {
        return new Value(operator, WrappedByteArray.copyOf(data));
    }

    public static Value of(Operator operator, byte[] data) {
        return new Value(operator, WrappedByteArray.of(data));
    }

    public byte[] getBytes() {
        byte[] value = data.getBytes();
        if (value == null) {
            return null;
        }

        return Arrays.copyOf(value, value.length);
    }
}
