/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Provides access to HDF4 files via the plug_hdf executable.
 */
public class HDF {
    
    private static final int INT_SIZE = 4;
    
    Process process;
    InputStream readable;
    OutputStream writable;
    
    public HDF(String exe) throws IOException {
      Runtime rt = Runtime.getRuntime();
      process = rt.exec(exe);
      writable = process.getOutputStream();
      readable = process.getInputStream();
    }

    int command(int cmd_id, byte[] paramBlock) 
      throws IOException {
      byte[] cc = intToByteArray(cmd_id);
      byte[] b_array = new byte[4+paramBlock.length];
      System.arraycopy(cc,0,b_array,0,4);
      System.arraycopy(paramBlock,0,b_array,4,paramBlock.length);
      writable.write(b_array);
      writable.flush();

      b_array = new byte[4];
      int n = readable.read(b_array,0,4);
      if (n != 4) throw new IOException("problem reading return code from command: "+cmd_id);
      int rc = byteArrayToInt(b_array);
      return rc;
    }

    synchronized int start(String filename) throws Exception {
      int rc = command(2, stringBlock(filename));
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("start failed on "+filename+", returned: "+rc);
      }
    }

    byte[] stringBlock(String name) throws IOException {
      int len = name.length();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream das = new DataOutputStream(bos);
      das.writeInt(len);
      byte[] int_byte = bos.toByteArray();
      byte[] str_bytes = name.getBytes();
      byte[] byte_array = new byte[4+len];
      System.arraycopy(int_byte, 0, byte_array, 0, 4);
      System.arraycopy(str_bytes, 0, byte_array, 4, len);
      return byte_array;
    }

    byte[] intToByteArray(int a) throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream das = new DataOutputStream(bos);
      das.writeInt(a);
      byte[] int_byte = bos.toByteArray();
      return int_byte;
    }

    byte[] intArrayToByteArray(int[] a) throws Exception {
      int a_len = a.length;
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      DataOutputStream das = new DataOutputStream(bos);
      das.writeInt(a_len);
      for (int k=0; k<a_len;k++) das.writeInt(a[k]);
      byte[] b_array = bos.toByteArray();
      return b_array;
    }

    int byteArrayToInt(byte[] b) throws IOException {
      ByteArrayInputStream bis = new ByteArrayInputStream(b);
      DataInputStream dis = new DataInputStream(bis);
      int d = dis.readInt();
      return d;
    }

    int[] byteArrayToInt(int n_elems, byte[] b) throws IOException {
      ByteArrayInputStream bis = new ByteArrayInputStream(b);
      DataInputStream dis = new DataInputStream(bis);
      int[] iarray = new int[n_elems];
      for (int k=0; k<n_elems; k++) {
        int d = dis.readInt();
        iarray[k] = d;
      }
      return iarray;
    }
    
    int readStruct() throws IOException {
      byte[] b_array = new byte[4];
      int n = readable.read(b_array,0,4);
      if (n != 4) throw new IOException("number of bytes read not what expected");
      int d = byteArrayToInt(b_array);
      return d;
    }

    String readString() throws IOException {
      byte[] b_array = new byte[4];
      int n = readable.read(b_array,0,4);
      if (n != 4) throw new IOException("number of bytes read not what expected");
      int nelem = byteArrayToInt(b_array);
      b_array = new byte[nelem];
      n = readable.read(b_array,0,nelem);
      if (n != nelem) throw new IOException("number of bytes read not what expected");
      return new String(b_array);
    }

    synchronized HDFArray readattr(int id, int attr_index) throws Exception {
      byte[] ba = intToByteArray(id);
      byte[] sb = intToByteArray(attr_index);
      byte[] cb = new byte[4+sb.length];
      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);

      int rc = command(4, cb);
      if (rc >= 0) {
        String data_type = readString();
        int element_count = readStruct();
        HDFArray obj = readRawBlock(data_type, element_count);
        return obj;
      }
      else {
        throw new Exception("readattr failed on id,attr_index: "+id+","+attr_index);
      }
    }

    synchronized int getdimid(int sds_id, int dim_index) throws Exception {
      byte[] ba = intToByteArray(sds_id);
      byte[] sb = intToByteArray(dim_index);
      byte[] cb = new byte[4+sb.length];
      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);

      int rc = command(10, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("getdimid failed on sds_id,dim_index: "+sds_id+","+dim_index);
      }
    }

    synchronized HDFDimension diminfo(int dim_id) throws Exception {
      byte[] ba = intToByteArray(dim_id);

      int rc = command(11, ba);
      if (rc >= 0) {
        String dim_name = readString();
        int dim_size = readStruct();
        int dim_type_code = readStruct();
        int dim_n_attrs = readStruct();
        return new HDFDimension(dim_name, dim_size, dim_type_code, dim_n_attrs);
      }
      else {
        throw new Exception("diminfo failed on dim_id: "+dim_id+" returned: "+rc);
      }

    }

    synchronized HDFVariableInfo getinfo(int sds_id) throws Exception {
      byte[] ba = intToByteArray(sds_id);
      int rc = command(13, ba);
      if (rc>=0) {
        String var_name = readString();
        int var_rank = readStruct();

        byte[] b_array = new byte[4];
        int n = readable.read(b_array, 0, 4);
        int n_words = byteArrayToInt(b_array);

        byte[] b_array2 = new byte[n_words*4];
        n = readable.read(b_array2, 0, b_array2.length);
        if (n != b_array2.length) throw new Exception("msg");
        int[] var_dim_lengths = byteArrayToInt(var_rank, b_array2);

        int var_data_type = readStruct();
        int var_num_attrs = readStruct();

        return new HDFVariableInfo(var_name, var_rank, var_dim_lengths, var_data_type, var_num_attrs);
      }
      else {
        throw new Exception("getinfo failed on sds_id: "+sds_id+" returned: "+rc);
      }
    }

    synchronized HDFFileInfo fileinfo(int sd_id) throws Exception {
      byte[] ba = intToByteArray(sd_id);
      int rc = command(12, ba);
      if (rc>=0) {
        int num_datasets = readStruct();
        int num_global_attrs = readStruct();
        return new HDFFileInfo(num_datasets, num_global_attrs);
      }
      else {
        throw new Exception("fileinfo failed on sd_id: "+sd_id+", returned: "+rc);
      }
    }

    synchronized int select(int id, int index) throws Exception {
      byte[] ba = intToByteArray(id);
      byte[] sb = intToByteArray(index);
      byte[] cb = new byte[4+sb.length];

      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);

      int rc = command(3, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("select failded on id, index: "+id+","+index+" returned: "+rc);
      }
    }

    synchronized HDFArray readRawBlock(String data_type, int element_count) throws IOException {
      byte[] b_array = new byte[4];
      int n = readable.read(b_array,0,4);
      if (n != 4) throw new IOException("msg");
      int nbytes = byteArrayToInt(b_array);

      ByteBuffer b_buf = ByteBuffer.allocate(nbytes);
      byte[] bb = b_buf.array();

      //- workaround for problem seen only on MAC OS X
      int ntotal = 0;
      while(ntotal < nbytes) {
        n = readable.read(bb, ntotal, nbytes-ntotal);
        ntotal += n;
      }

      if (data_type.equals("s")) {
        return HDFArray.make(new String[] {new String(bb)});
      }

      // use java.nio to format bytes
      b_buf.rewind();

      if ((data_type.equals("I")) || (data_type.equals("i"))) {
        IntBuffer buf = b_buf.asIntBuffer();
        int[] array = new int[element_count];
        buf.get(array);
        return HDFArray.make(array);
      }
      if ((data_type.equals("H")) || (data_type.equals("h"))) {
        ShortBuffer buf = b_buf.asShortBuffer();
        short[] array = new short[element_count];
        buf.get(array);
        return HDFArray.make(array);
      }
      if ((data_type.equals("D")) || (data_type.equals("d"))) {
        DoubleBuffer buf = b_buf.asDoubleBuffer();
        double[] array = new double[element_count];
        buf.get(array);
        return HDFArray.make(array);
      }
      if ((data_type.equals("F")) || (data_type.equals("f"))) {
        FloatBuffer buf = b_buf.asFloatBuffer();
        float[] array = new float[element_count];
        buf.get(array);
        return HDFArray.make(array);
      }

      return null;
    }

    synchronized int endaccess(int sds_id) throws Exception {
      int rc = command(7, intToByteArray(sds_id));
      if (rc >= 0) {
        return rc;
      }
      else {
        throw new Exception("endaccess failed on sds_id: "+sds_id+" returned: "+rc);
      }
    }

    synchronized int findattr(int id, String name) throws Exception {
      byte[] ba = intToByteArray(id);
      byte[] sb = stringBlock(name);
      byte[] cb = new byte[4+sb.length];
      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);
 
      int rc = command(5, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("findattr failed on id, name: "+id+","+name+" returned: "+rc);
      }
    }

    synchronized int nametoindex(int id, String name) throws Exception {
      byte[] ba = intToByteArray(id);
      byte[] sb = stringBlock(name);
      byte[] cb = new byte[4+sb.length];
      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);

      int rc = command(8, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("nametoindex failed on id,name: "+id+","+name+" returned: "+rc);
      }
    }

    synchronized HDFArray readdata(int sds_id, int[] start, int[] stride, int[] edges) throws Exception {
      int len = 0;

      byte[] ba_id = intToByteArray(sds_id);
      len += 4;
      byte[] ba_start = intArrayToByteArray(start);
      len += ba_start.length;
      byte[] ba_stride = intArrayToByteArray(stride);
      len += ba_stride.length;
      byte[] ba_edges = intArrayToByteArray(edges);
      len += ba_edges.length;
      byte[] b_array = new byte[len];

      len = 0;
      System.arraycopy(ba_id,0,b_array,len,ba_id.length);
      len += ba_id.length;
      System.arraycopy(ba_start,0,b_array,len,ba_start.length);
      len += ba_start.length;
      System.arraycopy(ba_stride,0,b_array,len,ba_stride.length);
      len += ba_stride.length;
      System.arraycopy(ba_edges,0,b_array,len,ba_edges.length);

      int rc = command(6, b_array);
      if (rc >= 0) {
        String data_type = readString();
        int elementCount = readStruct();
        return readRawBlock(data_type, elementCount);
      }
      else {
        throw new Exception("readdata failed on sds_id: "+sds_id+" returned: "+rc);
      }
    }

    synchronized int vStart(int f_id) throws Exception {
      byte[] ba = intToByteArray(f_id);
      int rc = command(16, ba);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("vStart failed on "+f_id+", returned: "+rc);
      }
    }

    synchronized int vEnd(int v_id) throws Exception {
      byte[] ba = intToByteArray(v_id);
      int rc = command(18, ba);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("vEnd failed on "+v_id+", returned: "+rc);
      }
    }
    
    synchronized int hOpen(String filename) throws Exception {
      int rc = command(14, stringBlock(filename));
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("hopen failed on "+filename+", returned: "+rc);
      }
    }

    synchronized int hClose(int f_id) throws Exception {
      byte[] ba = intToByteArray(f_id);
      int rc = command(15, ba);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("h_close failed on "+f_id+", returned: "+rc);
      }
    }

    synchronized int vsFind(int f_id, String data_name) throws Exception {
      byte[] ba = intToByteArray(f_id);
      byte[] sb = stringBlock(data_name);
      byte[] cb = new byte[4+sb.length];
      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);

      int rc = command(17, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("vsFind failed on "+f_id+",data_name: "+data_name+", returned: "+rc);
      }
    }

    synchronized int vsAttach(int f_id, int v_id) throws Exception {
      byte[] ba = intToByteArray(f_id);
      byte[] sb = intToByteArray(v_id);
      byte[] cb = new byte[4+sb.length];

      System.arraycopy(ba,0,cb,0,4);
      System.arraycopy(sb,0,cb,4,sb.length);
      int rc = command(18, cb);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("vsAttach failed on "+f_id+",v_id: "+v_id+", returned: "+rc);
      }
    }
    
    synchronized int vsDetach(int v_id) throws Exception {
      byte[] ba = intToByteArray(v_id);
      int rc = command(21, ba);
      if (rc >= 0) {
        return readStruct();
      }
      else {
        throw new Exception("vsDetach failed on "+v_id+", returned: "+rc);
      }
    }

    synchronized HDFArray vsRead(int v_id, String name, int start_idx, int nrecs, int stride) throws Exception {
      int len = 0;
      byte[] ba = intToByteArray(v_id);
      len += 4;
      byte[] sb = stringBlock(name);
      len += sb.length;
      byte[] st = intToByteArray(start_idx);
      len += 4;
      byte[] nr = intToByteArray(nrecs);
      len += 4;
      byte[] se = intToByteArray(stride);
      len += 4;
      
      byte[] cb = new byte[len];
      len = 0;
      System.arraycopy(ba, 0, cb, len, ba.length);
      len += ba.length;
      System.arraycopy(sb, 0, cb, len, sb.length);
      len += sb.length;
      System.arraycopy(st, 0, cb, len, st.length);
      len += st.length;
      System.arraycopy(nr, 0, cb, len, nr.length);
      len += nr.length;
      System.arraycopy(se, 0, cb, len, se.length);

      int rc = command(19, cb);
      if (rc >= 0) {
        String data_type = readString();
        int elementCount = readStruct();
        return readRawBlock(data_type, elementCount);
      }
      else {
        throw new Exception("vsRead failed on "+v_id+",name: "+name+",(start,nrecs,stride):"+start_idx+" "+nrecs+" "+stride+", returned: "+rc);
      }
    }
    
    synchronized public void close() throws IOException {
        process.destroy();
        readable.close();
        writable.close();
    }

 }
