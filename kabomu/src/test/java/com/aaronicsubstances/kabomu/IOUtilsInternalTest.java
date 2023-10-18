package com.aaronicsubstances.kabomu;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import com.aaronicsubstances.kabomu.exceptions.KabomuIOException;
import com.aaronicsubstances.kabomu.shared.ComparisonUtils;
import com.aaronicsubstances.kabomu.shared.RandomizedReadInputStream;

class IOUtilsInternalTest {

    @Test
    void testReadBytesFully() throws IOException {
        // arrange
        InputStream reader = new RandomizedReadInputStream(
            new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        byte[] readBuffer = new byte[6];

        // act
        IOUtilsInternal.readBytesFully(reader, readBuffer, 0, 3);

        // assert
        ComparisonUtils.compareData(new byte[] { 0, 1, 2 }, 0,
            readBuffer, 0, 3);

        // assert that zero length reading doesn't cause problems.
        IOUtilsInternal.readBytesFully(reader, readBuffer, 3, 0);

        // act again
        IOUtilsInternal.readBytesFully(reader, readBuffer, 1, 3);

        // assert
        ComparisonUtils.compareData(new byte[] { 3, 4, 5 }, 0,
            readBuffer, 1, 3);

        // act again
        IOUtilsInternal.readBytesFully(reader, readBuffer, 3, 2);

        // assert
        ComparisonUtils.compareData(new byte[] { 6, 7 }, 0,
            readBuffer, 3, 2);

        // test zero byte reads.
        readBuffer = new byte[] { 2, 3, 5, 8 };
        IOUtilsInternal.readBytesFully(reader, readBuffer, 0, 0);
        assertArrayEquals(new byte[] { 2, 3, 5, 8 }, readBuffer);
    }

    @Test
    void testReadBytesFullyForErrors() throws IOException {
        // arrange
        InputStream reader = new ByteArrayInputStream(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });
        byte[] readBuffer = new byte[5];

        // act
        IOUtilsInternal.readBytesFully(reader, readBuffer, 0, readBuffer.length);

        // assert
        ComparisonUtils.compareData(
            new byte[] { 0, 1, 2, 3, 4 }, 0,
            readBuffer, 0, readBuffer.length);

        // act and assert unexpected end of read
        KabomuIOException actualEx = assertThrowsExactly(KabomuIOException.class, () ->
            IOUtilsInternal.readBytesFully(reader, readBuffer, 0, readBuffer.length));
        assertThat(actualEx.getMessage(), containsString("end of read"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "xyz", "abcdefghi"})
    void testCopy(String srcData) throws IOException {
        // arrange
        byte[] expected = MiscUtilsInternal.stringToBytes(srcData);
        InputStream readerStream = new RandomizedReadInputStream(expected);
        ByteArrayOutputStream writerStream = new ByteArrayOutputStream();

        // act
        IOUtilsInternal.copy(readerStream, writerStream);

        // assert
        assertArrayEquals(expected, writerStream.toByteArray());
    }
}
