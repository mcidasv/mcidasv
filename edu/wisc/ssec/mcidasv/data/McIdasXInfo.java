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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class McIdasXInfo Holds the state of the McIDAS-X session
 * on the other end of the bridge
 */
public class McIdasXInfo {

    /** Conduit protocol info */
    private String versionString = "2";
    private String hostString = "localhost";
    private String portString = "8080";
    private String keyString = "00000000000000000000000000000000";
    
    /**
     * Constructor
     */
    public McIdasXInfo() {}

    /**
     * Copy constructor
     *
     * @param that The McIdasXInfo to copy
     *
     */
    public McIdasXInfo(McIdasXInfo that) {
        this.hostString = that.hostString;
        this.portString = that.portString;
        this.keyString  = that.keyString;
    }

    /**
     * Constructor
     */
    public McIdasXInfo(String host, String port, String key) {
        this.hostString = host;
        this.portString = port;
        this.keyString  = key;
    }

    /**
     * Get the hostString property.
     *
     * @return The hostString property.
     */
    public String getHostString() {
        return this.hostString;
    }

    /**
     * Get the portString property.
     *
     * @return The portString property.
     */
    public String getPortString() {
        return this.portString;
    }

    /**
     * Get the keyString property.
     *
     * @return The keyString property.
     */
    public String getKeyString() {
        return this.keyString;
    }

    /**
     * Set the hostString property.
     *
     * @param newValue The new vaue for the hostString property.
     */
    public void setHostString(String newValue) {
        this.hostString = newValue;
    }

    /**
     * Set the portString property.
     *
     * @param newValue The new vaue for the portString property.
     */
    public void setPortString(String newValue) {
        this.portString = newValue;
    }

    /**
     * Set the keyString property.
     *
     * @param newValue The new vaue for the keyString property.
     */
    public void setKeyString(String newValue) {
        this.keyString = newValue;
    }
    
    /**
     * Get the frame request string.
     *
     * @return The frame request string.
     */
    private String getFrameRequest() {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=V";
    }
    
    /**
     * Get the frames request string.
     *
     * @return The frames request string.
     */
    private String getFramesRequest() {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=U";
    }
    
    /**
     * Get the file request string.
     *
     * @return The file request string.
     */
    private String getFileRequest(String filename) {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=F&text=" + filename;
    }
    
    /**
     * Get the file request DataInputStream.
     *
     * @return The file request DataInputStream.
     */
    public DataInputStream getFileInputStream(String filename) {
    	return getXInputStream(getFileRequest(filename));
    }
    
    /**
     * Get the data request string.
     *
     * @return The data request string.
     */
    private String getDataRequest(Integer frame) {
    	if (frame < 1) frame = getCurrentFrame();
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=D&text=" + frame;
    }
    
    /**
     * Get the data request DataInputStream.
     *
     * @return The data request DataInputStream.
     */
    public DataInputStream getDataInputStream(Integer frame) {
    	if (frame < 1) frame = getCurrentFrame();
    	return getXInputStream(getDataRequest(frame));
    }
    
    /**
     * Get the graphics request string.
     *
     * @return The graphics request string.
     */
    private String getGraphicsRequest(Integer frame) {
    	if (frame < 1) frame = getCurrentFrame();
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=P&text=" + frame;
    }
    
    /**
     * Get the graphics request DataInputStream.
     *
     * @return The graphics request DataInputStream.
     */
    public DataInputStream getGraphicsInputStream(Integer frame) {
    	return getXInputStream(getGraphicsRequest(frame));
    }
    
    /**
     * Get the command request string.
     *
     * @return The command request string.
     */
    private String getCommandRequest(String commandLine) {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=T&text=" + commandLine;
    }
    
    /**
     * Get the command request string.
     *
     * @return The command request string.
     */
    private String getCommandRequest(String commandLine, int frame) {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=" + frame + "&x=0&y=0&type=T&text=" + commandLine;
    }
    
    /**
     * Get the command request DataInputStream.
     *
     * @return The command request DataInputStream.
     */
    public DataInputStream getCommandInputStream(String commandLine) {
    	return getXInputStream(getCommandRequest(commandLine));
    }
    
    /**
     * Get the command request DataInputStream.
     *
     * @return The command request DataInputStream.
     */
    public DataInputStream getCommandInputStream(String commandLine, int frame) {
    	return getXInputStream(getCommandRequest(commandLine, frame));
    }
    
    /**
     * Get the GIF request string.
     *
     * @return The GIF request string.
     */
    private String getGIFRequest(Integer frame) {
        return "http://" + hostString  + ":" + portString + "/?sessionkey=" + keyString +
        			"&version=" + versionString + "&frame=0&x=0&y=0&type=C&text=" + frame;
    }
    
