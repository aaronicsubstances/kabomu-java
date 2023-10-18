package com.aaronicsubstances.kabomu;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.aaronicsubstances.kabomu.exceptions.ExpectationViolationException;
import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;

public class IOUtilsInternal {

    /**
     * The default read buffer size. Equal to 8,192 bytes.
     */
    public static final int DEFAULT_READ_BUFFER_SIZE = 8_192;

    public static void readBytesFully(InputStream inputStream,
            byte[] data, int offset, int length) throws IOException {
        // allow zero-byte reads to proceed to touch the
        // stream, rather than just return.
        do {
            int bytesRead = inputStream.read(
                data, offset, length);
            if (bytesRead == -1) {
                throw KabomuIOException.createEndOfReadError();
            }
            if (bytesRead < 0) {
                throw new ExpectationViolationException(
                    "read returned invalid length: " + bytesRead);
            }
            if (bytesRead == 0 && length != 0) {
                throw new ExpectationViolationException(
                    "read returned zero bytes");
            }
            if (bytesRead > length) {
                throw new ExpectationViolationException(
                    "read beyond requested length: " +
                    String.format("(%s > %s)",
                        bytesRead, length));
            }
            offset += bytesRead;
            length -= bytesRead;
        } while (length > 0);
    }

    public static void copy(InputStream src,
            OutputStream dest) throws IOException {
        byte[] buf = new byte[DEFAULT_READ_BUFFER_SIZE];
        int length;
        while ((length = src.read(buf)) != -1) {
            if (length == 0) {
                throw new ExpectationViolationException(
                    "read returned zero bytes");
            }
            if (length < 0) {
                throw new ExpectationViolationException(
                    "read returned invalid length: " + length);
            }
            if (length > buf.length) {
                throw new ExpectationViolationException(
                    "read beyond buffer size: " +
                    String.format("(%s > %s)",
                        length, buf.length));
            }
            dest.write(buf, 0, length);
        }
    }
}
