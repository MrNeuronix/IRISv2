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

import java.io.IOException;
/**
 * This interface defines a location to write the output of the FLAC
 * encoder to. We don't want to require that the entire stream is buffered in the
 * encoder prior to being written, as that could require significant memory and
 * would make live handling of streams impossible. However, we can't write the
 * stream headers completely until the entire stream is encoded(specifically
 * because the MD5 hash which appears at the beginning of the FLAC stream,
 * isn't known till the last audio value is given to the encoder). Therefore,
 * the output stream would ideally be seekable, which prevents us from
 * outputting to just a standard "OutputStream". So we can't guarantee the
 * stream is seekable, can't write everything in order given, but can't always
 * buffer till we have the data for the stream headers. This interface allows
 * the implementation to determine the proper tradeoffs. Following is a
 * description of how the FLACEncoder class will treat objects of this type
 * <br><br><BLOCKQUOTE>
 * If canSeek() returns false: The file will be written as normal, but the
 * headers will not be updated once the stream is closed. This means the FLAC
 * file will not contain a count of the total number of samples, nor the MD5
 * hash of the original input(used for verifying the data).<br>
 * If canSeek() returns true: Data will be written as it becomes available, and
 * the encoder will seek() to a point near the beginning of the stream to fix
 * the stream headers once the stream is closed. <br>
 * </BLOCKQUOTE>
 * @author Preston Lacey
 *
 *
 */
public interface FLACOutputStream {

  /**
   * Attempt to seek to the given position.
   *
   * @param pos target position.
   * @return current position after seek.
   */
  public long seek(long pos) throws IOException;

  /**
   * Write the given number of bytes from a byte array.
   *
   * @param data array containing source bytes to write
   * @param offset index of source array to begin reading from.
   * @param count number of bytes to write.
   * @return number of bytes written.
   * @throws java.io.IOException IOException raised upon write error.
   */
  public int write(byte[] data, int offset, int count) throws IOException;

  /**
   * Get the number of bytes that have been written in length.
   * This takes into account seeking to different portions.
   *
   * @return total writtne length of stream.
   */
  public long size();

  /**
   * Write a single byte to the stream.
   *
   * @param data byte to write.
   * @throws java.io.IOException  IOException raised upon write error.
   */
  public void write(byte data) throws IOException;

  /**
   * Test whether this object allows seeking.
   *
   * @return true if seeking is allowed, false otherwise.
   */
  public boolean canSeek();

  /**
   * Get current write position of this stream. If stream cannot seek, then
   * this will return 0;
   *
   * @return current write position.
   */
  public long getPos();
}
