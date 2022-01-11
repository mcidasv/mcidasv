/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2022
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

import java.util.List;
import java.util.StringTokenizer;

import java.awt.Image;

import ucar.unidata.util.ColorTable;

/**
 * Class to hold McIDAS-X frame data sets.
 */
public class McIdasFrame {

    /** frame data */
    private int myFrameNumber = 0;
    private McIdasXFrameInfo myXFrameInfo;

    /**
     * Keep local copies of everything so we don't have to go back over the
     * bridge unless asked to refresh
     */
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
     * @param frameNumber Frame number.
     * @param xInfo State of McIDAS-X session.
     */
    public McIdasFrame(int frameNumber, McIdasXInfo xInfo) {
//    	System.out.println("McIdasFrame constructor for frame: " + frameNumber);
        this.myFrameNumber = frameNumber;
        this.myXFrameInfo = new McIdasXFrameInfo(frameNumber, xInfo);
    }

    /**
     * Returns frame number.
     *
     * @return {@link #myFrameNumber}.
     */
    public int getFrameNumber() {
//    	System.out.println("McIdasFrame getFrameNumber: " + this.myFrameNumber);
        return this.myFrameNumber;
    }

    /**
     * Tell {@link #myXFrameInfo} to refresh the cached data.
     *
     * @param refresh Whether or not to refresh cached data.
     */
    public void setRefreshData(boolean refresh) {
//		System.out.println("McIdasFrame setRefreshData(" + refresh + ")");
        this.myXFrameInfo.setRefreshData(refresh);
    }

    /**
     * Returns line size.
     *
     * @param refresh Whether or not to refresh {@link #myLineSize}.
     *
     * @return {@link #myLineSize}.
     */
    public int getLineSize(boolean refresh) {
//		System.out.println("McIdasFrame getLineSize(" + refresh + ")");
        if (this.myLineSize < 0 || refresh) {
            this.myLineSize = this.myXFrameInfo.getLineSize();
        }
        return this.myLineSize;
    }

    /**
     * Returns element size.
     *
     * @param refresh Whether or not {@link #myElementSize} should be refreshed.
     *
     * @return {@link #myElementSize}.
     */
    public int getElementSize(boolean refresh) {
//		System.out.println("McIdasFrame getElementSize(" + refresh + ")");
        if (this.myElementSize <0 || refresh) {
            this.myElementSize = this.myXFrameInfo.getElementSize();
        }
        return this.myElementSize;
    }

    /**
     * Returns frame directory.
     *
     * @param refresh Whether or not {@link #myFrameDirectory} should be
     *                refreshed.
     *
     * @return {@link #myFrameDirectory}.
     */
    public FrameDirectory getFrameDirectory(boolean refresh) {
//		System.out.println("McIdasFrame getFrameDirectory(" + refresh + ")");
        if (this.myFrameDirectory == null || refresh) {
            this.myFrameDirectory =
                new FrameDirectory(this.myXFrameInfo.getFrameDirectory());
        }
        return this.myFrameDirectory;
    }

    /**
     * Returns {@link ColorTable} used by {@link #myXFrameInfo}.
     *
     * @param refresh Whether or not {@link #myColorTable} should be refreshed.
     *
     * @return {@link #myColorTable}.
     */
    public ColorTable getColorTable(boolean refresh) {
//		System.out.println("McIdasFrame getColorTable(" + refresh + ")");
        if (this.myColorTable == null || refresh) {
            this.myColorTable =
                new ColorTable("McIDAS-X",ColorTable.CATEGORY_BASIC,
                    this.myXFrameInfo.getEnhancementTable());
        }
        return this.myColorTable;
    }

    /**
     * Returns image data.
     *
     * @param refresh Whether or not {@link #myImage} should be refreshed.
     *
     * @return {@link #myImage}.
     */
    public byte[] getImageData(boolean refresh) {
//		System.out.println("McIdasFrame getImageData(" + refresh + ")");
        if (this.myImage == null || refresh) {
            byte[] image = this.myXFrameInfo.getImage();
            int height = this.myLineSize;
            int width = this.myElementSize;
            this.myImage = new byte[height * width];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    this.myImage[i * width + j] =
                        image[(height - i - 1) * width + j];
                }
            }
        }
        return this.myImage;
    }

    /**
     * Returns graphics data.
     *
     * @param refresh Whether or not {@link #myGraphics} should be refreshed.
     *
     * @return {@link #myGraphics}.
     */
    public byte[] getGraphicsData(boolean refresh) {
//		System.out.println("McIdasFrame getGraphicsData(" + refresh + ")");
        if (this.myGraphics == null || refresh) {
            List graphics = this.myXFrameInfo.getGraphics();
            int height = this.myLineSize;
            int width = this.myElementSize;
            this.myGraphics = new byte[height*width];
            for (int i = 0; i < this.myGraphics.length; i++) {
                this.myGraphics[i] = (byte)255;
            }
            String line;
            StringTokenizer tok;
            int[] graphicsPt = new int[3];
            for (int i = 0; i < graphics.size(); i++) {
                line = (String)(graphics.get(i));
                tok = new StringTokenizer(line);
                for (int j = 0; j < 3; j++) {
                    graphicsPt[j] = new Integer(tok.nextToken()).intValue();
                }
                int color = graphicsPt[2];
                int x = graphicsPt[1] - 1;
                int y = graphicsPt[0] - 1;
                if (((y < height) && ( y > 0)) && ((x < width) && (x > 0))) {
                    this.myGraphics[y*width + x] = (byte)color;
                }
            }
        }
        return this.myGraphics;
    }

    /**
     * Returns image data as GIF.
     *
     * @return {@link #myXFrameInfo} in GIF format.
     */
    public Image getGIF() {
        return this.myXFrameInfo.getGIF();
    }

    /**
     * See if this McIdasFrame is equal to the object in question.
     *
     * @param o Object in question.
     *
     * @return true if {@code o} is a McIdasFrame and they area equivalent.
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
