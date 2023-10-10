package com.aaronicsubstances.kabomu.abstractions;

public interface QuasiHttpClientTransport extends QuasiHttpTransport {
    ConnectionAllocationResponse allocateConnection(
        Object remoteEndpoint, QuasiHttpProcessingOptions sendOptions);
    void releaseConnection(QuasiHttpConnection connection,
        QuasiHttpResponse response);
}
