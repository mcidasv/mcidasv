package ucar.unidata.ui.imagery.mcidas;

import ucar.unidata.data.imagery.mcidas.FrameDirectory;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;

import edu.wisc.ssec.mcidas.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
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
import java.util.Vector;

import ucar.unidata.idv.chooser.IdvChooser;

import ucar.unidata.ui.ChooserPanel;

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
    private JTextField keyLine;
    private JTextField encodingLine;
    private JLabel commandLineLabel;
    private JTextField commandLine;
    private JPanel commandPanel;
    private JButton sendBtn;
    private JTextArea textArea;
    private String keyString = null;
    private String encodingString = null;
    private boolean goodToGo = false;
 
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
        List allComps = new ArrayList();
        getComponents(allComps);
        JPanel imagePanel = GuiUtils.doLayout(allComps, 1, GuiUtils.WT_N,
                                              GuiUtils.WT_N);
        List textComps = new ArrayList();
        textArea = new JTextArea("", 20, 60);
        textComps.add(textArea);
        textComps.add(new JLabel(" "));
        textComps.add(new JLabel(" "));
        JPanel textPanel = GuiUtils.doLayout(textComps, 2, GuiUtils.WT_NN,
                                              GuiUtils.WT_N);
        return GuiUtils.topCenterBottom(imagePanel, textPanel, getDefaultButtons(this));
    }

    private void addSource() {
        if (keyString != null) {
            if (encodingString != null) {
                goodToGo = true;
                doLoad();
                commandLineLabel.setEnabled(true);
                commandLine.setEnabled(true);
            }
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
        JLabel keyLabel = GuiUtils.rLabel("Key: ");
        firstLine.add(keyLabel);
        keyLine = new JTextField("", 10);
        keyLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 keyString = (keyLine.getText()).trim();
                 sendKeyLine(keyString);
                 addSource();
            }
        });
        keyLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 keyString = (keyLine.getText()).trim();
                 sendKeyLine(keyString);
                 addSource();
            }
        });
        keyLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(keyLine);
        firstLine.add(new JLabel("  "));

        JLabel encodingLabel = GuiUtils.rLabel("Encoding: ");
        firstLine.add(encodingLabel);
        encodingLine = new JTextField("", 20);
        encodingLine.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                 encodingString = (encodingLine.getText()).trim();
                 sendEncodingLine(encodingString);
                 addSource();
            }
        });
        encodingLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 encodingString = (encodingLine.getText()).trim();
                 sendEncodingLine(encodingString);
                 addSource();
            }
        });
        encodingLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
            }
        });
        firstLine.add(encodingLine);
        firstLine.add(new JLabel(" "));
        double[] sixWt = { 0, 0, 0, 0, 0, 1 };
        JPanel firstPanel = GuiUtils.doLayout(firstLine, 6, sixWt,
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
            }
        });
        commandLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 String saveCommand = (commandLine.getText()).trim();
                 sendCommandLine(saveCommand);
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

     private void sendKeyLine(String keyLine) {
         //textArea.setText(keyLine);
     }

     private void sendEncodingLine(String encodingLine) {
         //textArea.setText(encodingLine);
     }

     private void sendCommandLine(String commandLine) {
         textArea.setText(commandLine);
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
            //firePropertyChange(NEW_SELECTION, null, getFrameList());
        } catch (Exception exc) {
            logException("doLoad", exc);
        }
    }

}
