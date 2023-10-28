package com.aaronicsubstances.kabomu.abstractions;

import java.util.Map;

/**
 * Used to configure parameters which affect processing of quasi http requests
 * and responses.
 */
public interface QuasiHttpProcessingOptions {

    /**
     * Gets any extra information which can help a transport to locate a communication endpoint.
     */
    Map<String, Object> getExtraConnectivityParams();

    void setExtraConnectivityParams(Map<String, Object> value);

    /**
     * Gets the wait time period in milliseconds for a send request to succeed. To indicate
     * forever wait or infinite timeout, use -1 or any negative value.
     */
    int getTimeoutMillis();

    void setTimeoutMillis(int value);

    /**
     * Gets the value that imposes a maximum size on the headers of requests and
     * responses which will be encountered during sending out requests and
     * receipt of responses.
     * 
     * Note that zero and negative values will be interpreted as unspecified,
     * and in the absence of any overriding options
     * a client-specific default value will be used.
     */
    int getMaxHeadersSize();

    void setMaxHeadersSize(int value);

    /**
     * Gets the value that imposes a maximum size on response bodies. To indicate absence
     * of a limit, use -1 or any negative value.
     * 
     * Note that zero will be interpreted as unspecified,
     * and in the absence of any overriding options
     * a client-specific default value will be used.
     */
    int getMaxResponseBodySize();

    void setMaxResponseBodySize(int value);
}
