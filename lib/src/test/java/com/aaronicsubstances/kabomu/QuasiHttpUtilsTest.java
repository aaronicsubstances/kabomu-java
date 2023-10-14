package com.aaronicsubstances.kabomu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.shared.ComparisonUtils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class QuasiHttpUtilsTest {

    @Test
    public void testClassConstants() {
        assertEquals("CONNECT", QuasiHttpUtils.METHOD_CONNECT);
        assertEquals("DELETE", QuasiHttpUtils.METHOD_DELETE);
        assertEquals("GET", QuasiHttpUtils.METHOD_GET);
        assertEquals("HEAD", QuasiHttpUtils.METHOD_HEAD);
        assertEquals("OPTIONS", QuasiHttpUtils.METHOD_OPTIONS);
        assertEquals("PATCH", QuasiHttpUtils.METHOD_PATCH);
        assertEquals("POST", QuasiHttpUtils.METHOD_POST);
        assertEquals("PUT", QuasiHttpUtils.METHOD_PUT);
        assertEquals("TRACE", QuasiHttpUtils.METHOD_TRACE);

        assertEquals(200, QuasiHttpUtils.STATUS_CODE_OK);
        assertEquals(500, QuasiHttpUtils.STATUS_CODE_SERVER_ERROR);
        assertEquals(400, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_BAD_REQUEST);
        assertEquals(401, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_UNAUTHORIZED);
        assertEquals(403, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_FORBIDDEN);
        assertEquals(404, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_NOT_FOUND);
        assertEquals(405, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_METHOD_NOT_ALLOWED);
        assertEquals(413, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_PAYLOAD_TOO_LARGE);
        assertEquals(414, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_URI_TOO_LONG);
        assertEquals(415, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE);
        assertEquals(422, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_UNPROCESSABLE_ENTITY);
        assertEquals(429, QuasiHttpUtils.STATUS_CODE_CLIENT_ERROR_TOO_MANY_REQUESTS);
    }

    @Test
    void testMergeProcessingOptions1() {
        QuasiHttpProcessingOptions preferred = null;
        QuasiHttpProcessingOptions fallback = null;
        QuasiHttpProcessingOptions actual = QuasiHttpUtils.mergeProcessingOptions(
            preferred, fallback);
        assertNull(actual);
    }

    @Test
    void testMergeProcessingOptions2() {
        QuasiHttpProcessingOptions preferred = new DefaultQuasiHttpProcessingOptions();
        preferred.setExtraConnectivityParams(new HashMap<>());
        preferred.getExtraConnectivityParams().put("scheme", "tht");
        preferred.setMaxHeadersSize(10);
        preferred.setMaxResponseBodySize(-1);
        preferred.setTimeoutMillis(0);
        QuasiHttpProcessingOptions fallback = new DefaultQuasiHttpProcessingOptions();
        fallback.setExtraConnectivityParams(new HashMap<>());
        fallback.getExtraConnectivityParams().put("scheme", "htt");
        fallback.getExtraConnectivityParams().put("two", 2);
        fallback.setMaxHeadersSize(30);
        fallback.setMaxResponseBodySize(40);
        fallback.setTimeoutMillis(-1);
        QuasiHttpProcessingOptions actual = QuasiHttpUtils.mergeProcessingOptions(
            preferred, fallback);
        QuasiHttpProcessingOptions expected = new DefaultQuasiHttpProcessingOptions();
        expected.setExtraConnectivityParams(new HashMap<>());
        expected.getExtraConnectivityParams().put("scheme", "tht");
        expected.getExtraConnectivityParams().put("two", 2);
        expected.setMaxHeadersSize(10);
        expected.setMaxResponseBodySize(-1);
        expected.setTimeoutMillis(-1);
        ComparisonUtils.compareProcessingOptions(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("createTestDetermineEffectiveNonZeroIntegerOptionData")
    void testDetermineEffectiveNonZeroIntegerOption(
            Integer preferred, Integer fallback1, int defaultValue, int expected) {
        int actual = QuasiHttpUtils.determineEffectiveNonZeroIntegerOption(
            preferred, fallback1, defaultValue);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDetermineEffectiveNonZeroIntegerOptionData() {
        return Stream.of(
            Arguments.of(1, null, 20, 1),
            Arguments.of(5, 3, 11, 5),
            Arguments.of(-15, 3, -1, -15),
            Arguments.of(null, 3, -1, 3),
            Arguments.of(null, -3, -1, -3),
            Arguments.of(null, null, 2, 2),
            Arguments.of(null, null, -8, -8),
            Arguments.of(null, null, 0, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("createTestDetermineEffectivePositiveIntegerOptionData")
    void testDetermineEffectivePositiveIntegerOption(
            Integer preferred, Integer fallback1, int defaultValue, int expected) {
        int actual = QuasiHttpUtils.determineEffectivePositiveIntegerOption(
            preferred, fallback1, defaultValue);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDetermineEffectivePositiveIntegerOptionData() {
        return Stream.of(
            Arguments.of(null, 1, 30, 1),
            Arguments.of(5, 3, 11, 5),
            Arguments.of(null, 3, -1, 3),
            Arguments.of(null, null, 2, 2),
            Arguments.of(null, null, -8, -8),
            Arguments.of(null, null, 0, 0)
        );
    }

    @ParameterizedTest
    @MethodSource("createTestDetermineEffectiveOptionsData")
    void testDetermineEffectiveOptions(
            Map<String, Object> preferred,
            Map<String, Object> fallback,
            Map<String, Object> expected) {
        Map<String, Object> actual = QuasiHttpUtils.determineEffectiveOptions(
            preferred, fallback);
        assertEquals(expected, actual);
    }

    static List<Arguments> createTestDetermineEffectiveOptionsData() {
        List<Arguments> testData = new ArrayList<>();

        Map<String, Object> preferred = null;
        Map<String, Object> fallback = null;
        Map<String, Object> expected = new HashMap<>();
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = new HashMap<>();
        fallback = new HashMap<>();
        expected = new HashMap<>();
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = new HashMap<>();
        preferred.put("a", 2);
        preferred.put("b", 3);
        fallback = null;
        expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("b", 3);
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = null;
        fallback = new HashMap<>();
        fallback.put("a", 2);
        fallback.put("b", 3);
        expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("b", 3);
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = new HashMap<>();
        preferred.put("a", 2);
        preferred.put("b", 3);
        fallback = new HashMap<>();
        fallback.put("c", 4);
        fallback.put("d", 3);
        expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("b", 3);
        expected.put("c", 4);
        expected.put("d", 3);
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = new HashMap<>();
        preferred.put("a", 2);
        preferred.put("b", 3);
        fallback = new HashMap<>();
        fallback.put("a", 4);
        fallback.put("d", 3);
        expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("b", 3);
        expected.put("d", 3);
        testData.add(Arguments.of(preferred, fallback, expected));

        preferred = new HashMap<>();
        preferred.put("a", 2);
        fallback = new HashMap<>();
        fallback.put("a", 4);
        fallback.put("d", 3);
        expected = new HashMap<>();
        expected.put("a", 2);
        expected.put("d", 3);
        testData.add(Arguments.of(preferred, fallback, expected));
        
        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestDetermineEffectiveBooleanOptionData")
    void testDetermineEffectiveBooleanOption(
            Boolean preferred, Boolean fallback1, boolean defaultValue, boolean expected) {
        boolean actual = QuasiHttpUtils.determineEffectiveBooleanOption(
            preferred, fallback1, defaultValue);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDetermineEffectiveBooleanOptionData() {
        return Stream.of(
            Arguments.of(true, null, true, true),
            Arguments.of(false, true, true, false),
            Arguments.of(null, false, true, false),
            Arguments.of(null, true, false, true),
            Arguments.of(null, true, true, true),
            Arguments.of(null, null, true, true),
            Arguments.of(null, null, false, false),
            Arguments.of(true, true, false, true),
            Arguments.of(true, true, true, true),
            Arguments.of(false, false, false, false)
        );
    }
}
