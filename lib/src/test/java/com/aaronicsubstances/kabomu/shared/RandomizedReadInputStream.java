package com.aaronicsubstances.kabomu.shared;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RandomizedReadInputStream extends FilterInputStream {

    public RandomizedReadInputStream(byte[] srcData) {
        this(new ByteArrayInputStream(srcData));
    }

    public RandomizedReadInputStream(InputStream backingStream) {
        super(backingStream);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        if (len > 0) {
            len = (int)(Math.random() * len) + 1;
        }
        return super.read(b, off, len);
    }
}
