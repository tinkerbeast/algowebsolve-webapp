package com.algowebsolve.webapp.model;

import java.util.List;

public class TestPrimitiveArrays {
    //public byte[] aByteArray; // TODO: Currently unsupported. The reason is that jackson converts this into a base64 string,
                                //        however the json-schema generate by jackson is of type [integer]
    public short[] aShortArray;
    public int[] anIntArray;
    public long[] aLongArray;
    public float[] aFloatArray;
    public double[] aDoubleArray;
    public boolean[] aBooleanArray;
    public char[] aCharArray;
}
