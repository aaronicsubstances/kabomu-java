package com.aaronicsubstances.kabomu.abstractions;

public interface CustomDisposable {
    CheckedRunnable getDisposer();
    void setDisposer(CheckedRunnable value);
}
