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
/**
 * Handles taking a set of audio samples, and splitting it into the proper
 * subframes, and returning the resulting encoded data. This object will do any calculations
 * needed for preparing the “channel configuration” used in encoding, such as mid-side or
 * left-right(based upon the given configuration).
 * @author Preston Lacey
 */
public class Frame {

  /** For debugging: Higher level equals more output(generally in increments
   * of 10 */
  public static int DEBUG_LEV = 0;

  /* track the size of the last encoded frame */
  private int lastEncodeSize;
  /* Current EncodingConfiguration used. This must NOT be changed while a
   * Frame is being encoded, but may be changed between frames */
  EncodingConfiguration ec;

  /* Current stream configuration */
  StreamConfiguration sc;
  /* Number of channels currently configured. This comes from setting the
   * StreamConfiguration(and so is slightly redundant)
   */
  int channels;
  /* bits per sample as set by the StreamConfiguration...this is redundant */
  int bitsPerSample;
  /* Object used to generate the headers needed for FLAC frames */
  FrameHeader frameHeader;
  /* Used to calculate CRC16's of each frame */
  CRC16 crc16;
  /* Used for calculation of verbatimSubframes */
  Subframe_Verbatim verbatimSubframe;
  /* Used for calculation of fixedSubframes */
  Subframe_Fixed fixedSubframe;
  /* Used for calculation of lpcSubframes */
  Subframe_LPC lpcSubframe;
  /* Used for calculation of constantSubframes */
  Subframe_Constant constantSubframe;
  /* Flag tracking whether we need to test for a constant subframe */
  boolean testConstant;
  long[] _decorrelationResults = new long[4];
  boolean[] _constantCandidate = new boolean[4];
  int[] _midSideSamples = null;
  ChannelData[] indChanData = null;
  ChannelData[] msChanData = null;
  long[] r_sums = new long[4];
  /**
   * Constructor. Private to prevent it's use(if a StreamConfiguration isn't
   * set, then most methods will fail in an undefined fashion.
   */
  private Frame() {
  }

  /**
   * Constructor. Sets the StreamConfiguration to use at creation of object.
   * If the StreamConfiguration needs to be changed, you *MUST* create a new
   * Frame object.
   *
   * @param sc StreamConfiguration to use for encoding with this frame.
   */
  public Frame(StreamConfiguration sc) {
    lastEncodeSize = 0;
    channels = sc.getChannelCount();
    this.sc = sc;
    frameHeader = new FrameHeader();
    crc16 = new CRC16();
    ec = null;
    verbatimSubframe = new Subframe_Verbatim(sc);
    fixedSubframe = new Subframe_Fixed(sc);
    lpcSubframe = new Subframe_LPC(sc);
    constantSubframe = new Subframe_Constant(sc);
    bitsPerSample = sc.getBitsPerSample();
    testConstant = true;
    registerConfiguration(new EncodingConfiguration());
  }

  /**
   * This method is used to set the encoding configuration. This
   * configuration can be altered throughout the stream, but cannot be called while an
   * encoding process is active.
   *
   * @param ec    encoding configuration to use.
   * @return      <code>true</code> if configuration was changed.
   *              <code>false</code> otherwise(i.e, and encoding process was active
   *              at the time of change)
   */
  public boolean registerConfiguration(EncodingConfiguration ec) {
    boolean changed = false;
    if(sc.getChannelCount() != 2)
      ec.setChannelConfig(EncodingConfiguration.ChannelConfig.INDEPENDENT);
    this.ec = new EncodingConfiguration(ec);
    verbatimSubframe.registerConfiguration(this.ec);
    fixedSubframe.registerConfiguration(this.ec);
    lpcSubframe.registerConfiguration(this.ec);
    constantSubframe.registerConfiguration(this.ec);
    changed = true;
    return changed;
  }


