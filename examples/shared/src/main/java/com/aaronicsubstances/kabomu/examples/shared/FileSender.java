package com.aaronicsubstances.kabomu.examples.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.QuasiHttpUtils;
import com.aaronicsubstances.kabomu.StandardQuasiHttpClient;

public class FileSender {
    private static final Logger LOG = LoggerFactory.getLogger(FileSender.class);
    private static final Random RAND_GEN = new Random();

    public static void startTransferringFiles(
            StandardQuasiHttpClient instance, Object serverEndpoint,
            String uploadDirPath) throws Exception {
        File directory = new File(uploadDirPath);
        int count = 0;
        long bytesTransferred = 0L;
        long startTime = new Date().getTime();
        File[] listOfFiles = directory.listFiles();
        if (listOfFiles != null) {
            for (File f : listOfFiles) {
                if (!f.isFile()) {
                    continue;
                }
                LOG.debug("Transferring {}", f.getName());
                transferFile(instance, serverEndpoint, f);
                LOG.debug("Successfully transferred {}", f.getName());
                bytesTransferred += f.length();
                count++;
            }
        }
        double timeTaken = Math.round((new Date().getTime() - startTime) / 10.0) / 100.0;
        double megaBytesTransferred = Math.round(bytesTransferred / (1024.0 * 1024.0) * 100) / 100.0;
        double rate = Math.round(megaBytesTransferred / timeTaken * 100.0) / 100.0;
        LOG.info("Successfully transferred {} bytes ({} MB) worth of data in {} files in {} seconds = {} MB/s",
            bytesTransferred, megaBytesTransferred, count, timeTaken, rate);
    }

    private static void transferFile(StandardQuasiHttpClient instance,
            Object serverEndpoint, File f) throws Exception {
        DefaultQuasiHttpRequest request = new DefaultQuasiHttpRequest();
        request.setHeaders(new HashMap<String, List<String>>());
        String encodedName = new String(Base64.getEncoder().encode(
            f.getName().getBytes(StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8);
        request.getHeaders().put("f", Arrays.asList(encodedName));
        boolean echoBodyOn = RAND_GEN.nextDouble() < 0.5;
        if (echoBodyOn) {
            encodedName = new String(Base64.getEncoder().encode(
                f.getAbsolutePath().getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);
            request.getHeaders().put("echo-body", Arrays.asList(encodedName));
        }

        // add body.
        InputStream fileStream = new FileInputStream(f);
        request.setBody(fileStream);
        request.setContentLength(-1);
        if (RAND_GEN.nextDouble() < 0.5) {
            request.setContentLength(f.length());
        }

        // determine options
        QuasiHttpProcessingOptions sendOptions = null;
        if (RAND_GEN.nextDouble() < 0.5) {
            sendOptions = new DefaultQuasiHttpProcessingOptions();
            sendOptions.setMaxResponseBodySize(-1);
        }

        QuasiHttpResponse res = null;
        try {
            if (RAND_GEN.nextDouble() < 0.5) {
                res = instance.send(serverEndpoint, request,
                    sendOptions);
            }
            else {
                res = instance.send2(serverEndpoint,
                    env -> request, sendOptions);
            }
            if (res.getStatusCode() == QuasiHttpUtils.STATUS_CODE_OK) {
                if (echoBodyOn) {
                    String actualResBody = IOUtils.toString(res.getBody(),
                        StandardCharsets.UTF_8);
                    actualResBody = new String(Base64.getDecoder().decode(
                        actualResBody.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8);
                    if (!actualResBody.equals(f.getAbsolutePath())) {
                        throw new Exception("expected echo body to be " +
                            String.format("%s but got %s", f.getAbsolutePath(),
                                actualResBody));
                    }
                }
                LOG.info("File {} sent successfully", f.getAbsolutePath());
            }
            else {
                String responseMsg = "";
                if (res.getBody() != null) {
                    try {
                        responseMsg = IOUtils.toString(res.getBody(),
                            StandardCharsets.UTF_8);
                    }
                    catch (Exception ignore) {}
                }
                throw new Exception(String.format(
                    "status code indicates error: %s\n%s",
                        res.getStatusCode(), responseMsg));
            }
        }
        catch (Exception e) {
            LOG.warn("File {} sent with error: {}", f.getAbsolutePath(),
                e.getMessage());
            throw e;
        }
        finally {
            fileStream.close();
            if (res != null) {
                res.close();
            }
        }
    }
}
