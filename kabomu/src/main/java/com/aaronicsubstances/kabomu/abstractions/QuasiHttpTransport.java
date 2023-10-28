package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Represents commonality of functions provided by TCP or IPC mechanisms
 * at both server and client ends.
 */
public interface QuasiHttpTransport {

    /**
     * Gets the readable stream associated with a connection
     * for reading a request or response from the connection.
     * @param connection connection with readable stream
     * @return readable stream
     * @throws Exception
     */
    InputStream getReadableStream(QuasiHttpConnection connection)
        throws Exception;

    /**
     * Gets the writable stream associated with a connection
     * for writing a request or response to the connection.
     * @param connection connection with writable stream
     * @return writable stream
     * @throws Exception
     */
    OutputStream getWritableStream(QuasiHttpConnection connection)
        throws Exception;
}
