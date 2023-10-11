package com.aaronicsubstances.kabomu.examples.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.aaronicsubstances.kabomu.abstractions.CheckedRunnable;
import com.aaronicsubstances.kabomu.abstractions.ConnectionAllocationResponse;
import com.aaronicsubstances.kabomu.abstractions.DefaultConnectionAllocationResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpClientTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;

public class LocalhostTcpClientTransport implements QuasiHttpClientTransport {
    private QuasiHttpProcessingOptions defaultSendOptions;

    public QuasiHttpProcessingOptions getDefaultSendOptions() {
        return defaultSendOptions;
    }

    public void setDefaultSendOptions(QuasiHttpProcessingOptions defaultSendOptions) {
        this.defaultSendOptions = defaultSendOptions;
    }

    @Override
    public ConnectionAllocationResponse allocateConnection(
            Object remoteEndpoint,
            QuasiHttpProcessingOptions sendOptions) throws Exception {
        int port = (int)remoteEndpoint;
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        SocketConnection connection = new SocketConnection(socket, true,
            sendOptions, defaultSendOptions);
        InetAddress hostIp = InetAddress.getByName("::1");
        int connectTimeout = connection.getProcessingOptions().getTimeoutMillis();
        CheckedRunnable connectTask = () -> {
            socket.connect(new InetSocketAddress(hostIp, port),
                connectTimeout);
        };
        DefaultConnectionAllocationResponse result = new DefaultConnectionAllocationResponse();
        result.setConnection(connection);
        result.setConnectTask(connectTask);
        return result;
    }
    
    @Override
    public void releaseConnection(QuasiHttpConnection connection,
            QuasiHttpResponse response) throws Exception {
        ((SocketConnection)connection).release(response);
    }

    @Override
    public InputStream getReadableStream(QuasiHttpConnection connection)
            throws Exception {
        return ((SocketConnection)connection).getInputStream();
    }

    @Override
    public OutputStream getWritableStream(QuasiHttpConnection connection) 
            throws Exception {
        return ((SocketConnection)connection).getOutputStream();
    }
}
