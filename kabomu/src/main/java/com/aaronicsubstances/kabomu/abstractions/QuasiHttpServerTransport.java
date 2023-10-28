package com.aaronicsubstances.kabomu.abstractions;

import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

/**
 * Equivalent of factory of sockets accepted from a TCP server socket,
 * that provides {@link StandardQuasiHttpServer} instances
 * with server operations for sending quasi http requests to servers at
 * remote endpoints.
 */
public interface QuasiHttpServerTransport extends QuasiHttpTransport {

    /**
     * Releases resources held by a connection of a quasi http transport instance.
     * @param connection the connection to release
     * @throws Exception
     */
    void releaseConnection(QuasiHttpConnection connection) throws Exception;
}
