package com.aaronicsubstances.kabomu;

import java.nio.charset.StandardCharsets;

public class MiscUtilsInternal {
    public static void serializeInt32BE(int v, byte[] dest, int offset) {
        dest[offset] = (byte) (v >> 24);
        dest[offset + 1] = (byte) (v >> 16);
        dest[offset + 2] = (byte) (v >> 8);
        dest[offset + 3] = (byte)v;
    }

    public static int deserializeInt32BE(byte[] src, int offset) {
        return ((src[offset] & 0xFF) << 24) | 
            ((src[offset + 1] & 0xFF) << 16) | 
            ((src[offset + 2] & 0xFF) << 8) | 
            (src[offset + 3] & 0xFF);
    }

    public static long parseInt48(String input) {
        long n = Long.parseLong(input != null ? input.trim() : null);
        if (n < -140_737_488_355_328L || n > 140_737_488_355_327L) {
            throw new NumberFormatException("invalid 48-bit integer: " + input);
        }
        return n;
    }

    public static int parseInt32(String input) {
        return Integer.parseInt(input != null ? input.trim() : null);
    }

    public static byte[] stringToBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] data) {
        return new String(data, StandardCharsets.UTF_8);
    }

    public static String bytesToString(byte[] data, int offset, int length) {
        return new String(data, offset, length, StandardCharsets.UTF_8);
    }
}
