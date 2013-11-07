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
 * EncodedElement class provides is used to store data in the proper bitstream
 * format for FLAC audio. Methods are provided to easily append values to the
 * bitstream. Conceptually, an EncodedElement is a list structure, in which the
 * encoded data is stored in a byte array. It is assumed that any data stored by
 * objects of this class will be retrieved through direct access to the
 * underlying byte array. This byte array is exposed to outside objects
 * primarily to allow faster access than "proper" object-oriented design might
 * allow.
 * 
 * @author Preston Lacey
 */
public class EncodedElement {
   
  /** For Debugging: Higher level equals more debug statements */
  static int DEBUG_LEV = 0;

  /** Previous element in list. At current times, this should not be dependend
   * on to be set */
  EncodedElement previous = null;

  /** Next element in list. */
  EncodedElement next = null;

  /** Data stored by this element. Member usableBits should be used to track
   * the last valid index at a bit level. */
  byte[] data = null;

  /** Use to track the last valid index of the data array at a bit level. For
   * example, a value of "10" would mean the first byte and two low-order bits
   * of the second byte are used. usableBits must always be equal or greater
   * than offset.
   */
  int usableBits = 0;//i.e, the last write location in 'data' array.

  /** Used to signify the index of the first valid bit of the data array. For
   * purposes of speed, it is not always best to pack the data starting at
   * bit zero of first byte. offset must always be less than or equal to
   * usableBits.
   */
  protected int offset;

  /**
   * Constructor, creates an empty element with offset of zero and array size
   * of 100. This array can be replaced with a call to setData(...).
   */
  public EncodedElement() {
    offset = 0;
    usableBits = 0;
    data = new byte[100];
  }

  /**
   * Constructor. Creates an EncodedElement with the given size and offset
   * @param array Byte array to use for this element.
   * @param off Offset to use for this element. Usablebits will also be set
   * to this value.
   */
  public EncodedElement(byte[] array, int off) {
    assert(array.length%4 == 0);
    offset = off;
    usableBits = off;
    data = array;
  }

  public EncodedElement(byte[] array, int off, int usedBits) {
    assert(array.length%4 == 0);
    offset = off;
    usableBits = usedBits;
    data = array;
}
    
  /**
   * Constructor. Creates an EncodedElement with the given size and offset
   * @param size Size of data array to use(in bytes)
   * @param off Offset to use for this element. Usablebits will also be set
   * to this value.
   */
  public EncodedElement(int size, int off) {
    if (size%4 != 4) size = (size/4+1)*4;
    data = new byte[size];
    usableBits = off;
    offset = off;
  }

  /**
   * Completely clear this element and use the given size and offset for the
   * new data array.
   *
   * @param size Size of data array to use(in bytes)
   * @param off Offset to use for this element. Usablebits will also be set to
   * this value.
   */
  public void clear(int size, int off) {
    if (size%4 != 4) size = (size/4+1)*4;
    next = null;
    previous = null;
    data = new byte[size];
    offset = off;
    for(int i = 0; i < data.length; i++)
      data[i] = 0;
    usableBits = off;
  }

  /**
   * Completely clear this element and use the given size and offset for the
   * new data array.
   *
   * @param size Size of data array to use(in bytes)
   * @param off Offset to use for this element. Usablebits will also be set to
   * this value.
   * @param keep true to keep current backing array, false to create new one.
   */
  public void clear(int size, int off, boolean keep) {
    if (size%4 != 4) size = (size/4+1)*4;
    next = null;
    previous = null;
    if(!keep)
      data = new byte[size];
    offset = off;
    for(int i = 0; i < data.length; i++)
      data[i] = 0;
    usableBits = off;
  }
  /**
   * Set the object previous to this in the list.
   * @param ele   the object to set as previous.
   * @return      <code>void</code>
   *
   * Precondition: none
   * Post-condition: getPrevious() will now return the given object. Any
   * existing “previous” was lost.
   */
  void setPrevious(EncodedElement ele) {
    previous = ele;
  }

  /**
   * Set the object next to this in the list.
   * @param ele   the object to set as next.
   * @return      void
   * Pre-condition: none
   * Post-condition: getNext() will now return the given object. Any existing
   * “next” was lost.
   */
  void setNext(EncodedElement ele) {
    next = ele;
  }

