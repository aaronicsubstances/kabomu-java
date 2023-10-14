package com.aaronicsubstances.kabomu;

import java.util.ArrayList;
import java.util.List;

public class CsvUtils {
    private static final int TOKEN_EOI = -1;
    private static final int TOKEN_COMMA = 1;
    private static final int TOKEN_QUOTE = 2;
    private static final int TOKEN_CRLF = 3;
    private static final int TOKEN_LF = 4;
    private static final int TOKEN_CR = 5;

    private static boolean locateNextToken(String csv, int start,
            boolean insideQuotedValue, int[] tokenInfo) {
        // set to end of input by default
        tokenInfo[0] = TOKEN_EOI;
        tokenInfo[1] = -1;
        for (int i = start; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (!insideQuotedValue && c == ',') {
                tokenInfo[0] = TOKEN_COMMA;
                tokenInfo[1] = i;
                return true;
            }
            if (!insideQuotedValue && c == '\n') {
                tokenInfo[0] = TOKEN_LF;
                tokenInfo[1] = i;
                return true;
            }
            if (!insideQuotedValue && c == '\r') {
                if (i + 1 < csv.length() && csv.charAt(i + 1) == '\n') {
                    tokenInfo[0] = TOKEN_CRLF;
                }
                else {
                    tokenInfo[0] = TOKEN_CR;
                }
                tokenInfo[1] = i;
                return true;
            }
            if (insideQuotedValue && c == '"') {
                if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    // skip quote pair.
                    i++;
                }
                else {
                    tokenInfo[0] = TOKEN_QUOTE;
                    tokenInfo[1] = i;
                    return true;
                }
            }
        }
        return false;
    }

    public static List<List<String>> deserialize(String csv) {
        List<List<String>> parsedCsv = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        int nextValueStartIdx = 0;
        boolean isCommaTheLastSeparatorSeen = false;
        int[] tokenInfo = new int[2];
        while (nextValueStartIdx < csv.length()) {
            // use to detect infinite looping
            int savedNextValueStartIdx = nextValueStartIdx;

            // look for comma, quote or newline, whichever comes first.
            int newlineLen = 1;
            boolean tokenIsNewline = false;
            isCommaTheLastSeparatorSeen = false;

            int nextValueEndIdx;
            int tokenType;

            // only respect quote separator at the very beginning
            // of parsing a column value
            if (csv.charAt(nextValueStartIdx) == '"') {
                tokenType = TOKEN_QUOTE;
                // locate ending quote, while skipping over
                // double occurences of quotes.
                if (!locateNextToken(csv, nextValueStartIdx + 1, true, tokenInfo)) {
                    throw createCsvParseError(parsedCsv.size(), currentRow.size(),
                        "ending double quote not found");
                }
                nextValueEndIdx = tokenInfo[1] + 1;
            }
            else {
                locateNextToken(csv, nextValueStartIdx, false, tokenInfo);
                tokenType = tokenInfo[0];
                if (tokenType == TOKEN_COMMA) {
                    nextValueEndIdx = tokenInfo[1];
                    isCommaTheLastSeparatorSeen = true;
                }
                else if (tokenType == TOKEN_LF || tokenType == TOKEN_CR) {
                    nextValueEndIdx = tokenInfo[1];
                    tokenIsNewline = true;
                }
                else if (tokenType == TOKEN_CRLF) {
                    nextValueEndIdx = tokenInfo[1];
                    tokenIsNewline = true;
                    newlineLen = 2;
                }
                else if (tokenType == TOKEN_EOI) {
                    nextValueEndIdx = csv.length();
                }
                else
                {
                    throw new UnsupportedOperationException("unexpected token type: " + tokenType);
                }
            }

            // create new value for current row,
            // but skip empty values between newlines, or between BOI and newline.
            if (nextValueStartIdx < nextValueEndIdx || !tokenIsNewline || currentRow.size() > 0){
                String nextValue;
                try {
                    nextValue = unescapeValue(csv.substring(nextValueStartIdx,
                        nextValueEndIdx));
                }
                catch (IllegalArgumentException ex) {
                    throw createCsvParseError(parsedCsv.size(), currentRow.size(), ex.getMessage());
                }
                currentRow.add(nextValue);
            }

            // advance input pointer.
            if (tokenType == TOKEN_COMMA) {
                nextValueStartIdx = nextValueEndIdx + 1;
            }
            else if (tokenType == TOKEN_QUOTE) {
                // validate that character after quote is EOI, comma or newline.
                nextValueStartIdx = nextValueEndIdx;
                if (nextValueStartIdx < csv.length()) {
                    char c = csv.charAt(nextValueStartIdx);
                    if (c == ',') {
                        isCommaTheLastSeparatorSeen = true;
                        nextValueStartIdx++;
                    }
                    else if (c == '\n' || c == '\r') {
                        parsedCsv.add(currentRow);
                        currentRow = new ArrayList<>();
                        if (c == '\r' && nextValueStartIdx + 1 < csv.length() &&
                                csv.charAt(nextValueStartIdx + 1) == '\n') {
                            nextValueStartIdx += 2;
                        }
                        else {
                            nextValueStartIdx++;
                        }
                    }
                    else {
                        throw createCsvParseError(parsedCsv.size(), currentRow.size(),
                            String.format("unexpected character '%s' found at beginning", c));
                    }
                }
                else {
                    // leave to aftermath processing.
                }
            }
            else if (tokenIsNewline) {
                parsedCsv.add(currentRow);
                currentRow = new ArrayList<>();
                nextValueStartIdx = nextValueEndIdx + newlineLen;
            }
            else {
                // leave to aftermath processing.
                nextValueStartIdx = nextValueEndIdx;
            }

            // ensure input pointer has advanced.
            if (savedNextValueStartIdx >= nextValueStartIdx) {
                throw createCsvParseError(parsedCsv.size(), currentRow.size(),
                    "algorithm bug detected as parsing didn't make an advance. Potential for infinite " +
                    "looping.");
            }
        }

        // generate empty value for case of trailing comma
        if (isCommaTheLastSeparatorSeen) {
            currentRow.add("");
        }

        // add any leftover values to parsed csv rows.
        if (currentRow.size() > 0) {
            parsedCsv.add(currentRow);
        }

        return parsedCsv;
    }

    private static RuntimeException createCsvParseError(int row, int column,
            String errorMessage) {
        return new IllegalArgumentException(String.format(
            "CSV parse error at row %s column %s: %s",
                row + 1, column + 1,
                errorMessage != null ? errorMessage : ""));
    }

    public static String serialize(List<List<String>> rows) {
        StringBuilder csvBuilder = new StringBuilder();
        for (List<String> row : rows) {
            boolean addCommaSeparator = false;
            for (String value : row) {
                if (addCommaSeparator) {
                    csvBuilder.append(",");
                }
                csvBuilder.append(escapeValue(value));
                addCommaSeparator = true;
            }
            csvBuilder.append("\n");
        }
        return csvBuilder.toString();
    }

    public static String escapeValue(String raw) {
        if (!doesValueContainSpecialCharacters(raw)) {
            // escape empty strings with two double quotes to resolve ambiguity
            // between an empty row and a row containing an empty string - otherwise both
            // serialize to the same CSV output.
            return raw == "" ? "\"\"" : raw;
        }
        return '"' + raw.replace("\"", "\"\"") + '"';
    }

    public static String unescapeValue(String escaped) {
        if (!doesValueContainSpecialCharacters(escaped)) {
            return escaped;
        }
        if (escaped.length() < 2 || !escaped.startsWith("\"") || !escaped.endsWith("\"")) {
            throw new IllegalArgumentException("missing enclosing double quotes around csv value: " + escaped);
        }
        StringBuilder unescaped = new StringBuilder();
        for (int i = 1; i < escaped.length() - 1; i++) {
            char c = escaped.charAt(i);
            unescaped.append(c);
            if (c == '"') {
                if (i == escaped.length() - 2 || escaped.charAt(i + 1) != '"') {
                    throw new IllegalArgumentException("unescaped double quote found in csv value: " + escaped);
                }
                i++;
            }
        }
        return unescaped.toString();
    }

    private static boolean doesValueContainSpecialCharacters(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',' || c == '"' || c == '\r' || c == '\n') {
                return true;
            }
        }
        return false;
    }
}
