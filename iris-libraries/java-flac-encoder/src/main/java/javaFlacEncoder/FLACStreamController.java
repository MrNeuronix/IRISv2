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
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
/**
 *
 * @author preston
 */
public class FLACStreamController {
  public static int DEBUG_LEV = 0;
  /** we set this to true while a flac stream has been opened and not
   * officially closed. Must use a streamLock to set and get this. Some actions
   * must not be taken while a stream is opened */
  volatile boolean flacStreamIsOpen = false;

  private final ReentrantLock streamLock = new ReentrantLock();
  
  /* Object to write results to. Must be set before opening stream */
  private FLACOutputStream out = null;

  /* contains FLAC_id used in the flac stream header to signify FLAC format */
  EncodedElement FLAC_id = FLACStreamIdentifier.getIdentifier();

  /* total number of samples encoded to output. Used in stream header */
  volatile long samplesInStream;

  /* next frame number to use */
  volatile long nextFrameNumber = 0;

  /* position of header in output stream location(needed so we can update
   * the header info(md5, minBlockSize, etc), once encoding is done
   */
  long streamHeaderPos = 0;

    /* minimum frame size seen so far. Used in the stream header */
  volatile int minFrameSize = 0x7FFFFFFF;

  /* maximum frame size seen so far. Used in stream header */
  volatile int maxFrameSize = 0;

  /* minimum block size used so far. Used in stream header */
  volatile int minBlockSize = 0x7FFFFFFF;

  /* maximum block size used so far. Used in stream header */
  volatile int maxBlockSize = 0;

  StreamConfiguration streamConfig = null;
  public FLACStreamController(FLACOutputStream fos, StreamConfiguration sc) {
    out = fos;
    streamConfig = new StreamConfiguration(sc);
    minFrameSize = 0x7FFFFFFF;
    maxFrameSize = 0;
    minBlockSize = 0x7FFFFFFF;
    maxBlockSize = 0;
    samplesInStream = 0;
    streamHeaderPos = 0;
    nextFrameNumber = 0;
  }
  public void setFLACOutputStream(FLACOutputStream fos) {
    streamLock.lock();
    try {
      if(flacStreamIsOpen)
        throw new IllegalStateException("Cannot set new output stream while flac stream is open");
      out = fos;
    }finally {
      streamLock.unlock();
    }
  }
  public FLACOutputStream getFLACOutputStream() { return out; }


  /**
   * Close the current FLAC stream. Updates the stream header information.
   * If called on a closed stream, operation is undefined. Do not do this.
   */
  public void closeFLACStream(byte[] md5Hash, StreamConfiguration streamConfig)
      throws IOException {
    //reset position in output stream to beginning.
    //re-write the updated stream info.
    streamLock.lock();
    try {
      if(!flacStreamIsOpen)
        throw new IllegalStateException("Error. Cannot close a non-opened stream");
      StreamConfiguration tempSC = new StreamConfiguration(streamConfig);
      tempSC.setMaxBlockSize(maxBlockSize);
      tempSC.setMinBlockSize(minBlockSize);
      EncodedElement streamInfo =  MetadataBlockStreamInfo.getStreamInfo(
          tempSC, minFrameSize, maxFrameSize, samplesInStream, md5Hash);
      if(out.canSeek()) {
        out.seek(streamHeaderPos);
        this.writeDataToOutput(streamInfo);
      }
      flacStreamIsOpen = false;
    } finally {
       streamLock.unlock();
    }
  }


/**
   * Write the data stored in an EncodedElement to the output stream.
   * All data will be written along byte boundaries, but the elements in the
   * given list need not end on byte boundaries. If the data of an element
   * does not end on a byte boundary, then the space remaining in that last
   * byte will be used as an offset, and merged(using an "OR"), with the first
   * byte of the following element.
   *
   * @param data
   * @return
   * @throws java.io.IOException
   */
  private int writeDataToOutput(EncodedElement data) throws IOException {
    
    int writtenBytes = 0;
    int offset = 0;
    EncodedElement current = data;
    int currentByte = 0;
    byte unfullByte = 0;
    byte[] eleData = null;
    int usableBits = 0;
    int lastByte = 0;
    while(current != null) {
      eleData = current.getData();
      usableBits = current.getUsableBits();
      currentByte = 0;
      //if offset is not zero, merge first byte with existing byte
      if(offset != 0) {
        unfullByte = (byte)(unfullByte | eleData[currentByte++]);
        out.write(unfullByte);
      }
      //write all full bytes of element.
      lastByte = usableBits/8;
      if(lastByte > 0)
        out.write(eleData, currentByte, lastByte-currentByte);
      //save non-full byte(if present), and set "offset" for next element.
      offset = usableBits %8;
      if(offset != 0) {
        unfullByte = eleData[lastByte];
      }
      //update current.
      current = current.getNext();
    }
    //if non-full byte remains. write.
    if(offset != 0) {
      out.write(eleData, lastByte, 1);
    }
    return writtenBytes;
  }