  /**
   * Get the object stored as the previous item in this list.
   *
   * @return EncodedElement
   */
  EncodedElement getPrevious() {
    return previous;
  }
  /**
   * Get the object stored as the next item in this list.
   *
   * @param EncodedElement;
   */
  EncodedElement getNext() {
    return next;
  }

  /**
   * Set the byte array stored by this object.
   *
   * @param data  the byte array to store.
   *
   * Pre-condition: None
   * Post-condition: 'data' is now stored by this object. Any previous data
   * stored was lost.
   */
  void setData(byte[] data) {
    assert(data.length%4 == 0);
    this.data = data;
  }

  /**
   * Set the number of bits of the given array that are usable data. Data is
   * packed from the lower indices to higher.
   *
   * @param bits  the value to store
   */
  void setUsableBits(int bits) {
    usableBits = bits;
  }

  /**
   * Get the byte array stored by this object(null if not set).
   *
   * @param byte[]    the data stored in this byte[] is likely not all usable.
   *                  Method getUsableBits() should be used to determine such.
   */
  byte[] getData() {
    return data;
  }

  /**
   * get the number of bits usable in the stored array.
   * @return int
   */
  int getUsableBits() {
    return usableBits;
  }

  /**
   * Return the last element of the list given. This is a static funtion to
   * provide minor speed improvement. Loops through all elements' "next"
   * pointers, till the last is found.
   * @param e EncodedElement list to find end of.
   * @return Final element in list.
   */
  protected static EncodedElement getEnd_S(EncodedElement e) {
    if(e == null)
      return null;
    EncodedElement temp = e.next;
    EncodedElement end = e;
    while(temp != null) {
      end = temp;
      temp = temp.next;
    }
    return end;
  }

  /**
   * Return the last element of the list given. Loops through all elements'
   * "next" pointers, till the last is found.
   * @return last element in this list
   */
  public EncodedElement getEnd() {
    EncodedElement temp = next;
    EncodedElement end = this;
    while(temp != null) {
      end = temp;
      temp = temp.next;
    }
    return end;
  }

  /**
   * Attach an element to the end of this list.
   *
   * @param e Element to attach.
   * @return True if element was attached, false otherwise.
   */
  public boolean attachEnd(EncodedElement e) {
    if(DEBUG_LEV > 0)
      System.err.println("EncodedElement::attachEnd : Begin");
    boolean attached = true;
    EncodedElement current = this;
    while(current.getNext() != null) {
      current = current.getNext();
    }
    current.setNext(e);
    e.setPrevious(current);
    if(DEBUG_LEV > 0)
      System.err.println("EncodedElement::attachEnd : End");
    return attached;
  }

  /**
   * Add a number of bits from a long to the end of this list's data. Will
   * add a new element if necessary. The bits stored are taken from the lower-
   * order of input.
   *
   * @param input Long containing bits to append to end.
   * @param bitCount Number of bits to append.
   * @return EncodedElement which actually contains the appended value.
   */
  public EncodedElement addLong(long input, int bitCount) {
    if(next != null) {
      EncodedElement end = EncodedElement.getEnd_S(next);
      return end.addLong(input, bitCount);
    }
    else if(data.length*8 <= usableBits+bitCount) {
      //create child and attach to next.
      //Set child's offset appropriately(i.e, manually set usable bits)
      int tOff = usableBits %8;
      int size = data.length/2+1;
      //guarantee that our new element can store our given value
      if(size < bitCount) size = bitCount*10;
      next = new EncodedElement(size, tOff);
      //add int to child
      return next.addLong(input, bitCount);
    }
    //At this point, we have the space, and we are the end of the chain.
    int startPos = this.usableBits;
    byte[] dest = this.data;
    EncodedElement.addLong(input, bitCount, startPos, dest);
    usableBits +=  bitCount;
    return this;
  }

