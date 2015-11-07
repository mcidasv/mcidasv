//
// DataCacheManager
//

/*
 * VisAD system for interactive analysis and visualization of numerical
 * data.  Copyright (C) 1996 - 2015 Bill Hibbard, Curtis Rueden, Tom
 * Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
 * Tommy Jasmin, Jeff McWhirter.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
 * MA 02111-1307, USA
 */


package visad.data;


import java.io.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import java.awt.*;
import javax.swing.*;


/**
 * This provides a global cache for primitive array data
 * You access it via the singelton:<pre>
     DataCacheManager.getCacheManager()
</pre>
* Client objects can store their data with:<pre>
  Object cacheId = DataCacheManager.getCacheManager().addToCache(theData);
</pre>
 * Where data can be 1D or  2D byte/float/int/double arrays
 * If the data changes then update the cache with:<pre>
        DataCacheManager.getCacheManager().updateData(cacheId, newData);
</pre>
 * When the client object is finalized or is finished with the data call:<pre>
        DataCacheManager.getCacheManager().removeFromCache(cacheId);
</pre>
 * When you want to access the data use one of:<pre>
      DataCacheManager.getCacheManager().getByteArray1D(cacheId); 
      DataCacheManager.getCacheManager().getByteArray2D(cacheId); 
      DataCacheManager.getCacheManager().getFloatArray1D(cacheId); 
      DataCacheManager.getCacheManager().getFloatArray2D(cacheId); 
      DataCacheManager.getCacheManager().getShortArray1D(cacheId); 
      DataCacheManager.getCacheManager().getShortArray2D(cacheId); 
      DataCacheManager.getCacheManager().getIntArray1D(cacheId); 
      DataCacheManager.getCacheManager().getIntArray2D(cacheId); 
      DataCacheManager.getCacheManager().getDoubleArray1D(cacheId); 
      DataCacheManager.getCacheManager().getDoubleArray2D(cacheId); 
</pre>

* The cachemanager will keep the data arrays in memory until the total size is greater than getMaxSize(). Then it will serialize the data arrays in a least recently used manner until the totalSize less than the max size.
 */

public class DataCacheManager  implements Runnable {


 private double memoryPercentage = 0.25;    

  /** the singleton */
  private static DataCacheManager cacheManager;


  /** Where to store the cached data */
  private File cacheDir;

  /** for unique ids */
  private int idCnt = 0;

  /** for unique ids */
  private long baseTime;


  /** The cache */
  private Hashtable<Object, CacheInfo> cache = new Hashtable<Object,
                                                 CacheInfo>();

  /** a mutex */
  private Object MUTEX = new Object();

  /** Total number of bytes in memory */
  private int totalSize = 0;

  private boolean running = false;



  /**
   * ctor
   */
  private DataCacheManager() {
    baseTime = System.currentTimeMillis();
    try {
        //Start  the cache monitor in a thread
        Thread t = new Thread(this);
        t.start();
    } catch(Exception exc) {
        throw new RuntimeException(exc);
    }
  }


  /**
   * The singleton access
   *
   * @return the cache manager
   */
  public static DataCacheManager getCacheManager() {
    if (cacheManager == null) {
      cacheManager = new DataCacheManager();
    }
    return cacheManager;
  }




    public void run() {
        if(running) return;
        running= true;
        try {
            while(true) {
                Thread.currentThread().sleep(5000);
                checkCache();
            }
        } catch(Exception exc) {
            System.err.println ("Error in DataCacheManager:");
            exc.printStackTrace();
        }
        running =false;
    }


  /**
   * set the directory to write files to
   *
   * @param f dir
   */
  public void setCacheDir(File f) {
    this.cacheDir = f;
  }


  /**
   * get the cache dir. If it was not set then default to current directory
   *
   * @return The cache dir
   */
  public File getCacheDir() {
    if (cacheDir == null) {
      cacheDir = new File(".");
    }
    return cacheDir;
  }

