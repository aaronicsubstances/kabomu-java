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
        while (true) {
            int bytesRead = inputStream.read(
                data, offset, length);
            if (bytesRead > length) {
                throw new ExpectationViolationException(
                    "read beyond requested length: " +
                    String.format("(%s > %s)",
                        bytesRead, length));
            }
            if (bytesRead < length) {
                if (bytesRead <= 0) {
                    throw KabomuIOException.createEndOfReadErrorInternal();
                }
                offset += bytesRead;
                length -= bytesRead;
            }
            else {
                break;
            }
        }
    }

    public static void copy(InputStream src,
            OutputStream dest) throws IOException {
        byte[] buf = new byte[DEFAULT_READ_BUFFER_SIZE];
        int length;
        while ((length = src.read(buf)) != -1) {
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
