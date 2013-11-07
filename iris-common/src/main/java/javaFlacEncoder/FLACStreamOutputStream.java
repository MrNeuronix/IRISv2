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
import java.io.OutputStream;
import java.io.IOException;
import java.io.Closeable;
/**
 * This class provides basic OutputStream support for writing from a FLACEncoder.
 * 
 * @author Preston Lacey
 */
public class FLACStreamOutputStream implements FLACOutputStream,Closeable {

  OutputStream out = null;
  long size = 0;
  boolean valid;

  /**
   * Constructor. Create a FLACStreamOutputStream using the given OutputStream.
   * @param out OutputStream to write the FLAC stream to.
   */
  public FLACStreamOutputStream(OutputStream out) throws IOException {
    this.out = out;
    size = 0;
  }
    
  /**
   * Attempt to seek to the given location within this stream. It is not
   * guaranteed that all implementations can or will support seeking. Use the
   * method canSeek()
   *
   * @param pos target position to seek to.
   * @return current position after seek attempt.
   */
  public long seek(long pos) {
    throw new UnsupportedOperationException("seek(long) is not supported on by FLACStreamOutputStream");
  }

  /**
   * Write a byte to this stream.
   * @param data byte to write.
   * @throws java.io.IOException IOException will be raised if an error occurred while
   * writing.
   */
  public void write(byte data) throws IOException {
    out.write(data);
    size++;
  }
  /**
   * Write the given number of bytes from the byte array. Return number of
   * bytes written.
   * @param data array containing bytes to be written.
   * @param offset start index of array to begin reading from.
   * @param count number of bytes to write.
   * @return number of bytes written.
   * @throws java.io.IOException IOException upon a write error.
   */
  public int write(byte[] data, int offset, int count) throws IOException {
    int result = count;
    out.write(data,offset,count);
    size += count;
    return result;
  }

  /**
   * Get the number of bytes that have been written by this object.
   * @return total length written.
   */
  public long size() {
    return size;
  }

  /**
   * Test whether this stream is seekable.
   * @return true if stream is seekable, false otherwise
   */
  public boolean canSeek() {
    return false;
  }

  /**
   * Get the current write position of this stream. If this stream cannot seek,
   * this will return 0;
   * @return current write position.
   */
  public long getPos() {
    return 0;
  }

  /**
   * Close OutputStream owned by this object.
   * @throws java.io.IOException
   */
  public void close() throws IOException {
    out.close();
  }
}
