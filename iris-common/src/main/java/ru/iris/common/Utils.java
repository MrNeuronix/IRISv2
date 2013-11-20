package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.09.12
 * Time: 17:46
 */

import org.zwave4j.Manager;
import org.zwave4j.ValueId;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicReference;

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */

/*
 * @(#)ASCIIUtility.java  1.10 05/08/29
 *
 * Copyright 1997-2005 Sun Microsystems, Inc. All Rights Reserved.
 */

public class Utils {

    public static byte[] getBytes(InputStream is) throws IOException {

        int len;
        int size = 1024;
        byte[] buf;

        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        return buf;
    }

    private ByteBuffer readFileAsByteBuffer(String inputFile, boolean directMemory) throws FileNotFoundException, IOException {

        FileChannel fc = new FileInputStream(inputFile).getChannel();
        long l = fc.size();

        ByteBuffer bb = null;

        if (directMemory) {
            bb = ByteBuffer.allocateDirect((int) l);
        } else {
            bb = ByteBuffer.allocate((int) l);
        }

        int read = fc.read(bb);
        fc.close();

        return bb;
    }

    public static Object getValue(ValueId valueId) {
        switch (valueId.getType()) {
            case BOOL:
                AtomicReference<Boolean> b = new AtomicReference<>();
                Manager.get().getValueAsBool(valueId, b);
                return b.get();
            case BYTE:
                AtomicReference<Short> bb = new AtomicReference<>();
                Manager.get().getValueAsByte(valueId, bb);
                return bb.get();
            case DECIMAL:
                AtomicReference<Float> f = new AtomicReference<>();
                Manager.get().getValueAsFloat(valueId, f);
                return f.get();
            case INT:
                AtomicReference<Integer> i = new AtomicReference<>();
                Manager.get().getValueAsInt(valueId, i);
                return i.get();
            case LIST:
                return null;
            case SCHEDULE:
                return null;
            case SHORT:
                AtomicReference<Short> s = new AtomicReference<>();
                Manager.get().getValueAsShort(valueId, s);
                return s.get();
            case STRING:
                AtomicReference<String> ss = new AtomicReference<>();
                Manager.get().getValueAsString(valueId, ss);
                return ss.get();
            case BUTTON:
                return null;
            case RAW:
                AtomicReference<short[]> sss = new AtomicReference<>();
                Manager.get().getValueAsRaw(valueId, sss);
                return sss.get();
            default:
                return null;
        }
    }
}