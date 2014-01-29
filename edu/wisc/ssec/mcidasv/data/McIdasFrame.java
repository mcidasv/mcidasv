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

import edu.wisc.ssec.mcidasv.data.McIdasXInfo;
import edu.wisc.ssec.mcidasv.data.McIdasXFrameInfo;

import java.util.List;
import java.util.StringTokenizer;

import java.awt.Image;

import ucar.unidata.util.ColorTable;

/**
 * Class to hold McIdas-X frame datasets
 */
public class McIdasFrame {
    
    /** frame data */
    private int	myFrameNumber = 0;
    private McIdasXFrameInfo myXFrameInfo;
    
    /** Keep local copies of everything so we dont have to go back over the bridge unless asked to refresh */
    private int myLineSize = -1;
    private int myElementSize = -1;
    private FrameDirectory myFrameDirectory;
    private ColorTable myColorTable;
    private byte[] myImage;
    private byte[] myGraphics;
    
    /**
     *  Empty constructor for XML encoding
     */
    public McIdasFrame() {}

    /**
     * Construct a new McIdasFrame from the given frame number
     *
     * @param frameNumber       frame number
     */
    public McIdasFrame(int frameNumber, McIdasXInfo xInfo) {
//    	System.out.println("McIdasFrame constructor for frame: " + frameNumber);
        this.myFrameNumber = frameNumber;
        this.myXFrameInfo = new McIdasXFrameInfo(frameNumber, xInfo);
    }
    
    /** Get frame number */
    public int getFrameNumber() {
//    	System.out.println("McIdasFrame getFrameNumber: " + this.myFrameNumber);
        return this.myFrameNumber;
    }
    
    /** Tell the XFrameInfo to refresh the cached data */
    public void setRefreshData(boolean refresh) {
//		System.out.println("McIdasFrame setRefreshData(" + refresh + ")");
    	this.myXFrameInfo.setRefreshData(refresh);
    }

    /** Get frame data */
    public int getLineSize(boolean refresh) {
//		System.out.println("McIdasFrame getLineSize(" + refresh + ")");
    	if (this.myLineSize <0 || refresh) {
    		this.myLineSize = this.myXFrameInfo.getLineSize();
    	}
    	return this.myLineSize;
    }

    /** Get frame data */
    public int getElementSize(boolean refresh) {
//		System.out.println("McIdasFrame getElementSize(" + refresh + ")");
    	if (this.myElementSize <0 || refresh) {
    		this.myElementSize = this.myXFrameInfo.getElementSize();
    	}
    	return this.myElementSize;
    }

    /** Get Frame Directory */
	public FrameDirectory getFrameDirectory(boolean refresh) {
//		System.out.println("McIdasFrame getFrameDirectory(" + refresh + ")");
		if (this.myFrameDirectory == null || refresh) {
			this.myFrameDirectory = new FrameDirectory(this.myXFrameInfo.getFrameDirectory());
		}
    	return this.myFrameDirectory;
    }

	/** Get Color Table */
	public ColorTable getColorTable(boolean refresh) {
//		System.out.println("McIdasFrame getColorTable(" + refresh + ")");
		if (this.myColorTable == null || refresh) {
			this.myColorTable = new ColorTable("McIDAS-X",ColorTable.CATEGORY_BASIC,
					this.myXFrameInfo.getEnhancementTable());
		}
    	return this.myColorTable;
    }
	
    /** Get image data */
    public byte[] getImageData(boolean refresh) {
//		System.out.println("McIdasFrame getImageData(" + refresh + ")");
    	if (this.myImage == null || refresh) {
	        byte[] image = this.myXFrameInfo.getImage();
	        int height = this.myLineSize;
	        int width = this.myElementSize;
            this.myImage = new byte[height*width];
	        for (int i=0; i<height; i++) {
	        	for (int j=0; j<width; j++) {
	        		this.myImage[i*width + j] = image[(height-i-1)*width + j];
	        	}
	        }
    	}
    	return this.myImage;
    }

    /** Get graphics data */
	public byte[] getGraphicsData(boolean refresh) {
//		System.out.println("McIdasFrame getGraphicsData(" + refresh + ")");
    	if (this.myGraphics == null || refresh) {
            List graphics = this.myXFrameInfo.getGraphics();
	        int height = this.myLineSize;
	        int width = this.myElementSize;
            this.myGraphics = new byte[height*width];
	        for (int i=0; i<this.myGraphics.length; i++) {
	        	this.myGraphics[i] = (byte)255;
	        }
            String line;
            StringTokenizer tok;
            int[] graphicsPt = new int[3];
            for (int i=0; i<graphics.size(); i++) {
                line = (String)(graphics.get(i));
                tok = new StringTokenizer(line);
                for (int j=0; j<3; j++) {
                    graphicsPt[j] = new Integer(tok.nextToken()).intValue();
                }
                int color = graphicsPt[2];
                int x = graphicsPt[1] - 1;
                int y = graphicsPt[0] - 1;
                if (((y<height)&&(y>0)) && ((x<width)&&(x>0))) {
                    this.myGraphics[y*width + x] = (byte)color;
                }
            }
    	}
    	return this.myGraphics;
	}
	
    /** Get image data */
    public Image getGIF() {
    	return this.myXFrameInfo.getGIF();
    }
	
    /**
     * See if this McIdasFrame is equal to the object in question
     *
     * @param o   object in question
     * @return  true if <code>o</code> is a McIdasFrame and
     *          they area equivalent
     */
    public boolean equals(Object o) {
        if ( !(o instanceof McIdasFrame)) {
            return false;
        }
        McIdasFrame that = (McIdasFrame) o;
//        System.out.println("McIdasFrame equals: " + this.toString() + " vs " + that.toString());
        return (this.myFrameNumber == that.myFrameNumber);
    }

    /**
     * Get a String representation of this object
     * 
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (this.myFrameNumber > 0) {
          buf.append("Frame " + this.myFrameNumber);
        }
//      System.out.println("McIdasFrame toString: " + buf);
        return buf.toString();
    }
  
}
