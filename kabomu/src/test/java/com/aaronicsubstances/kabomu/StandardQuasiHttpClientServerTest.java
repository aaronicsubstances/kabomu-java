package com.aaronicsubstances.kabomu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpClientTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpServerTransport;
import com.aaronicsubstances.kabomu.shared.ComparisonUtils;
import com.aaronicsubstances.kabomu.shared.RandomizedReadInputStream;

import static com.aaronicsubstances.kabomu.shared.ComparisonUtils.newInstance;
import static com.aaronicsubstances.kabomu.shared.ComparisonUtils.concatBuffers;

public class StandardQuasiHttpClientServerTest {

    @ParameterizedTest
    @MethodSource("createTestRequestSerializationData")
    void testRequestSerialization(
            byte[] expectedReqBodyBytes,
            QuasiHttpRequest request,
            QuasiHttpRequest expectedRequest,
            byte[] expectedSerializedReq) throws IOException {
        Object remoteEndpoint = new Object();
        if (expectedReqBodyBytes != null) {
            request.setBody(new ByteArrayInputStream(expectedReqBodyBytes));
        }
        QuasiHttpResponse dummyRes = new DefaultQuasiHttpResponse();
        ByteArrayOutputStream memOutputStream = new ByteArrayOutputStream();
        QuasiHttpProcessingOptions sendOptions = new DefaultQuasiHttpProcessingOptions();
        QuasiHttpConnectionImpl clientConnection = new QuasiHttpConnectionImpl();
        clientConnection.setProcessingOptions(sendOptions);
        clientConnection.setWritableStream(memOutputStream);
        StandardQuasiHttpClient client = new StandardQuasiHttpClient();
        ClientTransportImpl transport = new ClientTransportImpl(false) {

            @Override
            public QuasiHttpConnection allocateConnection(Object endPt, QuasiHttpProcessingOptions opts)
                    throws Exception {
                assertSame(remoteEndpoint, endPt);
                assertSame(sendOptions, opts);
                return clientConnection;
            }

            @Override
            public void establishConnection(QuasiHttpConnection conn) throws Exception {
                assertSame(clientConnection, conn);
            }
        };
        client.setTransport(transport);
        transport.responseDeserializer = conn -> {
            assertSame(clientConnection, conn);
            return dummyRes;
        };
        QuasiHttpResponse actualRes = client.send(remoteEndpoint, request,
            sendOptions);
        assertSame(dummyRes, actualRes);

        if (expectedSerializedReq != null) {
            assertArrayEquals(expectedSerializedReq,
                memOutputStream.toByteArray());
        }

        // reset for reading.
        ByteArrayInputStream memInputStream = new ByteArrayInputStream(
            memOutputStream.toByteArray()
        );

        // deserialize
        AtomicReference<QuasiHttpRequest> actualRequest =
            new AtomicReference<>();
        QuasiHttpConnectionImpl serverConnection = new QuasiHttpConnectionImpl();
        serverConnection.setReadableStream(
            new RandomizedReadInputStream(memInputStream));
        serverConnection.setEnvironment(new HashMap<>());
        StandardQuasiHttpServer server = new StandardQuasiHttpServer();
        ServerTransportImpl serverTransport = new ServerTransportImpl();
        serverTransport.responseSerializer = (conn, res) -> {
            assertSame(serverConnection, conn);
            assertSame(dummyRes, res);
            return true;
        };
        server.setTransport(serverTransport);
        server.setApplication(req -> {
            actualRequest.set(req);
            return dummyRes;
        });
        server.acceptConnection(serverConnection);

        // assert
        ComparisonUtils.compareRequests(expectedRequest,
            actualRequest.get(), expectedReqBodyBytes);
        assertSame(serverConnection.getEnvironment(),
            actualRequest.get().getEnvironment());
    }