  /**
   * Add a number of bits from an int to the end of this list's data. Will
   * add a new element if necessary. The bits stored are taken from the lower-
   * order of input.
   *
   * @param input Int containing bits to append to end.
   * @param bitCount Number of bits to append.
   * @return EncodedElement which actually contains the appended value.
   */
  public EncodedElement addInt(int input, int bitCount) {
    if(next != null) {
      EncodedElement end = EncodedElement.getEnd_S(next);
      return end.addInt(input, bitCount);
    }
    else if(data.length*8 < usableBits+bitCount) {
      //create child and attach to next.
      //Set child's offset appropriately(i.e, manually set usable bits)
      int tOff = usableBits %8;
      //int size = data.length/2+1;
      int size = 1000;
      //guarantee that our new element can store our given value
      //if(size <= bitCount+tOff) size = (size+tOff+bitCount)*10;
      next = new EncodedElement(size, tOff);
      System.err.println("creating next node of size:bitCount "+size+
              ":"+bitCount+":"+usableBits+":"+data.length);
      System.err.println("value: "+input);
              //+this.toString()+"::"+next.toString());
      //add int to child
      return next.addInt(input, bitCount);
    }
    else {
      //At this point, we have the space, and we are the end of the chain.
      int startPos = this.usableBits;
      byte[] dest = this.data;
      EncodedElement.addInt_new(input, bitCount, startPos, dest);
      //EncodedElement.addInt_buf2(input, bitCount, startPos, dest);
      /*if(startPos/8+8 > dest.length)
         EncodedElement.addInt(input, bitCount, startPos, dest);
      else
         EncodedElement.addInt_new(input, bitCount, startPos, dest);*/
      usableBits +=  bitCount;
      return this;
    }
  }

  /**
   * Append an equal number of bits from each int in an array within given
   * limits to the end of this list.
   *
   * @param inputArray Array storing input values.
   * @param bitSize number of bits to store from each value.
   * @param start index of first usable index.
   * @param skip number of indices to skip between values(in case input data
   * is interleaved with non-desirable data).
   * @param countA Number of total indices to store from.
   * @return EncodedElement containing end of packed data. Data may flow
   * between multiple EncodedElement's, if an existing element was not large
   * enough for all values.
   */
  public EncodedElement packInt(int[] inputArray, int bitSize,
      int start, int skip, int countA) {
    //go to end if we're not there.
    if(next != null) {
      EncodedElement end = EncodedElement.getEnd_S(next);
      return end.packInt(inputArray, bitSize, start, skip, countA);
    }
    //calculate how many we can pack into current.
    int writeCount = (data.length*8 - usableBits) / bitSize;
    if(writeCount > countA) writeCount = countA;
    //pack them and update usable bits.
    EncodedElement.packInt(inputArray, bitSize, usableBits, start, skip, countA, data);
    usableBits += writeCount * bitSize;
    //if more remain, create child object and add there
    countA -= writeCount;
    if(countA > 0) {
      int tOff = usableBits %8;
      int size = data.length/2+1;
      //guarantee that our new element can store our given value
      if(size < bitSize*countA) size = bitSize*countA+10;
      next = new EncodedElement(size, tOff);
      //add int to child
      return next.packInt(inputArray, bitSize, start+writeCount*(skip+1), skip, countA);
    }
    else {
      //return last object we write to.
      return this;
    }
  }

  /**
   * Pack a number of bits from each int of an array(within given limits)to
   * the end of this list.
   *
   * @param inputA Array containing input values.
   * @param inputBits Array containing number of bits to use for each index
   * packed. This array should be equal in size to the inputA array.
   * @param inputOffset Index of first usable index.
   * @param countA Number of indices to pack.
   * @return EncodedElement containing end of packed data. Data may flow
   * between multiple EncodedElement's, if an existing element was not large
   * enough for all values.
   */
  public EncodedElement packIntByBits(int[] inputA, int[] inputBits, int inputOffset,
      int countA) {
    //go to end if we're not there.
    if(next != null) {
      EncodedElement end = EncodedElement.getEnd_S(next);
      return end.packIntByBits(inputA, inputBits, inputOffset, countA);
    }
    //calculate how many we can pack into current.
    int writeBitsRemaining = data.length*8 - usableBits;
    int willWrite = 0;
    int writeCount = 0;
    //System.err.println("writeBitsRemaining: " + writeBitsRemaining);
    for(int i = 0; i < countA; i++) {
      writeBitsRemaining -= inputBits[inputOffset+i];
      if(writeBitsRemaining >= 0) {
        writeCount++;
        willWrite += inputBits[inputOffset+i];
      }
      else
        break;
    }
    //pack them and update usable bits.
    if(writeCount > 0) {
      EncodedElement.packIntByBits(inputA, inputBits, inputOffset,
          writeCount, usableBits, data);
      //EncodedElement.packIntByBits_newFast(inputA, inputBits, inputOffset,
      //   writeCount, usableBits, data);
      usableBits += willWrite;
    }
    //if more remain, create child object and add there
    countA -= writeCount;
    if(countA > 0) {
      inputOffset += writeCount;
      int tOff = usableBits %8;
      int size = data.length/2+1;
      //guarantee that our new element can store our given value
      int remainingToWrite = 0;
      for(int i = 0; i < countA; i++) {
        remainingToWrite += inputBits[inputOffset+i];
      }
      remainingToWrite = remainingToWrite / 8 + 1;
      if(size < remainingToWrite) size = remainingToWrite+10;
      //System.err.println("remaining: "+remainingToWrite);
      //System.err.println("creating size/offset : "+size+":"+tOff);
      next = new EncodedElement(size, tOff);
      //add int to child
      return next.packIntByBits(inputA, inputBits, inputOffset, countA);
    }
    else {
      //System.err.println("returning....done");
      //return if this is last object we wrote to.
      return this;
    }
  }

