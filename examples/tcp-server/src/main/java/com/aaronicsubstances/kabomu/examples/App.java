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
import com.aaronicsubstances.kabomu.StandardQuasiHttpServer;

import com.aaronicsubstances.kabomu.examples.shared.FileReceiver;
import com.aaronicsubstances.kabomu.examples.shared.LocalhostTcpServerTransport;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        try (InputStream propStream = new FileInputStream("application.xml")) {
            props.loadFromXML(propStream);
        }
        catch (FileNotFoundException ignore) {}

        int port = 5001;
        String uploadDirPath = "logs/server";
        int secsToWaitBeforeShutdown = -1;
        String portProp = props.getProperty("port");
        if (portProp != null && !portProp.isEmpty()) {
            port = Integer.parseInt(portProp);
        }
        String saveDirProp = props.getProperty("save.dir");
        if (saveDirProp != null && !saveDirProp.isEmpty()) {
            uploadDirPath = saveDirProp;
        }
        String waitSecsProp = props.getProperty("stop.time.wait.secs");
        if (waitSecsProp != null && !waitSecsProp.isEmpty()) {
            secsToWaitBeforeShutdown = Integer.parseInt(waitSecsProp);
        }
  
        FileReceiver application = new FileReceiver(port, uploadDirPath);
        StandardQuasiHttpServer instance = new StandardQuasiHttpServer();
        instance.setApplication(application);

        DefaultQuasiHttpProcessingOptions defaultProcessingOptions = new DefaultQuasiHttpProcessingOptions();
        defaultProcessingOptions.setTimeoutMillis(5_000);

        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        LocalhostTcpServerTransport transport = new LocalhostTcpServerTransport(port,
            scheduledExecutorService, null);
        transport.setQuasiHttpServer(instance);
        transport.setDefaultProcessingOptions(defaultProcessingOptions);

        instance.setTransport(transport);
        Object waitObj = new Object();
        try {
            transport.start();
            LOG.info("Started Tcp.FileServer at {}", port);

            synchronized (waitObj) {
                if (secsToWaitBeforeShutdown > 0) {
                    waitObj.wait(secsToWaitBeforeShutdown * 1000);
                }
                else {
                    waitObj.wait();
                }
            }
        }
        catch (Exception e) {
            LOG.atError().setCause(e).log("Fatal error encountered");
        }
        finally {
            LOG.debug("Stopping Tcp.FileServer...");
            transport.stop();
            scheduledExecutorService.shutdown();
        }
    }
}
