package com.aaronicsubstances.kabomu.protocolimpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import com.aaronicsubstances.kabomu.CsvUtils;
import com.aaronicsubstances.kabomu.IOUtilsInternal;
import com.aaronicsubstances.kabomu.MiscUtilsInternal;
import com.aaronicsubstances.kabomu.QuasiHttpUtils;
import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.DefaultQuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpConnection;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpProcessingOptions;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpRequest;
import com.aaronicsubstances.kabomu.abstractions.QuasiHttpResponse;
import com.aaronicsubstances.kabomu.abstractions.CustomTimeoutScheduler.TimeoutResult;
import com.aaronicsubstances.kabomu.exceptions.ExpectationViolationException;
import com.aaronicsubstances.kabomu.exceptions.MissingDependencyException;
import com.aaronicsubstances.kabomu.exceptions.QuasiHttpException;

public class ProtocolUtilsInternal {

    public static Boolean getEnvVarAsBoolean(Map<String, Object> environment,
            String key) {
        if (environment != null && environment.containsKey(key)) {
            Object value = environment.get(key);
            if (value instanceof Boolean) {
                return (Boolean)value;
            }
            else if (value != null) {
                return Boolean.parseBoolean((String)value);
            }
        }
        return null;
    }

    public static QuasiHttpResponse runTimeoutScheduler(
        CustomTimeoutScheduler timeoutScheduler, boolean forClient,
        Callable<QuasiHttpResponse> proc) throws Throwable {
        String timeoutMsg = forClient ? "send timeout" : "receive timeout";
        TimeoutResult result = timeoutScheduler.apply(proc);
        if (result != null) {
            Throwable error = result.getError();
            if (error != null) {
                throw error;
            }
            if (result.isTimeout() == true) {
                throw new QuasiHttpException(timeoutMsg,
                    QuasiHttpException.REASON_CODE_TIMEOUT);
            }
        }
        QuasiHttpResponse response = null;
        if (result != null) {
            response = result.getResponse();
        }
        if (forClient && response == null)
        {
            throw new QuasiHttpException(
                "no response from timeout scheduler");
        }
        return response;
    }

