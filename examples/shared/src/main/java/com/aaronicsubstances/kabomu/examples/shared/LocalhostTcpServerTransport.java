package com.aaronicsubstances.kabomu.examples.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpServerTransport;
import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

public class LocalhostTcpServerTransport implements QuasiHttpServerTransport {
    private static final Logger LOG = LoggerFactory.getLogger(LocalhostTcpServerTransport.class);
    private final int port;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ExecutorService executorService;
    private final ServerSocket tcpServer;
    private StandardQuasiHttpServer quasiHttpServer;
    private QuasiHttpProcessingOptions defaultProcessingOptions;

    public LocalhostTcpServerTransport(int port,
            ScheduledExecutorService scheduledExecutorService,
            ExecutorService executorService) throws Exception {
        tcpServer = new ServerSocket();
        this.port = port;
        this.scheduledExecutorService = scheduledExecutorService;
        this.executorService = executorService != null ?
            executorService : ForkJoinPool.commonPool();
    }

    public StandardQuasiHttpServer getQuasiHttpServer() {
        return quasiHttpServer;
    }

    public void setQuasiHttpServer(StandardQuasiHttpServer quasiHttpServer) {
        this.quasiHttpServer = quasiHttpServer;
    }

    public QuasiHttpProcessingOptions getDefaultProcessingOptions() {
        return defaultProcessingOptions;
    }

    public void setDefaultProcessingOptions(QuasiHttpProcessingOptions defaultProcessingOptions) {
        this.defaultProcessingOptions = defaultProcessingOptions;
    }

    public void start() throws Exception {
        InetAddress hostIp = InetAddress.getByName("::1");
        tcpServer.bind(new InetSocketAddress(hostIp, port));
        // don't wait.
        executorService.submit(this::acceptConnections);
    }

    public void stop() throws Exception {
        tcpServer.close();
        Thread.sleep(1_000);
    }

    private void acceptConnections() {
        try {
            while (true) {
                Socket socket = tcpServer.accept();
                // don't wait.
                executorService.submit(() -> {
                    receiveConnection(socket);
                });
            }
        }
        catch (Exception e) {
            if (e instanceof SocketException &&
                    e.getMessage().contains("socket closed")) {
                LOG.info("connection accept ended");
            }
            else {
                LOG.atWarn().setCause(e).log("connection accept error");
            }
        }
    }

    private void receiveConnection(Socket socket) {
        try {
            socket.setTcpNoDelay(true);
            SocketConnection connection = new SocketConnection(socket, null,
                defaultProcessingOptions, null, scheduledExecutorService,
                executorService);
            quasiHttpServer.acceptConnection(connection);
        }
        catch (Exception ex) {
            LOG.atWarn().setCause(ex).log("connection processing error");
        }
    }
    
    @Override
    public void releaseConnection(QuasiHttpConnection connection)
            throws Exception {
        ((SocketConnection)connection).release(null);
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
