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
 * Implements the Subframe abstract class, providing encoding support for the
 * FLAC Fixed-predictor Subframe.
 *
 * @author Preston Lacey
 */
public class Subframe_Fixed extends Subframe {
  /** For debugging: Higher values equals greater output, generally in
   * increments of 10 */
  public static int DEBUG_LEV = 0;
  /** Subframe type supported by this implementation. */
  public static final EncodingConfiguration.SubframeType type =
      EncodingConfiguration.SubframeType.FIXED;
  int sampleSize = 0;
  RiceEncoder rice = null;
  int [] bits;
  int [] lowOrderBits;
  long [] sum;

  int _error1[] = null;
  int _error2[] = null;
  int _error3[] = null;
  int _error4[] = null;
  int _lastCount = 0;
  int _order;
  int[] _errors = null;
  int _offset = 0;
  int _start = 0;
  int _skip = 0;
  int _errorStep = 0;
  int _totalBits;
  int[] _samples = null;
  int _errorOffset = 0;
  int _errorCount = 0;
  int _frameSampleSize = 0;

  private static final double LOG_2 = Math.log(2);
    
  /**
   * Constructor. Sets StreamConfiguration to use. If the StreamConfiguration
   * must later be changed, a new Subframe object must be created as well.
   *
   * @param sc StreamConfiguration to use for encoding.
   */
  public Subframe_Fixed(StreamConfiguration sc) {
    super(sc);
    sampleSize = sc.getBitsPerSample();
    rice = new RiceEncoder();
    bits = new int[5];
    lowOrderBits = new int[5];
    sum = new long[5];
    _lastCount = -1;
  }

  /**
   * This method is used to set the encoding configuration.
   * @param ec    encoding configuration to use.
   * @return      true if configuration was changed, false otherwise
   */
  @Override
  public boolean registerConfiguration(EncodingConfiguration ec) {
    super.registerConfiguration(ec);
    return true;
  }