  public long incrementFrameNumber() {
    return nextFrameNumber++;
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
      //reset all data.
      reset();
      flacStreamIsOpen = true;
      //write FLAC stream identifier
      out.write(FLAC_id.getData(), 0, FLAC_id.getUsableBits()/8);
      //write stream headers. These must be updated at close of stream
      byte[] md5Hash = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};//blank hash. Don't know it yet.
      EncodedElement streamInfo =  MetadataBlockStreamInfo.getStreamInfo(
          streamConfig, minFrameSize, maxFrameSize, samplesInStream, md5Hash);
      //mark stream info location(so we can return to it and re-write headers,
      //  assuming stream is seekable. Then write header.
      int size = streamInfo.getUsableBits()/8;
      EncodedElement metadataBlockHeader =
          MetadataBlockHeader.getMetadataBlockHeader(false,
          MetadataBlockHeader.MetadataBlockType.STREAMINFO, size);
      this.writeDataToOutput(metadataBlockHeader);
      streamHeaderPos = out.getPos();
      out.write(streamInfo.getData(), 0, size);
      writePaddingToFoolJFlac();
    }finally {
       streamLock.unlock();
    }
  }
  private void reset() {
    minFrameSize = 0x7FFFFFFF;
    maxFrameSize = 0;
    minBlockSize = 0x7FFFFFFF;
    maxBlockSize = 0;
    samplesInStream = 0;
    streamHeaderPos = 0;
    nextFrameNumber = 0;
  }
  private void writePaddingToFoolJFlac() throws IOException {
    int size = 40;
    byte[] padding = new byte[size];
    EncodedElement metadataBlockHeader =
        MetadataBlockHeader.getMetadataBlockHeader(true,
        MetadataBlockHeader.MetadataBlockType.PADDING, 40);
    this.writeDataToOutput(metadataBlockHeader);
    out.write(padding, 0,size);
  }

  public void writeBlock(BlockEncodeRequest ber) throws IOException {
    if(!flacStreamIsOpen)
      throw new IllegalStateException("Cannot write on a non-opened stream");
    writeDataToOutput(ber.result.getNext());
    //update encodedCount and count, and blocks, MD5
    if(ber.count != ber.encodedSamples) {
      System.err.println("Error encoding frame number: "+
          ber.frameNumber+", FLAC stream potentially invalid");
    }
    samplesInStream += ber.encodedSamples;
    if(ber.encodedSamples > maxBlockSize)
      maxBlockSize = ber.encodedSamples;
    if(ber.encodedSamples < minBlockSize)
      minBlockSize = ber.encodedSamples;
    int frameSize = ber.result.getTotalBits()/8;
    if(frameSize > maxFrameSize) maxFrameSize = frameSize;
    if(frameSize < minFrameSize) minFrameSize = frameSize;
  }

}
