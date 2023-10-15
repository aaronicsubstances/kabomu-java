package com.aaronicsubstances.kabomu.examples.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import com.aaronicsubstances.kabomu.abstractions.QuasiHttpClientTransport;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;

public class LocalhostTcpClientTransport implements QuasiHttpClientTransport {
    private QuasiHttpProcessingOptions defaultSendOptions;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executorService;

    public LocalhostTcpClientTransport(ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService != null ?
            executorService :
            ForkJoinPool.commonPool();
    }

    public QuasiHttpProcessingOptions getDefaultSendOptions() {
        return defaultSendOptions;
    }

    public void setDefaultSendOptions(QuasiHttpProcessingOptions defaultSendOptions) {
        this.defaultSendOptions = defaultSendOptions;
    }

    @Override
    public QuasiHttpConnection allocateConnection(
            Object remoteEndpoint,
            QuasiHttpProcessingOptions sendOptions) throws Exception {
        int port = (int)remoteEndpoint;
        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        SocketConnection connection = new SocketConnection(socket, port,
            sendOptions, defaultSendOptions, scheduledExecutorService,
            executorService);
        return connection;
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

    @Override
    public void establishConnection(QuasiHttpConnection connection) throws Exception {
        SocketConnection socketConnection = (SocketConnection)connection;
        Socket socket = socketConnection.getSocket();
        int port = socketConnection.getClientPort();
        InetAddress hostIp = InetAddress.getByName("::1");
        socket.connect(new InetSocketAddress(hostIp, port));
    }
}
