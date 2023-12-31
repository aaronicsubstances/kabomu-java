package com.aaronicsubstances.kabomu.tlv;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.aaronicsubstances.kabomu.IOUtilsInternal;
import com.aaronicsubstances.kabomu.MiscUtilsInternal;
import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;

class BodyChunkDecodingStreamInternal extends InputStream  {
    private final InputStream backingStream;
    private final int expectedTag;
    private final int tagToIgnore;
    private int _chunkDataLenRem;
    private boolean _lastChunkSeen;

    public BodyChunkDecodingStreamInternal(InputStream backingStream,
            int expectedTag, int tagToIgnore) {
        this.backingStream = Objects.requireNonNull(backingStream,
            "backingStream");
        this.expectedTag = expectedTag;
        this.tagToIgnore = tagToIgnore;
    }

    @Override
    public int read() throws IOException {
        // once empty data chunk is seen, return -1 for all subsequent reads.
        if (_lastChunkSeen) {
            return -1;
        }

        if (_chunkDataLenRem == 0) {
            _chunkDataLenRem = fetchNextTagAndLength();
            if (_chunkDataLenRem == 0) {
                _lastChunkSeen = true;
                return -1;
            }
        }

        int byteRead = backingStream.read();
        if (byteRead < 0) {
            throw KabomuIOException.createEndOfReadError();
        }
        _chunkDataLenRem--;
        return byteRead;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        // once empty data chunk is seen, return -1 for all subsequent reads.
        if (_lastChunkSeen) {
            return -1;
        }

        if (_chunkDataLenRem == 0) {
            _chunkDataLenRem = fetchNextTagAndLength();
            if (_chunkDataLenRem == 0) {
                _lastChunkSeen = true;
                return -1;
            }
        }

        len = Math.min(_chunkDataLenRem, len);
        len = backingStream.read(b, off, len);
        if (len <= 0) {
            throw KabomuIOException.createEndOfReadError();
        }
        _chunkDataLenRem -= len;

        return len;
    }

    private int fetchNextTagAndLength() throws IOException {
        int tag = readTagOnly();
        if (tag == tagToIgnore) {
            readAwayTagValue();
            tag = readTagOnly();
        }
        if (tag != expectedTag) {
            throw new KabomuIOException("unexpected tag: expected " +
                String.format("%s but found %s",
                expectedTag, tag));
        }
        return readLengthOnly();
    }

    private void readAwayTagValue() throws IOException {
        int length = readLengthOnly();
        backingStream.skip(length);
    }

    private int readTagOnly() throws IOException {
        byte[] encodedTag = new byte[4];
        IOUtilsInternal.readBytesFully(backingStream,
            encodedTag, 0, encodedTag.length);
        int tag = MiscUtilsInternal.deserializeInt32BE(
            encodedTag, 0);
        if (tag <= 0) {
            throw new KabomuIOException("invalid tag: " +
                tag);
        }
        return tag;
    }

    private int readLengthOnly() throws IOException {
        byte[] encodedLen = new byte[4];
        IOUtilsInternal.readBytesFully(backingStream,
            encodedLen, 0, encodedLen.length);
        int decodedLength = MiscUtilsInternal.deserializeInt32BE(
            encodedLen, 0);
        if (decodedLength < 0) {
            throw new KabomuIOException("invalid tag value length: " +
                decodedLength);
        }
        return decodedLength;
    }
}
