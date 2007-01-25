package ucar.unidata.idv.control.mcidas;

import java.awt.*;
import java.awt.event.*;
import java.lang.Class;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.event.*;
import javax.swing.*;
import javax.swing.JCheckBox;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceImpl;
//import ucar.unidata.data.imagery.mcidas.FrameComponentInfo;
import ucar.unidata.data.imagery.mcidas.FrameDirtyInfo;
import ucar.unidata.data.imagery.mcidas.ConduitInfo;
//import ucar.unidata.data.imagery.mcidas.McIDASConstants;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource;
import ucar.unidata.data.imagery.mcidas.McIDASDataSource.FrameDataInfo;
import ucar.unidata.data.imagery.mcidas.McIDASFrame;
import ucar.unidata.data.imagery.mcidas.McIDASXFrameDescriptor;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.control.WrapperWidget;
import ucar.unidata.idv.IntegratedDataViewer;

import ucar.unidata.ui.TextHistoryPane;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import visad.*;
import visad.georef.MapProjection;


/**
 * A DisplayControl for handling McIDAS-X image sequences
 */
public class McXImageSequenceControl extends ImageSequenceControl {

    private JLabel commandLineLabel;
    private JTextField commandLine;
    private JPanel commandPanel;
    private JButton sendBtn;
    private JTextArea textArea;
    private JPanel textWrapper;

    private int nlines, removeIncr,
                count = 0;

    private int ptSize = 12;

    /** Holds frame component information */
    private FrameComponentInfo frameComponentInfo;


    /**
     * Default ctor; sets the attribute flags
     */
    public McXImageSequenceControl() {
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
/*
        List textComps = new ArrayList();
        textArea = new TextHistoryPane(20, 20, true);
        textArea = new TextHistoryPane(500, 100, true);
        textComps.add(new JLabel(" "));
        textComps.add(new JLabel(" "));
        JPanel textPanel = GuiUtils.doLayout(textComps, 1, GuiUtils.WT_NN,
                                              GuiUtils.WT_N);
        textArea.setPreferredSize(new Dimension(20, 20));
        textPanel.add(textArea);
*/

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
/*
        controlWidgets.add(
            new WrapperWidget(
                this, doMakeTextArea()));
*/


        List frameNumbers = new ArrayList();
        Integer frmI = new Integer(0);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        McIDASDataSource mds = null;
        for (int i=0; i<dss.size(); i++) {
          DataSourceImpl ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McIDASDataSource) {
             mds = (McIDASDataSource)ds;
             frameNumbers = mds.getFrameNumbers();
             ArrayList frmAL = (ArrayList)(frameNumbers.get(0));
             frmI = (Integer)(frmAL.get(0));
            break;
          }
       }
       setShowNoteText(true);
       noteTextArea.setRows(20);
       noteTextArea.setLineWrap(true);
       noteTextArea.setEditable(false);
       //noteTextArea.setFont(new Font("Monospaced", Font.PLAIN, ptSize));
       //System.out.println("noteTextArea=" + noteTextArea +" class=" + noteTextArea.getClass());
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
        //appendLine(line);
        line = line.concat("\n");
        noteTextArea.append(line);
        //setNoteText(line);
    }

    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     * 
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
        DataChoice dc = getDataChoice();

        FrameDirtyInfo frameDirtyInfo = new FrameDirtyInfo(false,false,false);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        DataSourceImpl ds = null;
        for (int i=0; i<dss.size(); i++) {
          ds = (DataSourceImpl)dss.get(i);
          if (ds instanceof McIDASDataSource) {
            frameDirtyInfo = ((McIDASDataSource)ds).getFrameDirtyInfo();
            break;
          }
        }
        MapProjection saveMapProjection;
        if (frameDirtyInfo.dirtyImage) {
          saveMapProjection = null;
        } else {
          saveMapProjection = getMapViewProjection();
        }

        super.resetData();
        if (saveMapProjection != null) {
          MapViewManager mvm = getMapViewManager();
          mvm.setMapProjection(saveMapProjection, false);
        }
    }
}