  /**
   * Total number of usable bits stored by this entire list. This sums the
   * difference of each list element's "usableBits" and "offset".
   * @return Total valid bits in this list.
   */
  public int getTotalBits() {
    //this total calculates and removes the bits reserved for "offset"
    //   between the different children.
    int total = 0;
    EncodedElement iter = this;
    while(iter != null) {
      total += iter.usableBits - iter.offset;
      iter = iter.next;
    }
    return total;
  }

  /**
   * This method adds a given number of bits of an int to a byte array.
   * @param value int to store bits from
   * @param count number of low-order bits to store
   * @param startPos start bit location in array to begin writing
   * @param dest array to store bits in. dest MUST have enough space to store
   * the given data, or this function will fail.
   */
  protected static void addInt_new(int value, int count, int startPos, byte[] dest) {
    if(count <= 0) {
      return;
    }
    int secondInt = 0;
    int secondCount = 0;
    boolean doWrite = true;
    while(doWrite) {
      secondCount = (startPos%8+count)-32;
      int mask = (32-count >=32) ? 0:0xFFFFFFFF >>> (32-count);
      value = value & mask;//clean value
      boolean onIndex = (startPos%8 == 0);
      if(secondCount > 0) {
        value = (secondCount >= 32) ? 0:value>>>secondCount;//shift high-order down to write first
        secondInt = value;
        //secondCount = count2;
        count -= secondCount;
      }
      int index = startPos/8;
      int workingIntCache = 0;
      int bytesToUse = dest.length-startPos/8;
      if(bytesToUse > 4) bytesToUse = 4;
      switch(bytesToUse) {
        case 4: workingIntCache = dest[index++] << 24 | dest[index++] << 16 | dest[index++] << 8 | dest[index];break;
        case 3: workingIntCache = dest[index++] << 24 | dest[index++] << 16 | dest[index++] << 8;break;
        case 2: workingIntCache = dest[index++] << 24 | dest[index++] << 16;break;
        case 1: workingIntCache = dest[index++] << 24;break;
      }
      if(!onIndex) {
        int shiftCount = 32-(startPos%8+count);
        value = (shiftCount >= 32) ? 0:value << shiftCount;
        int workingInt = workingIntCache;
        mask = (32-startPos%8 >= 32) ? 0:0xFFFFFFFF<<(32-startPos%8);
        workingInt = workingInt & mask;//clear lower bits
        workingInt = workingInt | value;
        shiftCount = (32-count-startPos%8);
        value = (shiftCount >= 32) ? 0:workingInt>>>shiftCount;
      }
      int shiftCount = (32-count-startPos%8);
      value = (shiftCount >= 32) ? 0: value << shiftCount;//place into upper bits, we fill from top
      int workingInt = workingIntCache;
      mask = (count+startPos%8 >= 32) ? 0:0xFFFFFFFF >>> (count+startPos%8);
      workingInt = workingInt & mask;//clear upper bits
      workingInt = workingInt | value;
      index = startPos/8;
      int tempIndex = index+bytesToUse-1;
      index+= bytesToUse;
      switch(bytesToUse) {
        case 4: dest[tempIndex--] = (byte)(workingInt);
        case 3: dest[tempIndex--] = (byte)(workingInt >>> 8);
        case 2: dest[tempIndex--] = (byte)(workingInt >>> 16);
        case 1: dest[tempIndex--] = (byte)(workingInt >>> 24);
      }
      if(secondCount > 0) {
        //System.err.println("\t\tsecondCount > 0");
        startPos+=count;
        count = secondCount;
        value = secondInt;
      }
      else
        doWrite = false;
      }
    }

