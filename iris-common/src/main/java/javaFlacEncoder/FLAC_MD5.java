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

import java.security.MessageDigest;


/**
 *
 * @author preston
 */
public class FLAC_MD5 {
  private MessageDigest md = null;
  private byte[] _dataMD5 = null;
  
  public FLAC_MD5() throws java.security.NoSuchAlgorithmException {
    md = MessageDigest.getInstance("md5");
  }

  public MessageDigest getMD() {
    return md;
  }

  /**
   * Add samples to the MD5 hash.
   * CURRENTLY ONLY MAY WORK FOR: sample sizes which are divisible by 8. Need
   * to create some audio to test with.
   * @param samples
   * @param count
   * @param channels
   */
  public void addSamplesToMD5(int[] samples, int count, int channels,
      int sampleSize) {
    int bytesPerSample = sampleSize/8;
    if(sampleSize%8 != 0)
      bytesPerSample++;
    if(_dataMD5 == null || _dataMD5.length < count*bytesPerSample*channels) {
      _dataMD5 = new byte[count*bytesPerSample*channels];
    }
    byte[] dataMD5 = _dataMD5;
    splitSamplesToBytes(samples, count*channels, bytesPerSample, dataMD5);
    md.update(dataMD5, 0, count*bytesPerSample*channels);
  }

  /* Split Samples to bytes(for sending to MD5)
   * CURRENTLY ONLY MAY WORK FOR: sample sizes which are divisible by 8. Need
   * to create some audio to test with.*/
  private static final void splitSamplesToBytes(int[] samples, int totalSamples,
    int bytesPerSample, byte[] dataMD5) {
    int destIndexBase = 0;
    int i = 0;
    
    switch(bytesPerSample) {
      case 3:
        for(; i < totalSamples; i++) {
        dataMD5[destIndexBase++] = (byte)(samples[i]);
        dataMD5[destIndexBase++] = (byte)(samples[i] >> 8);
        dataMD5[destIndexBase++] = (byte)(samples[i] >> 16);
        }
        break;
      case 2:
        for(; i < totalSamples; i++) {
        dataMD5[destIndexBase++] = (byte)(samples[i]);
        dataMD5[destIndexBase++] = (byte)(samples[i] >> 8);
        }
        break;
      case 1:
        for(; i < totalSamples; i++) {
        dataMD5[i] = (byte)samples[i];
        }
    }
  }
}
