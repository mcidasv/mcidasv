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

import java.awt.Image;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Misc;
import visad.Data;
import visad.DateTime;
import visad.FlatField;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;
import visad.data.mcidas.AREACoordinateSystem;
import visad.meteorology.NavigatedImage;
import visad.meteorology.SingleBandedImage;
import edu.wisc.ssec.mcidasv.control.FrameComponentInfo;
import edu.wisc.ssec.mcidasv.control.McIdasComponents;
import edu.wisc.ssec.mcidasv.control.McIdasImageSequenceControl;

/**
 * Used to cache a data choice and its data.
 *
 * @author IDV development team
 * @version $Revision$
 */
public class McIdasXDataSource extends DataSourceImpl  {

    /** list of frames to load */
    private List frameNumbers = new ArrayList();

    /** list of McIDAS-X frames */
    private List frameList = new ArrayList();
    
    /** McIDAS-X connection info */
    private McIdasXInfo mcidasxInfo;

    /** list of 2D categories */          
    private List twoDCategories;  
                    
    /** list of 2D time series categories */
    private List twoDTimeSeriesCategories;
    
    /** image data arrays */
    private double values[][] = new double[1][1];

    //private boolean hasImagePreview = false;
    private boolean hasImagePreview = true;
    private Image theImage;
    private int lastPreview = -1;
    
    DisplayControlImpl dci;

    /**
     * Default bean constructor; does nothing
     */
    public McIdasXDataSource() {}

    /**
     * Create a McIdasXDataSource
     *
     *
     * @param descriptor the datasource descriptor
     * @param name my name
     * @param properties my properties
     */
    public McIdasXDataSource(DataSourceDescriptor descriptor, String name,
                            Hashtable properties) {
        super(descriptor, "McIDAS-X", "McIDAS-X", properties);
/*
        System.out.println("McIdasXDataSource:");
        System.out.println("    descriptor=" + descriptor);
        System.out.println("    name=" + name);
        System.out.println("    properties=" + properties);
*/
        if ((properties == null) || (properties.get(edu.wisc.ssec.mcidasv.chooser.FrameChooser.FRAME_NUMBERS_KEY) == null)) {
          List frames = new ArrayList();
          frames.add(new Integer(-1));
          properties.put(edu.wisc.ssec.mcidasv.chooser.FrameChooser.FRAME_NUMBERS_KEY, frames);
        }

        this.frameNumbers.clear();
        this.frameNumbers = getFrameNumbers();
        
        String host = (String)properties.get(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_HOST);
        String port = (String)properties.get(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_PORT);
        String key = (String)properties.get(edu.wisc.ssec.mcidasv.chooser.FrameChooser.REQUEST_KEY);
        mcidasxInfo = new McIdasXInfo(host, port, key);
        
        try {
        	this.frameList = makeFrames(this.frameNumbers);
        } catch (Exception e) {
            System.out.println("McIdasXDataSource constructor exception: " + e);
        }
    }
    
    /**
     * Make a list of McIDAS-X frames
     *
     * @param inFrameNumbers List of frame numbers. Cannot be {@code null}.
     *
     * @return ImageDataset
     */
    public List makeFrames(List inFrameNumbers) {
        List frames = new ArrayList();
        Integer frmInt;
        for (int i = 0; i < inFrameNumbers.size(); i++) {
          frmInt = (Integer)inFrameNumbers.get(i);
          frames.add(new McIdasFrame(frmInt.intValue(), mcidasxInfo));
        }
//        System.out.println("McIdasXDataSource makeFrames in: " + frameNumbers + ", out: " + frames);
        return frames;
    }
    
    /**
     * Get a frame from the frameList based on frame number
     */
    public McIdasFrame getFrame(int frameNumber) {
    	McIdasFrame checkFrame;
    	for (int i=0; i<this.frameList.size(); i++) {
    		checkFrame = (McIdasFrame)frameList.get(i);
    		if (checkFrame.getFrameNumber() == frameNumber) {
    			return(checkFrame);
    		}
    	}
    	return new McIdasFrame();
    }
    
