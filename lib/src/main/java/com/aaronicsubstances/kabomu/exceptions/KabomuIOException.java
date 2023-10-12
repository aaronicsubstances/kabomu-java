package com.aaronicsubstances.kabomu.exceptions;

public class KabomuIOException extends KabomuException {
    public KabomuIOException () {

    }

    public KabomuIOException (String message) {
        super(message);
    }

    public KabomuIOException (Throwable cause) {
        super(cause);
    }

    public KabomuIOException (String message, Throwable cause) {
        super(message, cause);
    }

    public static KabomuIOException createEndOfReadError() {
        return new KabomuIOException("unexpected end of read");
    }
}
