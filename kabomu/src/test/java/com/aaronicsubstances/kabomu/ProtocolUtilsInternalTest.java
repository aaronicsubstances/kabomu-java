package com.aaronicsubstances.kabomu;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler.DefaultTimeoutResult;
import com.aaronicsubstances.kabomu.exceptions.QuasiHttpException;
import com.aaronicsubstances.kabomu.shared.ComparisonUtils;

public class ProtocolUtilsInternalTest {
    
    @Test
    void testRunTimeoutScheduler1() throws Throwable {
        QuasiHttpResponse expected = new DefaultQuasiHttpResponse();
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            QuasiHttpResponse result = f.call();
            return new DefaultTimeoutResult(false, result, null);
        };
        QuasiHttpResponse actual = ProtocolUtilsInternal.runTimeoutScheduler(
            instance, true, proc);
        assertSame(expected, actual);
    }
    
    @Test
    void testRunTimeoutScheduler2() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            QuasiHttpResponse result = f.call();
            return new DefaultTimeoutResult(false, result, null);
        };
        QuasiHttpResponse actual = ProtocolUtilsInternal.runTimeoutScheduler(
            instance, false, proc);
        assertSame(expected, actual);
    }
    
    @Test
    void testRunTimeoutScheduler3() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            return null;
        };
        QuasiHttpResponse actual = ProtocolUtilsInternal.runTimeoutScheduler(
            instance, false, proc);
        assertNull(actual);
    }
    
    @Test
    void testRunTimeoutScheduler4() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            return null;
        };
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.runTimeoutScheduler(
                instance, true, proc);
        });
        assertEquals("no response from timeout scheduler", actualEx.getMessage());
        assertEquals(QuasiHttpException.REASON_CODE_GENERAL, actualEx.getReasonCode());
    }
    
    @Test
    void testRunTimeoutScheduler5() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            return new DefaultTimeoutResult(true, null, null);
        };
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.runTimeoutScheduler(
                instance, true, proc);
        });
        assertEquals("send timeout", actualEx.getMessage());
        assertEquals(QuasiHttpException.REASON_CODE_TIMEOUT, actualEx.getReasonCode());
    }
    
    @Test
    void testRunTimeoutScheduler6() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            return new DefaultTimeoutResult(true, null, null);
        };
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.runTimeoutScheduler(
                instance, false, proc);
        });
        assertEquals("receive timeout", actualEx.getMessage());
        assertEquals(QuasiHttpException.REASON_CODE_TIMEOUT, actualEx.getReasonCode());
    }
    
    @Test
    void testRunTimeoutScheduler7() throws Throwable {
        QuasiHttpResponse expected = null;
        Callable<QuasiHttpResponse> proc = () ->
            expected;
        CustomTimeoutScheduler instance = f -> {
            return new DefaultTimeoutResult(true, null,
                new IllegalArgumentException("risk"));
        };
        IllegalArgumentException actualEx = assertThrowsExactly(IllegalArgumentException.class, () -> {
            ProtocolUtilsInternal.runTimeoutScheduler(
                instance, false, proc);
        });
        assertEquals("risk", actualEx.getMessage());
    }

    @ParameterizedTest
    @MethodSource("createTestContainsOnlyPrintableAsciiCharsData")
    void testContainsOnlyPrintableAsciiChars(String v,
            boolean allowSpace, boolean expected) {
        boolean actual = ProtocolUtilsInternal.containsOnlyPrintableAsciiChars(
            v, allowSpace);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestContainsOnlyPrintableAsciiCharsData() {
        return Stream.of(
            Arguments.of("x.n", false, true),
            Arguments.of("x\n", false, false),
            Arguments.of("yd\u00c7ea", true, false),
            Arguments.of("x m", true, true),
            Arguments.of("x m", false, false),
            Arguments.of("x-yio", true, true),
            Arguments.of("x-yio", false, true),
            Arguments.of("x", true, true),
            Arguments.of("x", false, true),
            Arguments.of(" !@#$%^&*()_+=-{}[]|\\:;\"'?/>.<,'",
                false, false),
            Arguments.of("!@#$%^&*()_+=-{}[]|\\:;\"'?/>.<,'",
                false, true),
            Arguments.of(" !@#$%^&*()_+=-{}[]|\\:;\"'?/>.<,'",
                true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("createContainsOnlyHeaderNameCharsData")
    void testContainsOnlyHeaderNameChars(String v, boolean expected) {
        boolean actual = ProtocolUtilsInternal.containsOnlyHeaderNameChars(v);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createContainsOnlyHeaderNameCharsData() {
        return Stream.of(
            Arguments.of("x\n", false),
            Arguments.of("yd\u00c7ea", false),
            Arguments.of("x m", false),
            Arguments.of("xmX123abcD", true),
            Arguments.of("xm", true),
            Arguments.of("x-yio", true),
            Arguments.of("x:yio", false),
            Arguments.of("123", true),
            Arguments.of("x", true)
        );
    }

    @Test
    void testValidateHttpHeaderSection1() {
        List<List<String>> csv = Arrays.asList(
            Arrays.asList("GET", "/", "HTTP/1.0", "24")
        );
        ProtocolUtilsInternal.validateHttpHeaderSection(false,
            csv);
    }

    @Test
    void testValidateHttpHeaderSection2() {
        List<List<String>> csv = Arrays.asList(
            Arrays.asList("HTTP/1.0", "204", "No Content", "-10"),
            Arrays.asList("Content-Type", "application/json; charset=UTF8"),
            Arrays.asList("Transfer-Encoding", "chunked"),
            Arrays.asList("Date", "Tue, 15 Nov 1994 08:12:31 GMT"),
            Arrays.asList("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="),
            Arrays.asList("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/12.0")
        );
        ProtocolUtilsInternal.validateHttpHeaderSection(true,
            csv);
    }

    @ParameterizedTest
    @MethodSource("createTestValidateHttpHeaderSectionForErrorsData")
    void testValidateHttpHeaderSectionForErrors(boolean isResponse,
            List<List<String>> csv,
            String expectedErrorMessage) {
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.validateHttpHeaderSection(isResponse, csv);
        });
        assertEquals(QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION, actualEx.getReasonCode());
        assertThat(actualEx.getMessage(), containsString(expectedErrorMessage));
    }

    static List<Arguments> createTestValidateHttpHeaderSectionForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        boolean isResponse = true;
        List<List<String>> csv = Arrays.asList(
            Arrays.asList("HTTP/1 0", "200", "OK", "-10")
        );
        String expectedErrorMessage = "quasi http status line field contains spaces";
        testData.add(Arguments.of(isResponse, csv, expectedErrorMessage));

        isResponse = false;
        csv = Arrays.asList(
            Arrays.asList("HTTP/1.0", "20 4", "OK", "-10")
        );
        expectedErrorMessage = "quasi http request line field contains spaces";
        testData.add(Arguments.of(isResponse, csv, expectedErrorMessage));

        isResponse = true;
        csv = Arrays.asList(
            Arrays.asList("HTTP/1.0", "200", "OK", "-1 0")
        );
        expectedErrorMessage = "quasi http status line field contains spaces";
        testData.add(Arguments.of(isResponse, csv, expectedErrorMessage));

        isResponse = true;
        csv = Arrays.asList(
            Arrays.asList("HTTP/1.0", "200", "OK", "0"),
            Arrays.asList("Content:Type", "application/json; charset=UTF8")
        );
        expectedErrorMessage = "quasi http header name contains characters other than hyphen";
        testData.add(Arguments.of(isResponse, csv, expectedErrorMessage));

        isResponse = false;
        csv = Arrays.asList(
            Arrays.asList("HTTP/1.0", "200", "OK", "51"),
            Arrays.asList("Content-Type", "application/json; charset=UTF8\n")
        );
        expectedErrorMessage = "quasi http header value contains newlines";
        testData.add(Arguments.of(isResponse, csv, expectedErrorMessage));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestEncodeQuasiHttpHeadersData")
    void testEncodeQuasiHttpHeaders(boolean isResponse,
            List<String> reqOrStatusLine,
            Map<String, List<String>> remainingHeaders,
            String expected) {
        byte[] actual = ProtocolUtilsInternal.encodeQuasiHttpHeaders(
            isResponse, reqOrStatusLine, remainingHeaders);
        assertEquals(expected,
            new String(actual, StandardCharsets.UTF_8));
    }

    static List<Arguments> createTestEncodeQuasiHttpHeadersData() {
        List<Arguments> testData = new ArrayList<>();

        boolean isResponse = false;
        List<String> reqOrStatusLine = Arrays.asList(
            "GET",
            "/home/index?q=results",
            "HTTP/1.1",
            "-1"
        );
        LinkedHashMap<String, List<String>> remainingHeaders = new LinkedHashMap<>();
        remainingHeaders.put("Content-Type",
            Arrays.asList("text/plain"));
        String expected = "GET,/home/index?q=results,HTTP/1.1,-1\n" +
            "Content-Type,text/plain\n";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = true;
        reqOrStatusLine = Arrays.asList(
            "HTTP/1.1",
            "200",
            "OK",
            "12"
        );
        remainingHeaders = new LinkedHashMap<>();
        remainingHeaders.put("Content-Type",
            Arrays.asList("text/plain", "text/csv"));
        remainingHeaders.put("Accept", Arrays.asList("text/html"));
        remainingHeaders.put("Accept-Charset", Arrays.asList("utf-8"));
        expected = "HTTP/1.1,200,OK,12\n" +
            "Content-Type,text/plain,text/csv\n" +
            "Accept,text/html\n" +
            "Accept-Charset,utf-8\n";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = false;
        reqOrStatusLine = Arrays.asList(
            null,
            null,
            null,
            "0"
        );
        remainingHeaders = null;
        expected = "\"\",\"\",\"\",0\n";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestEncodeQuasiHttpHeadersForErrorsData")
    void testEncodeQuasiHttpHeadersForErrors(boolean isResponse,
            List<String> reqOrStatusLine,
            Map<String, List<String>> remainingHeaders,
            String expectedErrorMessage) {
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.encodeQuasiHttpHeaders(
                isResponse, reqOrStatusLine, remainingHeaders);
        });
        assertEquals(QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION,
            actualEx.getReasonCode());
        assertThat(actualEx.getMessage(), containsString(expectedErrorMessage));
    }

    static List<Arguments> createTestEncodeQuasiHttpHeadersForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        boolean isResponse = false;
        List<String> reqOrStatusLine = Arrays.asList(
            "GET",
            "/home/index?q=results",
            "HTTP/1.1",
            "-1"
        );
        Map<String, List<String>> remainingHeaders = new HashMap<>();
        remainingHeaders.put("", Arrays.asList("text/plain"));
        String expected = "quasi http header name cannot be empty";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = true;
        reqOrStatusLine = Arrays.asList(
            "HTTP/1.1",
            "400",
            "Bad Request",
            "12"
        );
        remainingHeaders = new HashMap<>();
        remainingHeaders.put("Content-Type",
            Arrays.asList("", "text/csv"));
        expected = "quasi http header value cannot be empty";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = false;
        reqOrStatusLine = Arrays.asList(
            "GET or POST",
            null,
            null,
            "0"
        );
        remainingHeaders = null;
        expected = "quasi http request line field contains spaces";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = false;
        reqOrStatusLine = Arrays.asList(
            "GET",
            null,
            null,
            "0 or 1"
        );
        remainingHeaders = null;
        expected = "quasi http request line field contains spaces";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        isResponse = true;
        reqOrStatusLine = Arrays.asList(
            "HTTP 1.1",
            "200",
            "OK",
            "0"
        );
        remainingHeaders = null;
        expected = "quasi http status line field contains spaces";
        testData.add(Arguments.of(isResponse, reqOrStatusLine,
            remainingHeaders, expected));

        return testData;
    }


    @ParameterizedTest
    @MethodSource("createTestDecodeQuasiHttpHeadersData")
    public void testDecodeQuasiHttpHeaders(boolean isResponse,
            byte[] data, int offset, int length,
            Map<String, List<String>> expectedHeaders,
            List<String> expectedReqOrStatusLine) {
        Map<String, List<String>> headersReceiver = new HashMap<>();
        List<String> actualReqOrStatusLine = ProtocolUtilsInternal.decodeQuasiHttpHeaders(
            isResponse, data, offset, length,
            headersReceiver);
        assertEquals(expectedReqOrStatusLine, actualReqOrStatusLine);
        ComparisonUtils.compareHeaders(expectedHeaders, headersReceiver);
    }

    static List<Arguments> createTestDecodeQuasiHttpHeadersData() {
        List<Arguments> testData = new ArrayList<>();

        boolean isResponse = false;
        byte[] data = MiscUtilsInternal.stringToBytes(
            "GET,/home/index?q=results,HTTP/1.1,-1\n" +
            "Content-Type,text/plain\n");
        int offset = 0;
        int length = data.length;
        Map<String, List<String>> expectedHeaders = new LinkedHashMap<>();
        expectedHeaders.put("content-type", Arrays.asList("text/plain"));
        List<String> expectedReqOrStatusLine = Arrays.asList(
            "GET",
            "/home/index?q=results",
            "HTTP/1.1",
            "-1"
        );
        testData.add(Arguments.of(isResponse, data, offset, length,
            expectedHeaders, expectedReqOrStatusLine));

        isResponse = true;
        data = MiscUtilsInternal.stringToBytes("HTTP/1.1,200,OK,12\n" +
            "Content-Type,text/plain,text/csv\n" +
            "content-type,application/json\n" +
            "\r\n" +
            "ignored\n" +
            "Accept,text/html\n" +
            "Accept-Charset,utf-8\n\"");
        offset = 0;
        length = data.length - 1;
        expectedHeaders = new LinkedHashMap<>();
        expectedHeaders.put("content-type", Arrays.asList(
        "text/plain", "text/csv", "application/json"));
        expectedHeaders.put("accept", Arrays.asList("text/html"));
        expectedHeaders.put("accept-charset", Arrays.asList("utf-8"));
        expectedReqOrStatusLine = Arrays.asList(
            "HTTP/1.1",
            "200",
            "OK",
            "12"
        );
        testData.add(Arguments.of(isResponse, data, offset, length,
            expectedHeaders, expectedReqOrStatusLine));
        
        isResponse = false;
        data = MiscUtilsInternal.stringToBytes("\"\",\"\",\"\",0\n");
        offset = 0;
        length = data.length;
        expectedHeaders = new HashMap<>();
        expectedReqOrStatusLine = Arrays.asList(
            "",
            "",
            "",
            "0"
        );
        testData.add(Arguments.of(isResponse, data, offset, length,
            expectedHeaders, expectedReqOrStatusLine));

        isResponse = true;
        data = MiscUtilsInternal.stringToBytes(
            "k\"GET,/home/index?q=results,HTTP/1.1,-1\n" +
            "Content-Type,text/plain\nk2\"");
        offset = 2;
        length = data.length - 5;
        expectedHeaders = new HashMap<>();
        expectedHeaders.put("content-type",
            Arrays.asList("text/plain"));
        expectedReqOrStatusLine = Arrays.asList(
            "GET",
            "/home/index?q=results",
            "HTTP/1.1",
            "-1"
        );
        testData.add(Arguments.of(isResponse, data, offset, length,
            expectedHeaders, expectedReqOrStatusLine));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestDecodeQuasiHttpHeadersForErrorsData")
    void testDecodeQuasiHttpHeadersForErrors(boolean isResponse,
            byte[] data, int offset, int length,
            String expectedErrorMessage) {
        QuasiHttpException actualEx = assertThrowsExactly(QuasiHttpException.class, () -> {
            ProtocolUtilsInternal.decodeQuasiHttpHeaders(
                isResponse, data, offset, length,
                new HashMap<>());
        });
        assertEquals(QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION,
            actualEx.getReasonCode());
        assertThat(actualEx.getMessage(), containsString(expectedErrorMessage));
    }

    static List<Arguments> createTestDecodeQuasiHttpHeadersForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        boolean isResponse = false;
        byte[] data = MiscUtilsInternal.stringToBytes(
            "\"k\n,lopp");
        int offset = 0;
        int length = data.length;
        String expectedErrorMessage = "invalid quasi http headers";
        testData.add(Arguments.of(isResponse, data, offset,
            length, expectedErrorMessage));

        isResponse = false;
        data = new byte[0];
        offset = 0;
        length = 0;
        expectedErrorMessage = "invalid quasi http headers";
        testData.add(Arguments.of(isResponse, data, offset,
            length, expectedErrorMessage));

        isResponse = true;
        data = MiscUtilsInternal.stringToBytes("HTTP/1.1,200");
        offset = 0;
        length = data.length;
        expectedErrorMessage = "invalid quasi http status line";
        testData.add(Arguments.of(isResponse, data, offset,
            length, expectedErrorMessage));

        isResponse = false;
        data = MiscUtilsInternal.stringToBytes("GET,HTTP/1.1,");
        offset = 0;
        length = data.length;
        expectedErrorMessage = "invalid quasi http request line";
        testData.add(Arguments.of(isResponse, data, offset,
            length, expectedErrorMessage));

        return testData;
    }
}
