package com.aaronicsubstances.kabomu.exceptions;

public class ExpectationViolationException extends KabomuException {
    public ExpectationViolationException () {

    }

    public ExpectationViolationException (String message) {
        super(message);
    }

    public ExpectationViolationException (Throwable cause) {
        super(cause);
    }

    public ExpectationViolationException (String message, Throwable cause) {
        super(message, cause);
    }
}
