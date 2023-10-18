package com.aaronicsubstances.kabomu.exceptions;

public class MissingDependencyException extends KabomuException {

    public MissingDependencyException () {

    }

    public MissingDependencyException (String message) {
        super(message);
    }

    public MissingDependencyException (Throwable cause) {
        super(cause);
    }

    public MissingDependencyException (String message, Throwable cause) {
        super(message, cause);
    }
}