  /**
   * Get a unique id
   *
   * @return unique id
   */
  public Object getId() {
    return "data_" + baseTime + "_" + (idCnt++);
  }


  /**
   * Add the data to the cache
   *
   * @param data the data (i.e., the array)
   * @param type The type of the data
   *
   * @return the unique id
   */
    private Object addToCache(String what, Object data, int type, boolean removeIfNeeded) {
    synchronized (MUTEX) {
        CacheInfo info = new CacheInfo(this, getId(), data, type, removeIfNeeded);
      if(what!=null) info.what = what;
      cache.put(info.getId(), info);
      totalSize += info.getSize();
      checkCache();
      return info.getId();
    }
  }





  /**
   * the data has changed for the given cache id
   *
   * @param cacheId  cache id
   * @param data  the new data
   */
  public void updateData(Object cacheId, Object data) {
    synchronized (MUTEX) {
        //      if(cacheId == null)
        //          return addToCache(data, findType(data));
        CacheInfo info = cache.get(cacheId);
//      if(info==null) {
//        return addToCache(data);
//      }

      int oldSize = info.data != null
                    ? info.getSize()
                    : 0;
      info.setData(data);
      int newSize = info.getSize();
      totalSize -= oldSize;
      totalSize += newSize;
      checkCache();
    }
  }

    public boolean inMemory(Object cacheId) {
        synchronized (MUTEX) {
        CacheInfo info =  cache.get(cacheId);
        if(info == null)return false;
        info.dataAccessed();
        return (info.data!=null);
        }
    }



  /**
   * 
   *
   * @param cacheId  the cache id
   *
   * @return 
   */
  private Object getData(Object cacheId) {
    CacheInfo info = null;
    Object data = null;
    synchronized (MUTEX) {
      info = cache.get(cacheId);
      if (info == null) return null;
      data = info.data;
      info.dataAccessed();
      if (data != null) return data;
      try {
          long t1 = System.currentTimeMillis();
        FileInputStream fis = new FileInputStream(info.cacheFile);
        BufferedInputStream bis = new BufferedInputStream(fis,100000);
        ObjectInputStream ois = new ObjectInputStream(bis);
        info.setDataFromCache(data = ois.readObject());
        long t2 = System.currentTimeMillis();
        System.err.println("Read " + info.getSize() +" bytes from file in " + (t2-t1) +" ms");
        totalSize += info.getSize();
        ois.close();
        bis.close();
        fis.close();
        checkCache();
        info.cacheMissed();
        return data;
      }
      catch (Exception exc) {
        throw new RuntimeException(exc);
      }
    }
  }

    public  File getCacheFile() {
        return new File(getCacheDir() + "/" + getId() + ".dat");
    }






  /**
   * Remove the  item from the cache 
   *
   * @param cacheId  the cache id 
   */
  public void removeFromCache(Object cacheId) {
    synchronized (MUTEX) {
        removeFromCache(cache.get(cacheId));
    }
  }


    private  void removeFromCache(CacheInfo info) {
        if (info == null) {
            return;
        }
        synchronized (MUTEX) {
            if (info.data != null) {
                info.data = null;
                totalSize -= info.getSize();
            }
            cache.remove(info.id);
            info.remove();
        }
    }


    public void flushAllCachedData() {
      synchronized (MUTEX) {
          for (CacheInfo info : getCacheInfos()) {
              flushCachedData(info);
          }
          Runtime.getRuntime().gc();
      }
    }


  /**
   * If this cacheinfo has never been written to disk then write it
   * null out the data reference
   *
   * @param info  the cacheinfo
   */
  private void flushCachedData(CacheInfo info) {
    try {
      if (info.removeIfNeeded) {
          removeFromCache(info);
          return;
      }


      if (info.data == null) {
        return;
      }



      if (!info.cacheFileGood) {
        FileOutputStream fos = new FileOutputStream(info.cacheFile);
        BufferedOutputStream bos = new BufferedOutputStream(fos,100000);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(info.data);
        oos.close();
        bos.close();
        fos.close();
      }
      info.data = null;
      totalSize -= info.getSize();
    }
    catch (Exception exc) {
      throw new RuntimeException(exc);
    }

  }


