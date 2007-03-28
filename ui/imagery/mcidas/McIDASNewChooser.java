package ucar.unidata.ui.imagery.mcidas;

import ucar.unidata.data.imagery.mcidas.FrameDirectory;
import ucar.unidata.data.imagery.mcidas.McIDASFrameDescriptor;

import edu.wisc.ssec.mcidas.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.*;
import java.nio.channels.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.imageio.ImageIO;

import ucar.unidata.data.imagery.mcidas.ConduitInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.ui.ChooserPanel;
//import ucar.unidata.ui.TextHistoryPane;

import ucar.unidata.util.Defaults;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.PreferenceList;

import visad.*;
import visad.util.*;

/**
 * Widget to select frames from McIDAS-X shared memory
 * Displays a list of the descriptors (names) of the frame datasets
 */
public class McIDASNewChooser extends FrameChooser {


    /** A widget for the command line text */
    private JTextField hostLine;
    private JTextField portLine;
    private JTextField keyLine;
/*
    private JLabel commandLineLabel;
    private JTextField commandLine;
    private JPanel commandPanel;
    private JButton sendBtn;
    private TextHistoryPane textArea;
*/
    private boolean goodToGo = true;
    private URLConnection urlc;
    private DataInputStream inputStream;

    private int frameNumber = 0;
    private int frmDir[];
    private int navBlock[];
    private static int dirLength = 64;
    private static int navLength = 640;

    private List frameNumbers = new ArrayList(); 
    /**
     *  The list of currently loaded frame Descriptor-s
     */
    private Vector frameDescriptors;

    /**
     *  Keep track of which image load we are on.
     */
    private int currentFrame = 0;

    private ConduitInfo conduitInfo;
    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public McIDASNewChooser(IdvChooser idvChooser,
                               PreferenceList descList) {
        conduitInfo = new ConduitInfo();
    }

    /**
     * Convenience method for lazy people who don't want to call
     * {@link ucar.unidata.util.LogUtil#logException(String, Throwable)}.
     *
     * @param msg    log message
     * @param exc    Exception to log
     */
/*
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }
*/

    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "McIDAS-X Frame Data";
    }

    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public String getDatasetName() {
        String temp = null;
        return temp;
    }

    /**
     * Check if we are ready to read times
     *
     * @return  true if times can be read
     */

    protected boolean canReadFrames() {
        return true;
    }


    /**
     * Check if a descriptor (image type) has been chosen
     *
     * @return  true if an image type has been chosen
     */
/*
    protected boolean haveDescriptorSelected() {
        return true;
    }
*/


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
/*
    public void handleUpdate()  {
    }
*/

    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        int ptSize = 12;
        List allComps = new ArrayList();
        getComponents(allComps);
        JPanel linkPanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_N,
                                             GuiUtils.WT_N);
        return GuiUtils.topCenter(linkPanel, getDefaultButtons(this));
    }

    private int getFrameNumbers() {
        int ret = -1;
        frameNumbers.clear();
	String statusRequest = conduitInfo.request + "U";
        //System.out.println("statusRequest=" + statusRequest);
        URL url;
        try
        {
            url = new URL(statusRequest);
            urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            inputStream =
              new DataInputStream(
                new BufferedInputStream(is));
        }
        catch (Exception e)
        {
            return ret;
        }
        String responseType = null;
        String lineOut = null;
        try {
            lineOut = inputStream.readLine();
            //System.out.println(lineOut);
            lineOut = inputStream.readLine();
            //System.out.println(lineOut);
        } catch (Exception e) {
            System.out.println("readLine exception=" + e);
            try {
                inputStream.close();
            } catch (Exception ee) {
            }
            return ret;
        }
        StringTokenizer tok;
        while (lineOut != null) {
            tok = new StringTokenizer(lineOut, " ");
            responseType = tok.nextToken();
            //System.out.println(lineOut + " responseType=" + responseType);
            if (!responseType.equals("U")) {
                try {
                    inputStream.close();
                } catch (Exception ee) {
                }
                return ret;
            }
            Integer frameInt = new Integer(lineOut.substring(2,5));
            frameNumbers.add(frameInt);
            try {
                lineOut = inputStream.readLine();
	    } catch (Exception e) {
	        System.out.println("readLine exception=" + e);
                try {
                    inputStream.close();
                } catch (Exception ee) {
                }
	        return ret;
	    }
        }
        try {
            inputStream.close();
        } catch (Exception ee) {
        }
        return ++ret;
    }

    private void addSource() {
        goodToGo = true;
    }

    /**
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        List firstLine = new ArrayList();

/* Host */
        JLabel hostLabel = GuiUtils.rLabel("Host: ");
        firstLine.add(hostLabel);
        hostLine = new JTextField(conduitInfo.hostString, 10);
        hostLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                //System.out.println("Host has gained the focus");
            }
            public void focusLost(FocusEvent e) {
                //System.out.println("Host has lost the focus");
                sendHost();
            }
        });
        hostLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendHost();
            }
        });
        hostLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(hostLine);
        firstLine.add(new JLabel("  "));

