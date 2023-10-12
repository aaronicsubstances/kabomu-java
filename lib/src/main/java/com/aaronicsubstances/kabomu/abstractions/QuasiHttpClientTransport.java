package com.aaronicsubstances.kabomu.abstractions;

public interface QuasiHttpClientTransport extends QuasiHttpTransport {
    QuasiHttpConnection allocateConnection(
        Object remoteEndpoint, QuasiHttpProcessingOptions sendOptions) throws Exception;
    void establishConnection(QuasiHttpConnection connection) throws Exception;
    void releaseConnection(QuasiHttpConnection connection,
        QuasiHttpResponse response) throws Exception;
}
