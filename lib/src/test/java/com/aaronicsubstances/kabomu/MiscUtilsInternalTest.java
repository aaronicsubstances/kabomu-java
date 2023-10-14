package com.aaronicsubstances.kabomu;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class MiscUtilsInternalTest {

    @ParameterizedTest
    @MethodSource("createTestSerializeInt32BEData")
    void testSerializeInt32BE(int v, byte[] rawBytes, int offset, byte[] expected) {
        byte[] actual = new byte[rawBytes.length];
        System.arraycopy(rawBytes, 0, actual, 0, rawBytes.length);
        MiscUtilsInternal.serializeInt32BE(v, actual, offset);
        assertArrayEquals(expected, actual);
    }

    static Stream<Arguments> createTestSerializeInt32BEData() {
        return Stream.of(
            Arguments.of(
                2001,
                new byte[] { 8, 2, 3, 4 },
                0,
                new byte[] { 0, 0, 7, (byte)0xd1 }
            ),
            Arguments.of(
                -10_999,
                new byte[5],
                1,
                new byte[] { 0, (byte)0xff, (byte)0xff, (byte)0xd5, 9 }
            ),
            Arguments.of(
                1_000_000,
                new byte[4],
                0,
                new byte[] { 0, 0xf, 0x42, 0x40 }
            ),
            Arguments.of(
                1_000_000_000,
                new byte[]{ 10, 20, 30, 40, 50 },
                0,
                new byte[] { 0x3b, (byte)0x9a, (byte)0xca, 0, 50 }
            ),
            Arguments.of(
                -1_000_000_000,
                new byte[]{ 10, 11, 12, 13, 10, 11, 12, 13 },
                2,
                new byte[] { 10, 11, (byte)0xc4, 0x65, 0x36, 0, 12, 13 }
            )
        );
    }

    @Test
    void testSerializeInt32BEForErrors() {
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.serializeInt32BE(1, new byte[2], 0);
        });
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.serializeInt32BE(2, new byte[4], 1);
        });
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.serializeInt32BE(3, new byte[20], 18);
        });
    }

    @ParameterizedTest
    @MethodSource("createTestDeserializeInt32BEData")
    void testDeserializeInt32BE(byte[] rawBytes, int offset, int expected) {
        int actual = MiscUtilsInternal.deserializeInt32BE(rawBytes, offset);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestDeserializeInt32BEData() {
        return Stream.of(
            Arguments.of(
                new byte[] { 0, 0, 7, (byte)0xd1 },
                0,
                2001
            ),
            Arguments.of(
                new byte[] { (byte)0xff, (byte)0xff, (byte)0xd5, 9 },
                0,
                -10_999
            ),
            Arguments.of(
                new byte[] { 0, 0xf, 0x42, 0x40 },
                0,
                1_000_000
            ),
            Arguments.of(
                new byte[] { 0x3b, (byte)0x9a, (byte)0xca, 0, 50 },
                0,
                1_000_000_000
            ),
            Arguments.of(
                new byte[] { (byte)0xc4, 0x65, 0x36, 0 },
                0,
                -1_000_000_000
            ),
            // the next would have been 2_294_967_196 if deserializing entire 32-bits as unsigned.
            Arguments.of(
                new byte[] { 8, 2, (byte)0x88, (byte)0xca, 0x6b, (byte)0x9c, 1 },
                2,
                -2_000_000_100
            )
        );
    }

    @Test
    void testDeserializeInt32BEForErrors() {
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.deserializeInt32BE(new byte[2], 0);
        });
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.deserializeInt32BE(new byte[4], 1);
        });
        assertThrows(Exception.class, () -> {
            MiscUtilsInternal.deserializeInt32BE(new byte[20], 17);
        });
    }

    @ParameterizedTest
    @MethodSource("createTestParseInt48Data")
    void testParseInt48(String input, long expected) {
        long actual = MiscUtilsInternal.parseInt48(input);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestParseInt48Data() {
        return Stream.of(
            Arguments.of("0", 0),
            Arguments.of("1", 1),
            Arguments.of("2", 2),
            Arguments.of(" 20", 20),
            Arguments.of(" 200 ", 200),
            Arguments.of("-1000", -1_000),
            Arguments.of("1000000", 1_000_000),
            Arguments.of("4294967295", 4_294_967_295L),
            Arguments.of("-50000000000000", -50_000_000_000_000L),
            Arguments.of("100000000000000", 100_000_000_000_000L),
            Arguments.of("140737488355327", 140_737_488_355_327L),
            Arguments.of("-140737488355328", -140_737_488_355_328L)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "false", "xyz", "1.23", "2.0",
        "140737488355328", "-140737488355329", "72057594037927935"})
    public void testParsetInt48ForErrors(String input) {
        Exception ex = assertThrows(Exception.class, () ->
            MiscUtilsInternal.parseInt48(input));
        if (input != null) {
            assertTrue(ex instanceof NumberFormatException);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "' 67 ',    67",
        "172,       172"
    })
    public void testParsetInt32(String input, int expected) {
        int actual = MiscUtilsInternal.parseInt32(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "x"})
    public void testParsetInt32ForErrors(String input) {
        assertThrowsExactly(NumberFormatException.class, () ->
            MiscUtilsInternal.parseInt32(input));
    }

    @Test
    void testStringToBytes() {
        byte[] actual = MiscUtilsInternal.stringToBytes("");
        assertArrayEquals(new byte[0], actual);

        actual = MiscUtilsInternal.stringToBytes("abc");
        assertArrayEquals(new byte[] { (byte)'a', (byte)'b', (byte)'c' }, actual);

        // NB: text between bar and baz is
        // supplementary character 0001d306
        actual = MiscUtilsInternal.stringToBytes("Foo \u00a9 bar \ud834\udf06 baz \u2603 qux");
        assertArrayEquals(new byte[] { 0x46, 0x6f, 0x6f, 0x20, (byte)0xc2, (byte)0xa9, 0x20, 0x62, 0x61, 0x72, 0x20,
            (byte)0xf0, (byte)0x9d, (byte)0x8c, (byte)0x86, 0x20, 0x62, 0x61, 0x7a, 0x20, (byte)0xe2, (byte)0x98, (byte)0x83,
            0x20, 0x71, 0x75, 0x78 }, actual);
    }

    @Test
    void testBytesToString() {
        byte[] data = new byte[] { };
        int offset = 0;
        int length = 0;
        String expected = "";
        String actual = MiscUtilsInternal.bytesToString(data, offset, length);
        assertEquals(expected, actual);
        actual = MiscUtilsInternal.bytesToString(data);
        assertEquals(expected, actual);

        offset = 0;
        data = new byte[] { (byte)'a', (byte)'b', (byte)'c' };
        length = data.length;
        expected = "abc";
        actual = MiscUtilsInternal.bytesToString(data, offset, length);
        assertEquals(expected, actual);
        actual = MiscUtilsInternal.bytesToString(data);
        assertEquals(expected, actual);

        offset = 1;
        data = new byte[] { 0x46, 0x6f, 0x6f, 0x20, (byte)0xc2, (byte)0xa9, 0x20, 0x62, 0x61, 0x72, 0x20,
            (byte)0xf0, (byte)0x9d, (byte)0x8c, (byte)0x86, 0x20, 0x62, 0x61, 0x7a, 0x20, (byte)0xe2, (byte)0x98, (byte)0x83,
            0x20, 0x71, 0x75, 0x78 };
        length = data.length - 2;
        // NB: text between bar and baz is
        // supplementary character 0001d306
        expected = "oo \u00a9 bar \ud834\udf06 baz \u2603 qu";
        actual = MiscUtilsInternal.bytesToString(data, offset, length);
        assertEquals(expected, actual);
    }
}
