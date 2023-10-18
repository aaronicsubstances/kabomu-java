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

class ContentLengthEnforcingStreamInternalTest {
    
    @ParameterizedTest
    @CsvSource({
        "0, '',     ''",
        "0, a,      ''",
        "1, ab,     a",
        "2, ab,     ab",
        "2, abc,    ab",
        "3, abc,    abc",
        "4, abcd,   abcd",
        "5, abcde,  abcde",
        "6, abcdefghi, abcdef"
    })
    void testReading(long contentLength, String srcData,
            String expected) throws IOException {
        // arrange
        InputStream stream = new RandomizedReadInputStream(srcData);
        InputStream instance = TlvUtils.createContentLengthEnforcingStream(
            stream, contentLength);

        // act
        String actual = IOUtils.toString(instance, StandardCharsets.UTF_8);

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
        "2, ''",
        "4, abc",
        "5, abcd",
        "15, abcdef"
    })
    void testReadingForErrors(long contentLength, String srcData) throws IOException {
        // arrange
        InputStream stream = new ByteArrayInputStream(
            MiscUtilsInternal.stringToBytes(srcData));
        InputStream instance = TlvUtils.createContentLengthEnforcingStream(
            stream, contentLength);

        // act and assert
        KabomuIOException actualEx = assertThrowsExactly(KabomuIOException.class, () -> {
            IOUtils.toString(instance, StandardCharsets.UTF_8);
        });

        assertThat(actualEx.getMessage(), containsString("end of read"));
    }

    @Test
    void testZeroByteReads() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[] { 0, 1, 2 });
        InputStream instance = TlvUtils.createContentLengthEnforcingStream(stream, 3);

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