  /**
   * Get the list of sorted CacheInfo objects
   *
   * @return  Sorted list of cacheinfos
   */
  private List<CacheInfo> getCacheInfos() {
    synchronized (MUTEX) {
      List<CacheInfo> infos = new ArrayList<CacheInfo>();
      for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
        CacheInfo info = cache.get(keys.nextElement());
        infos.add(info);
      }
      Collections.sort(infos);
      return infos;
    }
  }



  public  void setMemoryPercent(double percentage) {
      memoryPercentage = percentage;
      checkCache();
  }

  public int getMaxSize() {
      return (int)(memoryPercentage*Runtime.getRuntime().maxMemory());
  }

  /**
   *  Check if we are above the max size. If so then flush data from memory  until we are below the threshold
   */
    public  void checkCache() {
        if (totalSize < getMaxSize()) {
            return;
        }
        synchronized (MUTEX) {
            //First do the volatile ones
            for (CacheInfo info : getCacheInfos()) {
                if(info.removeIfNeeded) {
                    flushCachedData(info);
                    if (totalSize <= getMaxSize()) {
                        break;
                    }
                }
            }

            if (totalSize <= getMaxSize()) {
                for (CacheInfo info : getCacheInfos()) {
                    flushCachedData(info);
                    if (totalSize <= getMaxSize()) {
                        break;
                    }
                }
            }
        }
    }





  /**
   * Print out the cache statistics 
   */
  public void printStats() {
      System.err.println(getStats());
  }


  public String getStats() {
    synchronized (MUTEX) {
        StringBuffer sb = new StringBuffer();
        int mb =(int)( getMaxSize()/(double)1000000.0);
        int total =(int)( totalSize/(double)1000000.0);
        sb.append("Cache total size:" + total +" MB   max size:" + mb +" MB  (" + (100*memoryPercentage)+"% of max memory)");
        sb.append("\n");
        List<CacheInfo> infos= getCacheInfos();
        if(infos.size()==0) {
            sb.append("nothing in cache");
            sb.append("\n");
        } else {
            sb.append("entry size/in cache/data access/cache miss/last touched");
            sb.append("\n");
            int cnt = 0;
            for (CacheInfo info : infos) {
                sb.append("   #" + (++cnt) +" ");
                sb.append(info.toString());
                /*
                          "   #" + (++cnt) +" cache entry:" + info.getSize() + "   " + (info.data != null) +
                                   "   " + info.dataAccessedCnt + "   " + info.cacheMissedCnt + "   " +
                                   new Date(info.lastTime));
                */
                sb.append("\n");

                /*
                sb.append("what:" + info.what);
                sb.append("\n");
                sb.append(info.where.substring(300));
                sb.append("\n");
                */
            }

      }
        return sb.toString();
    }
  }


  /**
   * make sure that the totalSize is the same as the cacheinfos - for debugging
 
   *
   * @param where 
   */
  private void checkStats(String where) {
    synchronized (MUTEX) {
      int tmp = 0;
      for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
        CacheInfo info = cache.get(keys.nextElement());
        if (info.data != null) tmp += info.getSize();
      }

      if (tmp != totalSize) {
        System.err.println(
          "WHOAA: " + where + "  " + tmp + " != total size:" + totalSize);
        for (Enumeration keys = cache.keys(); keys.hasMoreElements(); ) {
          CacheInfo info = cache.get(keys.nextElement());
          System.err.println(
            "   cache entry:" + info.getSize() + " " + (info.data != null));
        }
      }
    }
  }



  /**
   * Class CacheInfo 
   *
   *
   * @author IDV Development Team
   */
  private static class CacheInfo implements Comparable<CacheInfo> {

    /**  */
    private DataCacheManager cacheManager;

    /**  */
    private int type;

    /**  */
    private int size;

    /**  */
    private Object id;

    /**  */
    private long lastTime;

    /**  */
    private Object data;

    /**  */
    private File cacheFile;

    /**           */
    private boolean cacheFileGood = false;

    /**           */
    private int dataAccessedCnt = 0;

    /**           */
    private int cacheMissedCnt = 0;

    private String where;

    private String what;

    private boolean removeIfNeeded = false;


    /**
     * 
     *
     * @param cacheManager 
     * @param cacheId  the cache id
     * @param data 
     * @param type 
     */
    public CacheInfo(DataCacheManager cacheManager, Object cacheId, Object data,
                     int type, boolean removeIfNeeded) {
      this.id = cacheId;
      this.cacheManager = cacheManager;
      this.type = type;
      this.removeIfNeeded = removeIfNeeded;
      cacheFile = new File(cacheManager.getCacheDir() + "/" + cacheId + ".dat");
      this.what = data.toString();
      where = "";
      //      where = ucar.unidata.util.LogUtil.getStackTrace();
      setData(data);
    }


    /**
     * 
     */
    private void dataAccessed() {
      lastTime = System.currentTimeMillis();
      dataAccessedCnt++;
    }

    /**
     * 
     */
    private void cacheMissed() {
      cacheMissedCnt++;
    }

    /**
     * 
     *
     * @param data 
     */
    private void setData(Object data) {
      lastTime = System.currentTimeMillis();
      this.data = data;
      cacheFileGood = false;
      size = getArraySize(type, data);
    }

    /**
     * Get a string representation of the type
     *
     * @param type  the type
     * @return the string name of the type
     */
    public String getTypeName(int type) {
        return getNameForType(type);
    }

    public String toString() {
        return what+"   " + getTypeName(type) + ":" + getSize() + "   " + (data != null) + "   " + dataAccessedCnt + "   " + cacheMissedCnt + "   " + new Date(lastTime);
    }

    /**
     * Compare this to another object
     *
     * @param o 
     *
     * @return  comparison
     */
    public int compareTo(CacheInfo o) {
      CacheInfo that = (CacheInfo)o;
      if (this.lastTime < that.lastTime) return -1;
      if (this.lastTime == that.lastTime) return 0;
      return 1;

    }

    /**
     * 
     *
     * @param data 
     */
    private void setDataFromCache(Object data) {
      lastTime = System.currentTimeMillis();
      this.data = data;
    }


    /**
     * 
     */
    private void remove() {
        if(cacheFile!=null)
            cacheFile.delete();
    }

    /**
     * 
     *
     * @return 
     */
    public int getSize() {
      return size;
    }


    /**
     * 
     *
     * @return 
     */
    public Object getId() {
      return id;
    }
  }


