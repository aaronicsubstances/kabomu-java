package com.aaronicsubstances.kabomu.abstractions;

/**
 * Interface which is {@link Runnable} whose sole method
 * can throw checked exceptions.
 */
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * Executes action represented by instance.
     * @throws Exception
     */
    void run() throws Exception;
}
