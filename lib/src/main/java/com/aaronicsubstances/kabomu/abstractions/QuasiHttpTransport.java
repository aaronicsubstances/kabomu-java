package com.aaronicsubstances.kabomu.abstractions;

import java.io.InputStream;
import java.io.OutputStream;

public interface QuasiHttpTransport {
    InputStream getReadableStream(QuasiHttpConnection connection);
    OutputStream getWritableStream(QuasiHttpConnection connection);
}
