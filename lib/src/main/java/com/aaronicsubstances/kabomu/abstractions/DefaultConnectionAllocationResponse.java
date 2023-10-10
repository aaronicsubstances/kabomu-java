package com.aaronicsubstances.kabomu.abstractions;

public class DefaultConnectionAllocationResponse implements ConnectionAllocationResponse {
    private QuasiHttpConnection connection;
    private CheckedRunnable connectTask;

    public QuasiHttpConnection getConnection() {
        return connection;
    }

    public void setConnection(QuasiHttpConnection connection) {
        this.connection = connection;
    }

    public CheckedRunnable getConnectTask() {
        return connectTask;
    }

    public void setConnectTask(CheckedRunnable connectTask) {
        this.connectTask = connectTask;
    }
}
