package com.algowebsolve.webapp.nsystem;

import java.io.IOException;

public interface PacketIoInterface extends IoInterface {
    void send(byte[] data, int prio) throws IOException;
    byte[] recv() throws IOException;
}