  /**
   * Encodes samples into the appropriate compressed format, saving the
   * result in the given “data” EncodedElement list. Encodes 'count' samples,
   * from index 'start', to index 'start' times 'skip', where “skip” is the
   * format that samples may be packed in an array. For example, 'samples' may
   * include both left and right samples of a stereo stream. Therefore, “skip”
   * would equal 2, resulting in the valid indices for the first channel being
   * even, and second being odd.
   * @param samples   the audio samples to encode. This array may contain
   *                  samples for multiple channels, interleaved; only one of
   *                  these channels is encoded by a subframe.
   * @param count     the number of samples to encode.
   * @param start     the index to start at in the array.
   * @param skip      the number of indices to skip between successive samples
   *                  (for use when channels are interleaved in the given
   *                  array).
   * @param data      the EncodedElement to attach encoded data to. Data in
   *                  Encoded Element given is not altered. New data is
   *                  attached starting with “data.getNext()”. If “data”
   *                  already has a “next” set, it will be lost!
   * @return int      Returns the number of inter-channel samples encoded;
   *                  i.e, if block-size is 4000, and it is stereo audio.
   *                  There are 8000 samples in this block, but the return
   *                  value is “4000”. There is always an equal number of
   *                  samples encoded from each channel. This exists primarily
   *                  to support dynamic block sizes in the future;
   * Pre-condition: none
   * Post-condition: Argument 'data' is the head of a list containing the resulting,
   *   encoded data stream.
   */
  public int encodeSamples_OLD(int[] samples, int count, int start, int skip,
      EncodedElement result, long frameNumber) {
    //System.err.println("FRAME::encodeSamples: frame#:"+frameNumber);
    if(DEBUG_LEV > 0) {
      System.err.println("FRAME::encodeSamples(...)");
      if(DEBUG_LEV > 10) {
        System.err.println("\tsamples.length:"+samples.length+":count:"+
            count+":start:"+start+":skip:"+skip+":frameNumber:"+
            frameNumber);
      }
    }
    int samplesEncoded = count;
    testConstant = true;
    EncodedElement data = null;
    //choose correct channel configuration. Get data for that config
    EncodingConfiguration.ChannelConfig chConf = ec.getChannelConfig();
    if(chConf == EncodingConfiguration.ChannelConfig.INDEPENDENT) {
      data = new EncodedElement(100,0);
      int size = encodeIndependent(samples, count, start, skip, data, 0);
      //int size = encodeMidSide(samples, count, start, skip, data, 0);
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.LEFT_SIDE) {
      data = new EncodedElement(100,0);
      int size = encodeLeftSide(samples, count, start, skip, data, 0);
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.MID_SIDE) {
      data = new EncodedElement(100,0);
      int size = Frame.encodeMidSide(samples, count, start, skip, data, 0, this);
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.RIGHT_SIDE) {
      data = new EncodedElement(100,0);
      int size = encodeRightSide(samples, count, start, skip, data, 0);
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.ENCODER_CHOICE) {
      data = new EncodedElement(100,0);
      int size = allChannelDecorrelation(samples, count, start, skip, data, 0, this);
      chConf = ec.channelConfig;
      ec.channelConfig = EncodingConfiguration.ChannelConfig.ENCODER_CHOICE;
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.EXHAUSTIVE) {
      //encode with all versions.
      EncodedElement dataLeftSide = new EncodedElement(100,0);
      ec.channelConfig = EncodingConfiguration.ChannelConfig.LEFT_SIDE;
      int sizeLeft = encodeLeftSide(samples, count, start, skip, dataLeftSide, 0);
      ec.channelConfig = EncodingConfiguration.ChannelConfig.MID_SIDE;
      EncodedElement dataMidSide = new EncodedElement(100,0);
      int sizeMid = Frame.encodeMidSide(samples, count, start, skip, dataMidSide, 0, this);
      ec.channelConfig = EncodingConfiguration.ChannelConfig.INDEPENDENT;
      EncodedElement dataIndependent = new EncodedElement(100,0);
      int sizeInd = encodeIndependent(samples, count, start, skip, dataIndependent, 0);
      //choose best
      ec.channelConfig = chConf;
      if(sizeLeft <= sizeMid && sizeLeft <= sizeInd) {
        data = dataLeftSide;
        chConf = EncodingConfiguration.ChannelConfig.LEFT_SIDE;
      }
      else if(sizeMid <= sizeInd) {
        data = dataMidSide;
        chConf = EncodingConfiguration.ChannelConfig.MID_SIDE;
      }
      else {
        data = dataIndependent;
        chConf = EncodingConfiguration.ChannelConfig.INDEPENDENT;
      }
      //update header to reflect change
    }

    //create header element; attach to result
    EncodedElement header = frameHeader.createHeader(true, count,
        sc.getSampleRate(), chConf, sc.getBitsPerSample(),
        frameNumber, channels, new EncodedElement(128, 0));
    result.setNext(header);
    //attach data to header
    header.attachEnd(data);
    //use "data" to zero-pad to byte boundary.
    //EncodedElement temp = data.getEnd();
    data.padToByte();
    //calculate CRC and affix footer
    EncodedElement crc16Ele = getCRC16(header);
    data.attachEnd(crc16Ele);
    if(DEBUG_LEV > 0)
      System.err.println("Frame::encodeSamples(...): End");
    return samplesEncoded;
  }

