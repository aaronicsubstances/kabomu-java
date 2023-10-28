package com.aaronicsubstances.kabomu.abstractions;

import java.util.Map;

import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;
import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

/**
 * Represens objects needed by
 * {@link QuasiHttpTransport} instances for reading or writing
 * data.
 */
public interface QuasiHttpConnection {

    /**
     * Gets the effective processing options that will be used to
     * limit sizes of headers and response bodies, and configure any
     * other operations by {@link StandardQuasiHttpClient} and
     * {@link StandardQuasiHttpServer} instances.
     */
    QuasiHttpProcessingOptions getProcessingOptions();

    /**
     * Gets an optional function which can be used by
     * {@link StandardQuasiHttpClient} and
     * {@link StandardQuasiHttpServer} instances, to impose
     * timeouts on request processing.
     */
    CustomTimeoutScheduler getTimeoutScheduler();

    /**
     * Gets any environment variables that can control decisions
     * during operations by
     * {@link StandardQuasiHttpClient} and
     * {@link StandardQuasiHttpServer} instances.
     */
    Map<String, Object> getEnvironment();
}
