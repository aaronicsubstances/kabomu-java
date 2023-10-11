package com.aaronicsubstances.kabomu.examples.shared;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpApplication;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.QuasiHttpUtils;

public class FileReceiver implements QuasiHttpApplication {
    private static final Logger LOG = LoggerFactory.getLogger(FileReceiver.class);
    private final Object remoteEndpoint;
    private final String downloadDirPath;
    private final Random randGen = new Random();

    public FileReceiver(Object remoteEndpoint,
            String downloadDirPath) {
        this.remoteEndpoint = remoteEndpoint;
        this.downloadDirPath = downloadDirPath;
    }

    @Override
    public QuasiHttpResponse processRequest(QuasiHttpRequest request) throws Exception {
        String fileName = request.getHeaders().get("f").get(0);
        fileName = new String(Base64.getDecoder().decode(
            fileName.getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
        fileName = FilenameUtils.getName(fileName);

        Exception transferError = null;
        try {
            // ensure directory exists.
            // just in case remote endpoint contains invalid file path characters...
            String pathForRemoteEndpoint = remoteEndpoint.toString()
                .replaceAll("\\W", "_");
            File directory = new File(downloadDirPath, pathForRemoteEndpoint);
            directory.mkdirs();
            File p = new File(directory, fileName);
            System.out.println("path is: " + p.getAbsolutePath());
            try (OutputStream fileStream = new FileOutputStream(p)) {
                LOG.debug("Starting receipt of file {} from {}...", fileName, remoteEndpoint);
                IOUtils.copy(request.getBody(), fileStream);
            }
        }
        catch (Exception e) {
            transferError = e;
        }

        DefaultQuasiHttpResponse response = new DefaultQuasiHttpResponse();
        String responseBody = null;
        if (transferError == null) {
            LOG.info("File {} received successfully", fileName);
            response.setStatusCode(QuasiHttpUtils.STATUS_CODE_OK);
            if (request.getHeaders().containsKey("echo-body")) {
                responseBody = String.join(",",
                    request.getHeaders().get("echo-body"));
            }
        }
        else {
            LOG.atError().setCause(transferError)
                .log("File {} received with error", fileName);
            response.setStatusCode(QuasiHttpUtils.STATUS_CODE_SERVER_ERROR);
            responseBody = transferError.getMessage();
        }
        if (responseBody != null) {
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            response.setBody(new ByteArrayInputStream(responseBytes));
            response.setContentLength(-1);
            if (randGen.nextDouble() < 0.5) {
                response.setContentLength(responseBytes.length);
            }
        }
        return response;
    }
}