  private EncodingConfiguration.ChannelConfig determineConfigUsed(ChannelData[] channels) {
    EncodingConfiguration.ChannelConfig result = EncodingConfiguration.ChannelConfig.INDEPENDENT;
    if(channels.length == 2) {
      ChannelData.ChannelName n1 = channels[0].getChannelName();
      ChannelData.ChannelName n2 = channels[1].getChannelName();
      if(n2 == ChannelData.ChannelName.SIDE) {
        if(n1 == ChannelData.ChannelName.LEFT)
          result = EncodingConfiguration.ChannelConfig.LEFT_SIDE;
        else if(n1 == ChannelData.ChannelName.MID)
          result = EncodingConfiguration.ChannelConfig.MID_SIDE;
        else {
          System.err.println("Error in n2, determinConfigUsed");
        }
      }
      if(n1 == ChannelData.ChannelName.SIDE) {
        if(n2 == ChannelData.ChannelName.RIGHT)
          result = EncodingConfiguration.ChannelConfig.RIGHT_SIDE;
        else
          System.err.println("Error in n1, determinConfigUsed");
      }
    }
    return result;
  }
  /**
   * Encodes samples into the appropriate compressed format, saving the
   * result in the given “data” EncodedElement list. Encodes 'count' samples,
   * from index 'start', to index 'start' times 'skip', where “skip” is the
   * format that samples may be packed in an array. For example, 'samples' may
   * include both left and right samples of a stereo stream. Therefore, “skip”
   * would equal 2, resulting in the valid indices for the first channel being
   * even, and second being odd.
   * @param samples   the audio samples to encode. This array may contain
   *                  samples for multiple channels, interleaved; only one of
   *                  these channels is encoded by a subframe.
   * @param count     the number of samples to encode.
   * @param start     the index to start at in the array.
   * @param skip      the number of indices to skip between successive samples
   *                  (for use when channels are interleaved in the given
   *                  array).
   * @param data      the EncodedElement to attach encoded data to. Data in
   *                  Encoded Element given is not altered. New data is
   *                  attached starting with “data.getNext()”. If “data”
   *                  already has a “next” set, it will be lost!
   * @return int      Returns the number of inter-channel samples encoded;
   *                  i.e, if block-size is 4000, and it is stereo audio.
   *                  There are 8000 samples in this block, but the return
   *                  value is “4000”. There is always an equal number of
   *                  samples encoded from each channel. This exists primarily
   *                  to support dynamic block sizes in the future;
   * Pre-condition: none
   * Post-condition: Argument 'data' is the head of a list containing the resulting,
   *   encoded data stream.
   */
  public int encodeSamples(int[] samples, int count, int start, int skip,
      EncodedElement result, long frameNumber) {
    //System.err.println("FRAME::encodeSamples: frame#:"+frameNumber);
    if(DEBUG_LEV > 0) {
      System.err.println("FRAME::encodeSamplesNew(...)");
      if(DEBUG_LEV > 10) {
        System.err.println("\tsamples.length:"+samples.length+":count:"+
            count+":start:"+start+":skip:"+skip+":frameNumber:"+
            frameNumber);
      }
    }
    int samplesEncoded = count;
    testConstant = true;
    EncodedElement data = null;
    ChannelData[][] chanConfigData = this.getChannelsToEncode(samples, count,
        sc.getChannelCount(), sc.getBitsPerSample());
    int size = Integer.MAX_VALUE;
    EncodingConfiguration.ChannelConfig chConf = EncodingConfiguration.ChannelConfig.INDEPENDENT;
    for(int i = 0; i < chanConfigData.length; i++) {
      EncodedElement temp = new EncodedElement();
      int configSize = encodeChannels(chanConfigData[i], temp);
      if(configSize < size) {
        size = configSize;
        data = temp;
        chConf = determineConfigUsed(chanConfigData[i]);
      }
    }

    //create header element; attach to result
    EncodedElement header = new EncodedElement(FrameHeader.MAX_HEADER_SIZE, 0);
    frameHeader.createHeader(true, count, sc.getSampleRate(), chConf,
        sc.getBitsPerSample(), frameNumber, channels, header);
    //result.setNext(header);
    result.attachEnd(header);
    //attach data to header
    header.attachEnd(data);
    //use "data" to zero-pad to byte boundary.
    //EncodedElement temp = data.getEnd();
    data.padToByte();
    //calculate CRC and affix footer
    EncodedElement crc16Ele = getCRC16(header);
    data.attachEnd(crc16Ele);
    if(DEBUG_LEV > 0)
      System.err.println("Frame::encodeSamples(...): End");
    return samplesEncoded;
  }

