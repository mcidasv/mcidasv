package ucar.unidata.data.imagery;

import edu.wisc.ssec.mcidas.McIDASUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Date;

import visad.*;

import java.rmi.RemoteException;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.Poller;
import ucar.unidata.data.*;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ImageSequenceControl;

import visad.Set;
import visad.java3d.*;
import visad.util.*;
import visad.data.mcidas.*;
import visad.meteorology.*;

import java.io.IOException;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.print.*;
import javax.swing.*;
import java.util.*;
import java.io.*;


/**
 * Used to cache  a data choice and its data
 *
 * @author IDV development team
 * @version $Revision$
 */


public class McIDASDataSource extends DataSourceImpl  {

    /**
     *  If we are polling on the McIDAS-X dirty flag we keep the Poller around so
     *  on a doRemove we can tell the poller to stop running.
     */
   private List pollers;

    /** Holds information for polling */
   private PollingInfo pollingInfo;

    /** Holds frame component information for polling */
   private FrameComponentInfo frameComponentInfo;

    /** Has the initPolling been called yet */
   private boolean haveInitedPolling = false;

   private FrmsubsImpl fsi = new FrmsubsImpl();

    /**
     * Default bean constructor; does nothing
     */
    public McIDASDataSource() {}


    /**
     * Create a McIDASDataSource
     *
     *
     * @param descriptor the datasource descriptor
     * @param name my name
     * @param properties my properties
     */
    public McIDASDataSource(DataSourceDescriptor descriptor, String name,
                            Hashtable properties) {
        //Name and description
        super(descriptor, "McIDAS data", "McIDAS data", properties);
    }



    /**
     * This is called after  this datasource has been fully created
     * and initialized after being unpersisted by the XmlEncoder.
     */
    public void initAfterUnpersistence() {
        initConnection();
    }


    /**
     * Gets called after creation. Initialize the connection
     */
    public void initAfterCreation() {
        initConnection();
        // initPolling();
    }



