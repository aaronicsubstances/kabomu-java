/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.aaronicsubstances.kabomu;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvUtilsTest {
    @ParameterizedTest
    @MethodSource("createTestEscapeValueData")
    void testEscapeValueData(String raw, String expected) {
        String actual = CsvUtils.escapeValue(raw);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> createTestEscapeValueData() {
        return Stream.of(
            Arguments.of("", "\"\"")
        );
    }
}
