package com.aaronicsubstances.kabomu.abstractions;

@FunctionalInterface
public interface QuasiHttpApplication {
    
    QuasiHttpResponse processRequest(QuasiHttpRequest request) throws Exception;
}
