package com.aaronicsubstances.kabomu.tlv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.aaronicsubstances.kabomu.shared.RandomizedReadInputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TlvUtilsTest {
    
    @ParameterizedTest
    @MethodSource("createTestEncodeTagData")
    void testEncodeTag(int tag, byte[] dest, int offset,
            byte[] expected) {
        byte[] actual = new byte[dest.length];
        System.arraycopy(dest, 0, actual, 0, dest.length);
        TlvUtils.encodeTag(tag, actual, offset);
        assertArrayEquals(expected, actual);
    }

    static Stream<Arguments> createTestEncodeTagData() {
        return Stream.of(
            Arguments.of(
                0x15c0,
                new byte[]{ 1, 1, 1, 2, 2, 2, 3 },
                2,
                new byte[] { 1, 1, 0, 0, 0x15, (byte)0xc0, 3 }
            ),
            Arguments.of(
                0x12342143,
                    new byte[5],
                    0,
                    new byte[] { 0x12, 0x34, 0x21, 0x43, 0 }
            ),
            Arguments.of(
                1,
                new byte[]{ 3, 2, 4, 5, (byte)187, 9 },
                1,
                new byte[] { 3, 0, 0, 0, 1, 9 }
            )
        );
    }

    @Test
    void testEncodeTagForErrors() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.encodeTag(10, new byte[4], 1);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.encodeTag(-1, new byte[5], 1);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.encodeTag(0, new byte[4], 0);
        });
    }
    
    @ParameterizedTest
    @MethodSource("createTestEncodeLengthData")
    void testEncodeLength(int tag, byte[] dest, int offset,
            byte[] expected) {
        byte[] actual = new byte[dest.length];
        System.arraycopy(dest, 0, actual, 0, dest.length);
        TlvUtils.encodeLength(tag, actual, offset);
        assertArrayEquals(expected, actual);
    }

    static Stream<Arguments> createTestEncodeLengthData() {
        return Stream.of(
            Arguments.of(
                0x34,
                new byte[4],
                0,
                new byte[] { 0, 0, 0, 0x34 }
            ),
            Arguments.of(
                0,
                new byte[]{ 2, 3, 2, 3, 4 },
                1,
                new byte[] { 2, 0, 0, 0, 0 }
            ),
            Arguments.of(
                0x78cdef01,
                new byte[] { 0, 0, 0, 1,
                    0x78, (byte)0xcd, (byte)0xef, 1 },
                2,
                new byte[] { 0, 0, 0x78, (byte)0xcd,
                    (byte)0xef, 1, (byte)0xef, 1 }
            )
        );
    }

    @Test
    void testEncodeLengthForErrors() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.encodeLength(10, new byte[3], 0);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.encodeLength(-1, new byte[5], 1);
        });
    }

    @ParameterizedTest
    @MethodSource("createTestDecodeTagData")
    void testDecodeTag(byte[] data, int offset,
            int expected) {
        int actual = TlvUtils.decodeTag(data, offset);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDecodeTagData() {
        return Stream.of(
            Arguments.of(
                new byte[] { 0, 0, 0, 1 },
                0,
                1
            ),
            Arguments.of(
                new byte[] { 0x03, 0x40, (byte)0x89, 0x11 },
                0,
                0x03408911
            ),
            Arguments.of(
                new byte[] { 1, 0x56, 0x10, 0x01, 0x20, 2 },
                1,
                0x56100120
            )
        );
    }

    @Test
    void testDecodeTagForErrors() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.decodeTag(new byte[] { 1, 1, 1 }, 0);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.decodeTag(new byte[4], 0);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.decodeTag(new byte[] { 5, 1, (byte)200, 3, 0, 3 }, 2);
        });
    }

    @ParameterizedTest
    @MethodSource("createTestDecodeLengthData")
    void testDecodeLength(byte[] data, int offset,
            int expected) {
        int actual = TlvUtils.decodeLength(data, offset);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDecodeLengthData() {
        return Stream.of(
            Arguments.of(
                new byte[] { 0, 0, 0, 0 },
                0,
                0
            ),
            Arguments.of(
                new byte[] { 0x03, 0x40, (byte)0x89, 0x11 },
                0,
                0x03408911
            ),
            Arguments.of(
                new byte[] { 1, 0x56, 0x10, 0x01, 0x20, 2 },
                1,
                0x56100120
            )
        );
    }

    @Test
    void testDecodeLengthForErrors() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.decodeLength(new byte[] { 1, 1, 1 }, 0);
        });
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            TlvUtils.decodeLength(new byte[] { 5, 1, (byte)200, 3, 0, 3 }, 2);
        });
    }

    @Test
    public void testCreateTlvEncodingWritableStream() throws IOException {
        // arrange
        byte srcByte = 45;
        int tagToUse = 16;
        byte[] expected = new byte[] {
            0, 0, 0, 16,
            0, 0, 0, 1,
            srcByte,
            0, 0, 0, 16,
            0, 0, 0, 0
        };
        ByteArrayOutputStream destStream = new ByteArrayOutputStream();
        OutputStream instance = TlvUtils.createTlvEncodingWritableStream(
            destStream, tagToUse);

        // act
        instance.write(new byte[] { srcByte });
        instance.write(new byte[0]);
        // write end of stream
        instance.write(null, 0, -1);

        // assert
        byte[] actual = destStream.toByteArray();
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
        "'', 1",
        "a, 4",
        "ab, 45",
        "abc, 60",
        "abcd, 120_000_000",
        "abcde, 34_000_000",
        "abcdefghi, 0x3245671d"
    })
    void testBodyChunkCodecStreams(String expected, int tagToUse) throws IOException {
        // arrange
        InputStream srcStream = new RandomizedReadInputStream(expected);
        ByteArrayOutputStream destStream = new ByteArrayOutputStream();
        OutputStream encodingStream = TlvUtils.createTlvEncodingWritableStream(
            destStream, tagToUse);

        // act
        IOUtils.copy(srcStream, encodingStream);
        // write end of stream
        encodingStream.write(null, 0, -1);
        // begin reading
        InputStream decodingStream = TlvUtils.createTlvDecodingReadableStream(
            new ByteArrayInputStream(destStream.toByteArray()),
            tagToUse, 0);
        String actual = IOUtils.toString(decodingStream,
            StandardCharsets.UTF_8);

        // assert
        assertEquals(expected, actual);
    }
}