  EncodedElement getCRC16(EncodedElement header) {
    EncodedElement crc16Ele = new EncodedElement(2,0);
    short valNew = header.getCRC16();
    crc16Ele.addInt(valNew, 16);
    return crc16Ele;
  }

  int encodeChannels(ChannelData[] channels, EncodedElement result) {
    int totalSize = 0;
    int channelLength = 0;
    int offset = 0;
    int count = channels[0].getCount();
    for(int i = 0; i < channels.length; i++) {
      EncodedElement temp = new EncodedElement();
      channelLength = encodeChannel(channels[i].getSamples(),count,0, 0, offset,
      temp, channels[i].getSampleSize());
      totalSize += channelLength;
      result.attachEnd(temp);
      offset = totalSize % 8;
    }

    return totalSize;
  }
  int encodeIndependent(int[] samples, int count, int start,
      int skip, EncodedElement result, int offset) {
    if(DEBUG_LEV > 0) {
      System.err.println("Frame::encodeIndependent : Begin");
      System.err.println("start:skip:offset::"+start+":"+skip+":"+offset);
    }
    //int startSize = result.getTotalBits();
    int totalSize = 0;
    int channelLength = 0;
    int channelCount = skip+1;
    int inputOffset = offset;
    EncodedElement subframes[] = new EncodedElement[channelCount];
    EncodingConfiguration.ChannelConfig chConf = ec.channelConfig;
    //encode each subframe, using prior offset in packing
    for(int i = 0; i < channelCount; i++) {
      int channelBitsPerSample = this.bitsPerSample;
      //System.err.println("independent: "+channelBitsPerSample);
      if(i == 1 && chConf == EncodingConfiguration.ChannelConfig.LEFT_SIDE)
        channelBitsPerSample++;
      else if(i == 1 && chConf == EncodingConfiguration.ChannelConfig.MID_SIDE)
        channelBitsPerSample++;
      else if(i == 0 && chConf == EncodingConfiguration.ChannelConfig.RIGHT_SIDE)
        channelBitsPerSample++;
      subframes[i] = new EncodedElement();
      //System.err.println("Frame::subframe begin offset: "+offset);
      channelLength = encodeChannel(samples, count, start+i, skip,
          offset, subframes[i], channelBitsPerSample);
      totalSize += channelLength;
      offset = (inputOffset+totalSize)%8;
    }
    //attach first subframe to result
    result.attachEnd(subframes[0]);
    //attach all remaining channels together
    for(int i = 1; i < channelCount; i++) {
      subframes[i-1].attachEnd(subframes[i]);
      //result.attachEnd(subframes[i]);
    }
    if(DEBUG_LEV > 0)
      System.err.println("Frame::encodeIndependent : End");
    //return total bit size(does not include given offset).
    return totalSize;
  }


