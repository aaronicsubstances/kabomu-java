package com.aaronicsubstances.kabomu.examples.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.QuasiHttpUtils;

public class SocketConnection implements QuasiHttpConnection {
    private static final QuasiHttpProcessingOptions defaultProcessingOptions =
        new DefaultQuasiHttpProcessingOptions();

    private final Socket socket;
    private final QuasiHttpProcessingOptions processingOptions;
    private Map<String, Object> environment;

    public SocketConnection(Socket socket, boolean isClient,
            QuasiHttpProcessingOptions processingOptions,
            QuasiHttpProcessingOptions fallbackProcessingOptions) {
        this.socket = Objects.requireNonNull(socket, "socket");
        QuasiHttpProcessingOptions effectiveProcessingOptions =
            QuasiHttpUtils.mergeProcessingOptions(
                processingOptions, fallbackProcessingOptions);
        if (effectiveProcessingOptions == null) {
            effectiveProcessingOptions = defaultProcessingOptions;
        }
        this.processingOptions = effectiveProcessingOptions;
    }

    @Override
    public QuasiHttpProcessingOptions getProcessingOptions() {
        return processingOptions;
    }

    @Override
    public Map<String, Object> getEnvironment() {
        return environment;
    }

    public void release(QuasiHttpResponse response) throws Exception {
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
