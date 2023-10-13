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
import com.aaronicsubstances.kabomu.protocolimpl.ProtocolUtilsInternal;

public class StandardQuasiHttpServer {
    private QuasiHttpApplication application;
    private QuasiHttpServerTransport transport;

    public StandardQuasiHttpServer() {
    }

    public QuasiHttpApplication getApplication() {
        return application;
    }
    public void setApplication(QuasiHttpApplication application) {
        this.application = application;
    }
    public QuasiHttpServerTransport getTransport() {
        return transport;
    }
    public void setTransport(QuasiHttpServerTransport transport) {
        this.transport = transport;
    }

    public void acceptConnection(QuasiHttpConnection connection)
            throws Exception {
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
            abort(transport, connection, false);
        }
        catch (Throwable e) {
            abort(transport, connection, true);
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

    private void abort(QuasiHttpServerTransport transport,
            QuasiHttpConnection connection, boolean errorOccured)
            throws Exception {
        if (errorOccured) {
            try {
                transport.releaseConnection(connection);
            }
            catch (Throwable ignore) { }
        }
        else {
            transport.releaseConnection(connection);
        }
    }
}
