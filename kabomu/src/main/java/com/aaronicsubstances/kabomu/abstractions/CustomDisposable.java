package com.aaronicsubstances.kabomu.abstractions;

/**
 * Common interface of classes in Kabomu library which perform
 * resource clean-up operations.
 * 
 * This interface exists as a more dynamic alternative to the
 * AutoCloseable resource clean-up protocols.
 */
public interface CustomDisposable {

    /**
     * Gets a function which if invoked,
     * performs any needed clean up operation on resources held
     * by the instance.
     */
    CheckedRunnable getDisposer();

    /**
     * Sets a function which if invoked,
     * performs any needed clean up operation on resources held
     * by the instance.
     * @param value
     */
    void setDisposer(CheckedRunnable value);
}