/********
  Begin generated access methods
*****/
private static final int TYPE_DOUBLE1D = 0;
private static final int TYPE_FLOAT1D = 1;
private static final int TYPE_INT1D = 2;
private static final int TYPE_SHORT1D = 3;
private static final int TYPE_BYTE1D = 4;
private static final int TYPE_DOUBLE2D = 5;
private static final int TYPE_FLOAT2D = 6;
private static final int TYPE_INT2D = 7;
private static final int TYPE_SHORT2D = 8;
private static final int TYPE_BYTE2D = 9;
private static final int TYPE_DOUBLE3D = 10;
private static final int TYPE_FLOAT3D = 11;
private static final int TYPE_INT3D = 12;
private static final int TYPE_SHORT3D = 13;
private static final int TYPE_BYTE3D = 14;



  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public double[] getDoubleArray1D(Object cacheId) {
        return (double[])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(double[] values) {
    return addToCache(null, values, TYPE_DOUBLE1D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[] values) {
      return addToCache(what, values, TYPE_DOUBLE1D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_DOUBLE1D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public float[] getFloatArray1D(Object cacheId) {
        return (float[])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(float[] values) {
    return addToCache(null, values, TYPE_FLOAT1D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[] values) {
      return addToCache(what, values, TYPE_FLOAT1D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_FLOAT1D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public int[] getIntArray1D(Object cacheId) {
        return (int[])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(int[] values) {
    return addToCache(null, values, TYPE_INT1D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[] values) {
      return addToCache(what, values, TYPE_INT1D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_INT1D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public short[] getShortArray1D(Object cacheId) {
        return (short[])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(short[] values) {
    return addToCache(null, values, TYPE_SHORT1D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[] values) {
      return addToCache(what, values, TYPE_SHORT1D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_SHORT1D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public byte[] getByteArray1D(Object cacheId) {
        return (byte[])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(byte[] values) {
    return addToCache(null, values, TYPE_BYTE1D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[] values) {
      return addToCache(what, values, TYPE_BYTE1D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_BYTE1D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public double[][] getDoubleArray2D(Object cacheId) {
        return (double[][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(double[][] values) {
    return addToCache(null, values, TYPE_DOUBLE2D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[][] values) {
      return addToCache(what, values, TYPE_DOUBLE2D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_DOUBLE2D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public float[][] getFloatArray2D(Object cacheId) {
        return (float[][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(float[][] values) {
    return addToCache(null, values, TYPE_FLOAT2D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[][] values) {
      return addToCache(what, values, TYPE_FLOAT2D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_FLOAT2D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public int[][] getIntArray2D(Object cacheId) {
        return (int[][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(int[][] values) {
    return addToCache(null, values, TYPE_INT2D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[][] values) {
      return addToCache(what, values, TYPE_INT2D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_INT2D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public short[][] getShortArray2D(Object cacheId) {
        return (short[][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(short[][] values) {
    return addToCache(null, values, TYPE_SHORT2D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[][] values) {
      return addToCache(what, values, TYPE_SHORT2D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_SHORT2D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public byte[][] getByteArray2D(Object cacheId) {
        return (byte[][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(byte[][] values) {
    return addToCache(null, values, TYPE_BYTE2D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[][] values) {
      return addToCache(what, values, TYPE_BYTE2D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_BYTE2D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public double[][][] getDoubleArray3D(Object cacheId) {
        return (double[][][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(double[][][] values) {
    return addToCache(null, values, TYPE_DOUBLE3D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[][][] values) {
      return addToCache(what, values, TYPE_DOUBLE3D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, double[][][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_DOUBLE3D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public float[][][] getFloatArray3D(Object cacheId) {
        return (float[][][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(float[][][] values) {
    return addToCache(null, values, TYPE_FLOAT3D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[][][] values) {
      return addToCache(what, values, TYPE_FLOAT3D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, float[][][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_FLOAT3D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public int[][][] getIntArray3D(Object cacheId) {
        return (int[][][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(int[][][] values) {
    return addToCache(null, values, TYPE_INT3D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[][][] values) {
      return addToCache(what, values, TYPE_INT3D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, int[][][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_INT3D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public short[][][] getShortArray3D(Object cacheId) {
        return (short[][][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(short[][][] values) {
    return addToCache(null, values, TYPE_SHORT3D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[][][] values) {
      return addToCache(what, values, TYPE_SHORT3D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, short[][][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_SHORT3D, removeIfNeeded);
  }




  /**
   * get the value from the cache
   *
   * @param cacheId  the cache id
   *
   * @return  the value
   */
    public byte[][][] getByteArray3D(Object cacheId) {
        return (byte[][][])getData(cacheId);
    }

  /**
   * add the data to the cache
   *
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(byte[][][] values) {
    return addToCache(null, values, TYPE_BYTE3D, false);
  }

  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[][][] values) {
      return addToCache(what, values, TYPE_BYTE3D, false);
  }
  
  /**
   * add the data to the cache
   *
   * @param what the name of the item. used for tracking cache behavior
   * @param values the values to add
   * @param removeIfNeeded If true then this data will not be written to disk and will be removed from the cache
   * when the cache is exceeding memory limits
   *
   * @return the cache id
   */
  public Object addToCache(String what, byte[][][] values, boolean removeIfNeeded) {
    return addToCache(what, values, TYPE_BYTE3D, removeIfNeeded);
  }



/** Get the size of the array **/
private static int getArraySize(int type, Object values) {

   if (type == TYPE_DOUBLE1D) {
        double[] data= (double[]) values;
        
        return 8*data.length;

   }

   if (type == TYPE_FLOAT1D) {
        float[] data= (float[]) values;
        
        return 4*data.length;

   }

   if (type == TYPE_INT1D) {
        int[] data= (int[]) values;
        
        return 4*data.length;

   }

   if (type == TYPE_SHORT1D) {
        short[] data= (short[]) values;
        
        return 2*data.length;

   }

   if (type == TYPE_BYTE1D) {
        byte[] data= (byte[]) values;
        
        return 1*data.length;

   }

   if (type == TYPE_DOUBLE2D) {
        double[][] data= (double[][]) values;
        if (data[0]==null) return 0;
        return 8*data.length * data[0].length;

   }

   if (type == TYPE_FLOAT2D) {
        float[][] data= (float[][]) values;
        if (data[0]==null) return 0;
        return 4*data.length * data[0].length;

   }

   if (type == TYPE_INT2D) {
        int[][] data= (int[][]) values;
        if (data[0]==null) return 0;
        return 4*data.length * data[0].length;

   }

   if (type == TYPE_SHORT2D) {
        short[][] data= (short[][]) values;
        if (data[0]==null) return 0;
        return 2*data.length * data[0].length;

   }

   if (type == TYPE_BYTE2D) {
        byte[][] data= (byte[][]) values;
        if (data[0]==null) return 0;
        return 1*data.length * data[0].length;

   }

   if (type == TYPE_DOUBLE3D) {
        double[][][] data= (double[][][]) values;
        if (data[0]==null) return 0; if(data[0][0]==null) return 0;
        return 8*data.length * data[0].length*data[0][0].length;

   }

   if (type == TYPE_FLOAT3D) {
        float[][][] data= (float[][][]) values;
        if (data[0]==null) return 0; if(data[0][0]==null) return 0;
        return 4*data.length * data[0].length*data[0][0].length;

   }

   if (type == TYPE_INT3D) {
        int[][][] data= (int[][][]) values;
        if (data[0]==null) return 0; if(data[0][0]==null) return 0;
        return 4*data.length * data[0].length*data[0][0].length;

   }

   if (type == TYPE_SHORT3D) {
        short[][][] data= (short[][][]) values;
        if (data[0]==null) return 0; if(data[0][0]==null) return 0;
        return 2*data.length * data[0].length*data[0][0].length;

   }

   if (type == TYPE_BYTE3D) {
        byte[][][] data= (byte[][][]) values;
        if (data[0]==null) return 0; if(data[0][0]==null) return 0;
        return 1*data.length * data[0].length*data[0][0].length;

   }

   throw new IllegalArgumentException("Unknown type:" + type);
}


/** Get the name of the type **/
private static String getNameForType(int type) {
    if (type == TYPE_DOUBLE1D) {return "double1d";}
    if (type == TYPE_FLOAT1D) {return "float1d";}
    if (type == TYPE_INT1D) {return "int1d";}
    if (type == TYPE_SHORT1D) {return "short1d";}
    if (type == TYPE_BYTE1D) {return "byte1d";}
    if (type == TYPE_DOUBLE2D) {return "double2d";}
    if (type == TYPE_FLOAT2D) {return "float2d";}
    if (type == TYPE_INT2D) {return "int2d";}
    if (type == TYPE_SHORT2D) {return "short2d";}
    if (type == TYPE_BYTE2D) {return "byte2d";}
    if (type == TYPE_DOUBLE3D) {return "double3d";}
    if (type == TYPE_FLOAT3D) {return "float3d";}
    if (type == TYPE_INT3D) {return "int3d";}
    if (type == TYPE_SHORT3D) {return "short3d";}
    if (type == TYPE_BYTE3D) {return "byte3d";}
 return "unknown type";
}


}