  int encodeChannel(int[] samples, int count, int start, int skip, int offset,
      EncodedElement data, int channelBitsPerSample) {
    if(DEBUG_LEV > 0)
      System.err.println("Frame::encodeChannel : Begin");
    int size = 0;
    //Calculate subframe using the methods requested.
    EncodingConfiguration.SubframeType subframeType = ec.getSubframeType();
    if(subframeType == EncodingConfiguration.SubframeType.VERBATIM) {
      //use verbatim subframe to encode channel.
      //note size.
      //System.err.println("Using verbatim with count: "+count);
      this.verbatimSubframe.encodeSamples(samples, count, start, skip,
              data, offset, channelBitsPerSample);
      size = verbatimSubframe.getEncodedSize();
    }
    else if(subframeType == EncodingConfiguration.SubframeType.FIXED) {
      //System.err.println("channelBitsPerSample: "+channelBitsPerSample);
      this.fixedSubframe.encodeSamples(samples, count, start, skip, data,
          offset, channelBitsPerSample);
      size = fixedSubframe.getEncodedSize();
    }
    else if(subframeType == EncodingConfiguration.SubframeType.LPC) {
      this.lpcSubframe.encodeSamples(samples, count, start, skip, data,
          offset, channelBitsPerSample);
      size = lpcSubframe.getEncodedSize();
    }
    else if(subframeType == EncodingConfiguration.SubframeType.EXHAUSTIVE) {
      int conCount = -1;
      EncodedElement constantEle = new EncodedElement(200,offset);
      EncodedElement smallest = null;
      if(testConstant) {
        //System.err.println("Testing constant");
        conCount = constantSubframe.encodeSamples(samples, count, start,
        skip, constantEle,offset, channelBitsPerSample);
      }
      //System.err.println("conCount: "+conCount);
      if(conCount == count) {
        //System.err.println("Using Constant!");
        size = constantSubframe.getEncodedSize();
        smallest = constantEle;
      }
      else {
        //calculate verbatim size:
        int verbatimSize = verbatimSubframe.estimateSize(count, channelBitsPerSample);
        fixedSubframe.encodeSamples(samples, count,
            start, skip, offset, channelBitsPerSample);
        int fixedSize = fixedSubframe.estimatedSize();
        //int fixedSize = fixedSubframe.getEncodedSize();
        lpcSubframe.encodeSamples(samples, count, start, skip,
            offset, channelBitsPerSample);
        int lpcSize = (int)lpcSubframe.estimatedSize();
        if(verbatimSize < lpcSize && verbatimSize < fixedSize) {//verbatim
          System.err.println("Running verbatim");
          smallest = new EncodedElement(verbatimSize,offset);
          verbatimSubframe.encodeSamples(samples, count, start, skip,
              smallest, offset, channelBitsPerSample);
          size = verbatimSubframe.getEncodedSize();
        }
        else if(lpcSize < fixedSize && lpcSize < verbatimSize){//lpc
          //size = lpcSize;
          smallest = lpcSubframe.getData();
          size = lpcSubframe.getEncodedSize();
          if(size > lpcSize)
            System.err.println("Lpc size wrong: exp:real : "+lpcSize+":"+size);
        }
        else {//fixed
          smallest = fixedSubframe.getData();
          size = fixedSubframe.getEncodedSize();
          if(size > fixedSize)
          System.err.println("Fixed size wrong: exp:real : "+fixedSize+":"+size);
        }
      }
      data.data = smallest.data;
      data.next = smallest.next;
      data.usableBits = smallest.usableBits;
      data.offset = smallest.offset;
    }
    //return total bit size of encoded subframe.
    if(DEBUG_LEV > 0)
      System.err.println("Frame::encodeChannel : End");
    return size;
  }
  /**
   * Returns the total number of valid bits used in the last encoding(i.e, the
   * number of compressed bits used). This is here for convenience, as the
   * calling object may also loop through the resulting EncodingElement from
   * the encoding process and sum the valid bits.
   *
   * @return  an integer with value of the number of bits used in last encoding.
   * Pre-condition: none
   * Post-condition: none
   */
  public int getEncodedSize() {
    if(DEBUG_LEV > 0)
      System.err.println("Frame::getEncodedSize : Begin");
    return lastEncodeSize;
  }

  private int encodeRightSide(int[] samples, int count, int start, int skip,
      EncodedElement data, int offset) {
    int[] rightSide = new int[samples.length];
    for(int i = 0; i < count; i++) {
      rightSide[2*i] = samples[2*i]-samples[2*i+1];
      rightSide[2*i+1] = samples[2*i+1];
    }
    return encodeIndependent(rightSide, count, start, skip, data, offset);
  }
  private int encodeLeftSide(int[] samples, int count, int start, int skip,
      EncodedElement data, int offset) {
    int[] leftSide = new int[samples.length];
    for(int i = 0; i < count; i++) {
      leftSide[2*i] = samples[2*i];
      leftSide[2*i+1] = samples[2*i]-samples[2*i+1];
    }
    return encodeIndependent(leftSide, count, start, skip, data, offset);
  }

