package com.algowebsolve.webapp;

import java.io.IOException;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

public class NativeIo implements AutoCloseable {

    // TODO: The values are manually derived - how to get them automatically from fcntl.h?
    // https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/fcntl.h.html
    public static final int O_RDONLY = 0;
    public static final int O_WRONLY = 1;
    public static final int O_RDWR = 2;

    static {
        Native.register( "c" );
    }

    private native int open(Pointer pathName, int flags, int mode);
    private native void close(int fd);
    private native long read(int fd, Pointer buf, long count);


    private static final String ENCODING = "utf8"; // TODO: How to mach java encoded sting to c runtime string encoding?
    private int fd;

    public NativeIo(String pathName, int mode) throws IOException {
        // String to native
        Pointer mem = new Memory(pathName.length() + 1);
        mem .setString(0, pathName, ENCODING);
        // Call native open
        fd = open(mem, 0, mode);
        if (fd < 0) {
            int errno = Native.getLastError();
            throw new IOException("Native call 'open' failed. errno=0x" + Integer.toHexString(errno));
        }
    }

    String read() throws IOException {
        Pointer mem = new Memory(1024);
        long ret = read(this.fd, mem, 1023);
        if (ret > 0) {
            byte[] data = mem.getByteArray(0, (int)ret);
            return new String(data);
        } else {
            int errno = Native.getLastError();
            throw new IOException("Native call 'read' failed. errno=0x" + Integer.toHexString(errno));
        }
    }

    public int getFd() {
         return fd;
    }

    @Override
    public void close() {
        close(fd);
    }
}
