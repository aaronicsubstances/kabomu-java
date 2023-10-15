package com.aaronicsubstances.kabomu.tlv;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;
import com.aaronicsubstances.kabomu.shared.RandomizedReadInputStream;

class BodyChunkDecodingStreamInternalTest {

    @ParameterizedTest
    @MethodSource("createTestReadingData")
    void testReading(byte[] srcData,
            int expectedTag, int tagToIgnore,
            byte[] expected) throws IOException {
        // arrange
        InputStream stream = new RandomizedReadInputStream(srcData);
        InputStream instance = TlvUtils.createTlvDecodingReadableStream(
            stream, expectedTag, tagToIgnore);

        // act
        byte[] actual = IOUtils.toByteArray(instance);

        assertArrayEquals(expected, actual);
    }

    static List<Arguments> createTestReadingData() {
        List<Arguments> testData = new ArrayList<>();

        byte[] srcData = new byte[] {
            0, 0, 0, 89,
            0, 0, 0, 0
        };
        int expectedTag = 89;
        int tagToIgnore = 5;
        byte[] expected = new byte[] { };
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 15,
            0, 0, 0, 2,
            2, 3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 8;
        tagToIgnore = 15;
        expected = new byte[] { };
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 8,
            0, 0, 0, 2,
            2, 3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 8;
        tagToIgnore = 15;
        expected = new byte[] { 2, 3 };
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 8,
            0, 0, 0, 1,
            2,
            0, 0, 0, 8,
            0, 0, 0, 1,
            3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 8;
        tagToIgnore = 15;
        expected = new byte[] { 2, 3 };
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0x3d, 0x15,
            0, 0, 0, 0,
            0x30, (byte)0xa3, (byte)0xb5, 0x17,
            0, 0, 0, 1,
            2,
            0, 0, 0x3d, 0x15,
            0, 0, 0, 7,
            0, 0, 0, 0, 0, 0, 0,
            0x30, (byte)0xa3, (byte)0xb5, 0x17,
            0, 0, 0, 1,
            3,
            0, 0, 0x3d, 0x15,
            0, 0, 0, 0,
            0x30, (byte)0xa3, (byte)0xb5, 0x17,
            0, 0, 0, 4,
            2, 3, 45, 62,
            0, 0, 0x3d, 0x15,
            0, 0, 0, 1,
            1,
            0x30, (byte)0xa3, (byte)0xb5, 0x17,
            0, 0, 0, 8,
            91, 100, 2, 3, 45, 62, 70, 87,
            0x30, (byte)0xa3, (byte)0xb5, 0x17,
            0, 0, 0, 0
        };
        expectedTag = 0x30a3b517;
        tagToIgnore = 0x3d15;
        expected = new byte[] { 2, 3, 2, 3, 45, 62,
            91, 100, 2, 3, 45, 62, 70, 87 };
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        return testData;
    }

    @ParameterizedTest
    @MethodSource("createTestDecodingForErrorsData")
    void testDecodingForErrors(byte[] srcData,
            int expectedTag, int tagToIgnore,
            String expected) {
        // arrange
        InputStream stream = new RandomizedReadInputStream(srcData);
        InputStream instance = TlvUtils.createTlvDecodingReadableStream(
            stream, expectedTag, tagToIgnore);

        // act
        KabomuIOException actualEx = assertThrowsExactly(KabomuIOException.class, () -> {
            IOUtils.toByteArray(instance);
        });

        // assert
        assertThat(actualEx.getMessage(), containsString(expected));
    }

    static List<Arguments> createTestDecodingForErrorsData() {
        List<Arguments> testData = new ArrayList<>();

        byte[] srcData = new byte[] {
            0, 0, 0x09, 0,
            0, 0, 0, 12
        };
        int expectedTag = 0x0900;
        int tagToIgnore = 0;
        String expected = "unexpected end of read";
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0x09, 0,
            0, 0, 0, 12
        };
        expectedTag = 10;
        tagToIgnore = 30;
        expected = "unexpected tag";
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 15,
            0, 0, 0, 2,
            2, 3,
            0, 0, 0, 15,
            0, 0, 0, 2,
            2, 3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 8;
        tagToIgnore = 15;
        expected = "unexpected tag";
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 0,
            0, (byte)0xff, (byte)0xff, (byte)0xec,
            2, 3,
            0, 0, 0, 14,
            0, 0, 0, 0,
            2, 3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 14;
        tagToIgnore = 8;
        expected = "invalid tag: 0";
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        srcData = new byte[] {
            0, 0, 0, 14,
            (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xec,
            2, 3,
            0, 0, 0, 14,
            0, 0, 0, 0,
            2, 3,
            0, 0, 0, 8,
            0, 0, 0, 0
        };
        expectedTag = 14;
        tagToIgnore = 15;
        expected = "invalid tag value length: -20";
        testData.add(Arguments.of(srcData, expectedTag, tagToIgnore,
            expected ));

        return testData;
    }
}