  private static void getIndependentChannels(int[] samples, int count,
      ChannelData[] channels, int sampleSize) {
    int channelCount = channels.length;
    int[][]independentSamples = new int[channels.length][];
    for(int i = 0; i < channelCount; i++) {
      ChannelData temp = channels[i];
      if(temp != null)
        independentSamples[i] = channels[i].getSamples();
      if(independentSamples[i] == null || independentSamples[i].length < count)
        independentSamples[i] = new int[count];
      if(temp != null)
        channels[i].setData(independentSamples[i], count, sampleSize, ChannelData.ChannelName.INDEPENDENT);
      else
        channels[i] = new ChannelData(independentSamples[i], count, sampleSize, ChannelData.ChannelName.INDEPENDENT);
    }

    for(int i = 0; i < count; i++) {
      for(int x = 0; x < channelCount; x++) {
        independentSamples[x][i] = samples[channelCount*i+x];
      }
    }
  }
    
  private static void getMidSideChannels(int[] samples, int count,
      ChannelData[] channels, int sampleSize) {
    ChannelData midData = channels[0];
    ChannelData sideData = channels[1];
    int[] midSamples = null;
    int[] sideSamples = null;
    if(midData != null)
      midSamples = midData.getSamples();
    if(sideData != null)
      sideSamples = sideData.getSamples();
    if(midSamples == null || midSamples.length < count) midSamples = new int[count];
    if(sideSamples == null || sideSamples.length < count) sideSamples = new int[count];

    if(midData != null)
      channels[0].setData(midSamples, count, sampleSize, ChannelData.ChannelName.MID);
    else
      channels[0] = new ChannelData(midSamples, count, sampleSize, ChannelData.ChannelName.MID);
    if(sideData != null)
      channels[1].setData(sideSamples, count, sampleSize+1, ChannelData.ChannelName.SIDE);
    else
      channels[1] = new ChannelData(sideSamples, count, sampleSize+1, ChannelData.ChannelName.SIDE);

    for(int i = 0; i < count; i++) {
      int temp = (samples[2*i]+samples[2*i+1]) >> 1;
      midSamples[i] = temp;
      sideSamples[i] = (samples[2*i]-samples[2*i+1]);
    }
  }
  private static long[] sumChannelSamplesABS(ChannelData[] channels) {
    long[] result = new long[channels.length];
    for(int i = 0; i < result.length; i++) result[i] = 0;
    int[][] samples = new int[channels.length][];
    for(int i = 0; i < channels.length; i++)
      samples[i] = channels[i].getSamples();
    for(int i = 0; i < channels[0].getCount(); i++) {
      for(int x = 0; x < channels.length; x++) {
        int temp = samples[x][i];
        if(temp < 0)
          temp = -temp;
        result[x] += temp;
      }
    }
    return result;
  }
  private ChannelData[][] getChannelsToEncode(int[] samples, int count, int channels, int sampleSize) {
    ChannelData[] independent = indChanData;
    ChannelData[] midSide = msChanData;
    //ChannelData[][] results = new ChannelData[channels];
    ChannelData[][] results = null;
    EncodingConfiguration.ChannelConfig chConf = ec.getChannelConfig();
    if(channels != 2)
      chConf = EncodingConfiguration.ChannelConfig.INDEPENDENT;

    if(chConf != EncodingConfiguration.ChannelConfig.MID_SIDE) {
      if(independent == null || independent.length != channels)
        independent = new ChannelData[channels];
      getIndependentChannels(samples, count, independent, sampleSize);
    }
    if(chConf != EncodingConfiguration.ChannelConfig.INDEPENDENT) {
      if(midSide == null || midSide.length != 2)
        midSide = new ChannelData[2];
      getMidSideChannels(samples,count,midSide,sampleSize);
    }
    if(chConf == EncodingConfiguration.ChannelConfig.ENCODER_CHOICE) {
      chConf = selectOptimalChannels(independent,midSide,count);
    }

    if(chConf == EncodingConfiguration.ChannelConfig.INDEPENDENT) {
      results = new ChannelData[1][];
      results[0] = independent;
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.LEFT_SIDE) {
      results = new ChannelData[1][2];
      independent[0].setChannelName(ChannelData.ChannelName.LEFT);
      results[0][0] = independent[0];
      results[0][1] = midSide[1];
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.MID_SIDE) {
      results = new ChannelData[1][2];
      results[0] = midSide;
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.RIGHT_SIDE) {
      results = new ChannelData[1][2];
      independent[1].setChannelName(ChannelData.ChannelName.RIGHT);
      results[0][1] = independent[1];
      results[0][0] = midSide[1];
    }
    else if(chConf == EncodingConfiguration.ChannelConfig.EXHAUSTIVE) {
      //encode with all versions.
      results = new ChannelData[4][];
      results[2] = new ChannelData[2];
      results[3] = new ChannelData[2];
      results[0] = independent;
      results[1] = midSide;
      results[2][0] = independent[0];//LEFT_SIDE
      independent[0].setChannelName(ChannelData.ChannelName.LEFT);
      results[2][1] = midSide[1];

      results[3][0] = midSide[1];//RIGHT_SIDE(better called SIDE_RIGHT)
      results[3][1] = independent[1];
      independent[1].setChannelName(ChannelData.ChannelName.RIGHT);
    }
    indChanData = independent;
    msChanData = midSide;
    return results;
  }