/* Port */
        JLabel portLabel = GuiUtils.rLabel("Port: ");
        firstLine.add(portLabel);
        portLine = new JTextField(conduitInfo.portString, 6);
        portLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 sendPort();
            }
        });
        portLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 sendPort();
            }
        });
        portLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(portLine);
        firstLine.add(new JLabel("  "));

/* Key */
        JLabel keyLabel = GuiUtils.rLabel("Key: ");
        firstLine.add(keyLabel);
        keyLine = new JTextField(conduitInfo.keyString, 32);
        keyLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 sendKey();
            }
        });
        keyLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 sendKey();
            }
        });
        keyLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(keyLine);
        firstLine.add(new JLabel("  "));

        double[] nineWt = { 0, 0, 0, 0, 0, 0, 0, 0, 1 };
        JPanel firstPanel = GuiUtils.doLayout(firstLine, 9, nineWt,
                                              GuiUtils.WT_N);
        comps.add(new JLabel(" "));
        comps.add(firstPanel);
        comps.add(new JLabel(" "));
    }

     private void sendHost() {
         //System.out.println("sendHost");
         conduitInfo.setHostString((hostLine.getText()).trim());
         addSource();
     }

     private void sendPort() {
         //System.out.println("sendPort");
         //System.out.println("   before: conduitInfo.request=" + conduitInfo.request);
         conduitInfo.setPortString((portLine.getText()).trim());
         //System.out.println("   after: conduitInfo.request=" + conduitInfo.request);
         addSource();
     }

     private void sendKey() {
         //System.out.println("sendKey");
         conduitInfo.setKeyString((keyLine.getText()).trim());
         addSource();
     }

/*
    private void getFrameData() {
        if (mcXFrame.myNumber == 0) {
            mcXFrame = new McXFrame(frameNumber);
        }
	String refreshFrameRequest = conduitInfo.request + "I&text=" + frameNumber;
	URL imageUrl;
        //System.out.println(refreshFrameRequest);
        try {
            imageUrl = new URL(refreshFrameRequest);
            mcXFrame.myURL = imageUrl;
            BufferedImage bufferedImage = ImageIO.read(imageUrl);
            //System.out.println("getFrameData: got a BufferedImage");
            mcXFrame.lines = bufferedImage.getHeight();
            mcXFrame.elems = bufferedImage.getWidth();
            int type = bufferedImage.getType();
            //System.out.println("    width=" + width + " height=" + height);
            ColorModel cm = bufferedImage.getColorModel();
            ImageProducer ip = bufferedImage.getSource();
            Image image = Toolkit.getDefaultToolkit().createImage(ip);
            FlatField field = DataUtility.makeField(image);
    
            //System.out.println("done with BufferedImage section.....");
        } catch (Exception e) {
            System.out.println("getFrameData ImageIO.read e=" + e);
        }
    }
*/

    private FrameDirectory getFrameDir(int frameNumber) {
        FrameDirectory frmdir = null;
	String frameDirRequest = conduitInfo.request + "B&text=" + frameNumber;
        URL dirUrl;
	URLConnection dirUrlc;
        DataInputStream dis = null;
	try
	{
	  dirUrl = new URL(frameDirRequest);
	  dirUrlc = dirUrl.openConnection();
	  InputStream is = dirUrlc.getInputStream();
          dis = new DataInputStream( new BufferedInputStream(is));
	}
	catch (Exception e) 
	{
	  System.out.println("getFrameDir create DataInputStream exception e=" + e);
	  return frmdir;
	}
	try
	{
	   int len = 0;
           int count = 0;
           while (len < dirLength+navLength) {
               try {
	           len = dis.available()/4;
	       } catch (Exception e) {
	           System.out.println("getFrameDir: I/O error getting file size");
                   try {
                       dis.close();
                   } catch (Exception ee) {
                   }
	           return frmdir;
               }
               //System.out.println("    len=" + len); 
               count++;
               if (count > 100)  {
                   try {
                       dis.close();
                   } catch (Exception ee) {
                   }
                   return frmdir;
               }    
	   }
           //System.out.println("getFrameDir: frameNumber=" + frameNumber + " len=" + len);
           if (len < dirLength+navLength) {
               try {
                   dis.close();
               } catch (Exception ee) {
               }
               return frmdir;
           }
           int[] dir = new int[len];
           for (int i=0; i<len; i++)  {
               dir[i] = dis.readInt();
           }
           frmdir = new FrameDirectory(dir);
	}
	catch (Exception e)
	{
	    System.out.println("getFrameDir exception e=" + e);
	}
        try {
            dis.close();
        } catch (Exception ee) {
        }
	return frmdir;
    }


    /**
     * Get one of the selected frames.
     *
     * @return One of the selected frames.
     */