   /** public static void addIntOld(int input, int count, int startPos, byte[] dest) {
        if(DEBUG_LEV > 30)
            System.err.println("EncodedElement::addInt : Begin");
        int currentByte = startPos/8;
        int currentOffset = startPos%8;
        int bitRoom;//how many bits can be placed in current byte
        int upMask;//to clear upper bits(lower bits auto-cleared by L-shift
        int downShift;//bits to shift down, isolating top bits of input
        int upShift;//bits to shift up, packing byte from top.
        while(count > 0) {
            //find how many bits can be placed in current byte
            bitRoom = 8-currentOffset;
            //get those bits
            //i.e, take upper 'bitsNeeded' of input, put to lower part of byte.
            downShift = count-bitRoom;
            upMask = 255 >>> currentOffset;
            upShift = 0;
            if(downShift < 0) {
                //upMask = 255 >>> bitRoom-count;
                upShift = bitRoom - count;
                upMask = 255 >>> (currentOffset+upShift);
                downShift = 0;
            }
            if(DEBUG_LEV > 30) {
                System.err.println("count:offset:bitRoom:downShift:upShift:" +
                        count+":"+currentOffset+":"+bitRoom+":"+downShift+":"+upShift);
            }
            int currentBits = (input >>> downShift) & (upMask);
            //shift bits back up to match offset
            currentBits = currentBits << upShift;
            upMask = (byte)upMask << upShift;

            dest[currentByte] = (byte)(dest[currentByte] & (~upMask));
            //merge bytes~
            dest[currentByte] = (byte)(dest[currentByte] | currentBits);
            //System.out.println("new currentByte: " + dest[currentByte]);
            count -= bitRoom;
            currentOffset = 0;
            currentByte++;
        }
        if(DEBUG_LEV > 30)
            System.err.println("EncodedElement::addInt : End");
    }
**/

  /**
   * This method adds a given number of bits of a long to a byte array.
   * @param input long to store bits from
   * @param count number of low-order bits to store
   * @param startPos start bit location in array to begin writing
   * @param dest array to store bits in. dest MUST have enough space to store
   * the given data, or this function will fail.
   */
  private static void addLong(long input, int count, int startPos, byte[] dest) {
    if(DEBUG_LEV > 30)
      System.err.println("EncodedElement::addLong : Begin");
    int currentByte = startPos/8;
    int currentOffset = startPos%8;
    int bitRoom;//how many bits can be placed in current byte
    long upMask;//to clear upper bits(lower bits auto-cleared by L-shift
    int downShift;//bits to shift down, isolating top bits of input
    int upShift;//bits to shift up, packing byte from top.
      while(count > 0) {
        //find how many bits can be placed in current byte
        bitRoom = 8-currentOffset;
        //get those bits
        //i.e, take upper 'bitsNeeded' of input, put to lower part of byte.
        downShift = count-bitRoom;
        upMask = 255 >>> currentOffset;
        upShift = 0;
        if(downShift < 0) {
          //upMask = 255 >>> bitRoom-count;
          upShift = bitRoom - count;
          upMask = 255 >>> (currentOffset+upShift);
          downShift = 0;
        }
        if(DEBUG_LEV > 30) {
          System.err.println("count:offset:bitRoom:downShift:upShift:" +
          count+":"+currentOffset+":"+bitRoom+":"+downShift+":"+upShift);
        }
        long currentBits = (input >>> downShift) & (upMask);
        //shift bits back up to match offset
        currentBits = currentBits << upShift;
        upMask = (byte)upMask << upShift;

        dest[currentByte] = (byte)(dest[currentByte] & (~upMask));
        //merge bytes~
        dest[currentByte] = (byte)(dest[currentByte] | currentBits);
        //System.out.println("new currentByte: " + dest[currentByte]);
        count -= bitRoom;
        currentOffset = 0;
        currentByte++;
      }
      if(DEBUG_LEV > 30)
        System.err.println("EncodedElement::addLong : End");
    }

