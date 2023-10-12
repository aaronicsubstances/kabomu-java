package com.aaronicsubstances.kabomu.examples.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler.DefaultTimeoutResult;
import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler.TimeoutResult;
import com.aaronicsubstances.kabomu.QuasiHttpUtils;

public class SocketConnection implements QuasiHttpConnection {
    private static final QuasiHttpProcessingOptions defaultProcessingOptions =
        new DefaultQuasiHttpProcessingOptions();

    private static final TimeoutResult DUMMY_RESULT = new DefaultTimeoutResult(false, null, null);

    private final Socket socket;
    private final Integer clientPort;
    private final QuasiHttpProcessingOptions processingOptions;
    private Map<String, Object> environment;

    private CustomTimeoutScheduler timeoutScheduler;
    private ScheduledFuture<Void> timeoutFuture;
    private AtomicReference<TimeoutResult> goChannel;

    public SocketConnection(Socket socket, Integer clientPort,
            QuasiHttpProcessingOptions processingOptions,
            QuasiHttpProcessingOptions fallbackProcessingOptions) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.clientPort = clientPort;
        QuasiHttpProcessingOptions effectiveProcessingOptions =
            QuasiHttpUtils.mergeProcessingOptions(
                processingOptions, fallbackProcessingOptions);
        if (effectiveProcessingOptions == null) {
            effectiveProcessingOptions = defaultProcessingOptions;
        }
        this.processingOptions = effectiveProcessingOptions;
    }

    public void setTimeoutScheduler(
            ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService) {
        if (scheduledExecutorService == null || executorService == null) {
            return;
        }
        int timeoutMillis = processingOptions.getTimeoutMillis();
        if (timeoutMillis <= 0) {
            return;
        }
        // set goChannel and timeoutFuture variables before multithreding starts.
        AtomicReference<TimeoutResult> goChannel = new AtomicReference<>();
        this.goChannel = goChannel;
        timeoutFuture = scheduledExecutorService.schedule(() -> {
            TimeoutResult timeoutResult = new DefaultTimeoutResult(true, null, null);
            goChannel.compareAndSet(null, timeoutResult);
            synchronized (goChannel) {
                goChannel.notifyAll();
            }
            return (Void)null;
        }, timeoutMillis, TimeUnit.MILLISECONDS);
        timeoutScheduler = new CustomTimeoutScheduler() {
            @Override
            public TimeoutResult apply(Callable<QuasiHttpResponse> proc)
                    throws Exception {
                Future<?> procFutureResult = executorService.submit(() -> {
                    try {
                        QuasiHttpResponse res = proc.call();
                        TimeoutResult successResult = new DefaultTimeoutResult(false, res, null);
                        goChannel.compareAndSet(null, successResult);
                    }
                    catch (Throwable e) {
                        TimeoutResult errorResult = new DefaultTimeoutResult(false, null, e);
                        goChannel.compareAndSet(null, errorResult);
                    }
                    synchronized (goChannel) {
                        goChannel.notifyAll();
                    }
                });

                // let thread wait for one of success, error, timeout
                // or release via DUMMY constant.
                TimeoutResult result;
                while ((result = goChannel.get()) == null) {
                    synchronized (goChannel) {
                        try {
                            goChannel.wait();
                        }
                        catch (InterruptedException ignore) {}
                    }
                }
                if (result == DUMMY_RESULT) {
                    procFutureResult.cancel(true);
                }
                return result;
            }
        };
    }

    public Socket getSocket() {
        return socket;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    @Override
    public QuasiHttpProcessingOptions getProcessingOptions() {
        return processingOptions;
    }

    @Override
    public CustomTimeoutScheduler getTimeoutScheduler() {
        return timeoutScheduler;
    }

    @Override
    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public void release(QuasiHttpResponse response) throws Exception {
        ScheduledFuture<Void> timeoutFuture = this.timeoutFuture;
        if (timeoutFuture != null) {
            timeoutFuture.cancel(true);
        }
        AtomicReference<TimeoutResult> goChannel = this.goChannel;
        if (goChannel != null) {
            goChannel.compareAndSet(null, DUMMY_RESULT);
            synchronized (goChannel) {
                goChannel.notifyAll();
            }
        }
        if (response != null && response.getBody() != null) {
            return;
        }
        socket.close();
    }

    public InputStream getInputStream() throws Exception {
        return socket.getInputStream();
    }

    public OutputStream getOutputStream() throws Exception {
        return socket.getOutputStream();
    }
}
