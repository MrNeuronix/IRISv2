/*
 * Copyright (C) 2010  Preston Lacey http://javaflacencoder.sourceforge.net/
 * All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package javaFlacEncoder;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * This class provides basic file output for writing from a FLACEncoder.
 *
 * @author Preston Lacey
 */
public class FLACFileOutputStream implements FLACOutputStream, Closeable {

    FileOutputStream fos = null;
    long position;
    long size = 0;
    boolean valid;

    /**
     * Constructor. Create a FLACFileOutputStream using the given filename. If
     * file exists, file will be overwritten.
     *
     * @param filename file to connect to output stream.
     */
    public FLACFileOutputStream(String filename) throws IOException {
        position = 0;
        fos = new FileOutputStream(filename);
        valid = true;
    }

    public FLACFileOutputStream(File file) throws IOException {
        position = 0;
        fos = new FileOutputStream(file);
        valid = true;
    }

    /**
     * Constructor. Create a FLACFileOutputStream using the given FileOutputStream.
     *
     * @param fos FileOutputStream to write to, must be open. Current position
     *            of fos will be used as this object's position.
     * @throws java.io.IOException
     */
    public FLACFileOutputStream(FileOutputStream fos) throws IOException {
        FileChannel fc = fos.getChannel();
        position = fc.position();
        this.fos = fos;
        valid = true;
    }

    /**
     * Get the status of this file stream(whether the file was successfully open
     * or not).
     *
     * @return true if file was successfully opened, false otherwise.
     */
    @Deprecated
    public boolean isValid() {
        return valid;
    }

    /**
     * Attempt to seek to the given location within this stream. It is not
     * guaranteed that all implementations can or will support seeking. Use the
     * method canSeek()
     *
     * @param pos target position to seek to.
     * @return current position after seek attempt.
     */
    public long seek(long pos) throws IOException {
        FileChannel fc = fos.getChannel();
        fc.position(pos);
        return pos;
    }

    /**
     * Write a byte to this stream.
     *
     * @param data byte to write.
     * @throws java.io.IOException IOException will be raised if an error occurred while
     *                             writing.
     */
    public void write(byte data) throws IOException {
        fos.write(data);
        if (position + 1 > size)
            size = position + 1;
        position += 1;
    }

    /**
     * Write the given number of bytes from the byte array. Return number of
     * bytes written.
     *
     * @param data   array containing bytes to be written.
     * @param offset start index of array to begin reading from.
     * @param count  number of bytes to write.
     * @return number of bytes written.
     * @throws java.io.IOException IOException upon a write error.
     */
    public int write(byte[] data, int offset, int count) throws IOException {
        int result = count;
        try {
            fos.write(data, offset, count);
            if (position + count > size)
                size = position + count;
            position += count;
        } catch (IOException e) {
            throw e;
        }
        return result;
    }

    /**
     * Get the number of bytes that have been written in length!
     * This takes into account seeking to different portions.
     *
     * @return total length written.
     */
    public long size() {
        return size;
    }

    /**
     * Test whether this stream is seekable.
     *
     * @return true if stream is seekable, false otherwise
     */
    public boolean canSeek() {
        return true;
    }

    /**
     * Get the current write position of this stream.
     *
     * @return current write position.
     */
    public long getPos() {
        return position;
    }

    /**
     * Close FileOutputStream owned by this object.
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        fos.close();
    }
}
