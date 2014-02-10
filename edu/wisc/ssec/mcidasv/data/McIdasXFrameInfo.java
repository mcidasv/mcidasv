/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

package edu.wisc.ssec.mcidasv.data;

import edu.wisc.ssec.mcidas.AREAnav;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import java.awt.Image;
import java.awt.Toolkit;

import ucar.unidata.util.Misc;

public class McIdasXFrameInfo {

    private int myFrameNumber;
    private McIdasXInfo myXInfo;

    private int width = 0;
    private int height = 0;
    private int[] stretchTable = new int[256];
    private int[] colorTable = new int[256];
    private int[] graphicsTable = new int[256];

    private byte myImage[];
    
    private boolean isLoaded = false;
    
    /**
     * Constructor
     */
    public McIdasXFrameInfo() {
    	this.myFrameNumber = 0;
    	this.myXInfo = new McIdasXInfo();
    	this.isLoaded = false;
    }

    /**
     * Copy constructor
     *
     * @param that The McIdasXFrameInfo to copy
     *
     */
    public McIdasXFrameInfo(McIdasXFrameInfo that) {
    	this.myFrameNumber = that.myFrameNumber;
    	this.myXInfo = that.myXInfo;
    	this.isLoaded = false;
    }
    
    /**
     * Create a new McIDAS-X frame using the given frame number and
     * {@literal "info"} object.
     *
     * @param frameNumber Frame number.
     * @param xInfo McIDAS-X {@literal "info"} object. Cannot be {@code null}.
     */
    public McIdasXFrameInfo(int frameNumber, McIdasXInfo xInfo) {
//    	System.out.println("McIdasXFrameInfo constructor: frame " + frameNumber + ", xInfo " + xInfo.toString());
    	this.myFrameNumber = frameNumber;
    	this.myXInfo = xInfo;
    	this.isLoaded = false;
    }
    
    public void setRefreshData(boolean refresh) {
    	if (refresh) this.isLoaded = false;
    }
    
    private int setFrameData() {
//    	System.out.println(" <=> setFrameData frame " + this.myFrameNumber);
    	int ret = -1;
    	DataInputStream inputStream = myXInfo.getDataInputStream(this.myFrameNumber);
    	try {
            this.width = inputStream.readInt();
            this.height = inputStream.readInt();
        } catch (Exception e) {
            System.out.println("getFrameData exception: " + e);
            return ret;
        }

        if (!getTable(inputStream, stretchTable)) return ret;
        if (!getTable(inputStream, colorTable)) return ret;
        if (!getTable(inputStream, graphicsTable)) return ret;

        int havebytes = 0;
        int needbytes = this.width * this.height;
        byte[] image = new byte[needbytes];
        int count = 0;
        int num = 0;
        int indx = 0;
        try {
            while (needbytes > 0) {
                num = inputStream.read(image, indx, needbytes);
                indx += num;
                havebytes += num;
                needbytes -= num;
                count++;
                Misc.sleep(10);
                if (count > 100) return ret;
            }
        } catch (Exception e) {
            System.out.println("getFrameData exception: " + e);
            return ret;
        }
        this.isLoaded = true;
        this.myImage = image;
        ret = 0;
        return ret;
    }

