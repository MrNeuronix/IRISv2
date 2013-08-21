package ru.phsystems.irisv2.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.09.12
 * Time: 16:24
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

public class MjpegFormat extends JpegFormat {
    /**
     * The MJPEG framing header should always contain a content type line as:
     * Content-Type: image/jpeg
     */
    public static final String CONTENT_TYPE = "Content-Type";

    /**
     * Optional MJPEG frame header key used to indicate bytes of jpeg file data.
     * This header is optional and depends on the API call used with the camera.
     */
    public static final String CONTENT_LENGTH = "Content-Length";

    /**
     * Optional MJPEG frame header key used to indicate milliseconds between frames.
     * This header is optional and depends on the API call used with the camera.
     */
    public static final String DELTA_TIME = "Delta-time";

    /**
     * Typical max length of header data.
     */
    public static int HEADER_MAX_LENGTH = 100;

    /**
     * Expected length of an mjpeg frame
     */
    public static int FRAME_MAX_LENGTH = JpegFormat.JPEG_MAX_LENGTH +
            HEADER_MAX_LENGTH;

    /**
     * Parse the content length string for a MJPEG frame from the given bytes.
     * The string is parsed into an int and returned.
     *
     * @return the Content-Length, or -1 if not found
     */
    public static int parseContentLength(byte[] headerBytes)
            throws IOException, NumberFormatException {
        return parseContentLength(new ByteArrayInputStream(headerBytes));
    }

    private static int parseContentLength(ByteArrayInputStream headerIn)
            throws IOException, NumberFormatException {
        Properties props = new Properties();
        props.load(headerIn);

        return Integer.parseInt(props.getProperty(MjpegFormat.CONTENT_LENGTH));
    }
}
