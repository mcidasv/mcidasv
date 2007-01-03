package ucar.unidata.ui.imagery.mcidas;

import ucar.unidata.data.imagery.mcidas.FrameDirectory;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;

import edu.wisc.ssec.mcidas.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.ImageProducer;

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

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.ui.ChooserPanel;
import ucar.unidata.ui.TextHistoryPane;

import ucar.unidata.util.Defaults;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.PreferenceList;

/**
 * Widget to select frames from McIDAS-X shared memory
 * Displays a list of the descriptors (names) of the frame datasets
 */
public class McCommandLineChooser extends FrameChooser {


    /** A widget for the command line text */
    private JTextField hostLine;
    private JTextField portLine;
    private JTextField keyLine;
    private JLabel commandLineLabel;
    private JTextField commandLine;
    private JPanel commandPanel;
    private JButton sendBtn;
    private TextHistoryPane textArea;
    private String keyString = "00000000000000000000000000000000";
    private String hostString = "occam";
    private String portString = "8080";
    private boolean goodToGo = true;
    private String request= "http://";
    private URLConnection urlc;
    private DataInputStream inputStream;

    private int frameNumber = 0;
    private int frmDir[];
    private int navBlock[];
    private static int dirLength = 64;
    private static int navLength = 640;
 
    /**
     *  The list of currently loaded frame Descriptor-s
     */
    private Vector frameDescriptors;

