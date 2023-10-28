package com.aaronicsubstances.kabomu.shared;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;

import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;

public class ComparisonUtils {

    public static void compareRequests(
            QuasiHttpRequest expected, QuasiHttpRequest actual,
            byte[] expectedReqBodyBytes) throws IOException {
        if (expected == null || actual == null) {
            assertSame(expected, actual);
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
        if (expected == null || actual == null) {
            assertSame(expected, actual);
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
        if (expected == null || actual == null) {
            assertSame(expected, actual);
            return;
        }
        List<List<String>> expectedExtraction = new ArrayList<>();
        List<String> sortedExpectedKeys = new ArrayList<>(expected.keySet());
        sortedExpectedKeys.sort(null);
        for (String key : sortedExpectedKeys) {
            List<String> row = new ArrayList<>();
            row.add(key);
            row.addAll(expected.get(key));
            expectedExtraction.add(row);
        }
        List<List<String>> actualExtraction = new ArrayList<>();
        List<String> sortedActualKeys = new ArrayList<>(actual.keySet());
        sortedActualKeys.sort(null);
        for (String key : sortedActualKeys) {
            List<String> row = new ArrayList<>();
            row.add(key);
            row.addAll(actual.get(key));
            actualExtraction.add(row);
        }
        assertEquals(expectedExtraction, actualExtraction);
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
