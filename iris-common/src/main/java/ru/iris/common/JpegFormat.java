package ru.iris.common;

/**
 * IRIS-X Project
 * Author: Nikolay A. Viguro
 * WWW: smart.ph-systems.ru
 * E-Mail: nv@ph-systems.ru
 * Date: 27.09.12
 * Time: 16:25
 */

import java.io.DataInputStream;
import java.io.IOException;


/**
 * Constants for the Axis JPEG Format documented at:
 * http://www2.axis.com/files/developer/camera/JPEG_format_1_1.pdf
 */
public class JpegFormat {
    /**
     * Start Of Image marker.
     * Size: 2 bytes
     * The first two bytes of every image.
     */
    public static final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};

    /**
     * JFIF (JPEG File Interchange Format) marker.
     * Size: 18 bytes including marker
     * Bytes three and four of every image.
     */
    public static final byte[] APP0_MARKER = {(byte) 0xFF, (byte) 0xE0};

    /**
     * Product Information Comment marker.
     * Includes the product information comment information such as
     * Hardware ID, firmware version and serial number.
     * <p/>
     * Size: 17 bytes including marker
     * <p/>
     * After these bytes, the next two bytes will be the length of the comment
     * (always equal to 15 - 0x000F) in bytes, including the two length bytes.
     */
    public static final byte[] COM_MARKER = {(byte) 0xFF, (byte) 0xFE};

    /**
     * End Of Image, size: 2 bytes
     * The last two bytes of every JPEG image.
     */
    public static final byte[] EOF_MARKER = {(byte) 0xFF, (byte) 0xD9};

    /**
     * Quantization Table - Luminance (Y)
     * or Quantization Table - Chrominance (Cb/Cr)
     * Size: 69
     */
    public static final byte[] DQT_MARKER = {(byte) 0xFF, (byte) 0xDB};

    /**
     * Start Of Frame (19 color/13 black&white)
     * Size: 19/13
     */
    public static final byte[] SOF_MARKER = {(byte) 0xFF, (byte) 0xC0};

    /**
     * Huffman Table - Luminance (Y) - DC Diff
     * or Huffman Table - Luminance (Y) - AC Coeff
     * or Huffman Table - Chrominance (Cb/Cr) - DC Diff
     * or Huffman Table - Chrominance (Cb/Cr) - AC Coeff
     * Size: 33, 183, 33, 183 respectively
     */
    public static final byte[] DHT_MARKER = {(byte) 0xFF, (byte) 0xC4};

    /**
     * Start Of Scan (14 color/13 black&white)
     * Size: 14/10
     */
    public static final byte[] SOS_MARKER = {(byte) 0xFF, (byte) 0xDA};

    /**
     * Typical max length of the jpeg data.
     */
    public static int JPEG_MAX_LENGTH = 3 * 240 * 352;

    /**
     * @param sequence
     * @return the index of the first byte after the given sequence, or -1 if not found
     * @throws java.io.IOException
     */
    public static int getEndOfSeqeunce(DataInputStream in, byte[] sequence)
            throws IOException {
        int seqIndex = 0; //tracks number of sequence chars found
        byte c;

        for (int i = 0; i < (MjpegFormat.FRAME_MAX_LENGTH); i++) {
            c = (byte) in.readUnsignedByte(); //read next byte

            //System.out.println("JPEG find "+i+": " + Integer.toHexString(sequence[seqIndex]) + " " + Integer.toHexString(c));
            if (c == sequence[seqIndex]) {
                seqIndex++; //increment seq char found index

                //check if we have the whole sequence
                if (seqIndex == sequence.length) {
                    //mIn.reset(); //reset to beginning of header
                    return i + 1;
                }
            } else {
                //reset index if we don't find all sequence characters before breaking
                seqIndex = 0;
            }
        }

        //mIn.reset(); //reset to beginning of header
        return -1;
    }

    /**
     * Get the index of of the beginning of the sequence
     */
    public static int getStartOfSequence(DataInputStream in, byte[] sequence)
            throws IOException {
        int end = getEndOfSeqeunce(in, sequence);

        return (end < 0) ? (-1) : (end - sequence.length);
    }
}