    public static void validateHttpHeaderSection(boolean isResponse,
            List<List<String>> csv) {
        if (csv.isEmpty()) {
            throw new ExpectationViolationException(
                "expected csv to contain at least the special header");
        }
        List<String> specialHeader = csv.get(0);
        if (specialHeader.size() != 4) {
            throw new ExpectationViolationException(
                "expected special header to have 4 values " +
                "instead of " + specialHeader.size());
        }
        for (int i = 0; i < specialHeader.size(); i++) {
            String item = specialHeader.get(i);
            if (!containsOnlyPrintableAsciiChars(item, isResponse && i == 2)) {
                throw new QuasiHttpException(
                    "quasi http " +
                    (isResponse ? "status" : "request") +
                    " line field contains spaces, newlines or " +
                    "non-printable ASCII characters: " +
                    item,
                    QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
            }
        }
        for (int i = 1; i < csv.size(); i++) {
            List<String> row = csv.get(i);
            if (row.size() < 2) {
                throw new ExpectationViolationException(
                    "expected row to have at least 2 values " +
                    "instead of " + row.size());
            }
            String headerName = row.get(0);
            if (!containsOnlyHeaderNameChars(headerName)) {
                throw new QuasiHttpException(
                    "quasi http header name contains characters " +
                    "other than hyphen and English alphabets: " +
                    headerName,
                    QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
            }
            for (int j = 1; j < row.size(); j++) {
                String headerValue = row.get(j);
                if (!containsOnlyPrintableAsciiChars(headerValue, true)) {
                    throw new QuasiHttpException(
                        "quasi http header value contains newlines or " +
                        "non-printable ASCII characters: " + headerValue,
                        QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
                }
            }
        }
    }

    public static boolean containsOnlyHeaderNameChars(String v) {
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c >= '0' && c <= '9') {
                // digits
            }
            else if (c >= 'A' && c <= 'Z') {
                // upper case
            }
            else if (c >= 'a' && c <= 'z') {
                // lower case
            }
            else if (c == '-') {
                // hyphen
            }
            else {
                return false;
            }
        }
        return true;
    }

    public static boolean containsOnlyPrintableAsciiChars(String v,
            boolean allowSpace) {
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c < ' ' || c > 126) {
                return false;
            }
            if (!allowSpace && c == ' ') {
                return false;
            }
        }
        return true;
    }

    public static byte[] encodeQuasiHttpHeaders(boolean isResponse,
            List<String> reqOrStatusLine,
            Map<String, List<String>> remainingHeaders) {
        Objects.requireNonNull(reqOrStatusLine, "reqOrStatusLine");
        List<List<String>> csv = new ArrayList<>();
        List<String> specialHeader = new ArrayList<String>();
        for (String v : reqOrStatusLine) {
            specialHeader.add(v != null ? v : "");
        }
        csv.add(specialHeader);
        if (remainingHeaders != null) {
            for (Map.Entry<String, List<String>> header : remainingHeaders.entrySet()) {
                if (header.getKey() == null || header.getKey().isEmpty()) {
                    throw new QuasiHttpException(
                        "quasi http header name cannot be empty",
                        QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
                }
                if (header.getValue() == null || header.getValue().isEmpty()) {
                    continue;
                }
                List<String> headerRow = new ArrayList<String>();
                headerRow.add(header.getKey());
                for (String v : header.getValue()) {
                    if (v == null || v.isEmpty()) {
                        throw new QuasiHttpException(
                            "quasi http header value cannot be empty",
                            QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
                    }
                    headerRow.add(v);
                }
                csv.add(headerRow);
            }
        }

        validateHttpHeaderSection(isResponse, csv);

        byte[] serialized = MiscUtilsInternal.stringToBytes(
            CsvUtils.serialize(csv));

        return serialized;
    }

    public static List<String> decodeQuasiHttpHeaders(boolean isResponse,
            byte[] data, int offset, int length,
            Map<String, List<String>> headersReceiver) {
        List<List<String>> csv;
        try {
            csv = CsvUtils.deserialize(MiscUtilsInternal.bytesToString(
                data, offset, length));
        }
        catch (Exception e) {
            throw new QuasiHttpException(
                "invalid quasi http headers",
                QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION,
                e);
        }
        if (csv.isEmpty()) {
            throw new QuasiHttpException(
                "invalid quasi http headers",
                QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
        }
        List<String> specialHeader = csv.get(0);
        if (specialHeader.size() < 4) {
            throw new QuasiHttpException(
                "invalid quasi http " +
                (isResponse ? "status" : "request") +
                " line",
                QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
        }

        // merge headers with the same normalized name in different rows.
        for (int i = 1; i < csv.size(); i++) {
            List<String> headerRow = csv.get(i);
            if (headerRow.size() < 2) {
                continue;
            }
            String headerName = headerRow.get(0).toLowerCase();
            if (!headersReceiver.containsKey(headerName)) {
                headersReceiver.put(headerName, new ArrayList<String>());
            }
            List<String> headerValues = headersReceiver.get(headerName);
            headerRow.stream().skip(1).forEachOrdered(headerValue -> {
                headerValues.add(headerValue);
            });
        }

        return specialHeader;
    }

    public static void writeQuasiHttpHeaders(
            boolean isResponse,
            OutputStream dest,
            List<String> reqOrStatusLine,
            Map<String, List<String>> remainingHeaders,
            int maxHeadersSize) throws IOException {
        byte[] encodedHeaders = encodeQuasiHttpHeaders(isResponse,
            reqOrStatusLine, remainingHeaders);
        if (maxHeadersSize <= 0) {
            maxHeadersSize = QuasiHttpUtils.DEFAULT_MAX_HEADERS_SIZE;
        }

        // finally check that byte count of csv doesn't exceed limit.
        if (encodedHeaders.length > maxHeadersSize) {
            throw new QuasiHttpException("quasi http headers exceed " +
                "max size (" + encodedHeaders.length + " >  " +
                maxHeadersSize + ")",
                QuasiHttpException.REASON_CODE_MESSAGE_LENGTH_LIMIT_EXCEEDED);
        }
        byte[] tagAndLen = new byte[8];
        TlvUtils.encodeTag(TlvUtils.TAG_FOR_QUASI_HTTP_HEADERS, tagAndLen, 0);
        TlvUtils.encodeLength(encodedHeaders.length, tagAndLen, 4);
        dest.write(tagAndLen);
        dest.write(encodedHeaders);
    }

    public static List<String> readQuasiHttpHeaders(
            boolean isResponse,
            InputStream src,
            Map<String, List<String>> headersReceiver,
            int maxHeadersSize) throws IOException {
        byte[] tagOrLen = new byte[4];
        IOUtilsInternal.readBytesFully(src, tagOrLen, 0,
            tagOrLen.length);
        int tag = TlvUtils.decodeTag(tagOrLen, 0);
        if (tag != TlvUtils.TAG_FOR_QUASI_HTTP_HEADERS)
        {
            throw new QuasiHttpException(
                "unexpected quasi http headers tag: " + tag,
                QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION);
        }
        IOUtilsInternal.readBytesFully(src, tagOrLen, 0,
            tagOrLen.length);
        int headersSize = TlvUtils.decodeLength(tagOrLen, 0);
        if (maxHeadersSize <= 0) {
            maxHeadersSize = QuasiHttpUtils.DEFAULT_MAX_HEADERS_SIZE;
        }
        if (headersSize > maxHeadersSize) {
            throw new QuasiHttpException("quasi http headers exceed " +
                "max size ( " + headersSize + " > " +
                maxHeadersSize + ")",
                QuasiHttpException.REASON_CODE_MESSAGE_LENGTH_LIMIT_EXCEEDED);
        }
        byte[] encodedHeaders = new byte[headersSize];
        IOUtilsInternal.readBytesFully(src,
            encodedHeaders, 0, encodedHeaders.length);
        return decodeQuasiHttpHeaders(isResponse,
            encodedHeaders, 0, encodedHeaders.length,
            headersReceiver);
    }

    public static void writeEntityToTransport(boolean isResponse,
            Object entity, OutputStream writableStream,
            QuasiHttpConnection connection) throws IOException {
        if (writableStream == null) {
            throw new MissingDependencyException(
                "no writable stream found for transport");
        }
        InputStream body;
        long contentLength;
        List<String> reqOrStatusLine;
        Map<String, List<String>> headers;
        if (isResponse) {
            QuasiHttpResponse response = (QuasiHttpResponse)entity;
            headers = response.getHeaders();
            body = response.getBody();
            contentLength = response.getContentLength();
            reqOrStatusLine = Arrays.asList(
                response.getHttpVersion(),
                response.getStatusCode() + "",
                response.getHttpStatusMessage(),
                contentLength + ""
            );
        }
        else {
            QuasiHttpRequest request = (QuasiHttpRequest)entity;
            headers = request.getHeaders();
            body = request.getBody();
            contentLength = request.getContentLength();
            reqOrStatusLine = Arrays.asList(
                request.getHttpMethod(),
                request.getTarget(),
                request.getHttpVersion(),
                contentLength + ""
            );
        }
        // treat content lengths totally separate from body.
        // This caters for the HEAD method
        // which can be used to return a content length without a body
        // to download.
        int maxHeadersSize = 0;
        QuasiHttpProcessingOptions processingOptions = connection.getProcessingOptions();
        if (processingOptions != null) {
            maxHeadersSize = processingOptions.getMaxHeadersSize();
        }
        writeQuasiHttpHeaders(isResponse, writableStream,
            reqOrStatusLine, headers, maxHeadersSize);
        if (body == null) {
            // don't proceed, even if content length is not zero.
            return;
        }
        if (contentLength > 0) {
            // don't enforce positive content lengths when writing out
            // quasi http bodies
            IOUtilsInternal.copy(body, writableStream);
        }
        else {
            // proceed, even if content length is 0.
            OutputStream bodyWriter = TlvUtils.createTlvEncodingWritableStream(
                writableStream, TlvUtils.TAG_FOR_QUASI_HTTP_BODY_CHUNK);
            IOUtilsInternal.copy(body, bodyWriter);
            // write end of stream
            bodyWriter.write(null, 0, -1);
        }
    }

    public static Object readEntityFromTransport(
            boolean isResponse, InputStream readableStream,
            QuasiHttpConnection connection) throws IOException {
        if (readableStream == null) {
            throw new MissingDependencyException(
                "no readable stream found for transport");
        }
        Map<String, List<String>> headersReceiver = new HashMap<>();
        int maxHeadersSize = 0;
        QuasiHttpProcessingOptions processingOptions = connection.getProcessingOptions();
        if (processingOptions != null) {
            maxHeadersSize = processingOptions.getMaxHeadersSize();
        }
        List<String> reqOrStatusLine = readQuasiHttpHeaders(
            isResponse,
            readableStream,
            headersReceiver,
            maxHeadersSize);

        long contentLength;
        try {
            contentLength = MiscUtilsInternal.parseInt48(
                reqOrStatusLine.get(3));
        }
        catch (Exception e) {
            throw new QuasiHttpException(
                "invalid quasi http " +
                (isResponse ? "response" : "request") +
                " content length",
                QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION,
                e);
        }
        InputStream body = null;
        if (contentLength != 0) {
            if (contentLength > 0) {
                body = TlvUtils.createContentLengthEnforcingStream(
                    readableStream, contentLength);
            }
            else {
                body = TlvUtils.createTlvDecodingReadableStream(readableStream,
                    TlvUtils.TAG_FOR_QUASI_HTTP_BODY_CHUNK,
                    TlvUtils.TAG_FOR_QUASI_HTTP_BODY_CHUNK_EXT);
            }
        }
        if (isResponse) {
            QuasiHttpResponse response = new DefaultQuasiHttpResponse();
            response.setHttpVersion(reqOrStatusLine.get(0));
            try {
                response.setStatusCode(MiscUtilsInternal.parseInt32(
                    reqOrStatusLine.get(1)));
            }
            catch (Exception e) {
                throw new QuasiHttpException(
                    "invalid quasi http response status code",
                    QuasiHttpException.REASON_CODE_PROTOCOL_VIOLATION,
                    e);
            }
            response.setHttpStatusMessage(reqOrStatusLine.get(2));
            response.setContentLength(contentLength);
            response.setHeaders(headersReceiver);
            if (body != null) {
                int bodySizeLimit = 0;
                processingOptions = connection.getProcessingOptions();
                if (processingOptions != null) {
                    bodySizeLimit = processingOptions.getMaxResponseBodySize();
                }
                if (bodySizeLimit >= 0) {
                    body = TlvUtils.createMaxLengthEnforcingStream(body,
                        bodySizeLimit);
                }
                // can't implement response buffering, because of
                // the HEAD method, with which a content length may
                // be given but without a body to download.
            }
            response.setBody(body);
            return response;
        }
        else {
            QuasiHttpRequest request = new DefaultQuasiHttpRequest();
            request.setEnvironment(connection.getEnvironment());
            request.setHttpMethod(reqOrStatusLine.get(0));
            request.setTarget(reqOrStatusLine.get(1));
            request.setHttpVersion(reqOrStatusLine.get(2));
            request.setContentLength(contentLength);
            request.setHeaders(headersReceiver);
            request.setBody(body);
            return request;
        }
    }
}
