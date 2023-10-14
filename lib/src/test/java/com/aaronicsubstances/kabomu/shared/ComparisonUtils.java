package com.aaronicsubstances.kabomu.shared;

import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;

public class ComparisonUtils {
    
    public static void compareData(byte[] expectedData, int expectedDataOffset,
            byte[] actualData, int actualDataOffset, int length) {
        byte[] temp1 = new byte[length];
        System.arraycopy(expectedData, expectedDataOffset, temp1, 0, length);
        byte[] temp2 = new byte[length];
        System.arraycopy(actualData, actualDataOffset, temp2, 0, length);
        assertArrayEquals(temp1, temp2);
    }

    public static void compareProcessingOptions(
            QuasiHttpProcessingOptions expected,
            QuasiHttpProcessingOptions actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getMaxResponseBodySize(),
            actual.getMaxResponseBodySize());
        assertEquals(expected.getTimeoutMillis(),
            actual.getTimeoutMillis());
        assertEquals(expected.getExtraConnectivityParams(),
            actual.getExtraConnectivityParams());
        assertEquals(expected.getMaxHeadersSize(),
            actual.getMaxHeadersSize());
    }
}
