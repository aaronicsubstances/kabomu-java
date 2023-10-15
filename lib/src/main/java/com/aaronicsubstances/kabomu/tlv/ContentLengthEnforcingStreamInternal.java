package com.aaronicsubstances.kabomu.tlv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;

class ContentLengthEnforcingStreamInternal extends InputStream {
    private final InputStream backingStream;
    private long bytesLeftToRead;

    public ContentLengthEnforcingStreamInternal(InputStream backingStream,
            long contentLength) {
        this.backingStream = Objects.requireNonNull(backingStream,
            "backingStream");
        bytesLeftToRead = contentLength;
    }

    @Override
    public int read() throws IOException {
        int bytesToRead = Math.min((int)bytesLeftToRead, 1);

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
        len = Math.min((int)bytesLeftToRead, len);
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

        // if end of read is encountered, ensure that all
        // requested bytes have been read.
        if (bytesLeftToRead > 0 && bytesRead <= 0) {
            throw KabomuIOException.createEndOfReadError();
        }
    }
}
