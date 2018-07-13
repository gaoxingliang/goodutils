/*
 * Copyright (c) 2008, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the Classpath exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package btrace;

import com.sun.btrace.BTraceUtils;
import com.sun.btrace.annotations.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sun.btrace.BTraceUtils.*;

/**
 * This sample prints all files opened for read/write
 * by a Java process. Note that if you pass FileDescriptor
 * to File{Input/Output}Stream or File{Reader/Writer},
 * that is not tracked by this script.
 */
@BTrace public class MonitorFile {
    @TLS private static String name;

    // record the read and write open counts
    static AtomicInteger readOpenCount = BTraceUtils.newAtomicInteger(0);
    static AtomicInteger writeOpenCount = BTraceUtils.newAtomicInteger(0);



    @OnMethod(
        clazz="java.io.FileInputStream",
        method="<init>"
    )
    public static void onNewFileInputStream(@Self FileInputStream self, File f) {
        name = Strings.str(f);
    }

    @OnMethod(
        clazz="java.io.FileInputStream",
        method="<init>",
        type="void (java.io.File)",
        location=@Location(Kind.RETURN)
    )
    public static void onNewFileInputStreamReturn() {
        if (name != null) {
            println("opened for read " + name);
            name = null;
            incrementAndGet(readOpenCount);
        }
    }

    @OnMethod(
        clazz="java.io.FileOutputStream",
        method="<init>"
    )
    public static void onNewFileOutputStream(@Self FileOutputStream self, File f, boolean b) {
        name = str(f);
    }

    @OnMethod(
        clazz="java.io.FileOutputStream",
        method="<init>",
        type="void (java.io.File, boolean)",
        location=@Location(Kind.RETURN)
    )
    public static void OnNewFileOutputStreamReturn() {
        if (name != null) {
            println("opened for write " + name);
            name = null;
            incrementAndGet(writeOpenCount);
        }
    }

    /**
     * print the metrics every 10 seconds
     */
    @OnTimer(10000)
    public static void stat() {
        println(BTraceUtils.timestamp("yyyy-MM-dd' 'HH:mm:ss") + " StatS openForRead=" + getAndSet(readOpenCount, 0) + " openForWrite=" + getAndSet(writeOpenCount, 0));
    }
}
