package com.aaronicsubstances.kabomu.abstractions;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface CustomTimeoutScheduler {
    TimeoutResult apply(Callable<QuasiHttpResponse> proc) throws Exception;

    public static interface TimeoutResult {
        boolean isTimeout();
        QuasiHttpResponse getResponse();
        Throwable getError();
    }

    public static class DefaultTimeoutResult implements TimeoutResult {
        private final boolean timeout;
        private final QuasiHttpResponse response;
        private final Throwable error;
        public DefaultTimeoutResult(boolean timeout,
                QuasiHttpResponse response,
                Throwable error) {
            this.timeout = timeout;
            this.response = response;
            this.error = error;
        }
        public boolean isTimeout() {
            return timeout;
        }
        public QuasiHttpResponse getResponse() {
            return response;
        }
        public Throwable getError() {
            return error;
        }
    }
}
