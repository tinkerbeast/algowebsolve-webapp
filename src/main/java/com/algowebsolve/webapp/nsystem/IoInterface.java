package com.algowebsolve.webapp.nsystem;

import java.io.IOException;

public interface IoInterface extends AutoCloseable {
    int getFd();
    byte[] read() throws IOException;
    void write(byte[] data) throws IOException;
}
