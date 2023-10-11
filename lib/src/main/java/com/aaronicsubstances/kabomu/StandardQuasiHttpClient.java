package com.aaronicsubstances.kabomu;

import java.util.Map;
import java.util.Objects;

import com.aaronicsubstances.kabomu.abstractions.CheckedRunnable;
import com.aaronicsubstances.kabomu.abstractions.ConnectionAllocationResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpClientTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport.DeserializerFunction;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport.SerializerFunction;
import com.aaronicsubstances.kabomu.exceptions.MissingDependencyException;
import com.aaronicsubstances.kabomu.exceptions.QuasiHttpException;
import com.aaronicsubstances.kabomu.protocolimpl.ProtocolUtilsInternal;

public class StandardQuasiHttpClient {

    @FunctionalInterface
    public static interface RequestGenerator {
        QuasiHttpRequest apply(Map<String, Object> environment) throws Exception;
    }

    private QuasiHttpClientTransport transport;

    public StandardQuasiHttpClient() {
    }

    public QuasiHttpClientTransport getTransport() {
        return transport;
    }

    public void setTransport(QuasiHttpClientTransport transport) {
        this.transport = transport;
    }

    public QuasiHttpResponse send(Object remoteEndpoint,
            QuasiHttpRequest request) throws Exception {
        return send(remoteEndpoint, request, null);
    }

    public QuasiHttpResponse send(Object remoteEndpoint,
            QuasiHttpRequest request, QuasiHttpProcessingOptions options)
            throws Exception {
        Objects.requireNonNull(request, "request");
        return sendInternal(remoteEndpoint, request, null, options);
    }

    public QuasiHttpResponse send2(Object remoteEndpoint,
            RequestGenerator requestFunc) throws Exception {
        return send2(remoteEndpoint, requestFunc,
            null);
    }

    public QuasiHttpResponse send2(Object remoteEndpoint,
            RequestGenerator requestFunc,
            QuasiHttpProcessingOptions options) throws Exception {
        Objects.requireNonNull(requestFunc, "requestFunc");
        return sendInternal(remoteEndpoint, null, requestFunc,
            options);
    }

    private QuasiHttpResponse sendInternal(Object remoteEndpoint, QuasiHttpRequest request,
            RequestGenerator requestFunc,
            QuasiHttpProcessingOptions sendOptions) throws Exception {
        // access fields for use per request call, in order to cooperate with
        // any implementation of field accessors which supports
        // concurrent modifications.
        QuasiHttpClientTransport transport = this.transport;

        if (transport == null) {
            throw new MissingDependencyException("client transport");
        }

        ConnectionAllocationResponse connectionAllocationResponse = transport.allocateConnection(
            remoteEndpoint, sendOptions);
        QuasiHttpConnection connection = null;
        if (connectionAllocationResponse != null) {
            connection = connectionAllocationResponse.getConnection();
        }
        if (connection == null) {
            throw new QuasiHttpException("no connection");
        }
        try {
            return processSend(request, requestFunc,
                transport, connection, connectionAllocationResponse);
        }
        catch (Exception e) {
            abort(transport, connection, true, null);
            if (e instanceof QuasiHttpException) {
                throw (QuasiHttpException)e;
            }
            QuasiHttpException abortError = new QuasiHttpException(
                "encountered error during send request processing",
                QuasiHttpException.REASON_CODE_GENERAL,
                e);
            throw abortError;
        }
    }

    private QuasiHttpResponse processSend(QuasiHttpRequest request,
            RequestGenerator requestFunc,
            QuasiHttpClientTransport transport2,
            QuasiHttpConnection connection,
            ConnectionAllocationResponse connectionAllocationResponse)
            throws Exception {
        CheckedRunnable ongoingConnectionTask = connectionAllocationResponse.getConnectTask();
        if (ongoingConnectionTask != null) {
            // wait for connection to be completely established.
            ongoingConnectionTask.run();
        }

        if (request == null) {
            request = requestFunc.apply(connection.getEnvironment());
            if (request == null) {
                throw new QuasiHttpException("no request");
            }
        }

        // send entire request first before
        // receiving of response.
        QuasiHttpAltTransport altTransport = null;
        SerializerFunction<QuasiHttpRequest> requestSerializer = null;
        if (transport instanceof QuasiHttpAltTransport) {
            altTransport = (QuasiHttpAltTransport)transport;
            requestSerializer = altTransport.getRequestSerializer();
        }
        boolean requestSerialized = false;
        if (requestSerializer != null) {
            requestSerialized = Boolean.TRUE.equals(requestSerializer.apply(
                connection, request));
        }
        if (!requestSerialized) {
            ProtocolUtilsInternal.writeEntityToTransport(
                false, request, transport.getWritableStream(connection),
                connection);
        }

        QuasiHttpResponse response = null;
        DeserializerFunction<QuasiHttpResponse> responseDeserializer = null;
        if (altTransport != null) {
            responseDeserializer = altTransport.getResponseDeserializer();
        }
        if (responseDeserializer != null) {
            response = responseDeserializer.apply(connection);
        }
        if (response == null) {
            response = (QuasiHttpResponse)ProtocolUtilsInternal.readEntityFromTransport(
                true, transport.getReadableStream(connection), connection);
            if (response.getBody() != null) {
                response.setDisposer(() -> {
                    transport.releaseConnection(connection, null);
                });
            }
        }
        abort(transport, connection, false, response);
        return response;
    }

    private static void abort(QuasiHttpClientTransport transport,
            QuasiHttpConnection connection, boolean errorOccured,
            QuasiHttpResponse response) throws Exception {
        if (errorOccured) {
            try {
                transport.releaseConnection(connection, null);
            }
            catch (Exception ignore) { }
        }
        else {
            transport.releaseConnection(connection, response);
        } 
    }
}
