package com.algowebsolve.webapp.nsystem.linux;

import com.algowebsolve.webapp.nsystem.PacketIoInterface;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;

import java.io.IOException;

public class MqIo implements PacketIoInterface {

    static {
        Native.register( "/lib/x86_64-linux-gnu/librt.so.1" ); // TODO: This is VERY VERY BAD
    }

    // TODO: ERROR - THIS CODE IS NOT PORTABLE (even between linux systems)
    //       The native types int and long are used based on a standard LP64 type Linux environment
    private native int mq_open(Pointer queueName, int flags, int mode, Pointer attr);
    //private native int mq_open(Pointer queueName, int flags);
    private native int mq_send(int mqdes, Pointer msg, long msgLen, int msgPrio);
    private native long mq_receive(int mqdes, Pointer msg, long msgLen, Pointer msgPrio);
    private native int mq_close(int mqdes);
    // TODO: blocking and nonblocking, and also real time APIs

    private static final String ENCODING = "utf8"; // TODO: Better way of specifying encoding
    private static final int MSG_LEN_MAX = 8192+1; // TODO: took arbitrary value, need to fetch from system limits
                                                //        /proc/sys/fs/mqueue/msgsize_max
                                                 // TODO: Allow attr modification for message len
    public static final int MSG_PRIORITY_DEFAULT = 0;

    private int mqdes = -1;
    private Pointer recvPrio = Pointer.NULL; // WARNING: This field value is note thread-safe
    private Pointer sendBuffer = Pointer.NULL;
    private Pointer recvBuffer = Pointer.NULL;

    public MqIo(String queueName, int flags) throws IOException {
        // allocate memory for recv prio
        queueName = "/" + queueName;
        this.recvPrio = new Memory(16); // TODO: 16 bytes is arbitrarily taken, ideally should be sizeof(native int)
        this.sendBuffer = new Memory(MSG_LEN_MAX);
        this.recvBuffer = new Memory(MSG_LEN_MAX);
        // open a message queue
        Pointer mem = new Memory(queueName.length() + 1);
        mem .setString(0, queueName, ENCODING);
        this.mqdes = mq_open(mem, flags, 0, Pointer.NULL);
        //this.mqdes = mq_open(mem, flags);
        if (this.mqdes < 0) {
            int errno = Native.getLastError();
            throw new IOException("Native call 'mq_open' failed. errno=" + errno);
        }
    }

    public void send(byte[] data, int msgPrio) throws IOException {

        if (data.length >= MSG_LEN_MAX) {
            throw new IOException("Native call 'mq_send' does not support large messages."
                    + " message_length=" + data.length
                    + " max_length=" + MSG_LEN_MAX
            );
        }

        synchronized (this.sendBuffer) {
            this.sendBuffer.write(0, data, 0, data.length);
            int ret = mq_send(this.mqdes, this.sendBuffer, data.length, msgPrio);
            if (ret < 0) {
                int errno = Native.getLastError();
                throw new IOException("Native call 'mq_send' failed. errno=" + errno);
            }
        }
    }

    public byte[] recv() throws IOException {
        synchronized (this.recvBuffer) {
            long ssize = mq_receive(this.mqdes, this.recvBuffer, MSG_LEN_MAX - 1, this.recvPrio);
            if (ssize < 0) {
                int errno = Native.getLastError();
                throw new IOException("Native call 'mq_receive' failed. errno=" + errno);
            } else {
                // NOTE: This cast is ok as long as MSG_LEN_MAX < Integer.MAX_INT (since received message size cannot exceed MSG_LEN_MAX)
                int size = (int)ssize; // TODO: Dangerous downcast here
                byte[] data = new byte[size];
                this.recvBuffer.read(0, data, 0, size);
                return data;
            }
        }
    }

    @Override
    public void close() throws IOException {
        int ret = mq_close(this.mqdes);
        if (ret < 0) {
            int errno = Native.getLastError();
            throw new IOException("Native call 'mq_close' failed. errno=" + errno);
        }
    }

    @Override
    public int getFd() {
        return this.mqdes;
    }

    @Override
    public byte[] read() throws IOException {
        return recv();
    }

    @Override
    public void write(byte[] data) throws IOException {
        send(data, MSG_PRIORITY_DEFAULT);
    }
}