    /**
     * Get the GIF request DataInputStream.
     *
     * @return The GIF request DataInputStream.
     */
    public DataInputStream getGIFInputStream(Integer frame) {
    	return getXInputStream(getGIFRequest(frame));
    }

    /**
     * Get a String representation of this object
     * 
     * @return a string representation
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("McIdasXInfo as string: ");
        buf.append("hostString=");
        buf.append(this.hostString);
        buf.append(", portString=");
        buf.append(this.portString);
        buf.append(", keyString=");
        buf.append(this.keyString);
        return buf.toString();
    }

    /**
     * Get a DataInputStream from a given request String.
     * 
     * @return a DataInputStream
     */
    private DataInputStream getXInputStream(String newRequest) {
        URL url;
        URLConnection urlc;
        DataInputStream retStream = null;
        try {
            url = new URL(newRequest);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            retStream = new DataInputStream(new BufferedInputStream(is));
        } catch (Exception e) {
        	return retStream;
        }
        return retStream;
    }
    
    /**
     * Get the current frame in McIDAS-X.
     * 
     * @return The current frame in McIDAS-X.
     */
    public int getCurrentFrame() {
    	int ret = -1;
        DataInputStream inputStream = getXInputStream(getFrameRequest());
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        	// Burn key line
        	String lineOut = br.readLine();
        	lineOut = br.readLine();
        	StringTokenizer tok = new StringTokenizer(lineOut, " ");
            String responseType = tok.nextToken();
            if (!responseType.equals("V")) {
    	        System.out.println("getCurrentFrame unexpected responseType: " + responseType);
    	        try { inputStream.close(); }
    	        catch (Exception ee) {}
                return ret;
            }
            ret = Integer.parseInt(tok.nextToken());
	    } catch (Exception e) {
	        System.out.println("getCurrentFrame exception: " + e);
            try { inputStream.close(); }
            catch (Exception ee) {}
	        return ret;
	    }
        System.out.println("getCurrentFrame: " + ret);
	    return ret;
    }
    
    /**
     * Get the number of frames in McIDAS-X.
     * 
     * @return The number of frames in McIDAS-X.
     */
    public int getNumberOfFrames() {
    	int ret = -1;
    	String token = null;
    	DataInputStream inputStream = getXInputStream(getFrameRequest());
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        	// Burn key line
        	String lineOut = br.readLine();
        	lineOut = br.readLine();
        	StringTokenizer tok = new StringTokenizer(lineOut, " ");
            String responseType = tok.nextToken();
            if (!responseType.equals("V")) {
    	        System.out.println("getNumberOfFrames unexpected responseType: " + responseType);
    	        try { inputStream.close(); }
    	        catch (Exception ee) {}
                return ret;
            }
            token = tok.nextToken();
            token = tok.nextToken();
            ret = Integer.parseInt(token.substring(1,4));
	    } catch (Exception e) {
	        System.out.println("getNumberOfFrames exception: " + e);
            try { inputStream.close(); }
            catch (Exception ee) {}
	        return ret;
	    }
        System.out.println("getNumberOfFrames: " + ret);
	    return ret;
    }
    
    /**
     * Get the list of frame numbers in McIDAS-X.
     * 
     * @return The list of frame numbers in McIDAS-X.
     */
    public List getFrameNumbers() {
        List frameNumbers = new ArrayList();
        DataInputStream inputStream = getXInputStream(getFramesRequest());
        if (inputStream == null) return frameNumbers;
        String responseType = null;
        StringTokenizer tok;
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        	// Burn key line
        	String lineOut = br.readLine();
        	lineOut = br.readLine();
        	while (lineOut != null) {
	        	tok = new StringTokenizer(lineOut, " ");
	            responseType = tok.nextToken();
	            if (!responseType.equals("U")) {
	    	        System.out.println("getFrameNumbers unexpected responseType: " + responseType);
	    	        try { inputStream.close(); }
	    	        catch (Exception ee) {}
	                return frameNumbers;
	            }
	            Integer frameInt = Integer.parseInt(tok.nextToken());
	            frameNumbers.add(frameInt);
	            lineOut = br.readLine();
        	}
	    } catch (Exception e) {
	        System.out.println("getFrameNumbers exception: " + e);
            try { inputStream.close(); }
            catch (Exception ee) {}
	        return frameNumbers;
	    }
//        System.out.println("getFrameNumbers: " + frameNumbers);
	    return frameNumbers;
    }
    
}