    /**
     *  Keep track of which image load we are on.
     */
    private int currentFrame = 0;

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public McCommandLineChooser(IdvChooser idvChooser,
                               PreferenceList descList) {
    }



    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        enableWidgets();
        return;
    }

    /**
     * Do we have frames selected. Either we are doing a frame 
     * loop and there are some selected in the list. Or we
     * are refreshing the current frame
     *
     * @return Do we have frames?
     */
    protected boolean timesOk() {
        if (getDoFrameLoop() && !haveFrameSelected()) {
            return false;
        }
        return true;
    }

    /**
     * Convenience method for lazy people who don't want to call
     * {@link ucar.unidata.util.LogUtil#logException(String, Throwable)}.
     *
     * @param msg    log message
     * @param exc    Exception to log
     */
    public void logException(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

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
    protected boolean haveDescriptorSelected() {
        return true;
    }


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate()  {
    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        int ptSize = 12;
        List allComps = new ArrayList();
        getComponents(allComps);
        JPanel imagePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_N,
                                              GuiUtils.WT_N);
        List textComps = new ArrayList();
        textArea = new TextHistoryPane(500, 100, true);
/*
        textArea.setLineWrap(true);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, ptSize));
*/
        //textComps.add(textArea);
        textComps.add(new JLabel(" "));
        textComps.add(new JLabel(" "));
        JPanel textPanel = GuiUtils.doLayout(textComps, 1, GuiUtils.WT_NN,
                                              GuiUtils.WT_N);
/*
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(620, 310));
        textPanel.add(sp, 0);
*/
        textArea.setPreferredSize(new Dimension(620, 310));
        textPanel.add(textArea);
        return GuiUtils.topCenterBottom(imagePanel, textPanel, getDefaultButtons(this));
    }

    private void addSource() {
        if (goodToGo == false) {
            goodToGo = true;
        }
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
        hostLine = new JTextField(hostString, 10);
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
        portLine = new JTextField(portString, 6);
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
        keyLine = new JTextField(keyString, 32);
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

        List sendList = new ArrayList();
        commandLineLabel = GuiUtils.rLabel("Command Line: ");
        commandLineLabel.setEnabled(false);
        sendList.add(commandLineLabel);
        commandLine = new JTextField("", 40);
        commandLine.setEnabled(false);
        commandLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 String saveCommand = (commandLine.getText()).trim();
                 sendCommandLine(saveCommand);
                 commandLine.setText(" ");
            }
        });
        commandLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 String saveCommand = (commandLine.getText()).trim();
                 sendCommandLine(saveCommand);
                 commandLine.setText(" ");
            }
        });
        commandLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                sendBtn.setEnabled(true);
            }
        });
        sendList.add(commandLine);
        sendList.add(new JLabel(" "));
        sendBtn = getSendButton();
        sendList.add(sendBtn);
        sendList.add(new JLabel(" "));
        double[] fiveWt = { 0, 0, 0, 0, 1 };
        commandPanel = GuiUtils.doLayout(sendList, 5, fiveWt,
                                              GuiUtils.WT_N);
        comps.add(GuiUtils.left(commandPanel));
        comps.add(new JLabel(" "));
        comps.add(new JLabel(" "));
    }

     protected JButton getSendButton() {
         sendBtn = new JButton("Send");
         sendBtn.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 String line = (commandLine.getText()).trim();
                 sendCommandLine(line);
             }
         });
         sendBtn.setEnabled(false);
         return sendBtn;
     }

     private void sendHost() {
         //System.out.println("sendHost");
         hostString = (hostLine.getText()).trim();
         addSource();
     }

     private void sendPort() {
         //System.out.println("sendPort");
         addSource();
     }

     private void sendKey() {
         //System.out.println("sendKey");
         addSource();
     }

     private void sendCommandLine(String commandLine) {
         commandLine = commandLine.toUpperCase();
         String commandName = null;
         StringTokenizer tok = new StringTokenizer(commandLine, " ");
         while (tok.hasMoreElements()) {
             commandName = tok.nextToken();
         }
         if (commandName == null) return;
         textArea.appendLine(commandLine);
         commandLine = commandLine.trim();
         commandLine = commandLine.replaceAll(" ", "+");
         String newRequest = request.concat(commandLine);

         URL url;
         try
         {
           url = new URL(newRequest);
           urlc = url.openConnection();
           InputStream is = urlc.getInputStream();
           inputStream =
             new DataInputStream(
               new BufferedInputStream(is));
         }
         catch (Exception e)
         {
           System.out.println("sendCommandLine exception e=" + e);
           return;
         }
         String responseType = null;
         String lineOut = null;
         try {
             lineOut = inputStream.readLine();
             lineOut = inputStream.readLine();
         } catch (Exception e) {
             System.out.println("readLine exception=" + e);
             return;
         }
         while (lineOut != null) {
             tok = new StringTokenizer(lineOut, " ");
             responseType = tok.nextToken();
             if (responseType.equals("U")) {
                 System.out.println(lineOut);
                 frameNumber = getFrameDir(lineOut.substring(2));
                 if (frameNumber > 0) {
                     System.out.println("Updating frame=" + frameNumber);
                     getFrameData();
                 }
             } else if (responseType.equals("V")) {
                 System.out.println(lineOut);
             } else if (responseType.equals("H")) {
                 System.out.println(lineOut);
             } else if (responseType.equals("K")) {
                 System.out.println(lineOut);
             } else if (responseType.equals("T") || responseType.equals("C") ||
                        responseType.equals("M") || responseType.equals("S") ||
                        responseType.equals("R")) { 
                 textArea.appendLine(lineOut.substring(6));
             }
             try {
                 lineOut = inputStream.readLine();
             } catch (Exception e) {
                 System.out.println("readLine exception=" + e);
                 return;
             }
         }
     }

    private void getFrameData() {
        String updateFrameRequest = "http://" + hostString + ":" + portString + "/?sessionkey=" + keyString +
            "&version=1&frame=0&x=0&y=0&type=I&text=" + frameNumber;
        System.out.println(updateFrameRequest);
        URL imageUrl;
        URLConnection imageUrlc;
        DataInputStream dis;
        try
        {
          imageUrl = new URL(updateFrameRequest);
          imageUrlc = imageUrl.openConnection();
          InputStream is = imageUrlc.getInputStream();
          dis = new DataInputStream( new BufferedInputStream(is));
        }
        catch (Exception e)
        {
          System.out.println("getFrameData exception e=" + e);
          return;
        }
        int len = 0;
        try {
            len = dis.available();
            System.out.println("    len=" + len);
        } catch (Exception e) {
          System.out.println("getFrameDir: I/O error getting file size");
          return;
        }
        System.out.println("getFrameData:");
        try
        {
            System.out.println("    " + dis.readChar());
        }
        catch (Exception e)
        {
            System.out.println("    readChar exception  e=" + e);
        }
    }

    private int getFrameDir(String lineOut) {
        Integer frameInt = new Integer(lineOut.substring(0,3));
        int frame = frameInt.intValue();
        int index = lineOut.indexOf("I") + 1;
        int frameFlag = (new Integer(lineOut.substring(index, index+1))).intValue();
        if (frameFlag == 0) return -1;
        //System.out.println("Update frame=" + frame);
        String updateFrameRequest = "http://" + hostString + ":" + portString + "/?sessionkey=" + keyString +
            "&version=1&frame=0&x=0&y=0&type=B&text=" + frameInt.toString();
        //System.out.println(updateFrameRequest);
        URL imageUrl;
        URLConnection imageUrlc;
        DataInputStream dis;
        try
        {
          imageUrl = new URL(updateFrameRequest);
          imageUrlc = imageUrl.openConnection();
          InputStream is = imageUrlc.getInputStream();
          dis = new DataInputStream( new BufferedInputStream(is));
        }
        catch (Exception e) 
        {
          System.out.println("getFrameDir exception e=" + e);
          return -1;
        }
        try
        {
           int len = 0;
           try {
               len = dis.available()/4;
               //System.out.println("    len=" + len);
           } catch (Exception e) {
             System.out.println("getFrameDir: I/O error getting file size");
             return -1;
           }
           if (len >= dirLength) {
               frmDir = new int[dirLength];
               len -= dirLength;
           }
           if (len >= navLength) {
               navBlock = new int[navLength];
               len -= navLength;
           }
           System.out.println("    length of frmDir = " + frmDir.length);
           System.out.println("    length of navBlock = " + navBlock.length);
           System.out.println("    " + len + " words remaining");
           for (int i=0; i<frmDir.length; i++)  {
               frmDir[i] = dis.readInt();
           }
           byte navType[] = { 0, 0, 0, 0 };
           for (int i=0; i<4; i++) {
               navType[i] = dis.readByte();
           }
           for (int i=1; i<navBlock.length; i++)  {
               navBlock[i] = dis.readInt();
           }
           McIDASUtil.flip(frmDir,0,frmDir.length-1);
           McIDASUtil.flip(navBlock,0,navBlock.length-1);
           System.out.println("    sensor=" + frmDir[0] + " day=" + frmDir[1] +
                              " time=" + frmDir[2]);
           System.out.println("    nav type = " + new String(navType, 0, 4));
        }
        catch (Exception e)
        {
            System.out.println("getFrameDir exception e=" + e);
        }
        return frame;
    }


    /**
     * Get one of the selected frames.
     *
     * @return One of the selected frames.
     */
    protected FrameDirectory getASelectedFrame() {
        if (haveFrameSelected()) {
            McIDASXFrameDescriptor fd =
                (McIDASXFrameDescriptor) getTimesList().getSelectedValue();
            if (fd != null) {
                return fd.getDirectory();
            }
        }
        return null;
    }


    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        boolean descriptorState = (canReadFrames());

        boolean timesOk = timesOk();
        getTimesList().setEnabled(getDoFrameLoop() && descriptorState);
    }

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
                  System.out.println("McCommandLineChooser: loadFrames  nF=" + nF);
                  return;
                }

                for (int i=0; i<nF; i++) {
                  if (fsi.getFrameDirectory(i+1) < 0) {
                    System.out.println("McCommandLineChooser: loadFrames  unable to get frame directory");
                    return;
                  }
                  int[] frmdir = fsi.myFrmdir;
                  FrameDirectory fd = new FrameDirectory(frmdir);
                  if (fd.sensorNumber != -1) {
                    numFrames++;
//                    String fdir=fd.toString();
                    McIDASXFrameDescriptor fdir = new McIDASXFrameDescriptor(fd, i+1);
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
        if ( !timesOk()) {
            return null;
        }
        if (getDoFrameLoop()) {
            List frames = new ArrayList();
            Object[] selectedTimes = getTimesList().getSelectedValues();
            for (int i = 0; i < selectedTimes.length; i++) {
              McIDASXFrameDescriptor fd = new McIDASXFrameDescriptor(
                                      (McIDASXFrameDescriptor) selectedTimes[i]);
              frames.add(fd);
            }
            return frames;
        }
        return null;
    }


    /**
     * Method to do the work of loading the data
     */
    public void doLoad() {
        try {
            if ( !getGoodToGo()) {
                updateStatus();
                return;
            }
            updateStatus();
            request = request.concat(hostString + ":" + portString + "/?sessionkey=" + keyString +
                "&version=1&frame=0&x=0&y=0&type=T&text=");
            //System.out.println(request);
            commandLineLabel.setEnabled(true);
            commandLine.setEnabled(true);
            //firePropertyChange(NEW_SELECTION, null, getFrameList());
        } catch (Exception exc) {
            logException("doLoad", exc);
        }
    }

}
