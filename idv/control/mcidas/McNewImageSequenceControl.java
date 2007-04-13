package ucar.unidata.idv.control.mcidas;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Class;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.JCheckBox;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.imagery.mcidas.ConduitInfo;
import ucar.unidata.data.imagery.mcidas.McNewDataSource;
import ucar.unidata.data.imagery.mcidas.McNewDataSource.FrameDataInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;
import ucar.unidata.data.imagery.mcidas.McIDASFrameDescriptor;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.control.WrapperWidget;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.TextHistoryPane;

import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.ui.colortable.ColorTableManager;

import visad.*;
import visad.georef.MapProjection;


/**
 * A DisplayControl for handling McIDAS-X image sequences
 */
public class McNewImageSequenceControl extends ImageSequenceControl {

    private JCheckBox imageCbx;
    private JCheckBox graphicsCbx;
    private JCheckBox colorTableCbx;

    private JLabel commandLineLabel;
    private JTextField commandLine;
    private JPanel commandPanel;
    private JButton sendBtn;
    private JTextArea textArea;
    private JPanel textWrapper;
    private String request;
    private URLConnection urlc;
    private DataInputStream inputStream;

    private int nlines, removeIncr,
                count = 0;

    private int ptSize = 12;

    private static DataChoice dc=null;
    private static Integer frmI;

    /** Holds frame component information */
    private FrameComponentInfo frameComponentInfo;
    List frameNumbers = new ArrayList();


