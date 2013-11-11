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
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class defines a FLAC Encoder with a simple interface for enabling FLAC
 * encoding support in an application. This class is appropriate for use in the
 * case where you have raw pcm audio samples that you wish to encode. Currently,
 * fixed-blocksize only is implemented, and the "Maximum Block Size" set in the
 * StreamConfiguration object is used as the actual block size.
 * <br><br><br>
 * An encoding process is simple, and should follow these steps:<br>
 * <BLOCKQUOTE>
 * 1) Set StreamConfiguration to appropriate values. After a stream is opened,
 * this must not be altered until the stream is closed.<br>
 * 2) Set FLACOutputStream, object to write results to.<br>
 * 3) Open FLAC Stream<br>
 * 4) Set EncodingConfiguration(if defaults are insufficient).<br>
 * 5) Add samples to encoder<br>
 * 6) Encode Samples<br>
 * 7) Close stream<br>
 * (note: steps 4,5, and 6 may be done repeatedly, in any order, with the
 * exception that step 4 must not be called while a concurrent step 6 is
 * executing(as in threaded mode). See related method documentation for info
 * on concurrent use)
 * (note: steps 4,5, and 6 may be done repeatedly, in any order, with the
 * exception that step 4 must not be called while a concurrent step 6 is
 * executing(as in threaded mode). See related method documentation for info
 * on concurrent use. For step 7, see the documentation for the
 * encodeSamples(...) methods' "end" parameter)
 * </BLOCKQUOTE><br><br>
 *
 * @author Preston Lacey
 */
public class FLACEncoder {

    /* For debugging, higher level equals more output */
    int DEBUG_LEV = 0;

    /**
     * Maximum Threads to use for encoding frames(more threads than this will
     * exist, these threads are reserved for encoding of frames only).
     */
    private int MAX_THREADED_FRAMES = Runtime.getRuntime().availableProcessors();

    /* encodingConfig: Must never stay null(default supplied by constructor) */
    volatile EncodingConfiguration encodingConfig = null;

    /* streamConfig: Must never stay null(default supplied by constructor) */
    volatile StreamConfiguration streamConfig = null;

    /* Set true if frames are actively being encoded(can't change settings
     * while this is true). Use streamLock for changing. */
    volatile Boolean isEncoding = false;

    /**
     * we set this to true while a flac stream has been opened and not
     * officially closed. Must use a streamLock to set and get this. Some actions
     * must not be taken while a stream is opened
     */
    volatile boolean flacStreamIsOpen = false;

    /* Lock to use when setting/reading/using flacStreamIsOpen variable */
    public final ReentrantLock streamLock = new ReentrantLock();

    /* Lock used when adding samples and when samples must not be added.*/
    private final ReentrantLock sampleLock = new ReentrantLock();

    /* Lock used when handling a finished block */
    private ReentrantLock blockFinishedLock = new ReentrantLock();

    /* Stores unfilled BlockEncodeRequest(not ready for queue, unless ending stream */
    volatile private BlockEncodeRequest unfilledRequest = null;

    /* Stores count of inter-frame samples in unfinishedBlock */
    volatile private int unfinishedBlockUsed = 0;

    /* Frame object used to encode when not using threads */
    volatile Frame frame = null;

    /* Used to calculate MD5 hash */
    FLAC_MD5 md5 = null;

    /* threadManager used with threaded encoding  */
    BlockThreadManager threadManager = null;

    /* threagedFrames keeps track of frames given to threadManager. We must still
     * update the configurations of them as needed. If we ever create new
     * frames(e.g, when changing stream configuration), we must create a new
     * threadManager as well.
     */
    Frame[] threadedFrames = null;

    /* Contains all logic for writes to the FLACOutputStream */
    FLACStreamController flacWriter = null;

    /* set when an IOException has occured that invalidates results
     * in a child encoding thread. IOException temporarily stored by
     * childException when this is true.*/
    boolean error = false;
    /* Throw this if exists, when we can, to notify main thread an exception
     * occured in a child thread.*/
    IOException childException = null;

    /* store used encodeRequests so we don't have to reallocate space for them*/
    LinkedBlockingQueue<BlockEncodeRequest> usedBlockEncodeRequests = null;
    LinkedBlockingQueue<BlockEncodeRequest> preparedRequests = null;
    ArrayRecycler recycler = null;

    /**
     * Constructor which creates a new encoder object with the default settings.
     * The StreamConfiguration should be reset to match the audio used and an
     * output stream set, but the default EncodingConfiguration should be ok for
     * most purposes. When using threaded encoding, the default number of
     * threads used is equal to FLACEncoder.MAX_THREADED_FRAMES.
     */
    public FLACEncoder() {
        usedBlockEncodeRequests = new LinkedBlockingQueue<BlockEncodeRequest>();
        preparedRequests = new LinkedBlockingQueue<BlockEncodeRequest>();
        //usedIntArrays = new LinkedBlockingQueue<int[]>();
        recycler = new ArrayRecycler();
        streamConfig = new StreamConfiguration();
        encodingConfig = new EncodingConfiguration();
        frame = new Frame(streamConfig);
        frame.registerConfiguration(encodingConfig);

        this.prepareThreadManager(streamConfig);
        try {
            md5 = new FLAC_MD5();
            //reset();
            clear();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error! FLACEncoder cannot function" +
                    "without a valid MD5 implementation.", e);
        }
    }

    /**
     * Tell encoder how many threads to use for encoding. More threads than this
     * will exist, but only the given amount should be in a running state at
     * any moment(the other threads are simply manager threads, waiting for
     * encoding-threads to end). A special case is setting "count" to zero; this
     * will tell the encoder not to use internal threads at all, and all
     * encoding will be done with the main thread. Otherwise, any encode methods
     * will return while the encode actually takes place in a separate thread.
     *
     * @param count Number of encoding threads to use. Count > 0 means use that
     *              many independent encoding threads, count == 0 means encode in main thread,
     *              count < 0 is ignored.
     * @return boolean value represents whether requested count was applied or
     *         not. This may be false if a FLAC stream is currently opened.
     */
    public boolean setThreadCount(int count) {
        boolean result = false;
        if (count < 0 || flacStreamIsOpen)
            return false;
        streamLock.lock();
        try {
            if (flacStreamIsOpen)
                result = false;
            else {
                MAX_THREADED_FRAMES = count;
                prepareThreadManager(streamConfig);
                result = true;
            }
        } finally {
            streamLock.unlock();
        }
        return result;
    }

    /**
     * Creates and configures a new BlockThreadManager if needed(or sets to null
     * if threads turned off.) Method must only be called if flacStreamIsOpen
     * equals false, and this must not change while executing this method.
     *
     * @param sc
     */
    private void prepareThreadManager(StreamConfiguration sc) {
        assert (!flacStreamIsOpen);
        if (MAX_THREADED_FRAMES > 0) {
            threadManager = new BlockThreadManager(this);
            threadedFrames = new Frame[MAX_THREADED_FRAMES];
            for (int i = 0; i < MAX_THREADED_FRAMES; i++) {
                threadedFrames[i] = new Frame(this.streamConfig);
                threadManager.addFrameThread(threadedFrames[i]);
            }
        } else {
            threadManager = null;
        }
    }

    /**
     * Get the number of threads this FLACEncoder is currently set to use.
     *
     * @return number of threads this encoder is currently set to use.
     */
    public int getThreadCount() {
        return this.MAX_THREADED_FRAMES;
    }

    /**
     * Set the encoding configuration to that specified. The given encoding
     * configuration is not stored by this object, but instead copied. This
     * is to prevent the alteration of the config during an encode process. This
     * must not be called while an encodeSamples(...) is active, or while
     * encoding threads are active. If using threaded mode, use a blocking-count
     * of zero in t_encodeSamples(...)to ensure the underlying encoding threads
     * have finished before calling this method.
     *
     * @param ec EncodingConfiguration to use.
     * @return true if the configuration was altered; false if the configuration
     *         cannot be altered(such as if another thread is currently encoding).
     */
    public boolean setEncodingConfiguration(EncodingConfiguration ec) {
        boolean changed = false;
        if (!isEncoding && ec != null) {//don't wait if we're already encoding.
            streamLock.lock();
            try {
                if (!isEncoding) {
                    encodingConfig = ec;
                    frame.registerConfiguration(ec);
                    for (int i = 0; i < MAX_THREADED_FRAMES; i++)
                        threadedFrames[i].registerConfiguration(ec);
                    changed = true;
                }
            } finally {
                streamLock.unlock();
            }
        }
        return changed;
    }

    /**
     * Set the stream configuration to that specified. The given stream
     * configuration is not stored by this object, but instead copied. This
     * is to prevent the alteration of the config during an encode process.
     * This method must not be called in the middle of a stream, stream contents
     * may become invalid. Calling this method clears any data stored by this
     * encoder. A call to setStreamConfiguration() should be followed next by
     * setting the output stream if not yet done, and then calling
     * openFLACStream();
     *
     * @param sc StreamConfiguration to use.
     * @return true if the configuration was altered; false if the configuration
     *         cannot be altered(such as if another thread is currently encoding).
     */
    public boolean setStreamConfiguration(StreamConfiguration sc) {
        boolean changed = false;
        sc = new StreamConfiguration(sc);
        if (sc != null) {
            if (flacStreamIsOpen || isEncoding)
                changed = false;
            else {
                streamLock.lock();
                try {
                    if (flacStreamIsOpen || isEncoding) {//can't change streamconfig on open stream.
                        changed = false;
                    } else {
                        streamConfig = sc;
                        reset();
                        frame = new Frame(sc);
                        prepareThreadManager(sc);
                        this.setEncodingConfiguration(this.encodingConfig);
                        clear();
                        changed = true;
                    }
                } finally {
                    streamLock.unlock();
                }
            }
        }
        return changed;
    }

    /**
     * Reset the values to their initial state, in preparation of starting a
     * new stream. Does *not* clear any stored, unwritten data. To flush stored
     * samples, call clear().
     */
    private void reset() {
        md5.getMD().reset();
        if (flacWriter != null)
            flacWriter = new FLACStreamController(flacWriter.getFLACOutputStream(), streamConfig);
    }

    /**
     * Clear all samples stored by this object, but not yet encoded. Should be
     * called between encoding differrent streams(before more samples are added),
     * unless you desire to keep unencoded samples. This does NOT reset or close
     * the active stream.
     */
    public final void clear() {
        unfilledRequest = null;
        this.preparedRequests.clear();
    }

    /**
     * Close the current FLAC stream. Updates the stream header information.
     * If called on a closed stream, operation is undefined. Do not do this.
     */
    private void closeFLACStream() throws IOException {
        //reset position in output stream to beginning.
        //re-write the updated stream info.
        checkForThreadErrors();
        if (DEBUG_LEV > 0)
            System.err.println("FLACEncoder::closeFLACStream : Begin");
        streamLock.lock();
        try {
            if (!flacStreamIsOpen)
                throw new IllegalStateException("Cannot close a non-opened stream");
            byte[] md5Hash = md5.getMD().digest();
            flacWriter.closeFLACStream(md5Hash, streamConfig);
            flacStreamIsOpen = false;
        } finally {
            streamLock.unlock();
        }
    }

    /**
     * Begin a new FLAC stream. Prior to calling this, you must have already
     * set the StreamConfiguration and the output stream, both of which must not
     * change until encoding is finished and the stream is closed. If this
     * FLACEncoder object has already been used to encode a stream, unencoded
     * samples may still be stored. Use clear() to dump them prior to calling
     * this method(if clear() not called, and samples are instead retained, the
     * StreamConfiguration must NOT have changed from the prior stream.
     *
     * @throws java.io.IOException if there is an error writing the headers to output.
     */
    public void openFLACStream() throws IOException {
        streamLock.lock();
        try {
            flacWriter.openFLACStream();
            flacStreamIsOpen = true;
        } finally {
            streamLock.unlock();
        }
    }

    private BlockEncodeRequest prepareRequest(int blockSize, int channels) {
        //int[] block = blockQueue.elementAt(0);
        int[] block = recycler.getArray(blockSize * channels);
        BlockEncodeRequest ber = usedBlockEncodeRequests.poll();
        if (ber == null)
            ber = new BlockEncodeRequest();
        EncodedElement result = new EncodedElement(1, 0);
        ber.setAll(block, 0, 0, channels - 1, 0, result);
        return ber;
    }

    /**
     * Add samples to the encoder, so they may then be encoded. This method uses
     * breaks the samples into blocks, which will then be made available to
     * encode.
     *
     * @param samples Array holding the samples to encode. For all multi-channel
     *                audio, the samples must be interleaved in this array. For example, with
     *                stereo: sample 0 will belong to the first channel, 1 the second, 2 the
     *                first, 3 the second, etc. Samples are interpreted according to the
     *                current configuration(for things such as channel and bits-per-sample).
     * @param count   Number of interchannel samples to add. For example, with
     *                stero: if this is 4000, then "samples" must contain 4000 left samples and
     *                4000 right samples, interleaved in the array.
     * @return true if samples were added, false otherwise. A value of false may
     *         result if "count" is set to a size that is too large to be valid with the
     *         given array and current configuration.
     */
    public void addSamples(int[] samples, int count) {
        assert (count * streamConfig.getChannelCount() <= samples.length);
        if (samples.length < count * streamConfig.getChannelCount())
            throw new IllegalArgumentException("count given exceeds samples array bounds");
        sampleLock.lock();
        try {
            //get number of channels
            int channels = streamConfig.getChannelCount();
            int maxBlock = streamConfig.getMaxBlockSize();
            if (unfilledRequest == null)
                unfilledRequest = prepareRequest(maxBlock, channels);
            int remaining = count;
            int offset = 0;
            while (remaining > 0) {
                int newRemaining = unfilledRequest.addInterleavedSamples(samples, offset, remaining, maxBlock);
                offset += (remaining - newRemaining) * channels;
                remaining = newRemaining;
                if (unfilledRequest.isFull(maxBlock)) {
                    this.preparedRequests.add(unfilledRequest);
                    unfilledRequest = null;
                }
                if (remaining > 0) {
                    unfilledRequest = prepareRequest(maxBlock, channels);
                }
            }
        } finally {
            sampleLock.unlock();
        }
    }

    private void writeFinishedBlock(BlockEncodeRequest ber) throws IOException {
        flacWriter.writeBlock(ber);
        md5.addSamplesToMD5(ber.samples, ber.encodedSamples, ber.skip + 1,
                streamConfig.getBitsPerSample());
        recycler.add(ber.samples);
        ber.result = null;
        ber.samples = null;
        usedBlockEncodeRequests.add(ber);
        if (threadManager.getTotalManagedCount() == 1) {//this is the final block
            streamLock.lock();
            try {
                if (threadManager.getTotalManagedCount() == 1)
                    isEncoding = false;
            } finally {
                streamLock.unlock();
            }
        }
    }

    /**
     * Notify the encoder that a BlockEncodeRequest has finished, and is now
     * ready to be written to file. The encoder expects that these requests come
     * back in the same order the encoder sent them out. This is intended to
     * be used in threading mode only at the moment(sending them to a
     * BlockThreadManager object)
     *
     * @param ber BlockEncodeRequest that is ready to write to file.
     */
    protected void blockFinished(BlockEncodeRequest ber) {
        assert (flacStreamIsOpen);
        blockFinishedLock.lock();
        try {
            writeFinishedBlock(ber);
        } catch (IOException e) {
            error = true;
            if (childException != null)
                childException = e;
        } finally {
            blockFinishedLock.unlock();
        }
    }

    /**
     * Attempts to throw a stored exception that had been caught from a child
     * thread. This method should be called regularly in any public method to
     * let the calling thread know a problem occured.
     *
     * @throws java.io.IOException
     */
    private void checkForThreadErrors() throws IOException {
        if (error == true && childException != null) {
            error = false;
            IOException temp = childException;
            childException = null;
            throw temp;
        }
    }

    /**
     * Attempt to Encode a certain number of samples(threaded version).
     * Encodes as close to count as possible. Uses multiple threads to speed up
     * encoding. If getThreadCount() <= 0, simply calls the non-threaded version,
     * encodeSamples(...), and blocks until it returns.
     *
     * @param count         number of samples to attempt to encode. Actual number
     *                      encoded may be greater or less if count does not end on a block boundary.
     *                      If "end" is false, we may set this value to something absurdly high, such
     *                      as Integer.MAX_VALUE to ensure all available, full blocks are encoded.
     * @param end           true to finalize stream after encode, false otherwise. If set
     *                      to true, and return value is greater than or equal to given count, no
     *                      more encoding must be attempted until a new stream is began.
     * @param blockingCount value is used for flow-control into this encoder.
     *                      This method will block until fewer than the given number of blocks remain
     *                      queued for encoding.
     * @return number of samples encoded. This may be greater or less than
     *         requested count if count does not end on a block boundary. This is NOT an
     *         error condition. If end was set "true", and returned count is less than
     *         requested count, then end was NOT done, if you still wish to end stream,
     *         call this again with end true and a count of of <= samplesAvailableToEncode()
     * @throws java.io.IOException if there was an error writing the results to output
     *                             stream.
     */
    public int t_encodeSamples(final int inCount, final boolean end, int blockingCount)
            throws IOException {
        int count = inCount;
        if (MAX_THREADED_FRAMES <= 0)
            return encodeSamples(count, end);
        int encodedCount = 0;
        if (end)
            sampleLock.lock();//lock to avoid race condition in unfinishedBlock section.
        try {
            checkForThreadErrors();
            streamLock.lock();
            try {
                while (count > 0 && preparedRequests.size() > 0) {
                    BlockEncodeRequest ber = preparedRequests.poll();
                    int encodedSamples = ber.count;
                    //ber.frameNumber = nextFrameNumber++;
                    ber.frameNumber = flacWriter.incrementFrameNumber();
                    threadManager.addRequest(ber);
                    isEncoding = true;
                    count -= encodedSamples;
                    encodedCount += encodedSamples;
                }
            } finally {
                streamLock.unlock();
            }
            threadManager.blockWhileQueueExceeds(blockingCount);
            if (end) {
                streamLock.lock();
                try {
                    if (count > 0 && unfilledRequest != null && unfilledRequest.count >= count) {
                        int encodedSamples = unfilledRequest.count;
                        threadManager.addRequest(unfilledRequest);
                        unfilledRequest = null;
                        isEncoding = true;
                        count -= encodedSamples;
                        encodedCount += encodedSamples;
                    }
                } finally {
                    streamLock.unlock();
                }
                //block while requests remain!!!!
                threadManager.blockWhileQueueExceeds(0);
                threadManager.stop();
            }
            //handle "end" setting
            if (end && encodedCount >= inCount) {//close if all requests were written.
                closeFLACStream();
            }
        } finally {
            if (end && sampleLock.isHeldByCurrentThread())
                sampleLock.unlock();
        }

        return encodedCount;
    }

    /**
     * Attempt to Encode a certain number of samples. Encodes as close to count
     * as possible.
     *
     * @param count number of samples to attempt to encode. Actual number
     *              encoded may be greater or less if count does not end on a block boundary.
     * @param end   true to finalize stream after encode, false otherwise. If set
     *              to true, and return value is greater than or equal to given count, no
     *              more encoding must be attempted until a new stream is began.
     * @return number of samples encoded. This may be greater or less than
     *         requested count if count does not end on a block boundary. This is NOT an
     *         error condition. If end was set "true", and returned count is less than
     *         requested count, then end was NOT done, if you still wish to end stream,
     *         call this again with end true and a count of of <= samplesAvailableToEncode()
     * @throws java.io.IOException if there was an error writing the results to file.
     */
    public int encodeSamples(int count, final boolean end) throws IOException {
        int encodedCount = 0;
        streamLock.lock();
        try {
            checkForThreadErrors();
            int channels = streamConfig.getChannelCount();
            boolean encodeError = false;
            while (count > 0 && preparedRequests.size() > 0 && !encodeError) {
                BlockEncodeRequest ber = preparedRequests.peek();
                int encodedSamples = encodeRequest(ber, channels);
                if (encodedSamples < 0) {
                    //ERROR! Return immediately. Do not add results to output.
                    System.err.println("FLACEncoder::encodeSamples : Error in encoding");
                    encodeError = true;
                    break;
                }
                preparedRequests.poll();//pop top off now that we've written.
                encodedCount += encodedSamples;
                count -= encodedSamples;
            }
            //handle "end" setting
            if (end) {
                if (threadManager != null)
                    threadManager.stop();
                //if(end && !encodeError && this.samplesAvailableToEncode() >= count) {
                if (count > 0 && unfilledRequest != null && unfilledRequest.count >= count) {
                    //handle remaining count
                    BlockEncodeRequest ber = unfilledRequest;
                    int encodedSamples = encodeRequest(ber, channels);
                    if (encodedSamples < 0) {
                        //ERROR! Return immediately. Do not add results to output.
                        System.err.println("FLACEncoder::encodeSamples : (end)Error in encoding");
                        count = -1;
                    } else {
                        count -= encodedSamples;
                        encodedCount += encodedSamples;
                        unfilledRequest = null;
                    }
                }
                if (count <= 0) {//close stream if all requested were written.
                    closeFLACStream();
                }
            } else if (end == true) {
                if (DEBUG_LEV > 30)
                    System.err.println("End set but not done. Error possible. " +
                            "This can also happen if number of samples requested to " +
                            "encode exeeds available samples");
            }
        } finally {
            streamLock.unlock();
        }
        return encodedCount;
    }

    private int encodeRequest(BlockEncodeRequest ber, int channels) throws IOException {
        ber.frameNumber = flacWriter.incrementFrameNumber();
        int[] block = ber.samples;
        int encodedSamples = ber.count;
        EncodedElement result = ber.result;
        int encoded = frame.encodeSamples(block, encodedSamples, 0, channels - 1,
                result, ber.frameNumber);
        if (encoded != encodedSamples) {
            //ERROR! Return immediately. Do not add results to output.
            System.err.println("FLACEncoder::encodeSamples : Error in encoding");
            return -1;
        }
        ber.encodedSamples = encoded;
        writeFinishedBlock(ber);

        return encodedSamples;
    }

    /**
     * Get number of samples which are ready to encode. More samples may exist
     * in the encoder as a partial block. Use samplesAvailableToEncode() if you
     * wish to include those as well.
     *
     * @return number of samples in full blocks, ready to encode.
     */
    public int fullBlockSamplesAvailableToEncode() {
        int available = 0;
        int channels = streamConfig.getChannelCount();
        for (BlockEncodeRequest ber : preparedRequests) {
            int[] block = ber.samples;
            available += block.length / channels;
        }
        return available;
    }

    /**
     * Get number of samples that are available to encode. This includes samples
     * which are in a partial block(and so would only be written if "end" was
     * set true in encodeSamples(int count,boolean end);
     *
     * @return number of samples availble to encode.
     */
    public int samplesAvailableToEncode() {
        int available = 0;
        //sum all in blockQueue
        int channels = streamConfig.getChannelCount();
        for (BlockEncodeRequest ber : preparedRequests) {
            int[] block = ber.samples;
            available += block.length / channels;
        }
        available += unfilledRequest.count;
        return available;
    }

    /**
     * Set the output stream to use. This must not be called while an encode
     * process is active, or a flac stream is already opened.
     *
     * @param fos output stream to use. This must not be null.
     */
    public void setOutputStream(FLACOutputStream fos) {
        if (fos == null)
            throw new IllegalArgumentException("FLACOutputStream fos must not be null.");
        if (flacWriter == null)
            flacWriter = new FLACStreamController(fos, streamConfig);
        else
            flacWriter.setFLACOutputStream(fos);
    }
}
