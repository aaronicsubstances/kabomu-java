package com.aaronicsubstances.kabomu.exceptions;

public class KabomuException extends RuntimeException {
    public KabomuException () {

    }

    public KabomuException (String message) {
        super(message);
    }

    public KabomuException (Throwable cause) {
        super(cause);
    }

    public KabomuException (String message, Throwable cause) {
        super(message, cause);
    }
}
