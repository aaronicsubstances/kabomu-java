package com.aaronicsubstances.kabomu.protocolimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;

class ContentLengthEnforcingStreamInternal extends InputStream {
    private final InputStream backingStream;
    private long bytesLeftToRead;

    public ContentLengthEnforcingStreamInternal(InputStream backingStream,
            long contentLength) {
        Objects.requireNonNull(backingStream, "backingStream");
        this.backingStream = backingStream;
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
        updateState(bytesToRead, bytesJustRead);
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int bytesToRead = Math.min((int)bytesLeftToRead, len);

        // if bytes to read is zero at this stage and
        // the length requested is zero,
        // go ahead and call backing reader
        // (e.g. so that any error in backing reader can be thrown).
        int bytesJustRead = 0;
        if (bytesToRead > 0 || len == 0) {
            bytesJustRead = backingStream.read(
                b, off, bytesToRead);
        }
        updateState(bytesToRead, bytesJustRead);
        return bytesJustRead;
    }

    private void updateState(int bytesToRead, int bytesJustRead) {
        bytesLeftToRead -= bytesJustRead;

        // if end of read is encountered, ensure that all
        // requested bytes have been read.
        boolean endOfRead = bytesToRead > 0 && bytesJustRead == 0;
        if (endOfRead && bytesLeftToRead > 0) {
            throw KabomuIOException.createEndOfReadErrorInternal();
        }
    }
}