   /** public static void packIntOLD_WORKING(int[] inputArray, int startBitSize, int startPos,
            int start, int skip, int count, byte[] dest) {
        if(DEBUG_LEV > 0)
            System.err.println("EncodedElement::packInt : Begin");
        if(DEBUG_LEV > 10)
            System.err.println("start:skip:count : " +start+":"+skip+":"+count);
        for(int i = 0; i < count; i++) {
            addInt(inputArray[i*(skip+1)+start], startBitSize, startPos, dest);
            startPos+=startBitSize;
        }
    }
**/

  /**
   * Append an equal number of bits from each int in an array within given
   * limits to the given byte array.
   *
   * @param inputArray Array storing input values.
   * @param bitSize number of bits to store from each value.
   * @param start index of first usable index.
   * @param skip number of indices to skip between values(in case input data
   * is interleaved with non-desirable data).
   * @param countA Number of total indices to store from.
   * @param startPosIn First usable index in destination array(byte
   * index = startPosIn/8, bit within that byte = startPosIn%8)
   * @param dest Destination array to store input values in. This array *must*
   * be large enough to store all values or this method will fail in an
   * undefined manner.
   */
  private static void packInt(int[] inputArray, int bitSize, int startPosIn,
      int start, int skip, int countA, byte[] dest) {
    if(DEBUG_LEV > 30)
      System.err.println("EncodedElement::packInt : Begin");
    for(int valI = 0; valI < countA; valI++) {
      //int input = inputArray[valI];
      int input = inputArray[valI*(skip+1)+start];
      int count = bitSize;
      int startPos = startPosIn+valI*bitSize;
      int currentByte = startPos/8;
      int currentOffset = startPos%8;
      int bitRoom;//how many bits can be placed in current byte
      int upMask;//to clear upper bits(lower bits auto-cleared by L-shift
      int downShift;//bits to shift down, isolating top bits of input
      int upShift;//bits to shift up, packing byte from top.
      while(count > 0) {
        //find how many bits can be placed in current byte
        bitRoom = 8-currentOffset;
        //get those bits
        //i.e, take upper 'bitsNeeded' of input, put to lower part of byte.
        downShift = count-bitRoom;
        //upMask = uRSHFT(255 ,currentOffset);
        upMask = (currentOffset >= 32) ? 0: 255>>>currentOffset;
        upShift = 0;
        if(downShift < 0) {
          //upMask = 255 >>> bitRoom-count;
          upShift = bitRoom - count;
          //upMask = uRSHFT(255,(currentOffset+upShift));
          upMask = ((currentOffset+upShift) >= 32) ? 0:255>>>(currentOffset+upShift);
          downShift = 0;
        }
        if(DEBUG_LEV > 30) {
          System.err.println("count:offset:bitRoom:downShift:upShift:" +
              count+":"+currentOffset+":"+bitRoom+":"+downShift+":"+upShift);
        }
        //int currentBits = uRSHFT(input, downShift) & (upMask);
        int currentBits = (downShift >= 32) ? 0:(input>>>downShift)&upMask;
        //shift bits back up to match offset
        //currentBits = lSHFT(currentBits, upShift);
        currentBits = (upShift >= 32) ? 0:currentBits << upShift;

        //upMask = lSHFT((byte)upMask, upShift);
        upMask = (upShift >= 32) ? 0:((byte)upMask)<<upShift;

        dest[currentByte] = (byte)(dest[currentByte] & (~upMask));
        //merge bytes~
        dest[currentByte] = (byte)(dest[currentByte] | currentBits);
        //System.out.println("new currentByte: " + dest[currentByte]);
        count -= bitRoom;
        currentOffset = 0;
        currentByte++;
      }
    }
    if(DEBUG_LEV > 30)
      System.err.println("EncodedElement::packInt: End");
  }

  /**
   * Force the usable data stored in this list ends on a a byte boundary, by
   * padding to the end with zeros.
   *
   * @return true if the data was padded, false if it already ended on a byte
   * boundary.
   */
  public boolean padToByte() {
    boolean padded = false;

    EncodedElement end = EncodedElement.getEnd_S(this);
    int tempVal = end.usableBits;
    if(tempVal % 8 != 0) {
      int toWrite = 8-(tempVal%8);
      end.addInt(0, toWrite);
      /* Assert FOR DEVEL ONLY: */
      assert((this.getTotalBits()+offset) % 8 == 0);
      padded = true;
    }
    return padded;
  }

