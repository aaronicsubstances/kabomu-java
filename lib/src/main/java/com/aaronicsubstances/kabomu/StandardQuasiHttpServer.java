package com.aaronicsubstances.kabomu;

import java.util.Objects;
import java.util.concurrent.Callable;

import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpApplication;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpServerTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport.DeserializerFunction;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpAltTransport.SerializerFunction;
import com.aaronicsubstances.kabomu.exceptions.MissingDependencyException;
import com.aaronicsubstances.kabomu.exceptions.QuasiHttpException;

/**
 * The standard implementation of the server side of the quasi http protocol
 * defined by the Kabomu library.
 *
 * This class provides the server facing side of networking for end users.
 * It is the complement to the {@link StandardQuasiHttpClient} class for
 * providing HTTP semantics whiles enabling underlying transport options
 * beyond TCP.
 *
 * Therefore this class can be seen as the equivalent of an HTTP server
 * in which the underlying transport of
 * choice extends beyond TCP to include IPC mechanisms.
 */
public class StandardQuasiHttpServer {
    private QuasiHttpApplication application;
    private QuasiHttpServerTransport transport;

    /**
     * Creates a new instance.
     */
    public StandardQuasiHttpServer() {
    }

    /**
     * Gets the function which is
     * responsible for processing requests to generate responses.
     * @return quasi http application
     */
    public QuasiHttpApplication getApplication() {
        return application;
    }

    /**
     * Sets the function which is
     * responsible for processing requests to generate responses.
     * @param application quasi http application
     */
    public void setApplication(QuasiHttpApplication application) {
        this.application = application;
    }

    /**
     * Gets the underlying transport (TCP or IPC) for retrieving requests
     * for quasi web applications, and for sending responses generated from
     * quasi web applications.
     * @return quasi http transport
     */
    public QuasiHttpServerTransport getTransport() {
        return transport;
    }

    /**
     * Sets the underlying transport (TCP or IPC) for retrieving requests
     * for quasi web applications, and for sending responses generated from
     * quasi web applications.
     * @param transport quasi http transport
     */
    public void setTransport(QuasiHttpServerTransport transport) {
        this.transport = transport;
    }

    /**
     * Used to process incoming connections from quasi http server transports.
     * @param connection represents a quasi http connection
     * @exception NullPointerException if the connection argument is null
     * @exception MissingDependencyException if the transport property
     * or the application property is null.
     * @throws QuasiHttpException if an error occurs with request processing.
     */
    public void acceptConnection(QuasiHttpConnection connection) {
        Objects.requireNonNull(connection, "connection");

        // access fields for use per processing call, in order to cooperate with
        // any implementation of field accessors which supports
        // concurrent modifications.
        QuasiHttpServerTransport transport = this.transport;
        QuasiHttpApplication application = this.application;
        if (transport == null) {
            throw new MissingDependencyException("server transport");
        }
        if (application == null) {
            throw new MissingDependencyException("server application");
        }

        try {
            CustomTimeoutScheduler timeoutScheduler = connection.getTimeoutScheduler();
            if (timeoutScheduler != null) {
                Callable<QuasiHttpResponse> proc = () -> processAccept(
                    application, transport, connection);
                ProtocolUtilsInternal.runTimeoutScheduler(
                    timeoutScheduler, false, proc);
            }
            else {
                processAccept(application, transport,
                    connection);
            }
            transport.releaseConnection(connection);
        }
        catch (Throwable e) {
            try {
                transport.releaseConnection(connection);
            }
            catch (Throwable ignore) { }
            if (e instanceof QuasiHttpException)
            {
                throw (QuasiHttpException)e;
            }
            QuasiHttpException abortError = new QuasiHttpException(
                "encountered error during receive request processing",
                QuasiHttpException.REASON_CODE_GENERAL,
                e);
            throw abortError;
        }
    }

    private QuasiHttpResponse processAccept(QuasiHttpApplication application,
            QuasiHttpServerTransport transport,
            QuasiHttpConnection connection) throws Exception {
        QuasiHttpAltTransport altTransport = null;
        DeserializerFunction<QuasiHttpRequest> requestDeserializer = null;
        if (transport instanceof QuasiHttpAltTransport) {
            altTransport = (QuasiHttpAltTransport)transport;
            requestDeserializer = altTransport.getRequestDeserializer();
        }
        QuasiHttpRequest request = null;
        if (requestDeserializer != null) {
            request = requestDeserializer.apply(connection);
        }
        if (request == null) {
            request = (QuasiHttpRequest)ProtocolUtilsInternal.readEntityFromTransport(
                false, transport.getReadableStream(connection), connection);
        }

        try (QuasiHttpResponse response = application.processRequest(request)) {
            if (response == null) {
                throw new QuasiHttpException("no response");
            }

            boolean responseSerialized = false;
            SerializerFunction<QuasiHttpResponse> responseSerializer = null;
            if (altTransport != null) {
                responseSerializer = altTransport.getResponseSerializer();
            }
            if (responseSerializer != null) {
                responseSerialized = responseSerializer.apply(connection, response);
            }
            if (!responseSerialized) {
                ProtocolUtilsInternal.writeEntityToTransport(
                    true, response, transport.getWritableStream(connection),
                    connection);
            }
        }
        return null;
    }
}