  public int encodeSamples(int[] samples, int count, int start, int skip,
      int offset, int unencSampleSize ) {
    int encodedSamples = count;
    if(DEBUG_LEV > 0) {
      System.err.println("Subframe_Fixed::encodeSamples(...) : Begin");
      if(DEBUG_LEV > 10) {
        System.err.println("--count : " +count);
        System.err.println("start:skip:offset:::"+start+":"+skip+":"+offset);
      }
    }
    int increment = skip+1;
    //create space for results: Need four sets for the 5 different versions,
    //  the e0 is sampe as input samples, so no duplicate needed.
    if(count != _lastCount) {
      _error1 = new int[count];
      _error2 = new int[count];
      _error3 = new int[count];
      _error4 = new int[count];
      _lastCount = count;
    }
    int [] error1 = _error1;
    int [] error2 = _error2;
    int [] error3 = _error3;
    int [] error4 = _error4;
    long sum0 = 0;
    long sum1 = 0;
    long sum2 = 0;
    long sum3 = 0;
    long sum4 = 0;
    //apply the algorithm to determine errors, summing abs vals as we go
    int tempI;
    int index = start;
    for(int i = 0; i < count; i++) {
      tempI = samples[index];
      if(tempI < 0) tempI = -tempI;
      sum0 += tempI;
      index += increment;
    }
    for(int i = 1; i < 5; i++) {
      error1[i] = samples[start+i*increment]-samples[start+(i-1)*increment];
      tempI = error1[i];
      tempI = (tempI < 0) ? -tempI:tempI;
      sum1 += tempI;
      if(i > 1) {
        error2[i] = error1[i]-error1[(i-1)];
        tempI = error2[i];
        tempI = (tempI < 0) ? -tempI:tempI;
        sum2 += tempI;
      }
      if(i > 2) {
        error3[i] = error2[i]-error2[(i-1)];
        tempI = error3[i];
        tempI = (tempI < 0) ? -tempI:tempI;
        sum3 += tempI;
      }
      if(i > 3) {
        error4[i] = error3[i]-error3[(i-1)];
        tempI = error4[i];
        tempI = (tempI < 0) ? -tempI:tempI;
        sum4 += tempI;
      }
    }
    index = start+5*increment;
    for(int i = 5; i < count; i++) {
      //error1[i] = samples[start+i*increment]-samples[start+(i-1)*increment];
      error1[i] = samples[index]-samples[index-increment];
      tempI = error1[i];
      tempI = (tempI < 0) ? -tempI:tempI;
      sum1 += tempI;
      error2[i] = error1[i]-error1[(i-1)];
      tempI = error2[i];
      tempI = (tempI < 0) ? -tempI:tempI;
      sum2 += tempI;
      error3[i] = error2[i]-error2[(i-1)];
      tempI = error3[i];
      tempI = (tempI < 0) ? -tempI:tempI;
      sum3 += tempI;
      error4[i] = error3[i]-error3[(i-1)];
      tempI = error4[i];
      tempI = (tempI < 0) ? -tempI:tempI;
      sum4 += tempI;
      index += increment;
    }
    //select best algorithm as indicated by bits needed from sum of values
    //  and number of priming samples needed.
    int order = 0;
    long sumsX;
    for(int i = 0; i < 5; i++) {
      if(i == 0)
        sumsX = sum0;
      else if(i == 1)
        sumsX = sum1;
      else if(i == 2)
        sumsX = sum2;
      else if(i == 3)
        sumsX = sum3;
      else
        sumsX = sum4;

      double tempLowOrderBits = LOG_2*(sumsX/(count-i));
      lowOrderBits[i] = (int)(Math.ceil(Math.log(tempLowOrderBits)/LOG_2));
      if(lowOrderBits[i] < 1)
        lowOrderBits[i] = 1;
      else if (lowOrderBits[i] > sampleSize)
        lowOrderBits[i] = sampleSize;
      //lowOrderBits[i]++;//DOUBLE CHECK VALIDITY OF THIS. Decreases the bits needed, but "shouldn't"
      //bits[i] = (int)(Math.log(sum[i])/Math.log(10))*(count-1)+sampleSize*i;
      bits[i] = (int)(lowOrderBits[i]*(count-i)+sampleSize*i+1);
      order = (bits[i] < bits[order]) ? i:order;
    }

    int[] errors = null;
    int errorCount = count-order;
    int errorOffset = order;
    int errorStep = 1;
    switch(order) {
      case 0: errors = samples;
              errorStep+=skip;
              errorOffset=start;break;
      case 1: errors = error1;break;
      case 2: errors = error2;break;
      case 3: errors = error3;break;
      case 4: errors = error4;break;
    }
    _order = order;
    _offset = offset;
    _start = start;
    _errorStep = errorStep;
    _errorOffset = errorOffset;
    _errorCount = errorCount;
    _skip = skip;
    _samples = samples;
    _frameSampleSize = unencSampleSize;
    _errors = errors;
    _totalBits = unencSampleSize*order+8+ RiceEncoder.calculateEncodeSize(
        errors,errorOffset, errorStep, errorCount, lowOrderBits[order]);
    return encodedSamples;
  }

  /**
   * Return the estimated size of the previous encode attempt in bits. Since
   * returning the data from an encode is costly(due to the rice encoding and FLAC
   * compliant bit-packing), this allows us to estimate the size first, and
   * therefore choose another subframe type if this is larger.
   *
   * @return estimated size in bits of encoded subframe.
   */
  public int estimatedSize() {
    return _totalBits;
  }

