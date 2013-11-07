/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package javaFlacEncoder;

/**
 *
 * @author preston
 */
public class ChannelData {
  public enum ChannelName {
    LEFT,
    RIGHT,
    MID,
    SIDE,
    INDEPENDENT
  }
  private int[] samples = null;
  private int count;
  private int sampleSize;
  private ChannelName name;

  public ChannelData(int[] samples, int count, int sampleSize, ChannelName n) {
    this.count = count;
    this.samples = samples;
    this.sampleSize = sampleSize;
    this.name = n;
  }

  public int[] getSamples() { return samples; }
  public int getCount() { return count; }
  public int getSampleSize() { return sampleSize; }
  public ChannelName getChannelName() { return name; }

  public int setData(int[] newSamples, int count, int sampleSize, ChannelName n) {
    samples = newSamples;
    this.sampleSize = sampleSize;
    this.name = n;
    return setCount(count);
  }

  public int setCount(int count) {
    this.count = (count <= samples.length) ? count:samples.length;
    return this.count;
  }

  public void setChannelName(ChannelName cn) {
    this.name = cn;
  }
}