    public List getGraphics() {
//    	System.out.println(" <=> getGraphics frame " + this.myFrameNumber);
    	DataInputStream inputStream = myXInfo.getGraphicsInputStream(this.myFrameNumber);
        List graphics = new ArrayList();
        int pixels = 0;
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        	String lineOut = br.readLine();
        	while (lineOut != null) {
//        		System.out.println("getGraphics adding pixel: " + lineOut);
                graphics.add(lineOut);
        		pixels++;
        		lineOut = br.readLine();
        	}
	    } catch (Exception e) {
	        System.out.println("getGraphics exception: " + e);
            try { inputStream.close(); }
            catch (Exception ee) {}
	        return graphics;
	    }
//        System.out.println("getGraphics: " + pixels);
	    return graphics;
    }
    
    public int[] getFrameDirectory() {
//    	System.out.println(" <=> getFrameDirectory frame " + this.myFrameNumber);
        String filename = "Frame" + this.myFrameNumber + ".0";
    	DataInputStream inputStream = myXInfo.getFileInputStream(filename);

        int dirLength = 64;
        int[] dir = getInts(inputStream, dirLength);
        
        // The next byte tells us what type of nav we have...
        // it also needs to be the first byte of the nav array
        int navLength;
        int[] navType = getInts(inputStream, 1);
        if (navType[0] == AREAnav.LALO) navLength = 128;
        else navLength = 640;
        int[] navRest = getInts(inputStream, navLength - 1);
        int[] nav = new int[navLength];
        System.arraycopy(navType, 0, nav, 0, 1);
        System.arraycopy(navRest, 0, nav, 1, navLength - 1);

        int auxLength = 0;
        int rows = 0;
        int cols = 0;
        int begLat= 0;
        int begLon =0;
        if (navLength == 128) {
            rows = nav[65];
            cols = nav[66];
            begLat = nav[78]/4;
            begLon = nav[79]/4;
            auxLength = begLon + (rows*cols);
        }
        int[] aux = getInts(inputStream, auxLength);

        int[] frmdir = new int[dirLength + navLength + auxLength];
        System.arraycopy(dir, 0, frmdir, 0, dirLength);
        System.arraycopy(nav, 0, frmdir, dirLength, navLength);
        if (auxLength > 0)
            System.arraycopy(aux, 0, frmdir, dirLength+navLength, auxLength);
      
        try { inputStream.close(); }
        catch (Exception ee) {}
 
        return frmdir;
    }

    private boolean getTable(DataInputStream inputStream, int[] tbl) {
        try {
            for (int i=0; i<256; i++) {
                tbl[i] = inputStream.readInt();
            }
        } catch (Exception e) {
            System.out.println("getTable exception: " + e);
            return false;
        }
        return true;
    }
    
    private int[] getInts(DataInputStream stream, int count) {
        int[] buf = new int[count];
        if (count < 1) return buf;

        int havebytes=0;
        int needbytes=count;

        try {
            while (havebytes<needbytes) {
                buf[havebytes] = stream.readInt();
                havebytes++;
            }
        } catch (Exception e) {
            System.out.println("getInts exception: " + e);
            return buf;
        }

        return buf;
    }
    
    public int getFrameNumber() {
    	return this.myFrameNumber;
    }

    public int getLineSize() {
        if (!this.isLoaded) {
           if (setFrameData() < 0) this.height = -1;
        }
        return this.height;
    }

    public int getElementSize() {
        if (!this.isLoaded) {
            if (setFrameData() < 0) this.width = -1;
        }
        return this.width;
    }

    public int[] getStretchTable() {
    	if (!this.isLoaded) {
    		if (setFrameData() < 0) this.stretchTable = new int[256];
    	}
        return this.stretchTable;
    }
    
    public int[] getColorTable() {
    	if (!this.isLoaded) {
    		if (setFrameData() < 0) this.colorTable = new int[256];
    	}
        return this.colorTable;
    }
    
    public int[] getGraphicsTable() {
    	if (!this.isLoaded) {
    		if (setFrameData() < 0) this.graphicsTable = new int[256];
    	}
        return this.graphicsTable;
    }
    
    public float[][] getEnhancementTable() {
        float[][] enhancementTable = new float[3][256];
    	if (!this.isLoaded) {
    		if (setFrameData() < 0) return enhancementTable;
    	}
        for (int i=1; i<18; i++) {
        	enhancementTable[0][i] = (float)((this.colorTable[i]/0x10000)&0xff);
        	enhancementTable[1][i] = (float)((this.colorTable[i]/0x100)&0xff);
        	enhancementTable[2][i] = (float)(this.colorTable[i]&0xff);
        }
        for (int i=18; i<256; i++) {
        	enhancementTable[0][i] = (float)((this.colorTable[this.stretchTable[i]]/0x10000)&0xff);
        	enhancementTable[1][i] = (float)((this.colorTable[this.stretchTable[i]]/0x100)&0xff);
        	enhancementTable[2][i] = (float)(this.colorTable[this.stretchTable[i]]&0xff);
        }
        for (int i=0; i<256; i++) {
        	enhancementTable[0][i] /= 0xff;
        	enhancementTable[1][i] /= 0xff;
        	enhancementTable[2][i] /= 0xff;
        }
        return enhancementTable;
    }

    public byte[] getImage() {
    	if (!this.isLoaded) {
    		if (setFrameData() < 0) this.myImage = new byte[0];
    	}
        return this.myImage;
    }

    public Image getGIF() {
    	int MAX_BYTES = 1048576;
    	byte[] imagebytes = new byte[MAX_BYTES];
    	DataInputStream inputStream = this.myXInfo.getGIFInputStream(this.myFrameNumber);
    	int n = 0;
    	int i = 0;
    	for (n=0; n<MAX_BYTES; n++) {
    		try {
    			i = inputStream.read();
    		} catch (Exception ee) { }
    		if (i < 0) break;
    		imagebytes[n] = (byte)i;
    	}
    	byte[] gifbytes = new byte[n];
    	System.arraycopy(imagebytes, 0, gifbytes, 0, n);
    	Image imageGIF = Toolkit.getDefaultToolkit().createImage(gifbytes);
    	imagebytes = null;
        try { inputStream.close(); }
        catch (Exception ee) {}
        return imageGIF;
    }

}
