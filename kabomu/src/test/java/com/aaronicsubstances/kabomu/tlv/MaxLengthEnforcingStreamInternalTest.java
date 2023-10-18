package com.aaronicsubstances.kabomu.tlv;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.IOUtilsInternal;
import com.aaronicsubstances.kabomu.MiscUtilsInternal;
import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;
import com.aaronicsubstances.kabomu.shared.RandomizedReadInputStream;

class MaxLengthEnforcingStreamInternalTest {

    @ParameterizedTest
    @CsvSource({
        "0, ''",
        "0, a",
        "2, a",
        "2, ab",
        "3, a",
        "3, abc",
        "4, abcd",
        "5, abcde",
        "60, abcdefghi"
    })
    void testReading(int maxLength, String expected) throws IOException {
        // arrange
        InputStream stream = new RandomizedReadInputStream(expected);
        InputStream instance = TlvUtils.createMaxLengthEnforcingStream(
            stream, maxLength);

        // act
        String actual = IOUtils.toString(instance, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
        "1, ab",
        "2, abc",
        "3, abcd",
        "5, abcdefxyz"
    })
    void testReadingForErrors(int maxLength, String srcData) throws IOException {
        // arrange
        InputStream stream = new ByteArrayInputStream(
            MiscUtilsInternal.stringToBytes(srcData));
        InputStream instance = TlvUtils.createMaxLengthEnforcingStream(
            stream, maxLength);

        // act and assert
        KabomuIOException actualEx = assertThrowsExactly(KabomuIOException.class, () -> {
            IOUtils.toString(instance, StandardCharsets.UTF_8);
        });

        assertThat(actualEx.getMessage(),
            containsString("exceeds limit of " + maxLength));
    }

    @Test
    void testZeroByteReads() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
        InputStream instance = TlvUtils.createMaxLengthEnforcingStream(stream, 3);

        int actualCount = instance.read(new byte[0], 0, 0);
        assertEquals(0, actualCount);

        byte[] actual = new byte[3];
        IOUtilsInternal.readBytesFully(instance,
            actual, 0, 3);
        assertArrayEquals(new byte[] { 0, 1, 2 }, actual);

        actualCount = instance.read(new byte[0]);
        assertEquals(0, actualCount);

        actualCount = instance.read(new byte[2], 0, 2);
        assertEquals(-1, actualCount);
    }
}