  public short getCRC16() {
    assert(getTotalBits()%8 == 0);
    assert(offset == 0);
    CRC16 crc = new CRC16();

    byte[] input = this.data;
    int stop = this.usableBits/8;
    crc.update(input, 0, stop);
    EncodedElement nextEl = this.getNext();
    if(nextEl != null) {
      byte partial = (this.usableBits%8==0)? 0:input[stop];
      //byte partial = input[stop];
      if(usableBits%8 != 0)
        System.err.println("UsableBits%8 == "+usableBits%8);
      nextEl.getCRC16(partial,this.usableBits%8, crc);
    }

    return crc.checksum();
  }
    
  private void getCRC16(byte leadByte, int bitCount, CRC16 crc) {
    assert(bitCount == offset%8);
    //combine lead bytes
    int start = offset/8;
    int stop = usableBits/8;
    byte[] input = this.data;
    if(bitCount > 0) {
      int inputByteMask = (0xFF >>> bitCount) &0xFF;
      int leadByteMask = 0xFF <<(8-bitCount);
      byte fullByte = (byte)(input[start] & inputByteMask);
      leadByte = (byte)(leadByte & leadByteMask);
      fullByte = (byte)(fullByte | leadByte);
      start+=1;
      crc.update(fullByte);
    }
    //getCRC16
    crc.update(input,start,stop);
    //pass to next
    EncodedElement nextEl = getNext();
    if(nextEl != null) {
      byte partial = (this.usableBits%8==0)? 0:data[stop];
      nextEl.getCRC16(partial,this.usableBits%8, crc);
    }
  }

  protected void print() {
    System.err.println("EncodedElement 0: ");
    System.err.println("\toffset: "+offset);
    System.err.println("\tusableBits: "+usableBits);
    System.err.println("\tdataLength: "+data.length);
    System.err.println("\tlastIndex: "+usableBits/8);
    System.err.println("\tleftoverBits: "+usableBits%8);
    if(next != null)
      next.print(1);
  }
  protected void print(int childCount) {
    System.err.println("EncodedElement "+(childCount++) + ": ");
    System.err.println("\toffset: "+offset);
    System.err.println("\tusableBits: "+usableBits);
    System.err.println("\tdataLength: "+data.length);
    System.err.println("\tlastIndex: "+usableBits/8);
    System.err.println("\tleftoverBits: "+usableBits%8);
    if(next != null)
      next.print(childCount);
  }

  protected static int packIntByBitsToByteBoundary(int[] input, int[] inputBits, int inputIndex,
      int inputCount, int destPos, byte[] dest) {
    int bitsNeeded = destPos % 8;
    if(bitsNeeded != 0) bitsNeeded = 8-bitsNeeded;
    while(bitsNeeded > 0 && inputCount > 0) {
      int inputVal = input[inputIndex];
      int inBits = inputBits[inputIndex];
      //if inBits > bitNeeded. shift value down, write what we need, done
      //else: take what we can, increment input index, try next
      if(inBits > bitsNeeded) {
        inputVal = (inBits-bitsNeeded >=32) ? 0:inputVal>>>(inBits-bitsNeeded);
        EncodedElement.addInt_new(inputVal, bitsNeeded, destPos, dest);
        destPos += bitsNeeded;
        inputBits[inputIndex] = inBits-bitsNeeded;
        bitsNeeded = 0;
      }
      else {
        if(inBits > 0) {
          EncodedElement.addInt_new(inputVal, inBits, destPos, dest);
          destPos += inBits;
          inputBits[inputIndex] = 0;
          bitsNeeded -= inBits;
        }
        inputIndex++;
        inputCount--;
      }
    }
    if(inputCount == 0) {
      inputIndex = -1;
    }
    return inputIndex;
  }