    static List<Arguments> createTestRequestSerializationData() {
        List<Arguments> testData = new ArrayList<>();

        AtomicReference<byte[]> expectedReqBodyBytes = new AtomicReference<>(
            MiscUtilsInternal.stringToBytes("tanner"));
        DefaultQuasiHttpRequest request = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("GET");
            t.setTarget("/");
            t.setHttpVersion("HTTP/1.0");
            t.setContentLength(expectedReqBodyBytes.get().length);
            Map<String, List<String>> headers = new LinkedHashMap<>();
            t.setHeaders(headers);
            headers.put("Accept", Arrays.asList("text/plain", "text/csv"));
            headers.put("Content-Type", Arrays.asList("application/json,charset=UTF-8"));
        });
        DefaultQuasiHttpRequest expectedRequest = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("GET");
            t.setTarget("/");
            t.setHttpVersion("HTTP/1.0");
            t.setContentLength(expectedReqBodyBytes.get().length);
            Map<String, List<String>> headers = new LinkedHashMap<>();
            t.setHeaders(headers);
            headers.put("accept", Arrays.asList("text/plain", "text/csv"));
            headers.put("content-type", Arrays.asList("application/json,charset=UTF-8"));
        });
        byte[] expectedSerializedReq = concatBuffers(
            new byte[] {
                0x68, 0x64, 0x72, 0x73,
                0, 0, 0, 90
            },
            MiscUtilsInternal.stringToBytes("GET,/,HTTP/1.0,6\n"),
            MiscUtilsInternal.stringToBytes("Accept,text/plain,text/csv\n"),
            MiscUtilsInternal.stringToBytes("Content-Type,\"application/json,charset=UTF-8\"\n"),
            expectedReqBodyBytes.get()
        );
        testData.add(Arguments.of(expectedReqBodyBytes.get(), request,
            expectedRequest, expectedSerializedReq));

        expectedReqBodyBytes.set(null);
        request = new DefaultQuasiHttpRequest();
        expectedRequest = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("");
            t.setTarget("");
            t.setHttpVersion("");
            t.setContentLength(0);
            t.setHeaders(new HashMap<String, List<String>>());
        });
        expectedSerializedReq = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 11,
            (byte)'"', (byte)'"', (byte)',', (byte)'"', (byte)'"',
            (byte)',', (byte)'"', (byte)'"', (byte)',', (byte)'0',
            (byte)'\n'
        };
        testData.add(Arguments.of(expectedReqBodyBytes.get(), request,
            expectedRequest, expectedSerializedReq));

        expectedReqBodyBytes.set(new byte[] { 8, 7, 8, 9 });
        request = new DefaultQuasiHttpRequest();
        request.setContentLength(-1);
        expectedRequest = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("");
            t.setTarget("");
            t.setHttpVersion("");
            t.setContentLength(-1);
            t.setHeaders(new HashMap<>());
        });
        expectedSerializedReq = null;
        testData.add(Arguments.of(expectedReqBodyBytes.get(), request,
            expectedRequest, expectedSerializedReq));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestRequestSerializationForErrorsData")
    void testRequestSerializationForErrors(
            QuasiHttpRequest request,
            QuasiHttpProcessingOptions sendOptions,
            String expectedErrorMsg,
            byte[] expectedSerializedReq) throws IOException {
        Object remoteEndpoint = new Object();
        QuasiHttpResponse dummyRes = new DefaultQuasiHttpResponse();
        ByteArrayOutputStream memOutputStream = new ByteArrayOutputStream();
        QuasiHttpConnectionImpl clientConnection = new QuasiHttpConnectionImpl();
        clientConnection.setProcessingOptions(sendOptions);
        clientConnection.setWritableStream(memOutputStream);
        StandardQuasiHttpClient client = new StandardQuasiHttpClient();
        ClientTransportImpl transport = new ClientTransportImpl(true) {

            @Override
            public QuasiHttpConnection allocateConnection(Object endPt, QuasiHttpProcessingOptions opts)
                    throws Exception {
                assertSame(remoteEndpoint, endPt);
                assertSame(sendOptions, opts);
                return clientConnection;
            }

            @Override
            public void establishConnection(QuasiHttpConnection conn) throws Exception {
                assertSame(clientConnection, conn);
            }
        };
        client.setTransport(transport);
        transport.responseDeserializer = conn -> {
            assertSame(clientConnection, conn);
            return dummyRes;
        };

        if (expectedErrorMsg == null) {
            QuasiHttpResponse actualRes = client.send(remoteEndpoint, request,
                sendOptions);
            assertSame(dummyRes, actualRes);

            if (expectedSerializedReq != null) {
                assertArrayEquals(expectedSerializedReq,
                    memOutputStream.toByteArray());
            }
        }
        else {
            Exception actualEx = assertThrows(Exception.class, () -> {
                client.send(remoteEndpoint, request, sendOptions);
            });
            assertThat(actualEx.getMessage(), containsString(expectedErrorMsg));
        }
    }

    static List<Arguments> createTestRequestSerializationForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        DefaultQuasiHttpRequest request = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("POST");
            t.setTarget("/Update");
            t.setContentLength(8);
        });
        DefaultQuasiHttpProcessingOptions sendOptions = new DefaultQuasiHttpProcessingOptions();
        sendOptions.setMaxHeadersSize(18);
        String expectedErrorMsg = null;
        byte[] expectedSerializedReq = concatBuffers(
            new byte[] {
                0x68, 0x64, 0x72, 0x73,
                0, 0, 0, 18
            },
            MiscUtilsInternal.stringToBytes("POST,/Update,\"\",8\n")
        );
        testData.add(Arguments.of(request, sendOptions, expectedErrorMsg,
            expectedSerializedReq));

        AtomicReference<byte[]> requestBodyBytes = new AtomicReference<>(
            new byte[] { 4 });
        request = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpMethod("PUT");
            t.setTarget("/Updates");
            t.setContentLength(0);
            t.setBody(new ByteArrayInputStream(requestBodyBytes.get()));
        });
        sendOptions = new DefaultQuasiHttpProcessingOptions();
        sendOptions.setMaxHeadersSize(19);
        expectedErrorMsg = null;
        expectedSerializedReq = concatBuffers(
            new byte[] {
                0x68, 0x64, 0x72, 0x73,
                0, 0, 0, 18
            },
            MiscUtilsInternal.stringToBytes("PUT,/Updates,\"\",0\n"),
            new byte[] { 0x62, 0x64, 0x74, 0x61 },
            new byte[] { 0, 0, 0, 1, 4 },
            new byte[] { 0x62, 0x64, 0x74, 0x61 },
            new byte[] { 0, 0, 0, 0 }
        );
        testData.add(Arguments.of(request, sendOptions, expectedErrorMsg,
            expectedSerializedReq));

        requestBodyBytes.set(new byte[] { 4, 5, 6 });
        request = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setContentLength(10);
            t.setBody(new ByteArrayInputStream(requestBodyBytes.get()));
        });
        sendOptions = null;
        expectedErrorMsg = null;
        expectedSerializedReq = concatBuffers(
            new byte[] {
                0x68, 0x64, 0x72, 0x73,
                0, 0, 0, 12
            },
            MiscUtilsInternal.stringToBytes("\"\",\"\",\"\",10\n"),
            requestBodyBytes.get()
        );
        testData.add(Arguments.of(request, sendOptions, expectedErrorMsg,
            expectedSerializedReq));

        request = new DefaultQuasiHttpRequest();
        sendOptions = new DefaultQuasiHttpProcessingOptions();
        sendOptions.setMaxHeadersSize(5);
        expectedErrorMsg = "quasi http headers exceed max size";
        expectedSerializedReq = null;
        testData.add(Arguments.of(request, sendOptions, expectedErrorMsg,
            expectedSerializedReq));

        request = newInstance(new DefaultQuasiHttpRequest(), t -> {
            t.setHttpVersion("no-spaces-allowed");
            Map<String, List<String>> headers = new HashMap<>();
            t.setHeaders(headers);
            headers.put("empty-prohibited", Arrays.asList("a: \nb"));
        });
        sendOptions = null;
        expectedErrorMsg = "quasi http header value contains newlines";
        expectedSerializedReq = null;
        testData.add(Arguments.of(request, sendOptions, expectedErrorMsg,
            expectedSerializedReq));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestResponseSerializationData")
    void testResponseSerialization(
            byte[] expectedResBodyBytes,
            QuasiHttpResponse response,
            QuasiHttpResponse expectedResponse,
            byte[] expectedSerializedRes) throws IOException {
        if (expectedResBodyBytes != null) {
            response.setBody(new ByteArrayInputStream(expectedResBodyBytes));
        }
        ByteArrayOutputStream memOutputStream = new ByteArrayOutputStream();
        QuasiHttpConnectionImpl serverConnection = new QuasiHttpConnectionImpl();
        serverConnection.setWritableStream(memOutputStream);
        DefaultQuasiHttpRequest dummyReq = new DefaultQuasiHttpRequest();
        StandardQuasiHttpServer server = new StandardQuasiHttpServer();
        ServerTransportImpl serverTransport = new ServerTransportImpl();
        serverTransport.requestDeserializer = (conn) -> {
            assertSame(serverConnection, conn);
            return dummyReq;
        };
        server.setTransport(serverTransport);
        server.setApplication(req -> {
            assertSame(dummyReq, req);
            return response;
        });
        server.acceptConnection(serverConnection);

        if (expectedSerializedRes != null) {
            assertArrayEquals(expectedSerializedRes,
                memOutputStream.toByteArray());
        }

        // reset for reading.
        ByteArrayInputStream memInputStream = new ByteArrayInputStream(
            memOutputStream.toByteArray()
        );
        
        // deserialize
        Object remoteEndpoint = new Object();
        QuasiHttpProcessingOptions sendOptions = new DefaultQuasiHttpProcessingOptions();
        QuasiHttpConnectionImpl clientConnection = new QuasiHttpConnectionImpl();
        clientConnection.setProcessingOptions(sendOptions);
        clientConnection.setReadableStream(
            new RandomizedReadInputStream(memInputStream)
        );
        StandardQuasiHttpClient client = new StandardQuasiHttpClient();
        ClientTransportImpl transport = new ClientTransportImpl(false) {

            @Override
            public QuasiHttpConnection allocateConnection(Object endPt, QuasiHttpProcessingOptions opts)
                    throws Exception {
                assertSame(remoteEndpoint, endPt);
                assertSame(sendOptions, opts);
                return clientConnection;
            }

            @Override
            public void establishConnection(QuasiHttpConnection conn) throws Exception {
                assertSame(clientConnection, conn);
            }
        };
        client.setTransport(transport);
        transport.requestSerializer = (conn, req) -> {
            assertSame(clientConnection, conn);
            assertSame(dummyReq, req);
            return true;
        };
        QuasiHttpResponse actualRes = client.send(remoteEndpoint, dummyReq,
            sendOptions);

        // assert
        ComparisonUtils.compareResponses(expectedResponse,
            actualRes, expectedResBodyBytes);
    }

    static List<Arguments> createTestResponseSerializationData() {
        List<Arguments> testData = new ArrayList<>();

        AtomicReference<byte[]> expectedResBodyBytes = new AtomicReference<>(
            MiscUtilsInternal.stringToBytes("sent"));
        QuasiHttpResponse response = newInstance(new DefaultQuasiHttpResponse(), t -> {
            t.setHttpVersion("HTTP/1.1");
            t.setStatusCode(400);
            t.setHttpStatusMessage("Bad, Request");
            t.setContentLength(expectedResBodyBytes.get().length);
            Map<String, List<String>> headers = new HashMap<>();
            t.setHeaders(headers);
            headers.put("Status", Arrays.asList("seen"));
        });
        QuasiHttpResponse expectedResponse = newInstance(new DefaultQuasiHttpResponse(), t -> {
            t.setHttpVersion("HTTP/1.1");
            t.setStatusCode(400);
            t.setHttpStatusMessage("Bad, Request");
            t.setContentLength(expectedResBodyBytes.get().length);
            Map<String, List<String>> headers = new HashMap<>();
            t.setHeaders(headers);
            headers.put("status", Arrays.asList("seen"));
        });
        byte[] expectedSerializedRes = concatBuffers(
            new byte[] {
                0x68, 0x64, 0x72, 0x73,
                0, 0, 0, 42
            },
            MiscUtilsInternal.stringToBytes("HTTP/1.1,400,\"Bad, Request\",4\n"),
            MiscUtilsInternal.stringToBytes("Status,seen\n"),
            expectedResBodyBytes.get()
        );
        testData.add(Arguments.of(expectedResBodyBytes.get(),
            response, expectedResponse, expectedSerializedRes));

        expectedResBodyBytes.set(null);
        response = new DefaultQuasiHttpResponse();
        expectedResponse = newInstance(new DefaultQuasiHttpResponse(), t -> {
            t.setHttpVersion("");
            t.setStatusCode(0);
            t.setHttpStatusMessage("");
            t.setContentLength(0);
            t.setHeaders(new HashMap<>());
        });
        expectedSerializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'0',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'0', (byte)'\n'
        };
        testData.add(Arguments.of(expectedResBodyBytes.get(),
            response, expectedResponse, expectedSerializedRes));

        expectedResBodyBytes.set(new byte[] { 8, 7, 8, 9, 2 });
        response = new DefaultQuasiHttpResponse();
        response.setContentLength(-5);
        expectedResponse = newInstance(new DefaultQuasiHttpResponse(), t -> {
            t.setHttpVersion("");
            t.setStatusCode(0);
            t.setHttpStatusMessage("");
            t.setContentLength(-5);
            t.setHeaders(new HashMap<>());
        });
        expectedSerializedRes = null;
        testData.add(Arguments.of(expectedResBodyBytes.get(),
            response, expectedResponse, expectedSerializedRes));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestResponseDeserializationForErrorsData")
    void testResponseDeserializationForErrors(
            byte[] serializedRes, QuasiHttpProcessingOptions sendOptions,
            String expectedErrorMsg) throws IOException {
        InputStream memInputStream = new ByteArrayInputStream(serializedRes);
        DefaultQuasiHttpRequest dummyReq = new DefaultQuasiHttpRequest();
        
        // deserialize
        Object remoteEndpoint = new Object();
        QuasiHttpConnectionImpl clientConnection = new QuasiHttpConnectionImpl();
        clientConnection.setProcessingOptions(sendOptions);
        clientConnection.setReadableStream(
            new RandomizedReadInputStream(memInputStream)
        );
        clientConnection.setEnvironment(new HashMap<>());
        StandardQuasiHttpClient client = new StandardQuasiHttpClient();
        ClientTransportImpl transport = new ClientTransportImpl(false) {

            @Override
            public QuasiHttpConnection allocateConnection(Object endPt, QuasiHttpProcessingOptions opts)
                    throws Exception {
                assertSame(remoteEndpoint, endPt);
                assertSame(sendOptions, opts);
                return clientConnection;
            }

            @Override
            public void establishConnection(QuasiHttpConnection conn) throws Exception {
                assertSame(clientConnection, conn);
            }
        };
        client.setTransport(transport);
        transport.requestSerializer = (conn, req) -> {
            assertSame(clientConnection, conn);
            assertSame(dummyReq, req);
            return true;
        };

        if (expectedErrorMsg == null) {
            QuasiHttpResponse res = client.send2(remoteEndpoint, (env) -> {
                assertSame(clientConnection.getEnvironment(), env);
                return dummyReq;
            }, sendOptions);
            if (res.getBody() != null) {
                IOUtils.toByteArray(res.getBody());
            }
        }
        else {
            Exception actualEx = assertThrows(Exception.class, () -> {
                QuasiHttpResponse res = client.send2(remoteEndpoint, (env) -> {
                    return dummyReq;
                }, sendOptions);
                if (res.getBody() != null) {
                    IOUtils.toByteArray(res.getBody());
                }
            });
            assertThat(actualEx.getMessage(), containsString(expectedErrorMsg));
        }
    }

    static List<Arguments> createTestResponseDeserializationForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        byte[] serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 30,
            (byte)'H', (byte)'T', (byte)'T', (byte)'P',
            (byte)'/', (byte)'1', (byte)'.', (byte)'1',
            (byte)',', (byte)'4', (byte)'0', (byte)'0',
            (byte)',', (byte)'"', (byte)'B', (byte)'a',
            (byte)'d', (byte)',', (byte)' ', (byte)'R',
            (byte)'e', (byte)'q', (byte)'u', (byte)'e',
            (byte)'s', (byte)'t', (byte)'"', (byte)',',
            (byte)'x', (byte)'\n',
            (byte)'s', (byte)'e', (byte)'n', (byte)'t'
        };
        QuasiHttpProcessingOptions sendOpts = null;
        String expectedErrorMsg = "invalid quasi http response content length";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'y',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'0', (byte)'\n'
        };
        sendOpts = null;
        expectedErrorMsg = "invalid quasi http response status code";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x10,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'0', (byte)'\n'
        };
        sendOpts = null;
        expectedErrorMsg = "unexpected quasi http headers tag";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'2', (byte)'\n',
            (byte)'0', (byte)'d'
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxResponseBodySize(2);
        expectedErrorMsg = null;
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'2', (byte)'\n',
            (byte)'0', (byte)'d'
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxResponseBodySize(1);
        expectedErrorMsg = "stream size exceeds limit";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'2', (byte)'\n',
            (byte)'0', (byte)'d'
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxHeadersSize(1);
        expectedErrorMsg = "quasi http headers exceed max size";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 10,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'2', (byte)'\n',
            (byte)'0', (byte)'d'
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxHeadersSize(11);
        sendOpts.setMaxResponseBodySize(-1);
        expectedErrorMsg = null;
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 11,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'-', (byte)'1', (byte)'\n',
            0x62, 0x65, 0x78, 0x74,
            0, 0, 0, 3,
            (byte)'a', (byte)'b', (byte)'c',
            0x62,0x64, 0x74, 0x61,
            0, 0, 0, 0
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxHeadersSize(11);
        sendOpts.setMaxResponseBodySize(1);
        expectedErrorMsg = null;
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 11,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'-', (byte)'1', (byte)'\n',
            0x62,0x64, 0x74, 0x61,
            0, 0, 0, 3,
            (byte)'a', (byte)'b', (byte)'c',
            0x62,0x64, 0x74, 0x61,
            0, 0, 0, 0
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxResponseBodySize(1);
        expectedErrorMsg = "stream size exceeds limit";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        serializedRes = new byte[] {
            0x68, 0x64, 0x72, 0x73,
            0, 0, 0, 11,
            (byte)'"', (byte)'"', (byte)',', (byte)'1',
            (byte)',', (byte)'"', (byte)'"', (byte)',',
            (byte)'8', (byte)'2', (byte)'\n',
            (byte)'a', (byte)'b', (byte)'c',
        };
        sendOpts = new DefaultQuasiHttpProcessingOptions();
        sendOpts.setMaxResponseBodySize(-1);
        expectedErrorMsg = "end of read";
        testData.add(Arguments.of(serializedRes, sendOpts,
            expectedErrorMsg));

        return testData;
    }
    
    static abstract class ClientTransportImpl implements QuasiHttpClientTransport, QuasiHttpAltTransport {
        public SerializerFunction<QuasiHttpRequest> requestSerializer;
        public SerializerFunction<QuasiHttpResponse> responseSerializer;
        public DeserializerFunction<QuasiHttpRequest> requestDeserializer;
        public DeserializerFunction<QuasiHttpResponse> responseDeserializer;

        public ClientTransportImpl(boolean initializeSerializerFunctions) {
            if (!initializeSerializerFunctions) {
                return;
            }
            requestSerializer = (x, y) -> false;
            responseSerializer = (x, y) -> false;
            requestDeserializer = x -> null;
            responseDeserializer = x -> null;
        }

        @Override
        public InputStream getReadableStream(QuasiHttpConnection connection) throws Exception {
            return ((QuasiHttpConnectionImpl)connection).getReadableStream();
        }

        @Override
        public OutputStream getWritableStream(QuasiHttpConnection connection) throws Exception {
            return ((QuasiHttpConnectionImpl)connection).getWritableStream();
        }

        @Override
        public SerializerFunction<QuasiHttpRequest> getRequestSerializer() throws Exception {
            return requestSerializer;
        }

        @Override
        public SerializerFunction<QuasiHttpResponse> getResponseSerializer() throws Exception {
            return responseSerializer;
        }

        @Override
        public DeserializerFunction<QuasiHttpRequest> getRequestDeserializer() throws Exception {
            return requestDeserializer;
        }

        @Override
        public DeserializerFunction<QuasiHttpResponse> getResponseDeserializer() throws Exception {
            return responseDeserializer;
        }

        @Override
        public void releaseConnection(QuasiHttpConnection connection, QuasiHttpResponse response) throws Exception {
        }
    }

    class ServerTransportImpl implements QuasiHttpServerTransport, QuasiHttpAltTransport {
        public SerializerFunction<QuasiHttpRequest> requestSerializer;
        public SerializerFunction<QuasiHttpResponse> responseSerializer;
        public DeserializerFunction<QuasiHttpRequest> requestDeserializer;
        public DeserializerFunction<QuasiHttpResponse> responseDeserializer;

        @Override
        public InputStream getReadableStream(QuasiHttpConnection connection) throws Exception {
            return ((QuasiHttpConnectionImpl)connection).getReadableStream();
        }

        @Override
        public OutputStream getWritableStream(QuasiHttpConnection connection) throws Exception {
            return ((QuasiHttpConnectionImpl)connection).getWritableStream();
        }

        @Override
        public SerializerFunction<QuasiHttpRequest> getRequestSerializer() throws Exception {
            return requestSerializer;
        }

        @Override
        public SerializerFunction<QuasiHttpResponse> getResponseSerializer() throws Exception {
            return responseSerializer;
        }

        @Override
        public DeserializerFunction<QuasiHttpRequest> getRequestDeserializer() throws Exception {
            return requestDeserializer;
        }

        @Override
        public DeserializerFunction<QuasiHttpResponse> getResponseDeserializer() throws Exception {
            return responseDeserializer;
        }

        @Override
        public void releaseConnection(QuasiHttpConnection connection) throws Exception {
        }
    }

    class QuasiHttpConnectionImpl implements QuasiHttpConnection {
        private InputStream readableStream;
        private OutputStream writableStream;
        private QuasiHttpProcessingOptions processingOptions;
        private Map<String, Object> environment;
        
        public InputStream getReadableStream() {
            return readableStream;
        }

        public void setReadableStream(InputStream readableStream) {
            this.readableStream = readableStream;
        }

        public OutputStream getWritableStream() {
            return writableStream;
        }

        public void setWritableStream(OutputStream writableStream) {
            this.writableStream = writableStream;
        }

        @Override
        public QuasiHttpProcessingOptions getProcessingOptions() {
            return processingOptions;
        }

        public void setProcessingOptions(QuasiHttpProcessingOptions processingOptions) {
            this.processingOptions = processingOptions;
        }

        @Override
        public Map<String, Object> getEnvironment() {
            return environment;
        }

        public void setEnvironment(Map<String, Object> environment) {
            this.environment = environment;
        }

        @Override
        public CustomTimeoutScheduler getTimeoutScheduler() {
            return null;
        }
    }
}
