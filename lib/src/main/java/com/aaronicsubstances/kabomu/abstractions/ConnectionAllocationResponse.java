package com.aaronicsubstances.kabomu.abstractions;

public interface ConnectionAllocationResponse {
    QuasiHttpConnection getConnection();
    CheckedRunnable getConnectTask();
}
