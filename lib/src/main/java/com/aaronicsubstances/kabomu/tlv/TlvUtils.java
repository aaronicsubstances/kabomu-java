package com.aaronicsubstances.kabomu.tlv;

import java.io.InputStream;
import java.io.OutputStream;

import com.aaronicsubstances.kabomu.MiscUtilsInternal;

/**
 * Provides functions for writing and reading of data in byte chunks
 * formatted int TlV (ie tag-length-value) format.
 */
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

    /**
     * Encodes positive number representing a tag into a
     * 4-byte buffer slice.
     * @param tag positive number
     * @param data destination buffer
     * @param offset starting position in destination buffer
     * @exception IllegalArgumentException if the tag
     * argument is not positive, or the byte bufer slice is invalid.
     */
    public static void encodeTag(int tag, byte[] data, int offset) {
        if (!MiscUtilsInternal.isValidByteBufferSlice(data, offset, 4)) {
            throw new IllegalArgumentException("invalid buffer slice");
        }
        if (tag <= 0) {
            throw new IllegalArgumentException("invalid tag: " + tag);
        }
        MiscUtilsInternal.serializeInt32BE(tag, data, offset);
    }

    /**
     * Encodes non-negative number representing a length into a
     * 4-byte buffer slice.
     * @param length non-negative number
     * @param data destination buffer
     * @param offset starting position in destination buffer
     * @exception IllegalArgumentException if the length
     * argument is negative, or the byte bufer slice is invalid.
     */
    public static void encodeLength(int length, byte[] data, int offset) {
        if (!MiscUtilsInternal.isValidByteBufferSlice(data, offset, 4)) {
            throw new IllegalArgumentException("invalid buffer slice");
        }
        if (length < 0) {
            throw new IllegalArgumentException("invalid tag value length: " +
                length);
        }
        MiscUtilsInternal.serializeInt32BE(length, data, offset);
    }

    /**
     * Decodes a 4-byte buffer slice into a positive number
     * representing a tag.
     * @param data source buffer
     * @param offset starting position in source buffer
     * @return decoded positive number
     * @exception IllegalArgumentException The byte bufer slice is invalid,
     * or the decoded tag is not positive.
     */
    public static int decodeTag(byte[] data, int offset) {
        if (!MiscUtilsInternal.isValidByteBufferSlice(data, offset, 4)) {
            throw new IllegalArgumentException("invalid buffer slice");
        }
        int tag = MiscUtilsInternal.deserializeInt32BE(
            data, offset);
        if (tag <= 0) {
            throw new IllegalArgumentException("invalid tag: " +
                tag);
        }
        return tag;
    }

    /**
     * Decodes a 4-byte buffer slice into a length.
     * @param data source buffer
     * @param offset starting position in source buffer
     * @return decoded length
     * @exception IllegalArgumentException The byte bufer slice is invalid,
     * or the decoded length is negative.
     */
    public static int decodeLength(byte[] data, int offset) {
        if (!MiscUtilsInternal.isValidByteBufferSlice(data, offset, 4)) {
            throw new IllegalArgumentException("invalid buffer slice");
        }
        int decodedLength = MiscUtilsInternal.deserializeInt32BE(
            data, offset);
        if (decodedLength < 0) {
            throw new IllegalArgumentException("invalid tag value length: " +
                decodedLength);
        }
        return decodedLength;
    }

    /**
     * Creates a stream which wraps another stream to
     * ensure that a given amount of bytes are read from it.
     * @param stream the readable stream to read from
     * @param length the expected number of bytes to read from stream
     * argument. Must not be negative.
     * @return stream which enforces a certain length on
     * readable stream argument
     */
    public static InputStream createContentLengthEnforcingStream(InputStream stream,
            long length) {
        return new ContentLengthEnforcingStreamInternal(stream, length);
    }

    /**
     * Creates a stream which wraps another stream, to ensure that
     * the number of bytes read from it does not exceed 128 MB.
     * @param stream the readable stream to read from
     * @return stream which enforces default maximum length on readable
     * stream argument.
     */
    public static InputStream createMaxLengthEnforcingStream(InputStream stream) {
        return createMaxLengthEnforcingStream(stream, 0);
    }

    /**
     * Creates a stream which wraps another stream to ensure that
     * a given amount of bytes are not exceeded when reading from it.
     * @param stream the readable stream to read from
     * @param maxLength the number of bytes beyond which
     * reads will fail. Can be zero, in which case a default of 128MB
     * will be used.
     * @return stream which enforces a maximum length on readable
     * stream argument.
     */
    public static InputStream createMaxLengthEnforcingStream(InputStream stream,
            int maxLength) {
        return new MaxLengthEnforcingStreamInternal(stream, maxLength);
    }

    /**
     * Creates a stream which wraps another stream to decode
     * TLV-encoded byte chunks from it.
     * @param stream the readable stream to read from
     * @param expectedTag the tag of the byte chunks
     * @param tagToIgnore the tag of any optional byte chunk
     * preceding chunks with the expected tag.
     * @return stream which decodes TLV-encoded bytes chunks.
     */
    public static InputStream createTlvDecodingReadableStream(InputStream stream,
            int expectedTag, int tagToIgnore) {
        return new BodyChunkDecodingStreamInternal(stream, expectedTag,
            tagToIgnore);
    }

    /**
     * Creates a stream which wraps another stream to encode byte chunks
     * into it in TLV format.
     * 
     * Note that the stream will only write terminating chunk
     * when it receives negative count directly
     * (ie in write* methods which accept counts).
     * @param stream the writable stream to write to
     * @param tagToUse the tag to use to encode byte chunks
     * @return stream which encodes byte chunks in TLV format
     */
    public static OutputStream createTlvEncodingWritableStream(OutputStream stream,
            int tagToUse) {
        return new BodyChunkEncodingStreamInternal(stream, tagToUse);
    }
}
