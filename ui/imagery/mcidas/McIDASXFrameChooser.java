package ucar.unidata.ui.imagery.mcidas;

import ucar.unidata.data.imagery.mcidas.FrameDirectory;
/* ???
import ucar.unidata.data.imagery.mcidas.FrmsubsImpl;
*/
import ucar.unidata.data.imagery.mcidas.FrmsubsMM;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;

import edu.wisc.ssec.mcidas.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

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
public class McIDASXFrameChooser extends FrameChooser {


    /** List of descriptors */
//    private PreferenceList descList;

    /** Holds the properties */
    private JPanel propPanel;

    /** Holds the mapping of id to xml Element */
    private Hashtable properties;

    /**
     * List of JComponent-s that depend on a descriptor being selected
     * to be enabled
     */
    protected ArrayList compsThatNeedDescriptor = new ArrayList();

    /** A widget for the list of dataset descriptors */
    protected JComboBox descriptorComboBox;

    /** Flag to keep from infinite looping */
    private boolean ignoreDescriptorChange = false;

    /** The descriptor names */
    protected String[] descriptorNames;

    /** Descriptor/name hashtable */
    protected Hashtable descriptorTable;

    /**
     *  The list of currently loaded frame Descriptor-s
     */
    private Vector frameDescriptors;


    /**
     *  Keep track of which image load we are on.
     */
    private int currentFrame = 0;

/* ???
    private FrmsubsImpl fsi = new FrmsubsImpl();
*/
    //private FrmsubsMM fsi = new FrmsubsMM();
   private static FrmsubsMM fsi;
   static {
     try {
       fsi = new FrmsubsMM();
       fsi.getMemoryMappedUC();
     } catch (Exception e) { }
   }

    /**
     * Construct an Adde image selection widget
     *
     * @param imageDefaults The xml resources for the image defaults
     * @param descList Holds the preferences for the image descriptors
     * @param serverList Holds the preferences for the adde servers
     */
    public McIDASXFrameChooser(PreferenceList descList) {
        super();
//        this.descList      = descList;
        setLayout(new BorderLayout(5, 5));
        this.add(doMakeContents(), BorderLayout.CENTER);
        this.add(getDefaultButtons(), BorderLayout.SOUTH);
        updateStatus();
    }



