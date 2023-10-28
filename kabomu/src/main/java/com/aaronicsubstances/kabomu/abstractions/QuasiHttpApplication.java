package com.aaronicsubstances.kabomu.abstractions;

import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

/**
 * Represents a quasi http request processing function used by
 * {@link StandardQuasiHttpServer} instances
 * to generate quasi http responses.
 */
@FunctionalInterface
public interface QuasiHttpApplication {
    
    /**
     * Processes a quasi http request.
     * @param request quasi http request to process
     * @return quasi http response to send back to caller
     * @throws Exception
     */
    QuasiHttpResponse processRequest(QuasiHttpRequest request) throws Exception;
}
