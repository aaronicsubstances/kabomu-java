package com.aaronicsubstances.kabomu.examples;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;

import com.aaronicsubstances.kabomu.examples.shared.FileSender;
import com.aaronicsubstances.kabomu.examples.shared.LocalhostTcpClientTransport;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (InputStream propStream = new FileInputStream("application.xml")) {
            props.loadFromXML(propStream);
        }
        catch (FileNotFoundException ignore) {}

        int serverPort = 5001;
        String uploadDirPath = "logs/client";
        String portProp = props.getProperty("server.port");
        if (portProp != null && !portProp.isEmpty()) {
            serverPort = Integer.parseInt(portProp);
        }
        String uploadDirProp = props.getProperty("upload.dir");
        if (uploadDirProp != null && !uploadDirProp.isEmpty()) {
            uploadDirPath = uploadDirProp;
        }
        
        DefaultQuasiHttpProcessingOptions defaultSendOptions = new DefaultQuasiHttpProcessingOptions();
        defaultSendOptions.setTimeoutMillis(5_000);
        
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        LocalhostTcpClientTransport transport = new LocalhostTcpClientTransport(
            scheduledExecutorService, null);
        transport.setDefaultSendOptions(defaultSendOptions);
        StandardQuasiHttpClient instance = new StandardQuasiHttpClient();
        instance.setTransport(transport);
        try {
            LOG.info("Connecting Tcp.FileClient to {}...", serverPort);

            FileSender.startTransferringFiles(instance, serverPort, uploadDirPath);
        }
        catch (Exception e) {
            LOG.atError().setCause(e).log("Fatal error encountered");
        }
        finally {
            scheduledExecutorService.shutdown();
        }
    }
}
