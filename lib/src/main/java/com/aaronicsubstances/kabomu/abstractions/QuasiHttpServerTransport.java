package com.aaronicsubstances.kabomu.abstractions;

public interface QuasiHttpServerTransport extends QuasiHttpTransport {
    void releaseConnection(QuasiHttpConnection connection);
}
