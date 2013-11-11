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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;

/**
 * AudioStreamEncoder provides commonly needed methods for encoding with a
 * FLACEncoder from an AudioInputStream.
 *
 * @author preston
 */
public class AudioStreamEncoder {

    public static final int SUPPORTED = 0;
    public static final int UNSUPPORTED_CHANNELCOUNT = 1;
    public static final int UNSUPPORTED_SAMPLESIZE = 2;
    public static final int UNSUPPORTED_SAMPLERATE = 4;
    public static final int UNSUPPORTED_ENCODINGTYPE = 8;

    /**
     * Encodes the given AudioInputStream, using the given FLACEncoder.
     * FLACEncoder must be in a state to accept samples and encode(FLACOutputStream,
     * EncodingConfiguration, and StreamConfiguration have been set, and FLAC stream
     * has been opened).
     *
     * @param sin
     * @return
     * @throws java.io.IOException
     * @throws IllegalArgumentException thrown if input sample size is not supported
     */
    public static int encodeAudioInputStream(AudioInputStream sin, int maxRead,
                                             FLACEncoder flac, boolean useThreads) throws IOException, IllegalArgumentException {
        AudioFormat format = sin.getFormat();
        int frameSize = format.getFrameSize();
        int sampleSize = format.getSampleSizeInBits();
        int bytesPerSample = sampleSize / 8;
        if (sampleSize % 8 != 0) {
            //end processing now
            throw new IllegalArgumentException("Unsupported Sample Size: size = " + sampleSize);
        }
        int channels = format.getChannels();
        boolean bigEndian = format.isBigEndian();
        boolean isSigned = format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
        byte[] samplesIn = new byte[(int) maxRead];
        int samplesRead;
        int framesRead;
        int[] sampleData = new int[maxRead * channels / frameSize];
        int unencodedSamples = 0;
        int totalSamples = 0;
        while ((samplesRead = sin.read(samplesIn, 0, maxRead)) > 0) {
            framesRead = samplesRead / (frameSize);
            if (bigEndian) {
                for (int i = 0; i < framesRead * channels; i++) {
                    int lower8Mask = 255;
                    int temp = 0;
                    int totalTemp = 0;
                    for (int x = bytesPerSample - 1; x >= 0; x++) {
                        int upShift = 8 * x;
                        if (x == 0)//don't mask...we want sign
                            temp = ((samplesIn[bytesPerSample * i + x]) << upShift);
                        else
                            temp = ((samplesIn[bytesPerSample * i + x] & lower8Mask) << upShift);
                        totalTemp = totalTemp | temp;
                    }
                    if (!isSigned) {
                        int reducer = 1 << (bytesPerSample * 8 - 1);
                        totalTemp -= reducer;
                    }
                    sampleData[i] = totalTemp;
                }
            } else {
                for (int i = 0; i < framesRead * channels; i++) {
                    int lower8Mask = 255;
                    int temp = 0;
                    int totalTemp = 0;
                    for (int x = 0; x < bytesPerSample; x++) {
                        int upShift = 8 * x;
                        if (x == bytesPerSample - 1 && isSigned)//don't mask...we want sign
                            temp = ((samplesIn[bytesPerSample * i + x]) << upShift);
                        else
                            temp = ((samplesIn[bytesPerSample * i + x] & lower8Mask) << upShift);
                        totalTemp = totalTemp | temp;
                    }
                    if (!isSigned) {
                        int reducer = 1 << (bytesPerSample * 8 - 1);
                        totalTemp -= reducer;
                    }
                    sampleData[i] = totalTemp;
                }
            }
            if (framesRead > 0) {
                flac.addSamples(sampleData, framesRead);
                unencodedSamples += framesRead;
            }

            if (useThreads)
                unencodedSamples -= flac.t_encodeSamples(unencodedSamples, false, 5);
            else
                unencodedSamples -= flac.encodeSamples(unencodedSamples, false);
            totalSamples += unencodedSamples;
        }
        totalSamples += unencodedSamples;
        if (useThreads)
            unencodedSamples -= flac.t_encodeSamples(unencodedSamples, true, 5);
        else
            unencodedSamples -= flac.encodeSamples(unencodedSamples, true);
        return totalSamples;
    }

    /**
     * Checks whether the given AudioFormat can be properly encoded by this
     * FLAC library.
     *
     * @param format AudioFormat to test for support.
     * @return Bit positions set according to issue:
     *         Bit Position : Problem-area
     *         0 : Channel count unsupported
     *         1 : Sample size unsupported
     *         2 : Sample Rate unsupported
     *         3 : Encoding Type Unsupported
     *         4-7: unused, always zero.
     *         return value of 0 means supported
     */
    public static int getDataFormatSupport(AudioFormat format) {
        int result = SUPPORTED;
        float sampleRate = format.getSampleRate();
        AudioFormat.Encoding encoding = format.getEncoding();
        if (format.getChannels() > 8 || format.getChannels() < 1)
            result |= UNSUPPORTED_CHANNELCOUNT;
        if (format.getSampleSizeInBits() > 24 || format.getSampleSizeInBits() % 8 != 0)
            result |= UNSUPPORTED_SAMPLESIZE;
        if (sampleRate <= 0 || sampleRate > 655350 || sampleRate == AudioSystem.NOT_SPECIFIED)
            result |= UNSUPPORTED_SAMPLERATE;
        if (!(AudioFormat.Encoding.ALAW.equals(encoding) ||
                AudioFormat.Encoding.ULAW.equals(encoding) ||
                AudioFormat.Encoding.PCM_SIGNED.equals(encoding) ||
                AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)))
            result |= UNSUPPORTED_ENCODINGTYPE;
        return result;
    }
}
