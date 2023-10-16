package com.aaronicsubstances.kabomu.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;

public class ComparisonUtils {

    public static void compareRequests(
            QuasiHttpRequest expected, QuasiHttpRequest actual,
            byte[] expectedReqBodyBytes) throws IOException {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.getHttpMethod(), actual.getHttpMethod());
        assertEquals(expected.getHttpVersion(), actual.getHttpVersion());
        assertEquals(expected.getTarget(), actual.getTarget());
        assertEquals(expected.getContentLength(), actual.getContentLength());
        compareHeaders(expected.getHeaders(), actual.getHeaders());
        compareBodies(actual.getBody(), expectedReqBodyBytes);
    }

    public static void compareResponses(
            QuasiHttpResponse expected, QuasiHttpResponse actual,
            byte[] expectedResBodyBytes) throws IOException {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        assertEquals(expected.getStatusCode(), actual.getStatusCode());
        assertEquals(expected.getHttpVersion(), actual.getHttpVersion());
        assertEquals(expected.getHttpStatusMessage(), actual.getHttpStatusMessage());
        assertEquals(expected.getContentLength(), actual.getContentLength());
        compareHeaders(expected.getHeaders(), actual.getHeaders());
        compareBodies(actual.getBody(), expectedResBodyBytes);
    }

    private static void compareBodies(
            InputStream actual, byte[] expectedBodyBytes)
            throws IOException {
        if (expectedBodyBytes == null) {
            assertNull(actual);
            return;
        }
        assertNotNull(actual);
        byte[] actualBodyBytes = IOUtils.toByteArray(actual);
        assertArrayEquals(expectedBodyBytes, actualBodyBytes);
    }

    public static void compareHeaders(
            Map<String, List<String>> expected,
            Map<String, List<String>> actual) {
        TreeMap<String, List<String>> sortedExpected =
            new TreeMap<>();
        if (expected != null) {
            for (Map.Entry<String, List<String>> entry : expected.entrySet()) {
                List<String> value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    sortedExpected.put(entry.getKey(), value);
                }
            }
        }
        TreeMap<String, List<String>> sortedActual =
            new TreeMap<>();
        if (actual != null) {
            for (Map.Entry<String, List<String>> entry : actual.entrySet()) {
                List<String> value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    sortedActual.put(entry.getKey(), value);
                }
            }
        }
        assertEquals(sortedExpected, sortedActual);
    }

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

    public static <T> T newInstance(T instance, Consumer<T> initializer) {
        initializer.accept(instance);
        return instance;
    }

    public static byte[] concatBuffers(byte[]... buffers) {
        int resultLength = Arrays.stream(buffers)
            .mapToInt(b -> b.length)
            .sum();
        byte[] result = new byte[resultLength];
        int offset = 0;
        for (byte[] buffer : buffers) {
            System.arraycopy(buffer, 0, result, offset, buffer.length);
            offset += buffer.length;
        }
        return result;
    }
}
