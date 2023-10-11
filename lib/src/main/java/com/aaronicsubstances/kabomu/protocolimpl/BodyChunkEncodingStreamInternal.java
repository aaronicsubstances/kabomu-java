package com.aaronicsubstances.kabomu.protocolimpl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class BodyChunkEncodingStreamInternal extends FilterOutputStream {
    private final OutputStream backingStream;
    private final byte[] tagToUse;
    private static final byte[] ENCODED_ZERO_LENGTH = new byte[4];

    public BodyChunkEncodingStreamInternal(OutputStream backingStream,
            int tagToUse) {
        super(backingStream);
        Objects.requireNonNull(backingStream, "backingStream");
        this.backingStream = backingStream;
        this.tagToUse = new byte[4];
        TlvUtils.encodeTag(tagToUse, this.tagToUse, 0);
    }

    @Override
    public void write(int b) throws IOException {
        backingStream.write(tagToUse);
        backingStream.write(0);
        backingStream.write(0);
        backingStream.write(0);
        backingStream.write(1);
        backingStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            backingStream.write(tagToUse);
            backingStream.write(ENCODED_ZERO_LENGTH);
        }
        else if (len == 0) {
            backingStream.write(b, off, len);
        }
        else {
            byte[] encodedLen = new byte[4];
            TlvUtils.encodeLength(len, encodedLen, 0);
            backingStream.write(tagToUse);
            backingStream.write(encodedLen);
            backingStream.write(b, off, len);
        }
    }
}