/*
    protected FrameDirectory getASelectedFrame() {
	if (haveFrameSelected()) {
	    McIDASFrameDescriptor fd =
		(McIDASFrameDescriptor) getTimesList().getSelectedValue();
	    if (fd != null) {
		return fd.getDirectory();
	    }
	}
	return null;
    }
*/

    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
/*
    protected void enableWidgets() {
	boolean descriptorState = (canReadFrames());

	getTimesList().setEnabled(getDoFrameLoop() && descriptorState);
    }

    protected boolean getDoFrameLoop() {
        return true;
    }
*/

    /**
     *  Read the set of image times available for the current server/group/type
     *  This method is a wrapper, setting the wait cursor and wrapping the
     *  call to {@link #readFramesInner(int)}; in a try/catch block
     */
    protected void readFrames() {
	clearFramesList();
	if ( !canReadFrames()) {
	    return;
	}
	Misc.run(new Runnable() {
	    public void run() {
		updateStatus();
		showWaitCursor();
		try {
		    readFramesInner();
		} catch (Exception e) {
		}
		showNormalCursor();
		updateStatus();
	    }
	});
    }


    /**
     * Set the list of dates/times based on the image selection
     *
     * @param framestep    the framestep for the image selection
     */
    protected void readFramesInner() {
	loadFrames();
    }


    /** locking mutex */
    private Object MUTEX = new Object();



    /**
     * Load the frames
     *
     */
    protected void loadFrames() {
	    synchronized (MUTEX) {
/*
		frameDescriptors = new Vector();
		int numFrames = 0;
		int nF = fsi.getNumberOfFrames();
		if (nF < 1) {
		  System.out.println("McIDASNewChooser: loadFrames  nF=" + nF);
		  return;
		}

		for (int i=0; i<nF; i++) {
		  if (fsi.getFrameDirectory(i+1) < 0) {
		    System.out.println("McIDASNewChooser: loadFrames  unable to get frame directory");
		    return;
		  }
		  int[] frmdir = fsi.myFrmdir;
		  FrameDirectory fd = new FrameDirectory(frmdir);
		  if (fd.sensorNumber != -1) {
		    numFrames++;
//                    String fdir=fd.toString();
		    McIDASFrameDescriptor fdir = new McIDASFrameDescriptor(fd, i+1);
		    frameDescriptors.add(fdir);
		  }
		}
		if (getDoFrameLoop()) {
		    getTimesList().setListData(frameDescriptors);
		}
		getTimesList().setVisibleRowCount(
		    Math.min(numFrames, getTimesListSize()));
		getTimesList().setSelectedIndex(0);
		getTimesList().ensureIndexIsVisible(
		    getTimesList().getSelectedIndex());
		revalidate();
*/
	    }
    }

    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
	return goodToGo;
    }

    /**
     * Returns a list of the images to load or null if none have been
     * selected.
     *
     * @return  list  get the list of image descriptors
     */
    public List getFrameList() {
        List frames = new ArrayList();
        if (getFrameNumbers() < 0) return frames;
        //System.out.println("frameNumbers=" + frameNumbers);
        for (int i = 0; i < frameNumbers.size(); i++) {
            Integer frmInt = (Integer)frameNumbers.get(i);
            int frmNo = frmInt.intValue();
            //FrameDirectory fDir = getFrameDir(frmNo);
            //if (fDir != null) {
                //McIDASFrameDescriptor fd = new McIDASFrameDescriptor(fDir, frmNo, conduitInfo.request);
                McIDASFrameDescriptor fd = new McIDASFrameDescriptor(frmNo, conduitInfo.request);
                frames.add(fd);
            //}
        }
        return frames;
    }


    /**
     * Method to do the work of loading the data
     */
    public void doLoad() {
        List frames = getFrameList();
        if (frames.size() < 1) {
            LogUtil.userMessage("Connection refused");
            return;
        }
	try {
           firePropertyChange(NEW_SELECTION, null, frames);
       } catch (Exception exc) {
           logException("doLoad", exc);
       }
    }
}
