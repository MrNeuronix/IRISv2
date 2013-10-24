package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.09.12
 * Time: 16:22
 */
/**
 * jipCam : The Java IP Camera Project
 * Copyright (C) 2005-2006 Jason Thrasher
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
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream with a built-in parser for MjpegFrame data structures.
 * This parser attempts to optimize reads from the input stream by taking
 * advantage of the Content-Length MJPEG frame header, if it exists.
 * <p/>
 * This class should be used as a direct parser for MJPEG streams from files
 * or the network.  On a P3, 1.8 GHz CPU each 240x352 pixel frame takes less
 * than 1 ms to parse in optimized mode, or about 3 ms to parse if the
 * Content-Length can't be found.  This is only a rough estimate of performance!
 * <p/>
 * Warning: if the underlying stream is read by any other process it will cause
 * frame data loss by this parser, and possible jpeg data corruption.  No other
 * threads should be reading from the stream that is wrapped by this class.
 *
 * @author Jason Thrasher
 */
public class MjpegInputStream extends DataInputStream {

    protected int mSequence = 0;
    protected int mContentLength = -1;
    protected boolean isContentLengthAvailable = false;
    protected boolean isFirstPass = true;

    /**
     * Wrap the given input stream with the MjpegInputStream.  Internal buffers
     * are created automatially.
     *
     * @param in stream to read and parse MJPEG frames from
     */
    public MjpegInputStream(InputStream in) {
        super(new BufferedInputStream(in, MjpegFormat.FRAME_MAX_LENGTH));
    }

    /**
     * Read the next MjpegFrame from the stream.
     *
     * @return the next MJPEG frame.
     * @throws java.io.IOException if there is an error while reading data
     */
    public MjpegFrame readMjpegFrame() throws IOException {
        //mark the start of the frame
        mark(MjpegFormat.FRAME_MAX_LENGTH);

        //get length of header
        int headerLen = MjpegFormat.getStartOfSequence(this,
                JpegFormat.SOI_MARKER); //position of first byte of the jpeg

        if (isFirstPass) {
            //attempt to parse content length
            isFirstPass = false; //do this once
            reset();

            byte[] header = new byte[headerLen];
            readFully(header);

            try {
                mContentLength = MjpegFormat.parseContentLength(header);
                isContentLengthAvailable = true; //flag for more efficientcy
            } catch (NumberFormatException nfe) {
                System.err.println(
                        "couldn't parse content length from header on first pass");
                isContentLengthAvailable = false;
            }
        }

        reset();

        if (isContentLengthAvailable) {
            //the fast way
            byte[] header = new byte[headerLen];
            readFully(header);

            try {
                mContentLength = MjpegFormat.parseContentLength(header);
            } catch (NullPointerException npe) {
                System.err.println(
                        "couldn't parse content length, failover to jpeg EOF search");
                mContentLength = MjpegFormat.getEndOfSeqeunce(this,
                        JpegFormat.EOF_MARKER); //position of first byte after the jpeg
            }

            //(JpegFormat.EOF_MARKER); //position of first byte after the jpeg
        } else {
            //the slow way, because we have to test (if/then) every byte!
            mContentLength = MjpegFormat.getEndOfSeqeunce(this,
                    JpegFormat.EOF_MARKER); //position of first byte AFTER the jpeg
        }

        //create frame array
        byte[] frameData = new byte[headerLen + mContentLength];
        reset();
        readFully(frameData);

        return new MjpegFrame(frameData, mContentLength, mSequence++);
    }
}
