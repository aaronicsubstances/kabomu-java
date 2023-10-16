package com.aaronicsubstances.kabomu;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
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

/**
 * The standard implementation of the client side of the quasi http protocol
 * defined by the Kabomu library.
 *
 * This class provides the client facing side of networking for end users.
 * It is the complement to the  {@link StandardQuasiHttpServer} class for
 * supporting the semantics of HTTP client libraries
 * whiles enabling underlying transport options beyond TCP.
 *
 * Therefore this class can be seen as the equivalent of an HTTP client
 * that extends underlying transport beyond TCP
 * to IPC mechanisms.
 */
public class StandardQuasiHttpClient {

    @FunctionalInterface
    public static interface RequestGenerator {
        QuasiHttpRequest apply(Map<String, Object> environment) throws Exception;
    }

    private QuasiHttpClientTransport transport;

    /**
     * Creates a new instance.
     */
    public StandardQuasiHttpClient() {
    }

    /**
     * Gets the underlying transport (TCP or IPC) by which connections
     * will be allocated for sending requests and receiving responses.
     * @return quasi http transport
     */
    public QuasiHttpClientTransport getTransport() {
        return transport;
    }

    /**
     * Sets the underlying transport (TCP or IPC) by which connections
     * will be allocated for sending requests and receiving responses.
     * @param transport quasi http transport
     */
    public void setTransport(QuasiHttpClientTransport transport) {
        this.transport = transport;
    }

    /**
     * Sends a quasi http request via quasi http transport.
     * @param remoteEndpoint the destination endpoint of the request
     * @param request the request to send
     * @return the quasi http response returned from the remote endpoint
     * @exception NullPointerException if the request argument is null
     * @exception MissingDependencyException if the transport property is null
     * @throws QuasiHttpException if an error occurs with request processing.
     */
    public QuasiHttpResponse send(Object remoteEndpoint,
            QuasiHttpRequest request) {
        return send(remoteEndpoint, request, null);
    }

    /**
     * Sends a quasi http request via quasi http transport and with
     * send options specified.
     * @param remoteEndpoint the destination endpoint of the request
     * @param request the request to send
     * @param options optional send options
     * @return the quasi http response returned from the remote endpoint
     * @exception NullPointerException if the request argument is null
     * @exception MissingDependencyException if the transport property is null
     * @throws QuasiHttpException if an error occurs with request processing.
     */
    public QuasiHttpResponse send(Object remoteEndpoint,
            QuasiHttpRequest request, QuasiHttpProcessingOptions options)  {
        Objects.requireNonNull(request, "request");
        return sendInternal(remoteEndpoint, request, null, options);
    }

    /**
     * Sends a quasi http request via quasi http transport and makes it
     * posssible to receive connection allocation information before
     * creating request.
     * @param remoteEndpoint the destination endpoint of the request
     * @param requestFunc a callback which receives any environment
     * associated with the connection that is created.
     * @return the quasi http response returned from the remote endpoint.
     * @exception NullPointerException if the requestFunc argument is null
     * @exception MissingDependencyException if the transport property is null
     * @throws QuasiHttpException if an error occurs with request processing.
     */
    public QuasiHttpResponse send2(Object remoteEndpoint,
            RequestGenerator requestFunc) {
        return send2(remoteEndpoint, requestFunc,
            null);
    }

    /**
     * Sends a quasi http request via quasi http transport and makes it
     * posssible to receive connection allocation information before
     * creating request.
     * @param remoteEndpoint the destination endpoint of the request
     * @param requestFunc a callback which receives any environment
     * associated with the connection that is created.
     * @param options optional send options
     * @return the quasi http response returned from the remote endpoint.
     * @exception NullPointerException if the requestFunc argument is null
     * @exception MissingDependencyException if the transport property is null
     * @throws QuasiHttpException if an error occurs with request processing.
     */
    public QuasiHttpResponse send2(Object remoteEndpoint,
            RequestGenerator requestFunc,
            QuasiHttpProcessingOptions options) {
        Objects.requireNonNull(requestFunc, "requestFunc");
        return sendInternal(remoteEndpoint, null, requestFunc,
            options);
    }

    private QuasiHttpResponse sendInternal(Object remoteEndpoint, QuasiHttpRequest request,
            RequestGenerator requestFunc,
            QuasiHttpProcessingOptions sendOptions) {
        // access fields for use per request call, in order to cooperate with
        // any implementation of field accessors which supports
        // concurrent modifications.
        QuasiHttpClientTransport transport = this.transport;

        if (transport == null) {
            throw new MissingDependencyException("client transport");
        }

        QuasiHttpConnection connection = null;
        try {
            connection = transport.allocateConnection(
                remoteEndpoint, sendOptions);
            if (connection == null) {
                throw new QuasiHttpException("no connection");
            }
            CustomTimeoutScheduler timeoutScheduler = connection.getTimeoutScheduler();
            QuasiHttpResponse response;
            if (timeoutScheduler != null) {
                QuasiHttpConnection nonNullConnection = connection;
                Callable<QuasiHttpResponse> proc = () -> processSend(
                    request, requestFunc, transport, nonNullConnection);
                response = ProtocolUtilsInternal.runTimeoutScheduler(
                    timeoutScheduler, true, proc);
            }
            else {
                response = processSend(request, requestFunc,
                    transport, connection);
            }
            
            transport.releaseConnection(connection, response);
            return response;
        }
        catch (Throwable e) {
            if (connection != null) {
                try {
                    transport.releaseConnection(connection, null);
                }
                catch (Throwable ignore) { }
            }
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

    private static QuasiHttpResponse processSend(QuasiHttpRequest request,
            RequestGenerator requestFunc,
            QuasiHttpClientTransport transport,
            QuasiHttpConnection connection)
            throws Exception {
        // wait for connection to be completely established.
        transport.establishConnection(connection);

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
            response.setDisposer(() -> {
                transport.releaseConnection(connection, null);
            });
        }
        return response;
    }
}
