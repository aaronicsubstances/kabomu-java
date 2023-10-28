package com.aaronicsubstances.kabomu.abstractions;

import java.util.Map;

/**
 * Provides default implementation of the {@link QuasiHttpProcessingOptions}
 * interface.
 */
public class DefaultQuasiHttpProcessingOptions implements QuasiHttpProcessingOptions {
    private Map<String, Object> extraConnectivityParams;
    private int timeoutMillis;
    private int maxHeadersSize;
    private int maxResponseBodySize;
    public Map<String, Object> getExtraConnectivityParams() {
        return extraConnectivityParams;
    }
    public void setExtraConnectivityParams(Map<String, Object> extraConnectivityParams) {
        this.extraConnectivityParams = extraConnectivityParams;
    }
    public int getTimeoutMillis() {
        return timeoutMillis;
    }
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }
    public int getMaxHeadersSize() {
        return maxHeadersSize;
    }
    public void setMaxHeadersSize(int maxHeadersSize) {
        this.maxHeadersSize = maxHeadersSize;
    }
    public int getMaxResponseBodySize() {
        return maxResponseBodySize;
    }
    public void setMaxResponseBodySize(int maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }

}
