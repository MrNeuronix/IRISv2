/*
 * Copyright 2012-2014 Nikolay A. Viguro
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class Utils
{

	public static byte[] getBytes(InputStream is) throws IOException
	{

		int len;
		int size = 1024;
		byte[] buf;

		if (is instanceof ByteArrayInputStream)
		{
			size = is.available();
			buf = new byte[size];
			len = is.read(buf, 0, size);
		}
		else
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			buf = new byte[size];
			while ((len = is.read(buf, 0, size)) != -1)
			{
				bos.write(buf, 0, len);
			}
			buf = bos.toByteArray();
		}
		return buf;
	}

	public static Object getValue(ValueId valueId)
	{
		switch (valueId.getType())
		{
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

	public static String getValueType(ValueId valueId)
	{
		switch (valueId.getType())
		{
			case BOOL:
				return "BOOL";
			case BYTE:
				return "BYTE";
			case DECIMAL:
				return "DECIMAL";
			case INT:
				return "INT";
			case LIST:
				return "LIST";
			case SCHEDULE:
				return "SCHEDULE";
			case SHORT:
				return "SHORT";
			case STRING:
				return "STRING";
			case BUTTON:
				return "BUTTON";
			case RAW:
				return "RAW";
			default:
				return null;
		}
	}

	private ByteBuffer readFileAsByteBuffer(String inputFile, boolean directMemory) throws IOException
	{

		FileChannel fc = new FileInputStream(inputFile).getChannel();
		long l = fc.size();

		ByteBuffer bb;

		if (directMemory)
		{
			bb = ByteBuffer.allocateDirect((int) l);
		}
		else
		{
			bb = ByteBuffer.allocate((int) l);
		}

		int read = fc.read(bb);
		fc.close();

		return bb;
	}
}