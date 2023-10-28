package com.aaronicsubstances.kabomu.abstractions;

import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;

/**
 * Equivalent of TCP client socket factory that provides
 * {@link StandardQuasiHttpClient} instances
 * with client connections for sending quasi http requests
 * to servers at remote endpoints.
 */
public interface QuasiHttpClientTransport extends QuasiHttpTransport {

    /**
     * Creates a connection to a remote endpoint.
     * @param remoteEndpoint the target endpoint of the connection
     * allocation request
     * @param sendOptions any options given to one of the send*() methods of
     * the {@link StandardQuasiHttpClient} class
     * @return a connection to remote endpoint
     * @throws Exception
     */
    QuasiHttpConnection allocateConnection(
        Object remoteEndpoint, QuasiHttpProcessingOptions sendOptions) throws Exception;
    
    /**
     * Activates or establishes a connection created with
     * {@link #allocateConnection(Object, QuasiHttpProcessingOptions)}
     * @param connection connection to establish before use
     * @throws Exception
     */
    void establishConnection(QuasiHttpConnection connection) throws Exception;

    /**
     * Releases resources held by a connection of a quasi http transport instance.
     * @param connection the connection to release
     * @param response an optional response which may still need the connection
     * to some extent
     * @throws Exception
     */
    void releaseConnection(QuasiHttpConnection connection,
        QuasiHttpResponse response) throws Exception;
}
