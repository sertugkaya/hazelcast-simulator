package com.hazelcast.simulator.tests.network;

import static java.lang.String.format;

public class PayloadUtils {

    public static final boolean COMPRESS_HEX_OUTPUT = true;

    private PayloadUtils() {
    }

    public static String toHexString(byte[] bytes) {
        if (bytes.length == 0) {
            return "";
        }

        StringBuffer sb = new StringBuffer();

        byte prev = bytes[0];
        int count = 1;
        for (int k = 1; k < bytes.length; k++) {
            byte current = bytes[k];
            if (COMPRESS_HEX_OUTPUT && prev == current) {
                count++;
            } else {
                if (count == 1) {
                    sb.append(format("%02X", prev));
                } else {
                    sb.append(format("%02X(%d)", prev, count));
                }
                count = 1;
            }

            prev = current;
        }

        if (count == 1) {
            sb.append(format("%02X", prev));
        } else {
            sb.append(format("%02X(%d)", prev, count));
        }

        return sb.toString();
    }

    public static byte[] makePayload(int payloadSize) {
        if (payloadSize <= 0) {
            return null;
        }

        byte[] payload = new byte[payloadSize];

        // put a well known head and tail on the payload; for debugging.
        if (payload.length >= 6 + 8) {
            addHeadTailMarkers(payload);
        }

        return payload;
    }

    public static void addHeadTailMarkers(byte[] payload) {
        payload[0] = 0xA;
        payload[1] = 0xB;
        payload[2] = 0xC;

        int length = payload.length;
        payload[length - 3] = 0xC;
        payload[length - 2] = 0xB;
        payload[length - 1] = 0xA;
    }

    public static void checkHeadTailMarkers(byte[] payload) {
        check(payload, 0, 0XA);
        check(payload, 1, 0XB);
        check(payload, 2, 0XC);

        int length = payload.length;
        check(payload, length - 3, 0XC);
        check(payload, length - 2, 0XB);
        check(payload, length - 1, 0XA);
    }

    public static void check(byte[] payload, int index, int value) {
        byte found = payload[index];
        if (found != value) {
            throw new IllegalStateException(format(
                    "invalid byte at index:%d, found:%02X, expected:%02X payload=", index, found, value)
                    + toHexString(payload));
        }
    }

    public static long readLong(byte[] bytes, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (bytes[i + offset] & 0xFF);
        }
        return result;
    }

    public static void writeLong(byte[] bytes, int offset, long value) {
        for (int i = 7; i >= 0; i--) {
            bytes[i + offset] = (byte) (value & 0xFF);
            value >>= 8;
        }
    }

    public static void main(String[] args) {
        byte[] bytes = new byte[50];

        long sequenceId = Long.MAX_VALUE;

        addHeadTailMarkers(bytes);
        writeLong(bytes, 3, sequenceId);
        writeLong(bytes, bytes.length - (8 + 3), sequenceId);
        System.out.println(toHexString(bytes));

        checkHeadTailMarkers(bytes);

    }
}