  /**
   * Pack a number of bits from each int of an array(within given limits)to
   * the end of this list.
   *
   * @param inputA Array containing input values.
   * @param inputBits Array containing number of bits to use for each index
   * packed. This array should be equal in size to the inputA array.
   * @param inputOffset Index of first usable index.
   * @param countA Number of indices to pack.
   * @param startPosIn First usable bit-level index in destination array(byte
   * index = startPosIn/8, bit within that byte = startPosIn%8)
   * @param dest Destination array to store input values in. This array *must*
   * be large enough to store all values or this method will fail in an
   * undefined manner.
   */
  protected static void packIntByBits(int[] inputA, int[] inputBits, int inputIndex,
    int inputCount, int destPos, byte[] dest) {
    int origInputIndex = inputIndex;
    inputIndex = packIntByBitsToByteBoundary(inputA, inputBits, inputIndex, inputCount,
    destPos, dest);
    if(destPos%8 > 0) destPos = (destPos/8+1)*8;//put dest where we know it should be
    if(inputIndex < 0)//done, no more to write.
      return;

    inputCount = inputCount - (inputIndex - origInputIndex);
    inputCount = EncodedElement.compressIntArrayByBits(inputA, inputBits, inputCount, inputIndex);
    assert(destPos%8 == 0);//sanity check.
    if(inputCount >1) {
      int stopIndex = inputCount-1;
      EncodedElement.mergeFullOnByte(inputA, stopIndex, dest, destPos/8);
      destPos += (stopIndex)*32;
    }
    if(inputCount >0) {
      int index = inputCount-1;
      EncodedElement.addInt_new(inputA[index], inputBits[index], destPos, dest);
      destPos+=inputBits[index];
    }
  }

  protected static int cleanInts(int[] input, int[] inputBits, int inputIndex, int count) {
    int outIndex = 0;
    for(int i = inputIndex; i < inputIndex+count; i++) {
      if(inputBits[i] > 0) {
        int mask = 0xFFFFFFFF >>>(32-inputBits[i]);
        inputBits[outIndex] = inputBits[i];
        input[outIndex++] = input[i] & mask;
      }
    }
    return outIndex;
  }

  protected static int compressIntArrayByBits(int[] input, int[] inputBits, int inCount,
      int inputIndex) {
    inCount = cleanInts(input, inputBits, inputIndex, inCount);

    int outIndex = 0;
    int workingVal = 0;
    int workingBits = 0;
    for(int i = 0; i < inCount; i++) {
      //look at bits for next number:
      //if workingBits+bits <= 32, shift int up and OR into workingVal;
      //if workingBits+bits > 32, shift down and OR into workingVal, add to output, shift leftovers up and set workingVal
      int bits = inputBits[i];
      if(bits+workingBits <= 32) {
        workingBits += bits;
        workingVal |= (input[i] << (32-workingBits));
        if(workingBits == 32) {
          inputBits[outIndex] = workingBits;
          input[outIndex++] = workingVal;
          workingBits = 0;
          workingVal = 0;
        }
      }
      else {
        workingBits += bits;
        workingVal |= input[i] >>> (workingBits-32);
        inputBits[outIndex] = workingBits;
        input[outIndex++] = workingVal;
        workingBits = workingBits-32;
        workingVal = input[i] << (32-workingBits);
      }
    }
      
    if(workingBits >0) {
      inputBits[outIndex] = workingBits;
      input[outIndex++] = workingVal>>>(32-workingBits);
    }
    else if(workingBits == 0 && outIndex == 0)//nothing written!
      outIndex = -1;
    return outIndex;
  }
   
  protected static void mergeFullOnByte(int[] input, int inCount, byte[] dest, int destIndex) {
    //System.err.println("mergeFullOnByte::begin  inBitCount: "+inCount+"  :: destBitOffset: "+destIndex);
    int INPUT_WIDTH = Integer.SIZE;
    int DEST_WIDTH = Byte.SIZE;
    assert(inCount*(INPUT_WIDTH/DEST_WIDTH) <= dest.length-destIndex);//input must fit fully inside dest
    for(int i = 0; i < inCount; i++) {
      int inVal = input[i];
      dest[destIndex++] = (byte)(inVal >>> 24);
      dest[destIndex++] = (byte)(inVal >>> 16);
      dest[destIndex++] = (byte)(inVal >>> 8);
      dest[destIndex++] = (byte)(inVal);
    }
  }

  private static final int uRSHFT2(int value,int count) {
    if(count >= 32)
      return 0;
    else
      return value >>> count;
  }
  private static long uRSHFT_L(long value, int count) {
    if(count >= 64)
      return 0;
    else
      return value >>> count;
  }
  private static final int lSHFT2(int value, int count) {
    if(count >= 32)
      return 0;
    else
      return (value << count);
  }
  private static final long lSHFT_L(long value, int count) {
    if(count >= 64)
      return 0;
    else
      return value << count;
  }
}