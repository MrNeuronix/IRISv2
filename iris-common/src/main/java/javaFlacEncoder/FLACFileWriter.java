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

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.spi.AudioFileWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Class provides FLAC encoding using javax sound SPI.
 */
public class FLACFileWriter extends AudioFileWriter {

    /**
     * Specifies a FLAC file.
     */
    public static final Type FLAC = new Type("FLAC", "flac");

    /**
     * FLAC type
     */
    private static final Type flacTypes[] = {FLAC};

    /**
     * Maximum number of bytes to read from file at once
     */
    private static final int MAX_READ = 16384;

    public FLACFileWriter() {

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sound.sampled.spi.AudioFileWriter#getAudioFileTypes()
     */
    @Override
    public Type[] getAudioFileTypes() {
        Type[] localArray = new Type[flacTypes.length];
        System.arraycopy(flacTypes, 0, localArray, 0, flacTypes.length);
        return localArray;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sound.sampled.spi.AudioFileWriter#getAudioFileTypes(javax.sound.sampled.AudioInputStream)
     */
    @Override
    public Type[] getAudioFileTypes(AudioInputStream stream) {
        Type[] filetypes = getAudioFileTypes();

        // make sure we can write this stream
        AudioFormat format = stream.getFormat();
        if (AudioStreamEncoder.getDataFormatSupport(format) == AudioStreamEncoder.SUPPORTED) {
            return filetypes;
        }
        return new Type[0];
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sound.sampled.spi.AudioFileWriter#write(javax.sound.sampled.AudioInputStream,
     * javax.sound.sampled.AudioFileFormat.Type, java.io.File)
     */
    @Override
    public int write(AudioInputStream stream, Type fileType, File out) throws IOException {

        // Check, if writing is supported
        if (!flacTypes[0].equals(fileType)) {
            throw new IllegalArgumentException("File type " + fileType + " not supported.");
        } else if (AudioStreamEncoder.getDataFormatSupport(stream.getFormat()) != AudioStreamEncoder.SUPPORTED) {
            throw new IllegalArgumentException("Data Format not supported");
        }

        FLACFileOutputStream flacFileOutputStream = null;
        int bytesWritten = 0;
        try {
            flacFileOutputStream = new FLACFileOutputStream(out);
            AudioFormat format = stream.getFormat();

            // Sanitize and optimize configurations
            StreamConfiguration streamConfiguration = adjustConfigurations(format);
            EncodingConfiguration encodingConfiguration = new EncodingConfiguration();
            FLACEncoder flac = new FLACEncoder();
            if (!flac.setStreamConfiguration(streamConfiguration) ||
                    !flac.setEncodingConfiguration(encodingConfiguration)) {
                throw new IOException("FLAC encoder initialization failed.");
            }

            flac.setOutputStream(flacFileOutputStream);
            flac.openFLACStream();

            AudioStreamEncoder.encodeAudioInputStream(stream, MAX_READ, flac, false);

        } finally {
            if (flacFileOutputStream != null) {
                bytesWritten = Long.valueOf(flacFileOutputStream.size()).intValue();
                flacFileOutputStream.close();
            }
        }

        return bytesWritten;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.sound.sampled.spi.AudioFileWriter#write(javax.sound.sampled.AudioInputStream,
     * javax.sound.sampled.AudioFileFormat.Type, java.io.OutputStream)
     */
    @Override
    public int write(AudioInputStream stream, Type fileType, OutputStream out) throws IOException {

        // Check, if writing is supported
        if (!flacTypes[0].equals(fileType)) {
            throw new IllegalArgumentException("File type " + fileType + " not supported.");
        } else if (AudioStreamEncoder.getDataFormatSupport(stream.getFormat()) != AudioStreamEncoder.SUPPORTED) {
            throw new IllegalArgumentException("Data Format not supported");
        }

        FLACStreamOutputStream flacStreamOutputStream = null;
        int bytesWritten = 0;
        try {
            flacStreamOutputStream = new FLACStreamOutputStream(out);
            AudioFormat format = stream.getFormat();

            // Sanitize and optimize configurations
            StreamConfiguration streamConfiguration = adjustConfigurations(format);
            EncodingConfiguration encodingConfiguration = new EncodingConfiguration();

            FLACEncoder flac = new FLACEncoder();
            if (!flac.setStreamConfiguration(streamConfiguration) ||
                    !flac.setEncodingConfiguration(encodingConfiguration)) {
                throw new IOException("FLAC encoder initialization failed.");
            }

            flac.setOutputStream(flacStreamOutputStream);
            flac.openFLACStream();

            AudioStreamEncoder.encodeAudioInputStream(stream, MAX_READ, flac, false);

        } finally {
            if (flacStreamOutputStream != null) {
                bytesWritten = Long.valueOf(flacStreamOutputStream.size()).intValue();
                flacStreamOutputStream.close();
            }
        }

        return bytesWritten;
    }

    /**
     * Method sets input stream configuration for encoder.
     *
     * @param format input format
     * @return stream configuration for encoder.
     */
    private StreamConfiguration adjustConfigurations(AudioFormat format) {
        int sampleRate = (int) format.getSampleRate();
        int sampleSize = format.getSampleSizeInBits();
        int channels = format.getChannels();

        StreamConfiguration streamConfiguration = new StreamConfiguration();
        streamConfiguration.setSampleRate(sampleRate);
        streamConfiguration.setBitsPerSample(sampleSize);
        streamConfiguration.setChannelCount(channels);

        return streamConfiguration;
    }
}
