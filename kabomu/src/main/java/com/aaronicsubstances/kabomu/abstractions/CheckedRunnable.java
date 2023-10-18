package com.aaronicsubstances.kabomu.abstractions;

@FunctionalInterface
public interface CheckedRunnable {
    void run() throws Exception;
}