    /**
     * Default ctor; sets the attribute flags
     */
    public McNewImageSequenceControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
        frameComponentInfo = initFrameComponentInfo();
    }

    /**
     * Override the base class method that creates request properties
     * and add in the appropriate frame component request parameters.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(McIDASComponents.IMAGE, new Boolean(frameComponentInfo.getIsImage()));
        props.put(McIDASComponents.GRAPHICS, new Boolean(frameComponentInfo.getIsGraphics()));
        props.put(McIDASComponents.COLORTABLE, new Boolean(frameComponentInfo.getIsColorTable()));
        return props;
    }

    /**
     * Get control widgets specific to this control.
     *
     * @param controlWidgets   list of control widgets from other places
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
        throws VisADException, RemoteException {

        super.getControlWidgets(controlWidgets);

        controlWidgets.add(
            new McIDASWrapperWidget(
                this, GuiUtils.rLabel("Frame components:"),
                doMakeImageBox(),doMakeGraphicsBox(),doMakeColorTableBox()));

        doMakeCommandField();
        getSendButton();
        JPanel commandLinePanel =
            GuiUtils.hflow(Misc.newList(commandLine, sendBtn), 2, 0);
        controlWidgets.add(
            new WrapperWidget( this, GuiUtils.rLabel("Command Line:"), commandLinePanel));

        final JTextField labelField = new JTextField("" , 20);

        ActionListener labelListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
              setNameFromUser(labelField.getText()); 
              updateLegendLabel();
            }
        };

        labelField.addActionListener(labelListener);
        JButton labelBtn = new JButton("Apply");
        labelBtn.addActionListener(labelListener);

        JPanel labelPanel =
            GuiUtils.hflow(Misc.newList(labelField, labelBtn),
                                         2, 0);

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Label:"), labelPanel));

        frmI = new Integer(0);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        McNewDataSource mds = null;
        List frameI = new ArrayList();
        for (int i=0; i<dss.size(); i++) {
          DataSourceImpl ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McNewDataSource) {
             frameNumbers.clear();
             mds = (McNewDataSource)ds;
             DataContext dataContext = mds.getDataContext();
             ColorTableManager colorTableManager = 
                 ((IntegratedDataViewer)dataContext).getColorTableManager();
             ColorTable ct = colorTableManager.getColorTable("MCIDAS-X");
             setColorTable(ct);
             request = mds.request;
             this.dc = getDataChoice();
             String choiceStr = this.dc.toString();
             if (choiceStr.equals("Frame Sequence")) {
                 frameI = mds.getFrameNumbers();
                 ArrayList frmAL = (ArrayList)(frameI.get(0));
                 for (int indx=0; indx<frmAL.size(); indx++) {
                     frmI = (Integer)(frmAL.get(indx));
                     frameNumbers.add(frmI);
                 }
             } else {
                 StringTokenizer tok = new StringTokenizer(this.dc.toString());
                 String str = tok.nextToken();
                 if (!str.equals("Frame")) {
                     frmI = new Integer(tok.nextToken());
                     frameNumbers.add(frmI);
                 } else {
                     frmI = new Integer(1);
                     frameNumbers.add(frmI);
                 }
             }
             break;
          }
       }
       setShowNoteText(true);
       noteTextArea.setRows(20);
       noteTextArea.setLineWrap(true);
       noteTextArea.setEditable(false);
       noteTextArea.setFont(new Font("Monospaced", Font.PLAIN, ptSize));
    }

    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */

    protected Component doMakeImageBox()
        throws VisADException, RemoteException {

        imageCbx = new JCheckBox("Image",frameComponentInfo.getIsImage());

        final boolean isImage = imageCbx.isSelected();
        imageCbx.setToolTipText("Set to import image data");
        imageCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsImage() != isImage) {
                 frameComponentInfo.setIsImage(isImage);
              } else {
                 frameComponentInfo.setIsImage(!isImage);
              }
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("image exception");
              }
           }
        });
        return imageCbx;
    }


    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */

    protected Component doMakeGraphicsBox() {
        graphicsCbx = new JCheckBox("Graphics", frameComponentInfo.getIsGraphics());

        final boolean isGraphics = graphicsCbx.isSelected();
        graphicsCbx.setToolTipText("Set to import graphics data");
        graphicsCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsGraphics() != isGraphics) {
                 frameComponentInfo.setIsGraphics(isGraphics);
              } else {
                 frameComponentInfo.setIsGraphics(!isGraphics);
              }
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("graphics exception");
              }
           }
        });
        return graphicsCbx;
    }


    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */

    protected Component doMakeColorTableBox() {
        colorTableCbx = new JCheckBox("ColorTable", frameComponentInfo.getIsColorTable());
        final boolean isColorTable = colorTableCbx.isSelected();
        colorTableCbx.setToolTipText("Set to import color table data");
        colorTableCbx.addItemListener(new ItemListener() {
           public void itemStateChanged(ItemEvent e) {
              if (frameComponentInfo.getIsColorTable() != isColorTable) {
                 frameComponentInfo.setIsColorTable(isColorTable);
              } else {
                 frameComponentInfo.setIsColorTable(!isColorTable);
              }
              getRequestProperties();
              try {
                resetData();
              } catch (Exception ex) {
                System.out.println(ex);
                System.out.println("colortable exception");
              }
           }
        });
        return colorTableCbx;
    }


    private void appendLine(String line) {
        if (count >= nlines) {
            try {
                int remove = Math.max(removeIncr, count - nlines);  // nlines may have changed
                int offset = noteTextArea.getLineEndOffset(remove);
                noteTextArea.replaceRange("", 0, offset);
            } catch (Exception e) {
                System.out.println("BUG in appendLine");       // shouldnt happen
            }
            count = nlines - removeIncr;
        }
        noteTextArea.append(line);
        noteTextArea.append("\n");
        count++;

        // scroll to end
        noteTextArea.setCaretPosition(noteTextArea.getText().length());
    }


    private JPanel doMakeTextArea() {
       textArea = new JTextArea(50, 30);
       textArea.setText("This is only a test.");
       JScrollPane sp =
           new JScrollPane(
               textArea,
               ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
               ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

       JViewport vp = sp.getViewport();
       vp.setViewSize(new Dimension(60, 30));
       textWrapper = GuiUtils.inset(sp, 4);
       return textWrapper;
    }
 


    private void doMakeCommandField() {
        commandLine = new JTextField("", 30);
        commandLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                 String saveCommand = (commandLine.getText()).trim();
                 commandLine.setText(" ");
                 sendCommandLine(saveCommand);
            }
        });
        commandLine.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                sendBtn.setEnabled(true);
            }
        });

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

        
     protected void getSendButton() {
         sendBtn = new JButton("Send");
         sendBtn.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent ae) {
                 String line = (commandLine.getText()).trim();
                 sendCommandLine(line);
             }
         });
         //sendBtn.setEnabled(false);
         return;
     }

    private void sendCommandLine(String line) {
        if (line.length() < 1) return;
        line = line.toUpperCase();
        String appendLine = line.concat("\n");
        noteTextArea.append(appendLine);
        line = line.trim();
        line = line.replaceAll(" ", "+");
        String newRequest = request + "T&text=" + line;
        //System.out.println(newRequest);

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
            try {
                inputStream.close();
            } catch (Exception ee) {
            }
            return;
        }
        //System.out.println(" ");
        while (lineOut != null) {
            //System.out.println(lineOut);
            StringTokenizer tok = new StringTokenizer(lineOut, " ");
            responseType = tok.nextToken();
            //System.out.println("   responseType=" + responseType);
            if (responseType.equals("U")) {
                String frm = tok.nextToken();
                //System.out.println("   frm=" + frm);
                //System.out.println("   frameNumbers=" + frameNumbers);
                for (int i=0; i<frameNumbers.size(); i++) {
                    if (new Integer(frm).equals(frameNumbers.get(i))) {
                        frameComponentInfo = new FrameComponentInfo(false,false,false);
                        if (lineOut.substring(7,8).equals("1")) {
                            //System.out.println("update image");
                            frameComponentInfo.setIsImage(true);
                        }
                        if (lineOut.substring(9,10).equals("1")) {
                            //System.out.println("update graphics");
                            frameComponentInfo.setIsGraphics(true);
                        }
                        if (lineOut.substring(11,12).equals("1")) {
                            //System.out.println("update colortable");
                            frameComponentInfo.setIsColorTable(true);
                        }
                        updateImage();
                    }
                    break;
                }
            } else if (responseType.equals("V")) {
            } else if (responseType.equals("H")) {
            } else if (responseType.equals("K")) {
            } else if (responseType.equals("T") || responseType.equals("C") ||
                       responseType.equals("M") || responseType.equals("S") ||
                       responseType.equals("R")) {
                noteTextArea.append(lineOut.substring(6));
                noteTextArea.append("\n");
            }
            try {
                lineOut = inputStream.readLine();
            } catch (Exception e) {
                System.out.println("readLine exception=" + e);
                try {
                    inputStream.close();
                } catch (Exception ee) {
                }
                return;
            }
        }
    }

    private void updateImage() {
        try {
            resetData();
        } catch (Exception e) {
            System.out.println("updateImage failed  e=" + e);
        }
    }

    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     * 
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        if (frameComponentInfo == null) {
            frameComponentInfo = new FrameComponentInfo(false,false,false);
            return;
        }
        super.resetData();

        MapProjection mp = getDataProjection();
        if (mp != null) {
          MapViewManager mvm = getMapViewManager();
          mvm.setMapProjection(mp, false);
        }
    }
}