  private boolean determineTestConstant(long[] sums, ChannelData[] channels) {
    boolean result = false;
    int count = channels[0].getCount();
    for(int i = 0; i < sums.length; i++) {
      if(sums[i]/count == channels[i].getSamples()[0])
      result = true;
    }
    return result;
  }


  private EncodingConfiguration.ChannelConfig selectOptimalChannels
      (ChannelData[] independent,ChannelData[] midSide,int count) {
    EncodingConfiguration.ChannelConfig result;
    //Calculate sum for each channel.
    long[] sumInd = sumChannelSamplesABS(independent);
    long[] sumMS = sumChannelSamplesABS(midSide);
    boolean ind = determineTestConstant(sumInd, independent);
    boolean ms = determineTestConstant(sumMS, midSide);
    testConstant = ind | ms;

    //Check each total channel configuration sum, lowest is winner
    //long[] r_sums = new long[4];
    r_sums[0] = sumInd[0]+sumInd[1];//INDEPENDENT
    r_sums[1] = sumMS[0]+sumMS[1];//MID_SIDE
    r_sums[2] = sumInd[0]+sumMS[1];//LEFT_SIDE
    r_sums[3] = sumInd[1]+sumMS[1];//RIGHT_SIDE
    long lowestVal = r_sums[0];
    int lowestIndex = 0;
    for(int i = 0; i < 4; i++) {
      if( lowestVal > r_sums[i]) {
        lowestVal = r_sums[i];
        lowestIndex = i;
      }
    }
    switch(lowestIndex) {
      case 3: result = EncodingConfiguration.ChannelConfig.RIGHT_SIDE;break;
      case 2: result = EncodingConfiguration.ChannelConfig.LEFT_SIDE;break;
      case 1: result = EncodingConfiguration.ChannelConfig.MID_SIDE;break;
      default: result = EncodingConfiguration.ChannelConfig.INDEPENDENT;
    }
    return result;
  }

