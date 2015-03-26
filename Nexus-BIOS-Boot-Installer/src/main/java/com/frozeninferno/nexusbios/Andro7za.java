/*
 * Copyright (C) 2013 Brian D. Allred
 *
 *     This software source code is protected by copyright
 *     and may not be used, modified, or distributed
 *     without my permission.
 *
 *     All rights reserved.
 */

package com.frozeninferno.nexusbios;

import android.util.Log;

/**
 * Created by Brian on 1/25/14.
 */
public final class Andro7za {

    //public native int a7za_print_usage();

    public native int a7za_unpack(String ouputPath, String inputFile);

    /**
     * <code>printUsage</code> print 7za usage.
     *
    public void printUsage() {
        Log.d(JNI_TAG, "Call a7za_print_usage()");
        int ret = a7za_print_usage();
        Log.d(JNI_TAG, "a7za_print_usage() return code " + ret);
    }*/

    public int unpack(String ouputPath, String inputFile) {
        Log.d("7zaJNI", "Call a7za_unpack()");
        int ret = a7za_unpack(ouputPath, inputFile);
        Log.d("7zaJNI", "a7za_unpack() return code " + ret);
        return ret;
    }

    static {
        // Dynamically load stl_port, see jni/Application.mk
        // System.loadLibrary("stlport_shared");
        try {
            System.loadLibrary("7za");
        } catch (UnsatisfiedLinkError error) {
            error.printStackTrace();
        }
    }
}
