package com.aaronicsubstances.kabomu.protocolimpl;

import java.io.InputStream;
import java.io.OutputStream;

import com.aaronicsubstances.kabomu.MiscUtilsInternal;

public class TlvUtils {

    /**
     * Tag number for quasi http headers.
     */
    public static final int TAG_FOR_QUASI_HTTP_HEADERS = 0x68647273;

    /**
     * Tag number for quasi http body chunks.
     */
    public static final int TAG_FOR_QUASI_HTTP_BODY_CHUNK = 0x62647461;

    /**
     * Tag number for quasi http body chunk extensions.
     */
    public static final int TAG_FOR_QUASI_HTTP_BODY_CHUNK_EXT = 0x62657874;

    public static void encodeTag(int tag, byte[] data, int offset) {
        if (tag <= 0) {
            throw new IllegalArgumentException("invalid tag: " + tag);
        }
        MiscUtilsInternal.serializeInt32BE(tag, data, offset);
    }

    public static void encodeLength(int length, byte[] data, int offset) {
        if (length < 0) {
            throw new IllegalArgumentException("invalid tag value length: " +
                length);
        }
        MiscUtilsInternal.serializeInt32BE(length, data, offset);
    }

    public static int decodeTag(byte[] data, int offset) {
        int tag = MiscUtilsInternal.deserializeInt32BE(
            data, offset);
        if (tag <= 0) {
            throw new IllegalArgumentException("invalid tag: " +
                tag);
        }
        return tag;
    }

    public static int decodeLength(byte[] data, int offset) {
        int decodedLength = MiscUtilsInternal.deserializeInt32BE(
            data, offset);
        if (decodedLength < 0) {
            throw new IllegalArgumentException("invalid tag value length: " +
                decodedLength);
        }
        return decodedLength;
    }
     
    public static InputStream createContentLengthEnforcingStream(InputStream stream,
            long length) {
        return new ContentLengthEnforcingStreamInternal(stream, length);
    }

    public static InputStream createMaxLengthEnforcingStream(InputStream stream) {
        return createMaxLengthEnforcingStream(stream, 0);
    }

    public static InputStream createMaxLengthEnforcingStream(InputStream stream,
            int maxLength) {
        return new MaxLengthEnforcingStreamInternal(stream, maxLength);
    }

    public static InputStream createTlvDecodingReadableStream(InputStream stream,
            int expectedTag, int tagToIgnore) {
        return new BodyChunkDecodingStreamInternal(stream, expectedTag,
            tagToIgnore);
    }

    public static OutputStream createTlvEncodingWritableStream(OutputStream stream,
            int tagToUse) {
        return new BodyChunkEncodingStreamInternal(stream, tagToUse);
    }
}