  private static int encodeMidSide(int[] samples, int count, int start, int skip,
      EncodedElement data, int offset, Frame f) {
    int[] midSide = new int[samples.length];
    for(int i = 0; i < count; i++) {
      int temp = (samples[2*i]+samples[2*i+1]) >> 1;
      // if(temp %2 != 0) temp++;
      midSide[2*i] = temp;
      midSide[2*i+1] = (samples[2*i]-samples[2*i+1]);

      //midSide[2*i+1] = 0;
    }
    return f.encodeIndependent(midSide, count, start, skip, data, offset);
  }
  private static double getVariance(ChannelData chan, double mean) {
    double variance = 0;
    int[] samples = chan.getSamples();
    for(int i = 0; i < chan.getCount(); i++) {
      double val = mean-(double)samples[i];
      variance += val*val;
    }
    return variance;
  }
  private static double getVariance(double mean, int[] samples, int count, int start,
    int increment) {
    double var = 0;

    for(int i = 0; i < count; i++) {
      int loc = start+i*increment;
      double val = (mean-samples[loc]);
      var += val*val;
    }

    return var;
  }
  private static int allChannelDecorrelation(int[] samples, int count, int start, int skip,
      EncodedElement data, int offset, Frame f) {
    if(DEBUG_LEV > 0) {
      System.err.println("Frame::allChannelDecorrelation(...)");
    }
    //calculate size for each type
    //int size = 0;
    /*
    * sums[0]: left
    * sums[1]: right
    * sums[2]: mid
    * sums[3]: side
    */
    //long[] sums = new long[4];
    long sums0 = 0;
    long sums1 = 0;
    long sums2 = 0;
    long sums3 = 0;
    //boolean [] constantCandidate = new boolean[4];
    boolean[] constantCandidate = f._constantCandidate;
    for(int i = 0; i < 4; i++) {
      //sums[i] = 0;
      constantCandidate[i] = false;
    }

    int[] midSideSamples = f._midSideSamples;
    if(midSideSamples == null || midSideSamples.length < samples.length) {
      midSideSamples = new int[samples.length];
      f._midSideSamples = midSideSamples;
    }
    int index = 0;
    for(int i = 0; i < count; i++) {
      int temp = (samples[index]+samples[index+1]) >> 1;
      midSideSamples[index] = temp;
      midSideSamples[index+1] = (samples[index]-samples[index+1]);
      index += 2;
    }
    index = 0;
    for(int i = 0; i < count; i++ ) {
      long temp;
      temp = samples[index];
      if(temp < 0) temp = -temp;
      sums0 += temp;
      temp = samples[index+1];
      if(temp < 0) temp = -temp;
      sums1 += temp;
      temp = midSideSamples[index];
      if(temp < 0) temp = -temp;
      sums2 += temp;
      temp = midSideSamples[index+1];
      if(temp < 0) temp = -temp;
      sums3 += temp;
      index += 2;
    }
    //for(int i = 0; i < sums.length; i++) sums[i] =sums[i]/count;
    sums0 = sums0/count;
    sums1 = sums1/count;
    sums2 = sums2/count;
    sums3 = sums3/count;

    constantCandidate[0] = (samples[0] == sums0);
    constantCandidate[1] = (samples[1] == sums1);
    constantCandidate[2] = (midSideSamples[2] == sums2);
    constantCandidate[3] = (midSideSamples[3] == sums3);

    //long[] results = new long[4];
    long[] results = f._decorrelationResults;
    results[0]  = sums0+sums1;//independent
    results[1]  = sums2+sums3;//midSide
    results[2]  = sums0+sums3;//leftSide
    results[3]  = sums3+sums1;//rightSide
    //choose the best
    int choice = 0;
    for(int i = 0; i < 4; i++) {
      if(results[choice] > results[i]) choice = i;
    }
    int encodeResult = -1;
    if(choice == 0) {
      f.testConstant = constantCandidate[0] || constantCandidate[1];
      f.ec.channelConfig = EncodingConfiguration.ChannelConfig.INDEPENDENT;
      encodeResult = f.encodeIndependent(samples, count, start, skip, data, offset);
    }
    else if(choice == 1) {
      f.testConstant = constantCandidate[2] || constantCandidate[3];
      f.ec.channelConfig = EncodingConfiguration.ChannelConfig.MID_SIDE;
      encodeResult = f.encodeIndependent(midSideSamples, count, start, skip, data, offset);
    }
    else if(choice == 2) {
      f.testConstant = constantCandidate[0] || constantCandidate[3];
      f.ec.channelConfig = EncodingConfiguration.ChannelConfig.LEFT_SIDE;
      for(int i = 0; i < count; i++) midSideSamples[2*i] = samples[2*i];
      encodeResult = f.encodeIndependent(midSideSamples, count, start, skip, data, offset);
    }
    else {
      f.testConstant = constantCandidate[1] || constantCandidate[3];
      f.ec.channelConfig = EncodingConfiguration.ChannelConfig.RIGHT_SIDE;
      for(int i = 0; i < count; i++) {
        midSideSamples[2*i] = midSideSamples[2*i+1];
        midSideSamples[2*i+1] = samples[2*i+1];
      }
      encodeResult = f.encodeIndependent(midSideSamples, count, start, skip, data, offset);
    }
    return encodeResult;
  }
}
