package com.aaronicsubstances.kabomu.abstractions;

import java.util.Map;

public interface QuasiHttpProcessingOptions {
    Map<String, Object> getExtraConnectivityParams();
    void setExtraConnectivityParams(Map<String, Object> value);
    int getTimeoutMillis();
    void setTimeoutMillis(int value);
    int getMaxHeadersSize();
    void setMaxHeadersSize(int value);
    int getMaxResponseBodySize();
    void setMaxResponseBodySize(int value);
}
