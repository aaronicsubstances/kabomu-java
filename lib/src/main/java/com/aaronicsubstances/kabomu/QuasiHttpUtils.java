package com.aaronicsubstances.kabomu;

import java.util.HashMap;
import java.util.Map;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;

public class QuasiHttpUtils {

    /**
     *  Request environment variable for local server endpoint.
     */
    public static final String ENV_KEY_LOCAL_PEER_ENDPOINT = "kabomu.local_peer_endpoint";

    /**
     *  Request environment variable for remote client endpoint.
     */
    public static final String ENV_KEY_REMOTE_PEER_ENDPOINT = "kabomu.remote_peer_endpoint";

    /**
     * Request environment variable for the transport instance from
     * which a request was received.
     */
    public static final String ENV_KEY_TRANSPORT_INSTANCE = "kabomu.transport";

    /**
     * Request environment variable for the connection from which a
     * request was received.
     */
    public static final String ENV_KEY_CONNECTION = "kabomu.connection";

    public static final String METHOD_CONNECT = "CONNECT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_PATCH = "PATCH";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_TRACE = "TRACE";

    /**
     * 200 OK
     */
    public static final int STATUS_CODE_OK = 200;

    /**
     * 400 Bad Request
     */
    public static final int STATUS_CODE_CLIENT_ERROR_BAD_REQUEST = 400;

    /**
     * 401 Unauthorized
     */
    public static final int STATUS_CODE_CLIENT_ERROR_UNAUTHORIZED = 401;

    /**
     * 403 Forbidden
     */
    public static final int STATUS_CODE_CLIENT_ERROR_FORBIDDEN = 403;

    /**
     * 404 Not Found
     */
    public static final int STATUS_CODE_CLIENT_ERROR_NOT_FOUND = 404;

    /**
     * 405 Method Not Allowed
     */
    public static final int STATUS_CODE_CLIENT_ERROR_METHOD_NOT_ALLOWED = 405;

    /**
     * 413 Payload Too Large
     */
    public static final int STATUS_CODE_CLIENT_ERROR_PAYLOAD_TOO_LARGE = 413;

    /**
     * 414 URI Too Long
     */
    public static final int STATUS_CODE_CLIENT_ERROR_URI_TOO_LONG = 414;

    /**
     * 415 Unsupported Media Type
     */
    public static final int STATUS_CODE_CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE = 415;

    /**
     * 422 Unprocessable Entity
     */
    public static final int STATUS_CODE_CLIENT_ERROR_UNPROCESSABLE_ENTITY = 422;

    /**
     * 429 Too Many Requests
     */
    public static final int STATUS_CODE_CLIENT_ERROR_TOO_MANY_REQUESTS = 429;

    /**
     * 500 Internal Server Error
     */
    public static final int STATUS_CODE_SERVER_ERROR = 500;

    /**
     * The default value of maximum size of headers in a request or response.
     */
    public static final int DEFAULT_MAX_HEADERS_SIZE = 8_192;

    public static QuasiHttpProcessingOptions mergeProcessingOptions(
            QuasiHttpProcessingOptions preferred,
            QuasiHttpProcessingOptions fallback) {
        if (preferred == null || fallback == null) {
            if (preferred != null) {
                return preferred;
            }
            return fallback;
        }
        QuasiHttpProcessingOptions mergedOptions = new DefaultQuasiHttpProcessingOptions();
        mergedOptions.setTimeoutMillis(
            determineEffectiveNonZeroIntegerOption(
                preferred.getTimeoutMillis(),
                fallback.getTimeoutMillis(),
                0));

        mergedOptions.setExtraConnectivityParams(
            determineEffectiveOptions(
                preferred.getExtraConnectivityParams(),
                fallback.getExtraConnectivityParams()));

        mergedOptions.setMaxHeadersSize(
            determineEffectivePositiveIntegerOption(
                preferred.getMaxHeadersSize(),
                fallback.getMaxHeadersSize(),
                0));

        mergedOptions.setMaxResponseBodySize(
            determineEffectiveNonZeroIntegerOption(
                preferred.getMaxResponseBodySize(),
                fallback.getMaxResponseBodySize(),
                0));
        return mergedOptions;
    }

    static int determineEffectiveNonZeroIntegerOption(Integer preferred,
        Integer fallback1, int defaultValue)
    {
        if (preferred != null) {
            int effectiveValue = preferred;
            if (effectiveValue != 0) {
                return effectiveValue;
            }
        }
        if (fallback1 != null) {
            int effectiveValue = fallback1;
            if (effectiveValue != 0) {
                return effectiveValue;
            }
        }
        return defaultValue;
    }

    static int determineEffectivePositiveIntegerOption(Integer preferred,
        Integer fallback1, int defaultValue) {
        if (preferred != null) {
            int effectiveValue = preferred;
            if (effectiveValue > 0) {
                return effectiveValue;
            }
        }
        if (fallback1 != null) {
            int effectiveValue = fallback1;
            if (effectiveValue > 0) {
                return effectiveValue;
            }
        }
        return defaultValue;
    }

    static Map<String, Object> determineEffectiveOptions(
            Map<String, Object> preferred, Map<String, Object> fallback) {
        Map<String, Object> dest = new HashMap<>();
        // since we want preferred options to overwrite fallback options,
        // set fallback options first.
        if (fallback != null) {
            for (Map.Entry<String, Object> item : fallback.entrySet()) {
                dest.put(item.getKey(), item.getValue());
            }
        }
        if (preferred != null) {
            for (Map.Entry<String, Object> item : preferred.entrySet()) {
                dest.put(item.getKey(), item.getValue());
            }
        }
        return dest;
    }

    static boolean determineEffectiveBooleanOption(
            Boolean preferred, Boolean fallback1, boolean defaultValue) {
        if (preferred != null) {
            return preferred;
        }
        if (fallback1 != null) {
            return fallback1;
        }
        return defaultValue;
    }
}