    /**
     * Set a frame in the framelist based on frame number
     */
    public void setFrame(int frameNumber, McIdasFrame inFrame) {
    	McIdasFrame checkFrame;
    	for (int i=0; i<this.frameList.size(); i++) {
    		checkFrame = (McIdasFrame)frameList.get(i);
    		if (checkFrame.getFrameNumber() == frameNumber) {
    			this.frameList.set(i, inFrame);
    		}
    	}
    }
   
    /**
     * This is called after this datasource has been fully created
     * and initialized after being unpersisted by the XmlEncoder.
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        this.frameNumbers.clear();
        this.frameNumbers = getFrameNumbers();
        this.frameList = makeFrames(this.frameNumbers);
    }

    /**
     * Gets called after creation. Initialize the connection
     */
    public void initAfterCreation() {
        initConnection();
    }

    /**
     * Initialize the connection to McIdas-X.
     * This gets called when the data source is newly created
     * or decoded form a bundle.
     */
    private void initConnection() {
      int istat = 0;

      if (istat < 0)
        setInError(true,"Unable to connect to McIDAS-X");
    }

    protected boolean shouldCache(Data data) {
        return false;
    }
    
    protected void initDataSelectionComponents(
            List<DataSelectionComponent> components, final DataChoice dataChoice) {

    	getDataContext().getIdv().showWaitCursor();
    	makePreviewImage(dataChoice);
    	if (hasImagePreview) {
    		try {
    			components.add(new ImagePreviewSelection(theImage));
    		} catch (Exception e) {
    			System.out.println("Can't make preview image: "+e);
    			e.printStackTrace();
    		}
    	}
    	getDataContext().getIdv().showNormalCursor();
    }

    private void makePreviewImage(DataChoice dataChoice) {
    	int dataFrame = -1;
    	if (dataChoice.getDescription().indexOf("Frame ") >= 0) {
	    	try {
	    		dataFrame = Integer.parseInt(dataChoice.getDescription().substring(6));
	    	}
	    	catch (Exception e) {
	    		hasImagePreview = false;
	    		return;
	    	}
        }
    	if (dataFrame <= 0) {
    		hasImagePreview = false;
    		return;
    	}
    	if (dataFrame != lastPreview) {
        	McIdasFrame mxf = new McIdasFrame(dataFrame, mcidasxInfo);
        	theImage = mxf.getGIF();
    	}
    	hasImagePreview = true;
    	lastPreview = dataFrame;
    }
      
    /**
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
/*
    	System.out.println("McIdasXDataSource getDataInner:");
        System.out.println("   dataChoice=" + dataChoice);
        System.out.println("   category=" + category);
        System.out.println("   dataSelection=" + dataSelection);
        System.out.println("   requestProperties=" + requestProperties);
*/
    	
    	// Read the properties to decide which frame components should be requested
        FrameComponentInfo frameComponentInfo = new FrameComponentInfo();
        Boolean mc;
        mc = (Boolean)(requestProperties.get(McIdasComponents.IMAGE));
        if (mc == null)  mc=Boolean.TRUE; 
        frameComponentInfo.setIsImage(mc.booleanValue());
        mc = (Boolean)(requestProperties.get(McIdasComponents.GRAPHICS));
        if (mc == null)  mc=Boolean.TRUE; 
        frameComponentInfo.setIsGraphics(mc.booleanValue());
        mc = (Boolean)(requestProperties.get(McIdasComponents.COLORTABLE));
        if (mc == null)  mc=Boolean.TRUE; 
        frameComponentInfo.setIsColorTable(mc.booleanValue());
        mc = (Boolean)(requestProperties.get(McIdasComponents.ANNOTATION));
        if (mc == null)  mc=Boolean.TRUE; 
        frameComponentInfo.setIsAnnotation(mc.booleanValue());
        mc = (Boolean)(requestProperties.get(McIdasComponents.FAKEDATETIME));
        if (mc == null)  mc=Boolean.TRUE; 
        frameComponentInfo.setFakeDateTime(mc.booleanValue());
        
        List defList = null;
        frameNumbers = (List)getProperty(edu.wisc.ssec.mcidasv.chooser.FrameChooser.FRAME_NUMBERS_KEY, defList);
        