    /**
     * Update labels, enable widgets, etc.
     */
    protected void updateStatus() {
        super.updateStatus();
        enableWidgets();

//            setDescriptors(null);
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
        return getSelectedDescriptor();
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
     * Respond to a change in the descriptor list.
     */
    protected void descriptorChanged() {
        readFrames();
        updateStatus();
    }


    /**
     * Check if a descriptor (image type) has been chosen
     *
     * @return  true if an image type has been chosen
     */
    protected boolean haveDescriptorSelected() {
        if ( !GuiUtils.anySelected(descriptorComboBox)) {
            return false;
        }
        return (getDescriptor() != null);
    }


    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate()  {
      descriptorChanged();
    }


    /**
     * Make the UI for this selector.
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        List allComps = new ArrayList();
        getComponents(allComps);
        JPanel imagePanel = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NN,
                                              GuiUtils.WT_N);
        return GuiUtils.centerBottom(GuiUtils.wrap(imagePanel),
                                     getBottomComponent());
    }


    /**
     * Create the bottom advanced gui panel
     *
     * @return The bottom panel
     */
    protected JComponent getBottomComponent() {
        List     bottomComps     = new ArrayList();
//        Insets   dfltGridSpacing = new Insets(4, 0, 4, 0);
//        String   dfltLblSpacing  = " ";
        propPanel = GuiUtils.doLayout(bottomComps, 3, GuiUtils.WT_N,
                                      GuiUtils.WT_N);
        return propPanel;
    }


    /**
     * Make the components (label/widget) and return them
     *
     *
     * @param comps The list to add components to
     */
    protected void getComponents(List comps) {
        descriptorComboBox = new JComboBox();
        descriptorComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreDescriptorChange
                        && (e.getStateChange() == e.SELECTED)) {
                    descriptorChanged();
                }
            }
        });

        JPanel framesListPanel = makeFramesPanel();
        getTimesList().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !getDoFrameLoop()) {
                    return;
                }
                getASelectedFrame();
            }
        });
        comps.add(GuiUtils.left(framesListPanel));
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

        for (int i = 0; i < compsThatNeedDescriptor.size(); i++) {
            JComponent comp = (JComponent) compsThatNeedDescriptor.get(i);
            GuiUtils.enableTree(comp, descriptorState);
        }

        boolean timesOk = timesOk();
        getTimesList().setEnabled(getDoFrameLoop() && descriptorState);
    }

    /**
     * Get the selected descriptor.
     *
     * @return  the currently selected descriptor.
     */
    protected String getDescriptor() {
        return getDescriptorFromSelection(getSelectedDescriptor());
    }

    /**
     * Get the descriptor relating to the selection.
     *
     * @param selection   String name from the widget
     *
     * @return  the descriptor
     */
    protected String getDescriptorFromSelection(String selection) {
        if (descriptorTable == null) {
            return null;
        }
        if (selection == null) {
            return null;
        }
        return (String) descriptorTable.get(selection);
    }

    /**
     * Get the selected descriptor.
     *
     * @return the selected descriptor
     */
    protected String getSelectedDescriptor() {
        String selection = (String) descriptorComboBox.getSelectedItem();
        if (selection == null) {
            return null;
        }
        return selection;
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
                    resetDescriptorBox();
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
                frameDescriptors = new Vector();
                int numFrames = 0;
                int[] frmdir = new int[704];
                int nF = fsi.getNumberOfFrames();
                if (nF < 1) {
                  System.out.println("McIDASXFrameChooser: loadFrames  nF=" + nF);
                  return;
                }

                for (int i=0; i<nF; i++) {
                  if (fsi.getFrameDirectory(i+1, frmdir) < 0) {
                    System.out.println("McIDASXFrameChooser: loadFrames  unable to get frame directory");
                    return;
                  }
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
                getTimesList().setSelectedIndex(numFrames - 1);
                getTimesList().ensureIndexIsVisible(
                    getTimesList().getSelectedIndex());
                revalidate();
                readDescriptors();
            }
    }

    /**
     * Reset the descriptor stuff
     */
    private void resetDescriptorBox() {
        ignoreDescriptorChange = true;
        ignoreDescriptorChange = false;
    }


    /**
     * Does this selector have all of its state set to load in data
     *
     * @return Has the user chosen everything they need to choose to load data
     */
    protected boolean getGoodToGo() {
        if (getDoFrameLoop()) {
//            return haveASeries();
//        } else {
            return canReadFrames();
        }
        //return false;
        return true;
    }





    /**
     *  Generate a list of image descriptors for the descriptor list.
     */
    private void readDescriptors() {
      String[]    names = new String[frameDescriptors.size()];
      McIDASXFrameDescriptor fd = new McIDASXFrameDescriptor();
      for (int i = 0; i<frameDescriptors.size(); i++) {
          fd = (McIDASXFrameDescriptor)frameDescriptors.get(i);
          names[i] = fd.toString();
      }
//      Arrays.sort(names);
      setDescriptors(names);
    }


    /**
     * Initialize the descriptor list from a list of names
     *
     * @param names  list of names
     */
    protected void setDescriptors(String[] names) {
        synchronized (WIDGET_MUTEX) {
            ignoreDescriptorChange = true;
            descriptorComboBox.removeAllItems();
            descriptorNames = names;
            if ((names == null) || (names.length == 0)) {
                return;
            }
            for (int j = 0; j < names.length; j++) {
                descriptorComboBox.addItem(names[j]);
            }
            ignoreDescriptorChange = false;
        }
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
            firePropertyChange(NEW_SELECTION, null, getFrameList());
        } catch (Exception exc) {
            logException("doLoad", exc);
        }
    }

}