  public EncodedElement getData() {
    EncodedElement dataEle = new EncodedElement(_totalBits/8+1,_offset);
    getData(dataEle);
    return dataEle;
  }
  /**
   * Get the data from the last encode attempt. Data is returned in an
   * EncodedElement, properly packed at the bit-level to be added directly to
   * a FLAC stream.
   *
   * @return EncodedElement containing encoded subframe
   */
  public EncodedElement getData(EncodedElement dataEle) {
    //EncodedElement dataEle = new EncodedElement(_totalBits/8+1,_offset);
    int startSize = dataEle.getTotalBits();
    int unencSampleSize = _frameSampleSize;
    //write headers
    int encodedType = 1<<3 | _order;
    dataEle.addInt(0, 1);
    dataEle.addInt(encodedType, 6);
    dataEle.addInt(0, 1);
    if(_order > 0) {
      dataEle.packInt(_samples, unencSampleSize, _start, _skip, _order);
    }

    //send best data to rice encoder
    int paramSize = (lowOrderBits[_order] > 14) ? 5:4;
    boolean fiveBitParam = (paramSize < 5) ? false:true;
    RiceEncoder.beginResidual(fiveBitParam, (byte)0, dataEle);
    /*for(int i = 0; i < errorCount; i++) {
        int error = errors[errorOffset+i*errorStep];
        if(error >= 32767 || error <= -32767)
            System.err.println("Error Bound issue?: " + error);
    }*/
    rice.encodeRicePartition(_errors, _errorOffset, _errorStep,
        _errorCount, dataEle, lowOrderBits[_order], fiveBitParam);

    this.lastEncodedSize = dataEle.getTotalBits()-startSize;
    if(DEBUG_LEV > 0)
      System.err.println("Subframe_Fixed::encodeSamples(...): End");
    return dataEle;
  }
    
  public int encodeSamples(int[] samples, int count, int start, int skip,
      EncodedElement dataEle, int offset, int unencSampleSize ) {
    int encodedSamples = 0;
    encodedSamples = encodeSamples(samples, count, start, skip, offset, unencSampleSize);
    dataEle.clear(_totalBits/8+1,offset);
    getData(dataEle);
    return encodedSamples;
  }
    
    /*private int[] calculatePartitionsCount(int[] errors, int errorOffset, int errorStep,
                int errorCount) {
        int maxPartitions = 8;
        int[][] sums = new int[(int)Math.pow(2,maxPartitions)][];
        for(int i = 0; i < maxPartitions; i++) {
            sums[i] = new int[(int)Math.pow(2, i)];
        }
        int[] counts = new int[maxPartitions];
        int[] usedPartitions = new int[maxPartitions];
        int[] lastPartitionCount = new int[maxPartitions];
        for(int i = 0; i < maxPartitions; i++) {
            counts[i] = errorCount/sums[i].length;
            if(errorCount % sums[i].length != 0) counts[i]++;
            usedPartitions[i] = errorCount/counts[i];
            if(errorCount %counts[i] != 0) usedPartitions[i]++;
        }
        for(int i = 0; i < maxPartitions; i++) {
            int temp = errorCount+errorOffset/
        }
        int[] sizes = new int[maxPartitions];

        int temp = 0;
        double log2 = Math.log(2);
        for(int i = 0; i < errorCount; i++) {
            temp = errors[errorOffset+i*errorStep];
            if(temp < 0) temp = -temp;
            for(int x = 0; x < maxPartitions; x++) {
                float destDiv = i/counts[x];
                int destIndex = (int)(i/destDiv);
                sums[x][destIndex] += temp;
            }

        }
        //sum up all bit sizes per partition, choose best size.
        for(int i = 0; i < maxPartitions; i++) {
            int tempTotal = 0;
            sizes[i] = 0;

            for(int x = 0; x < usedPartitions[i]; x++) {
                tempTotal = (int)(Math.log(sums[i][x])/Math.log(2));
                float destDiv = (float)errorCount/sums[x].length;
                int destCount = errorCount/sums[x].length;
                int destIndex = (int)(i/destDiv);
                sizes[i] += tempTotal+4+;
            }
        }
        return results;
    }*/

}
