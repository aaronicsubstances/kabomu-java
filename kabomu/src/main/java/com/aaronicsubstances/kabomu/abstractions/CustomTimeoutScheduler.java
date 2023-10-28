package com.aaronicsubstances.kabomu.abstractions;

import java.util.concurrent.Callable;

import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;
import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

/**
 * Represents timeout API for instances of
 * {@link StandardQuasiHttpClient} and {@link StandardQuasiHttpServer}
 * classes to impose timeouts on request processing.
 */
@FunctionalInterface
public interface CustomTimeoutScheduler {

    /**
     * Applies timeout to request processing.
     * @param proc the procedure to run under timeout
     * @return a result indicating whether a timeout occurred,
     * and gives the return value of the function argument.
     * @throws Exception
     */
    TimeoutResult apply(Callable<QuasiHttpResponse> proc) throws Exception;

    /**
     * Represents result of using timeout API as represented by
     * {@link CustomTimeoutScheduler} instances.
     */
    public static interface TimeoutResult {

        /**
         * Returns true or false depending on whether a timeout occurred
         * or not respectively.
         */
        boolean isTimeout();

        /**
         * Gets the value returned by the function argument to the
         * timeout API represented by an instance of the
         * {@link CustomTimeoutScheduler} class.
         */
        QuasiHttpResponse getResponse();

        /**
         * Gets any error which was thrown by function argument to the
         * timeout API represented by an instance of the
         * {@link CustomTimeoutScheduler} class.
         */
        Throwable getError();
    }

    /**
     * Provides default implementation of the
     * {@link TimeoutResult} interface, in which properties are mutable.
     */
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
