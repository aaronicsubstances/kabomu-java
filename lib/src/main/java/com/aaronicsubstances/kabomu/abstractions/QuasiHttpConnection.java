package com.aaronicsubstances.kabomu.abstractions;

import java.util.Map;

public interface QuasiHttpConnection {
    QuasiHttpProcessingOptions getProcessingOptions();
    Map<String, Object> getEnvironment();
}
