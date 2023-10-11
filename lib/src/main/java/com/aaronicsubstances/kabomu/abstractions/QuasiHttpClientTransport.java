package com.aaronicsubstances.kabomu.abstractions;

public interface QuasiHttpClientTransport extends QuasiHttpTransport {
    ConnectionAllocationResponse allocateConnection(
        Object remoteEndpoint, QuasiHttpProcessingOptions sendOptions) throws Exception;
    void releaseConnection(QuasiHttpConnection connection,
        QuasiHttpResponse response) throws Exception;
}