    	// Read the properties to decide which frame components need to be requested
        FrameDirtyInfo frameDirtyInfo = new FrameDirtyInfo();
        List frameDirtyInfoList = new ArrayList();
        frameDirtyInfoList = (ArrayList)(requestProperties.get(McIdasComponents.DIRTYINFO));
        
        if (frameDirtyInfoList == null) {
        	frameDirtyInfoList = new ArrayList();
        	for (int i=0; i<frameNumbers.size(); i++) {
        		frameDirtyInfo = new FrameDirtyInfo((Integer)frameNumbers.get(i), true, true, true);
        		frameDirtyInfoList.add(frameDirtyInfo);
        	}
        }

        Data data=null;
        if (frameNumbers.size() < 1) {
        	return data;
        }
        if (frameNumbers.size() < 2) {
        	for (int i=0; i<frameDirtyInfoList.size(); i++) {
        		frameDirtyInfo = (FrameDirtyInfo)frameDirtyInfoList.get(i);
        		if (frameDirtyInfo.getFrameNumber() == (Integer)frameNumbers.get(0)) {
//        			System.out.println("frameDirtyInfo: " + frameDirtyInfo);
                	data = (Data) getMcIdasSequence((Integer)frameNumbers.get(0), frameComponentInfo, frameDirtyInfo);
        		}
        	}
        } else {
        	String dc="";
        	String fd="";
        	for (int i=0; i<frameNumbers.size(); i++) {
        		dc = dataChoice.toString();
        		fd = (this.frameList.get(i)).toString();
        		if (dc.compareTo(fd) == 0) {
        			if (i > 0) {
        				frameComponentInfo.setIsColorTable(false);
        			}
                	for (int j=0; j<frameDirtyInfoList.size(); j++) {
                		frameDirtyInfo = (FrameDirtyInfo)frameDirtyInfoList.get(j);
                		if (frameDirtyInfo.getFrameNumber() == (Integer)frameNumbers.get(i)) {
//                			System.out.println("frameDirtyInfo: " + frameDirtyInfo);
                        	data = (Data) getMcIdasSequence((Integer)frameNumbers.get(i), frameComponentInfo, frameDirtyInfo);
                		}
                	}
        		}
        	}
        }
        return data;
    }

    /**
     * make a time series from selected McIdas-X frames
     */
    private SingleBandedImage getMcIdasSequence(int frameNumber,
    		FrameComponentInfo frameComponentInfo,
    		FrameDirtyInfo frameDirtyInfo)
            throws VisADException, RemoteException {
/*
      System.out.println("McIdasXDataSource getMcIdasSequence:");
      System.out.println("   frmNo=" + frmNo);
      System.out.println("   frameComponentInfo=" + frameComponentInfo);
*/
      SingleBandedImage image = getMcIdasFrame(frameNumber, frameComponentInfo, frameDirtyInfo);
      if (image != null) {
         if (shouldCache((Data)image)) {
            Integer fo = new Integer(frameNumber);
            putCache(fo,image);
         }
      }
      return image;
    }

    private DisplayControlImpl getDisplayControlImpl() {
      dci = null;
      List dcl = getDataChangeListeners();
      if (dcl != null) {
        for (int i=0; i< dcl.size(); i++) {
          if (dcl.get(i) instanceof McIdasImageSequenceControl) {
            dci= (DisplayControlImpl)(dcl.get(i));
            break;
          }
        }
      }
      return dci;
    }

    /**
     * Get frame numbers
     *
     * @return frame numbers 
     */
    public List getFrameNumbers() {
    	List defList = null;
        List gotFrameNumbers = (List)getProperty(edu.wisc.ssec.mcidasv.chooser.FrameChooser.FRAME_NUMBERS_KEY, defList);
        return gotFrameNumbers;
    }

    /**
     * Get the name for the main data object
     *
     * @return name of main data object
     */
    public String getDataName() {
        String dataName = (String) getProperty(edu.wisc.ssec.mcidasv.chooser.FrameChooser.DATA_NAME_KEY, "Frame Sequence");
        if (dataName.equals("")) {
        	dataName = "Frame Sequence";
        }
        return dataName;
    }
    
    /**
     * Get McIdasXInfo object
     * 
     * @return mcidasxInfo
     */
    public McIdasXInfo getMcIdasXInfo() {
    	return mcidasxInfo;
    }

    /**
     * Initialize the {@link ucar.unidata.data.DataCategory} objects that
     * this data source uses.
     */
    private void makeCategories() {
        twoDTimeSeriesCategories = DataCategory.parseCategories("MCIDASX;", false);
        twoDCategories = DataCategory.parseCategories("MCIDASX;", false);
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * single time step data.
     *
     * @return A list of categories.
     */
    public List getTwoDCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDCategories;
    }

    /**
     * Return the list of {@link ucar.unidata.data.DataCategory} used for
     * multiple time step data.
     *
     * @return A list of categories.
     */

    public List getTwoDTimeSeriesCategories() {
        if (twoDCategories == null) {
            makeCategories();
        }
        return twoDTimeSeriesCategories;
    }


    /**
     * Create the set of {@link ucar.unidata.data.DataChoice} that represent
     * the data held by this data source.  We create one top-level
     * {@link ucar.unidata.data.CompositeDataChoice} that represents
     * all of the image time steps. We create a set of children
     * {@link ucar.unidata.data.DirectDataChoice}, one for each time step.
     */
    public void doMakeDataChoices() {
        if (this.frameList == null) return;
        CompositeDataChoice composite = new CompositeDataChoice(this,
                                            getFrameNumbers(), getName(),
                                            getDataName(),
                                            (this.frameList.size() > 1)
                                            ? getTwoDTimeSeriesCategories()
                                            : getTwoDCategories()) {
            public List getSelectedDateTimes() {
                return dataSource.getSelectedDateTimes();
            }
        };
        addDataChoice(composite);
        doMakeDataChoices(composite);
    }

    /**
     * Make the data choices and add them to the given composite
     *
     * @param composite The parent data choice to add to
     */
    private void doMakeDataChoices(CompositeDataChoice composite) {
        int cnt = 0;
        List frameNos = new ArrayList();
        List frameChoices = new ArrayList();
        for (Iterator iter = frameList.iterator(); iter.hasNext(); ) {
            Object              object     = iter.next();
            McIdasFrame			frame      = getFrame(object);
            String              name       = frame.toString();
            DataSelection       frameSelect = null;
            Integer frameNo = frame.getFrameNumber();
            if (frameNo != null) {
            	frameNos.add(frameNo);
            	//We will create the  data choice with an index, not with the actual frame number.
            	frameSelect = new DataSelection(Misc.newList(new Integer(cnt)));
            }
            frameSelect = null;
            DataChoice choice =
                new DirectDataChoice(this, new FrameDataInfo(cnt, frame),
                                     composite.getName(), name,
                                     getTwoDCategories(), frameSelect);
            cnt++;
            frameChoices.add(choice);
        }

        //Sort the data choices.
        composite.replaceDataChoices(sortChoices(frameChoices));
    }

    /**
     * Sort the list of data choices on their frame numbers 
     *
     * @param choices The data choices
     *
     * @return The data choices sorted
     */
    private List sortChoices(List choices) {
        Object[]   choicesArray = choices.toArray();
/*
        Comparator comp         = new Comparator() {
            public int compare(Object o1, Object o2) {
                McIdasFrameDescriptor fd1 = getDescriptor(o1);
                McIdasFrameDescriptor fd2 = getDescriptor(o2);
                return fd1.getFrameNumber().compareTo(fd2.getFrameNumber());
            }
        };
        Arrays.sort(choicesArray, comp);
*/
        return new ArrayList(Arrays.asList(choicesArray));
    }

    /**
     * A utility method that helps us deal with legacy bundles that used to
     * have String file names as the id of a data choice.
     *
     * @param object     May be an AddeImageDescriptor (for new bundles) or a
     *                   String that is converted to an image descriptor.
     * @return The image descriptor.
     */
    private McIdasFrame getFrame(Object object) {
        if (object == null) {
            return null;
        }

        if (object instanceof McIdasFrame) {
        	return (McIdasFrame) object;
        }
        
        return new McIdasFrame();
    }

    /**
     * Class FrameDataInfo Holds an index and a McIdasFrame
     */
    public class FrameDataInfo {

        /** The index */
        private int index;

        /** The FD */
        private McIdasFrame frame;

        /**
         * Ctor for xml encoding
         */
        public FrameDataInfo() {}

        /**
         * CTOR
         *
         * @param index The index
         * @param frame The {@literal "FD"}.
         */
        public FrameDataInfo(int index, McIdasFrame frame) {
            this.index = index;
            this.frame = frame;
        }

        /**
         * Get the index
         *
         * @return The index
         */
        public int getIndex() {
            return index;
        }

        /**
         * Set the index
         *
         * @param v The index
         */
        public void setIndex(int v) {
            index = v;
        }
        
        /**
         * Get the frame
         *
         * @return The frame
         */
        public McIdasFrame getFrame() {
            return frame;
        }

        /**
         * Set the frame
         *
         * @param v The frame
         */
        public void setFrame(McIdasFrame v) {
            frame = v;
        }

        /**
         * toString
         *
         * @return toString
         */
        public String toString() {
            return "index: " + index + ", frame: " + frame.getFrameNumber();
        }

    }

    public SingleBandedImage getMcIdasFrame(int frameNumber,
    		FrameComponentInfo frameComponentInfo,
    		FrameDirtyInfo frameDirtyInfo)
           throws VisADException, RemoteException {
/*
        System.out.println("McIdasXDataSource getMcIdasFrame:");
        System.out.println("   frameNumber=" + frameNumber);
        System.out.println("   frameComponentInfo=" + frameComponentInfo);
        System.out.println("   frameDirtyInfo=" + frameDirtyInfo);
*/
        FlatField image_data = null;
        SingleBandedImage field = null;

        if (frameNumber < 1) return field;
        
        // Get the appropriate frame out of the list
        McIdasFrame frm = getFrame(frameNumber);
        
        // Tell the frame once whether or not to refresh cached data
        frm.setRefreshData(frameDirtyInfo.getDirtyImage() || frameDirtyInfo.getDirtyColorTable());

        FrameDirectory fd = frm.getFrameDirectory(frameDirtyInfo.getDirtyImage());
        int[] nav = fd.getFrameNav();
        int[] aux = fd.getFrameAux();
        
        if (nav[0] == 0) return field;
        
        // Set the time of the frame.  Because IDV uses time-based ordering, give the user the option
        // of "faking" the date/time by using frame number for year.  This preserves -X frame ordering.
        Date nominal_time;
        if (!frameComponentInfo.getFakeDateTime()) {
            nominal_time = fd.getNominalTime();
        }
        else {
            Calendar calendarDate = new GregorianCalendar(frameNumber, Calendar.JANUARY, 1, 0, 0, 0);
            calendarDate.setTimeZone(TimeZone.getTimeZone("UTC"));
            nominal_time = calendarDate.getTime();
        }

        int height = frm.getLineSize(frameDirtyInfo.getDirtyImage());
        if (height < 0) return field;
        int width = frm.getElementSize(frameDirtyInfo.getDirtyImage());
        if (width < 0) return field;
       
        // check for frameComponentInfo.isColorTable == true
        if (frameComponentInfo.getIsColorTable()) {
            DataContext dataContext = getDataContext();
            ColorTableManager colorTableManager = ((IntegratedDataViewer)dataContext).getColorTableManager();
            List dcl = ((IntegratedDataViewer)dataContext).getDisplayControls();
            DisplayControlImpl dc = null;
            for (int i=dcl.size()-1; i>=0; i--) {
                DisplayControlImpl dci = (DisplayControlImpl)dcl.get(i);
                if (dci instanceof ImageSequenceControl) {
                    dc = dci;
                    break;
                }
            }
            ColorTable mcidasXColorTable = frm.getColorTable(frameDirtyInfo.getDirtyColorTable());
            // TODO: Add a transparent value to the color table when only graphics were requested 
/*
            // if image wasn't requested, make color table with entry 0 as transparent
            if (!frameComponentInfo.getIsImage()) {
                float[][] mcidasXColorTableAlpha = mcidasXColorTable.getAlphaTable();
                mcidasXColorTableAlpha[3][0] = 0.0f;
                mcidasXColorTable.setTable(mcidasXColorTableAlpha);
            }
*/
            colorTableManager.addUsers(mcidasXColorTable);
            dc.setColorTable("default", mcidasXColorTable);
        }

        // check for frameComponentInfo.isAnnotation == true
        int skip = 0;
        if (!frameComponentInfo.getIsAnnotation()) {
        	skip = 12;
        }
        height = height - skip;
        
        values = new double[1][width*height];

        // check for frameComponentInfo.isImage == true
        if (frameComponentInfo.getIsImage()) {
        	byte[] image = frm.getImageData(frameDirtyInfo.getDirtyImage());
        	double pixel;
        	for (int i=0; i<width*height; i++) {
        		pixel = (double)image[i];
        		if (pixel < 0.0) pixel += 256.0;
        		values[0][i] = pixel;
        	}
        }
        else {
        	for (int i=0; i<width*height; i++) {
        		// TODO: Use a special value that is transparent in the color table
        		values[0][i] = 0.0;
        	}
        }
        
        // check for frameComponentInfo.isGraphics == true
        if (frameComponentInfo.getIsGraphics()) {
        	byte[] graphics = frm.getGraphicsData(frameDirtyInfo.getDirtyGraphics());
        	for (int i=0; i<width*height; i++) {
        		if (graphics[i] != (byte)255) {
        			values[0][i] = (double)graphics[i];
        		}
        	}
        }
        
        // Done working with the frame, put it back in the list
        setFrame(frameNumber, frm);

        // fake an area directory
        int[] adir = new int[64];
        adir[5] = fd.getULLine();
        adir[6] = fd.getULEle();
        adir[8] = height;
        adir[9] = width;
        adir[11] = fd.getLineRes();
        adir[12] = fd.getEleRes();

        AREACoordinateSystem cs;
        try {
            cs = new AREACoordinateSystem( adir, nav, aux);
        } catch (Exception e) {
            System.out.println("AREACoordinateSystem exception: " + e);
            return field;
        }

/*
        double[][] linele = new double[2][4];
        double[][] latlon = new double[2][4];
       // LR
        linele[0][0] = (double)(width-1);
        linele[1][0] = 0.0;
       // UL
        linele[0][1] = 0.0;
        linele[1][1] = (double)(height-1);
       // LL
        linele[0][2] = 0.0;
        linele[1][2] = 0.0;
       // UR
        linele[0][3] = (double)(width-1);
        linele[1][3] = (double)(height-1);
                                                                                              
        latlon = cs.toReference(linele);
        System.out.println("linele: " + linele[0][0] + " " + linele[1][0] + " " +
                           linele[0][1] + " " + linele[1][1] + " " +
                           linele[0][2] + " " + linele[1][2] + " " +
                           linele[0][3] + " " + linele[1][3]);
        System.out.println("latlon: " + latlon[0][0] + " " + latlon[1][0] + " " +
                           latlon[0][1] + " " + latlon[1][1] + " " +
                           latlon[0][2] + " " + latlon[1][2] + " " +
                           latlon[0][3] + " " + latlon[1][3]);
*/
 
        RealType[] domain_components = {RealType.getRealType("ImageElement", null, null),
              RealType.getRealType("ImageLine", null, null)};
        RealTupleType image_domain =
                   new RealTupleType(domain_components, cs, null);

		//  Image numbering is usually the first line is at the "top"
		//  whereas in VisAD, it is at the bottom.  So define the
		//  domain set of the FlatField to map the Y axis accordingly
        Linear2DSet domain_set = new Linear2DSet(image_domain,
                                 0, (width - 1), width,
                                 (height - 1), 0, height );
        RealType range = RealType.getRealType("brightness");
                                                                                              
        FunctionType image_func = new FunctionType(image_domain, range);
                                                                                              
        // now, define the Data objects
        image_data = new FlatField(image_func, domain_set);
        DateTime date = new DateTime(nominal_time);
        image_data = new NavigatedImage(image_data, date, "McIdas Image");

        // put the data values into the FlatField image_data
        image_data.setSamples(values,false);
        field = (SingleBandedImage) image_data;

        return field;
    }
    
}
