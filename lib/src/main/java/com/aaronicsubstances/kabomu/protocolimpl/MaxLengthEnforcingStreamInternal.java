package com.aaronicsubstances.kabomu.protocolimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;

public class MaxLengthEnforcingStreamInternal extends InputStream {
    private static final int DEFAULT_MAX_LENGTH = 134_217_728;

    private final InputStream backingStream;
    private final int maxLength;
    private int bytesLeftToRead;

    public MaxLengthEnforcingStreamInternal(InputStream backingStream,
            int maxLength) {
        Objects.requireNonNull(backingStream, "backingStream");
        if (maxLength == 0) {
            maxLength = DEFAULT_MAX_LENGTH;
        }
        else if (maxLength <= 0) {
            throw new IllegalArgumentException(
                "max length cannot be negative: " + maxLength);
        }
        this.backingStream = backingStream;
        this.maxLength = maxLength;
        bytesLeftToRead = maxLength + 1; // check for excess read.
    }

    @Override
    public int read() throws IOException {
        int bytesToRead = Math.min(bytesLeftToRead, 1);

        int byteRead = -1;
        int bytesJustRead = 0;
        if (bytesToRead > 0) {
            byteRead = backingStream.read();
            bytesJustRead = byteRead >= 0 ? 1 : 0;
        }
        updateState(bytesJustRead);
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        len = Math.min(bytesLeftToRead, len);
        if (len != 0) {
            len = backingStream.read(b, off, len);
        }
        updateState(len);
        return len <= 0 ? -1 : len;
    }

    private void updateState(int bytesRead) {
        if (bytesRead > 0) {
            bytesLeftToRead -= bytesRead;
        }
        if (bytesLeftToRead == 0) {
            throw new KabomuIOException(String.format(
                "stream size exceeds limit of %s bytes",
                maxLength));
        }
    }
}