    /**
     * Initialize the connection to McIDAS.
     * This gets called when the data source is newly created
     * or decoded form a bundle.
     */
    private void initConnection() {
       int istat = fsi.getSharedMemory();
        //If something bad happens then call:
        //        setInError(true,"The error message");
       if (istat < 0)
         setInError(true,"Unable to attach McIDAS-X shared memory");
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

    protected Data getDataInner(DataChoice dataChoice, DataCategory category, DataSelection dataSelection, Hashtable requestProperties)
            throws VisADException, RemoteException {
        Object    id   = dataChoice.getId();

        //start up polling if we have not done so already.
        initPolling();

        //This is where you want to create the FieldImpl.
        Data data = (Data) getMcIdasFrame();

        //Normally the DataSourceImpl base class will cache it.
        //If you want to never have the base class cache it then 
        //overwrite the method:
        //protected boolean shouldCache(Data data) {return null;}

        //When you want to notify the displays of new data then call:
        //reloadData();

        return data;
    }


    protected boolean shouldCache(Data data) {return false;}


    /**
     * Popup the polling properties dialog.
     */
    private void showPollingPropertiesDialog() {
        pollingInfo = initPollingInfo();
        //System.out.println("from showPollingPropertiesDialog: Polling interval = " + pollingInfo.getInterval());

        JTextField intervalFld = new JTextField("" + pollingInfo.getInterval()
                                                / 1000.0 / 60.0, 5);
        JCheckBox pollingCbx = new JCheckBox("", isPolling());
        JCheckBox hiddenCbx  = new JCheckBox("", pollingInfo.getIsHiddenOk());
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Poll Interval:"),
            GuiUtils.centerRight(intervalFld, GuiUtils.lLabel(" minutes")),
            GuiUtils.rLabel("Currently Polling:"), pollingCbx,
            GuiUtils.rLabel("Include Hidden Files"), hiddenCbx
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        if ( !GuiUtils.showOkCancelDialog(null, "Polling Properties",
                                          contents, null)) {
            return;
        }

        boolean needToRestart = false;
        long newInterval =
            (long) (60 * 1000
                    * new Double(intervalFld.getText()).doubleValue());
        boolean newRunning = pollingCbx.isSelected();
        boolean hiddenOk   = hiddenCbx.isSelected();
        if (pollingInfo.getInterval() != newInterval) {
            needToRestart = true;
            pollingInfo.setInterval(newInterval);
        }
        if (pollingInfo.getIsHiddenOk() != hiddenOk) {
            needToRestart = true;
            pollingInfo.setIsHiddenOk(hiddenOk);
        }

        if (newRunning != isPolling()) {
            needToRestart = true;
        }
    
        if (needToRestart) {
            stopPolling();
            if (newRunning) {
                startPolling(); 
            }
        }

    }

    /**
     * Popup the select frame componentsdialog.
     */
    private void selectFrameComponentsDialog() {
        frameComponentInfo = initFrameComponentInfo();

        JCheckBox imageCbx = new JCheckBox("", frameComponentInfo.getIsImage());
        JCheckBox graphicsCbx = new JCheckBox("", frameComponentInfo.getIsGraphics());
        JCheckBox colorTableCbx = new JCheckBox("", frameComponentInfo.getIsColorTable());

        JPanel contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("      Image Data      "), imageCbx,
            GuiUtils.rLabel("      Graphics Data      "), graphicsCbx,
            GuiUtils.rLabel("      Color Table Data      "), colorTableCbx
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        if ( !GuiUtils.showOkCancelDialog(null, "Components",
                                          contents, null)) {
            return;
        }

        boolean needToRestart = false;
        boolean isImage = imageCbx.isSelected();
        boolean isGraphics = graphicsCbx.isSelected();
        boolean isColorTable = colorTableCbx.isSelected();
        if (frameComponentInfo.getIsImage() != isImage) {
            needToRestart = true;
            frameComponentInfo.setIsImage(isImage);
        }
        if (frameComponentInfo.getIsGraphics() != isGraphics) {
            needToRestart = true;
            frameComponentInfo.setIsGraphics(isGraphics);
        }
        if (frameComponentInfo.getIsColorTable() != isColorTable) {
            needToRestart = true;
            frameComponentInfo.setIsColorTable(isColorTable);
        }

        if (needToRestart) {
            stopPolling();
            reloadData();
            if (initPollingInfo().getIsActive()) {
                startPolling();
            }
        }

    }

    /**
     * See if this data source can poll
     *
     * @return true if can poll
     */
    public boolean canPoll() {
        return pollingInfo != null;
    }

    /**
     * Creates, if needed, and returns the pollingInfo member.
     *
     * @return The pollingInfo
     */
    private PollingInfo initPollingInfo() {
        if (pollingInfo == null) {
            pollingInfo = new PollingInfo((String)null, (long)500, (String)null, true, false);
        }
        return pollingInfo;
    }

    /**
     * Creates, if needed, and returns the frameComponentInfo member.
     *
     * @return The frameComponentInfo
     */
    private FrameComponentInfo initFrameComponentInfo() {
        if (frameComponentInfo == null) {
            frameComponentInfo = new FrameComponentInfo(true, true, true);
        }
        return frameComponentInfo;
    }

    /**
     * Poll the McIDAS-X dirty flag.
     *
     * @param milliSeconds Poll interval in milliseconds
     */
    private void startPolling() {
      //initPollingInfo().setIsActive(true);
      //initPollingInfo().setInterval(500);
      McIDASPoller mcidasPoller = new McIDASPoller(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           reloadData();
         }
      }, pollingInfo);
      addPoller(mcidasPoller);
    }

    /**
     * Stop polling
     */
    private void stopPolling() {
        initPollingInfo().setIsActive(false);
        if (pollers != null) {
            for (int i = 0; i < pollers.size(); i++) {
                ((Poller) pollers.get(i)).stopRunning();
            }
            pollers = null;
        }
    }

    /**
     * Called after created or unpersisted to start up polling if need be.
     */
    private void initPolling() {
        if (haveInitedPolling) {
            return;
        }
        haveInitedPolling = true;
        pollingInfo = initPollingInfo();

        if (pollingInfo.getIsActive()) {
            //System.out.println("startPolling");
            startPolling();
        }
    }

    /**
     * Get any {@link Action}-s associated with this DataSource.  The actions
     * can be used to create menus, buttons, etc.  Subclasses should implement
     * this method making sure to call super.getActions()
     *
     * @param actions List of actions
     */
    protected void addActions(List actions) {
        AbstractAction a = null;
        a = new AbstractAction("Reload Data") {
            public void actionPerformed(ActionEvent ae) {
                Misc.run(new Runnable() {
                    public void run() {
                        reloadData();
                    }
                });
            }
        };
        actions.add(a);

        if (canPoll()) {
            if (isPolling()) {
                a = new AbstractAction("Turn Off Polling") {
                    public void actionPerformed(ActionEvent ae) {
                        stopPolling(); 
                    }
                };
            } else {
                a = new AbstractAction("Turn On Polling") {
                    public void actionPerformed(ActionEvent ae) {
                        startPolling();
                    }
                };

            }
            actions.add(a);

            a = new AbstractAction("Set Polling Properties...") {
                public void actionPerformed(ActionEvent ae) {
                    showPollingPropertiesDialog();
                }
            };
            actions.add(a);

            a = new AbstractAction("Select Frame Components...") {
                public void actionPerformed(ActionEvent ae) {
                    selectFrameComponentsDialog();
                }
            };
            actions.add(a);
        }
    }

    /**
     * Get the location where we poll.
     * 
     * @return Directory to poll on.
     */
    protected File getLocationForPolling() {
      return null;
    }


    /**
     * Get the DataChoices.
     *
     * @return  List of DataChoices.
     */
    public List getDataChoices() {
        //The false says that this is not a "display category"., i.e., it is not
        //used in the tree view or other areas of the GUI
        //The IMAGE-2D-TIME is the category used for image displays
        List categories = DataCategory.parseCategories("IMAGE-2D;",
                              false);
        List choices = new ArrayList();

        //Just add one. If you wanted a set of different ones then keep adding
        //new ones. The "some id" is an object which identifies the data choice
        DirectDataChoice choice = new DirectDataChoice(this, "some id",
                                      "McIDAS data",
                                      "McIDAS data description", categories,
                                      (Hashtable) null);

        choices.add(choice);
        return choices;
    }

   public SingleBandedImage getMcIdasFrame()
          throws VisADException, RemoteException {
                                                                                              
     FlatField image_data = null;
     SingleBandedImage field = null;
                                                                                              
     int[] curfrm_a  = new int[] {0};
     int[] linsize_a = new int[] {0};
     int[] elesize_a = new int[] {0};
                                                                                              
     
     curfrm_a[0] = -1;           
     int ret = fsi.getFrameSize(curfrm_a, linsize_a, elesize_a);
     if (ret >= 0) {
       int cfrm = fsi.getCurrentFrame();
       int lines = linsize_a[0];
       int elems = elesize_a[0];
       // System.out.println("current frame: "+cfrm+"\n"+"linsize: "+lines+"\n"+"elesize: "+elems);
                                                                                              
       byte[] img =  new byte[(lines)*elems];
       int[] stab = new int[256];
       int[] ctab = new int[256];
       int[] gtab = new int[256];
       double[][] values = new double[1][lines*elems];
                                                                                              
       int fret;
       if ((fret = fsi.getFrameData(cfrm, lines, elems, img, stab, ctab, gtab)) < 0 ) {
         System.out.println("problem in getFrameData: "+fret);
       }
       else {
         // System.out.println("Frame data acquired");
                                                                                              
         if (initFrameComponentInfo().getIsImage()) {
           for (int i=0; i<lines-12; i++) {
             for (int j=0; j<elems; j++) {
               values[0][i*elems + j] = (double)img[(lines-i-1)*elems + j];
               if (values[0][i*elems + j] < 0.0 ) values[0][i*elems + j] += 256.0;
             }
           }
         }

    // color table
         if (initFrameComponentInfo().getIsColorTable()) {
           // System.out.println("Setting up color table...");
           float[][] enhTable = new float[3][256];
           for (int i=1; i<18; i++) {
             enhTable[0][i] = (float)((ctab[i]/0x10000)&0xff);
             enhTable[1][i] = (float)((ctab[i]/0x100)&0xff);
             enhTable[2][i] = (float)(ctab[i]&0xff);
             // System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
             //                      + " blue=" + enhTable[2][i]);
           }
           for (int i=18; i<256; i++) {
             enhTable[0][i] = (float)((ctab[stab[i]]/0x10000)&0xff);
             enhTable[1][i] = (float)((ctab[stab[i]]/0x100)&0xff);
             enhTable[2][i] = (float)(ctab[stab[i]]&0xff);
             // System.out.println(i + ": red=" + enhTable[0][i] + " green=" + enhTable[1][i]
             //                      + " blue=" + enhTable[2][i]);
           }
           for (int i=0; i<256; i++) {
             enhTable[0][i] /= 0xff;
             enhTable[1][i] /= 0xff;
             enhTable[2][i] /= 0xff;
           }
           ColorTable mcidasXColorTable = new ColorTable("MCIDAS-X",ColorTable.CATEGORY_BASIC,enhTable);

           DataContext dataContext = getDataContext();
           ColorTableManager colorTableManager = ((IntegratedDataViewer)dataContext).getColorTableManager();
           colorTableManager.addUsers(mcidasXColorTable);
           List dcl = ((IntegratedDataViewer)dataContext).getDisplayControls();

           for (int i=0; i<dcl.size(); i++) {
             DisplayControlImpl dc = (DisplayControlImpl)dcl.get(i);
             if (dc instanceof ImageSequenceControl) {
               dc.setColorTable("default", mcidasXColorTable);
               break;
             }
           }
         }
       
         if (initFrameComponentInfo().getIsGraphics()) {
           int[] npts_a = new int[] {0};
           int[] nblocks_a = new int[] {0};
           int[] mask_a = new int[] {0};
           int gret;
           // System.out.println("Calling fsi.getGraphicsSize");
           if ((gret = fsi.getGraphicsSize(cfrm, npts_a, nblocks_a, mask_a)) < 0) {
             System.out.println("problem in getGraphicsSize: "+gret);
           }
           else {
             int npts = npts_a[0];
             int nblocks = nblocks_a[0];
             int mask = mask_a[0];
             // System.out.println("npts=" + npts + " nblocks=" + nblocks + " mask=" + mask);
                                                                                              
             int[] gra = new int[npts];
             int graret;
             if ((graret = fsi.getGraphics(cfrm, npts, gra)) < 0) {
               System.out.println("problem in getGraphicsSize: "+graret);
             }
             else {
               int[] color_pts = new int[npts];
               int[][] loc_pts = new int[2][npts];
               int loc,lin;
               int gpts = 0;
               for (int i=0; i<npts; i++) {
                   loc = gra[i]/0x100;
                   lin = (loc-1)/elems;
                   if (lin >= 12) {
                     loc_pts[0][gpts] = lin;
                     loc_pts[1][gpts] = (loc-1) % elems;
                     color_pts[gpts] = gra[i]&0x000000ff;
                     gpts++;
                 }
               }
               // System.out.println("Number of graphics points = " + gpts);
               double dlin;
               for (int i=0; i<gpts; i++) {
                 if (color_pts[i] > 0) {
                   lin  = lines - 1 -loc_pts[0][i];
                   values[0][lin*elems + loc_pts[1][i]] = (double)color_pts[i];
                 }
               }
             }
           }
         }

         int dret;
         int[] frmdir = new int[704];
         if ((dret = fsi.getFrameDirectory(cfrm, frmdir)) < 0 ) {
           System.out.println("problem in getFrameDirectory: "+dret);
         }
         else {
           //System.out.println("Number of frames = " + fsi.getNumberOfFrames());
           FrameDirectory fd = new FrameDirectory(frmdir);
           //System.out.println(fd);
           //Date nominal_time = new Date(1000*McIDASUtil.mcDayTimeToSecs(fd.cyd,fd.hms));
           Date nominal_time = fd.getNominalTime();
                                                                                          
  // fake an area directory
           int[] adir = new int[64];
           adir[5] = fd.uLLine;
           adir[6] = fd.uLEle;
           // System.out.println("UL line = " + adir[5] + "  element = " + adir[6]);
           adir[8] = lines;
           adir[9] = elems;
           adir[11] = fd.lineRes;
           adir[12] = fd.eleRes;
                                                                                          
           AREACoordinateSystem cs = new AREACoordinateSystem( adir, fd.nav);
                                                                                          
           double[][] linele = new double[2][4];
           double[][] latlon = new double[2][4];
           // LR
           linele[0][0] = (double)(elems-1);
           linele[1][0] = 0.0;
           // UL
           linele[0][1] = 0.0;
           linele[1][1] = (double)(lines-1);
           // LL
           linele[0][2] = 0.0;
           linele[1][2] = 0.0;
           // UR
           linele[0][3] = (double)(elems-1);
           linele[1][3] = (double)(lines-1);
                                                                                              
           latlon = cs.toReference(linele);
           // System.out.println("LR: " +  latlon[0][0] + " " + latlon[1][0]);
           // System.out.println("UL: " +  latlon[0][1] + " " + latlon[1][1]);
           // System.out.println("LL: " +  latlon[0][2] + " " + latlon[1][2]);
           // System.out.println("UR: " +  latlon[0][3] + " " + latlon[1][3]);
                                                                                              
           RealType[] domain_components = {RealType.getRealType("ImageElement", null, null),
                  RealType.getRealType("ImageLine", null, null)};
           RealTupleType image_domain =
                       new RealTupleType(domain_components, cs, null);
                                                                                              
//  Image numbering is usually the first line is at the "top"
//  whereas in VisAD, it is at the bottom.  So define the
//  domain set of the FlatField to map the Y axis accordingly
 
           Linear2DSet domain_set = new Linear2DSet(image_domain,
                                       0, (elems - 1), elems,
                                       (lines - 1), 0, lines );
           RealType range = RealType.getRealType("brightness");
                                                                                              
           FunctionType image_func = new FunctionType(image_domain, range);
                                                                                              
// now, define the Data objects
           image_data = new FlatField(image_func, domain_set);
           DateTime date = new DateTime(nominal_time);
           image_data = new NavigatedImage(image_data, date, "McIDAS Image");
                                                                                              
// put the data values into the FlatField image_data
           image_data.setSamples(values);
           field = (SingleBandedImage) image_data;
         }
       }
     }
     return field;
   }
}

