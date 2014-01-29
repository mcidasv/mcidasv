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

import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.PlainDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.miginfocom.swing.MigLayout;

import edu.wisc.ssec.mcidasv.util.McVGuiUtils;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.IconPanel;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Prefer;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import edu.wisc.ssec.mcidasv.util.WebBrowser;
import edu.wisc.ssec.mcidasv.Constants;

import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.idv.IntegratedDataViewer;

import visad.ConstantMap;
import visad.Display;
import visad.FlatField;
import visad.python.JPythonMethods;
import visad.ss.BasicSSCell;
import visad.ss.FancySSCell;


public class BadNetCDFWidget implements Constants {
    
    private static final Logger logger = LoggerFactory.getLogger(BadNetCDFWidget.class);
    
    private IntegratedDataViewer idv;
    
    private NetcdfDataset ncFile;
    private List<Variable> varList;
    private List<String> varNames;
    
    
    // For NcML Editor
    private JEditorPane NcMLeditor;
    
    
    // For variable display
    BasicSSCell display;
    ConstantMap[] cmaps;
    
    
    // For nav specification
    private JRadioButton radioLatLonVars = new JRadioButton("Variables", true);
    private JRadioButton radioLatLonBounds = new JRadioButton("Bounds", false);
    
    private JComboBox refComboBox = new JComboBox();
    private JComboBox latComboBox = new JComboBox();
    private JComboBox lonComboBox = new JComboBox();

    private JPanel panelLatLonVars = new JPanel();
    private JPanel panelLatLonBounds = new JPanel();

    private JTextField textLatUL = new JTextField();
    private JTextField textLonUL = new JTextField();
    private JTextField textLatLR = new JTextField();
    private JTextField textLonLR = new JTextField();

    
    // TODO: DO WE NEED THESE?
    private JTextField textLatLonScale = new JTextField();
    private JCheckBox checkEastPositive = new JCheckBox("East positive");
    
    
    
    
    public BadNetCDFWidget(NetcdfDataset ncFile, IntegratedDataViewer idv)
    {
        this.idv = idv;
        this.ncFile = ncFile;
        varList = ncFile.getVariables();  
        
        varNames = new LinkedList<String>();
        
        //System.out.println("Our file has " + varList.size() + " variables named:");
        Iterator <Variable> varIt = varList.iterator();
        while(varIt.hasNext())
        {
            Variable ourVar = varIt.next();
            varNames.add(ourVar.getFullName());
            //System.out.println("Name: " + ourVar.getName());
        }
    }
    
    
    // Passes through any exception from openDataset - this function
    // doesn't provide an IDV and should only be used for testing. (Some functionality
    // using the rest of the IDV won't work.)
    public BadNetCDFWidget(String filepath) throws IOException
    {
        this(NetcdfDataset.openDataset(filepath), null);
    }
    
    
    
    // Tester function to pick a file and send it through the paces.
    public static void main(String[] args)
    {
        String testfile = FileManager.getReadFile();        
        System.out.println(testfile);
        
        //String testfile = "/Users/nickb/testdata/tester.nc";
        
        
        BadNetCDFWidget bfReader;
        try { bfReader = new BadNetCDFWidget(testfile); }
        catch (Exception exe)
        {
            //System.out.println("This file cannot be read by the BadFileReader!");
            exe.printStackTrace();
            return;
        }
        
        bfReader.showChoices();
        //bfReader.showNavChooser();
    }
    
    

    /////////////////////////////////////////////////////////
    // Displays our "main menu" of choices to fix the given file!
    // Everything else needed can get called from here.
    /////////////////////////////////////////////////////////
    public void showChoices()
    {
      EventQueue.invokeLater(new Runnable() {
          public void run() {
              try {
                  BadNetCDFDialog dialog = new BadNetCDFDialog();
                  dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                  dialog.setVisible(true);
                  dialog.toFront();
              } catch (Exception e) {
                  e.printStackTrace();
              }
          }
      });
    }

    /////////////////////////////////////////////////////////
    // Creates an editor for NcML and displays it in a window - includes buttons for saving just
    // the NcML and the full NetCDF file with the changes made.
    /////////////////////////////////////////////////////////
    private void showNcMLEditor()
    {
        NcMLeditor = new JEditorPane();
        
        // We use this to store the actual ncml - 10000 is just the number toolsUI used
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        try
        {
            ncFile.writeNcML(bos, null);
            NcMLeditor.setText(bos.toString());
            NcMLeditor.setCaretPosition(0);
            
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
            //setInError(true, false, "");  DataSourceImpl - doesn't work if we're not a data source
            return;
        }
        
        NcMLeditor.setEditable(true);

        // Set the font style.
        NcMLeditor.setFont(new Font("Courier", Font.PLAIN, 12));
        
        // Set the tab size
        NcMLeditor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 2);
        

        // Button to save NcML as text, 
        // popup allows them to specify where.
        JButton saveNcMLBtn = new JButton("Save NcML as text");
        ActionListener saveAction = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                // Begin with getting the filename we want to write to.
                String ncLocation = ncFile.getLocation();
                
                if (ncLocation == null) ncLocation = "test";
                int pos = ncLocation.lastIndexOf(".");
                if (pos > 0)
                    ncLocation = ncLocation.substring(0, pos);

                String filename = FileManager.getWriteFile(ncLocation + ".ncml");  
                if (filename == null) return;
               // System.out.println("Write NcML to filename:" + filename);
                
                
                // Once we have that, we can actually write to the file!
                try{
                    IOUtil.writeFile(new File(filename), NcMLeditor.getText());
                } catch(Exception exc)
                {
                    // TODO: Should probably add some kind of exception handling.
                    exc.printStackTrace();
                    return;
                }
            }
        };
        saveNcMLBtn.addActionListener(saveAction);
        

        // Button to merge the NcML with NetCDF 
        // a'la ToolsUI and write it back out as NetCDF3.
        JButton saveNetCDFBtn = new JButton("Merge and save NetCDF");
        ActionListener saveNetCDFAction = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                // Begin with getting the filename we want to write to.
                String ncLocation = ncFile.getLocation();
                
                if (ncLocation == null) ncLocation = "test";
                int pos = ncLocation.lastIndexOf(".");
                if (pos > 0)
                    ncLocation = ncLocation.substring(0, pos);

                String filename = FileManager.getWriteFile(ncLocation + ".nc");  
                if (filename == null) return;
              //  System.out.println("Write NetCDF to filename:" + filename);
                
                
                // Once we have that, we can actually write to the file!
                try {
                    ByteArrayInputStream bis = new ByteArrayInputStream(NcMLeditor.getText().getBytes());
                    NcMLReader.writeNcMLToFile(bis, filename);
                } catch(Exception exc)
                {
                    // TODO: Should probably add some kind of exception handling.
                    exc.printStackTrace();
                    return;
                }
            }
        };
        saveNetCDFBtn.addActionListener(saveNetCDFAction);
        
        
        // Button to load this data into McV from NcML
        JButton sendToMcVBtn = new JButton("Attempt to load with this NcML");
        ActionListener sendToMcVAction = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                // TODO: save the current NcML into the NetcdfDataSource
                createIDVdisplay();
                return;
            }
        };
        sendToMcVBtn.addActionListener(sendToMcVAction);
        
        
        JToolBar toolbar = new JToolBar("NcML Editor Controls");

        toolbar.add(saveNcMLBtn);
        toolbar.add(saveNetCDFBtn);
        toolbar.add(sendToMcVBtn);
        
        
        JScrollPane scrollPane = new JScrollPane(NcMLeditor,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        scrollPane.setPreferredSize(new Dimension(600, 600));  // TODO: PREFERRED SIZE?
        
        JPanel panel = GuiUtils.topCenter(toolbar, scrollPane);
        JFrame editorWindow = GuiUtils.makeWindow("NcML Editor", GuiUtils.inset(panel, 10), 0, 0);
        editorWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        editorWindow.setVisible(true);
        editorWindow.toFront();

      //  System.out.println("NcML Editor Created!");
    }
    
    
    
    
    
    /////////////////////////////////////////////////////////
    // Takes our ncFile and puts it back into IDV (McV)
    /////////////////////////////////////////////////////////
    private void createIDVdisplay()
    {
        
        // Make a NetcdfDataset from our NcML

        ByteArrayInputStream bis = new ByteArrayInputStream(NcMLeditor.getText().getBytes());
        
        try {
            ncFile = NcMLReader.readNcML(bis, null);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return;
        }

        
        // Now try to turn that NetcdfDataset into a legitimate DataSource!
        GridDataset gd;
        
       // System.out.println("Creating the grid dataset...");
        
        try {
            gd = new GridDataset(ncFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        
        ncFile.getLocation();
      //  System.out.println("Grid dataset created!  Adding to data manager...");
//        GeoGridDataSource ggds = new GeoGridDataSource(gd);
        DataSourceDescriptor dsd = new DataSourceDescriptor();
        dsd.setLabel("NcML DS Label");
        GeoGridDataSource ggds = new GeoGridDataSource(dsd, gd, "NcML Data Source", ncFile.getLocation());
        ggds.initAfterCreation();
        this.idv.getDataManager().addDataSource(ggds);
    }
    
    
    
    
    
    /////////////////////////////////////////////////////////
    // Shows a window that gives a choice of variables!
    /////////////////////////////////////////////////////////
    private void showVarPicker()
    {
        // DataImpl
        //Array arr = var.read();
                
        JComboBox varDD = new JComboBox();
        GuiUtils.setListData(varDD, varNames);
        
        ActionListener getVarAction = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                JComboBox cb = (JComboBox)ae.getSource();
                Variable plotVar = varList.get(cb.getSelectedIndex());
                String varName = (String) cb.getSelectedItem();
                
                
                float [] varVals;
                try{
                    // TODO: Is there a better way to convert this?  Is there another function like reshape?
                    Array varArray = plotVar.read();
                    varVals  = (float[]) varArray.get1DJavaArray(float.class);
                    // TODO: Better exception handling
                }
                catch (IOException IOexe)
                {
                    IOexe.printStackTrace();
                    return;
                }
                
                    int size = plotVar.getDimensions().size();
                    if( size != 2)
                    {
                        //System.err.println("We should fail here because size != 2, size ==" + size);
                        JOptionPane.showMessageDialog(null, 
                                                    ("<html>Variables must have 2 dimensions to be displayed here.<br><br>\"" + varName + "\" has " + size + ".</html>"),
                                                    "Invalid Dimensions",
                                                    JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    int xdim = plotVar.getDimensions().get(0).getLength();
                    int ydim = plotVar.getDimensions().get(1).getLength();
                    
                  //  System.out.println("xdim: " + xdim + " ydim: " + ydim);
                    
                    float[][] var2D = reshape(varVals, ydim, xdim);
                    
                    //JPythonMethods.plot(varVals);
                    //JPythonMethods.plot(var2D);
                    
               try {
                    FlatField varField = JPythonMethods.field(var2D);
                    

                    ConstantMap[] cmaps = {
                              new ConstantMap(1.0, Display.Red),
                              new ConstantMap(1.0, Display.Green),
                              new ConstantMap(1.0, Display.Blue)
                            }; 
                    
                    // Clear out the display or we get some weird stuff going on.
                    display.clearCell();
                    display.clearMaps();
                    display.clearDisplay();  
                    
                    display.addData(varField, cmaps);                 
                    
                  /*  // Make sure data isn't lingering around:
                    String[] dataSources = display.getDataSources();
                    System.out.println("Data sources: " + dataSources.length);  */ 
               }
                // TODO: Better exception handling - throughout this whole file, really.
                catch (Exception exe)
                {
                    exe.printStackTrace();
                    return;

                }
            }
        };
        varDD.addActionListener(getVarAction);
        
        //BasicSSCell display = new FancySSCell("Variable!");
        //display.setDimension(BasicSSCell.JAVA3D_3D);
        
        // TODO: exception handling
        try 
        { 
            // Heavily borrowed from VISAD's JPythonMethods
            display = new FancySSCell("Variable Viewer");
            display.setDimension(BasicSSCell.JAVA3D_3D);
            display.setPreferredSize(new Dimension(256, 256));
            JFrame frame = new JFrame("Variable Viewer");
            JPanel pane = new JPanel();
            pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
            frame.setContentPane(pane);
            pane.add(varDD);
            pane.add(display);
            JButton controls = new JButton("Controls");
            JPanel buttons = new JPanel();
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.add(controls);
            pane.add(buttons);
            final FancySSCell fdisp = (FancySSCell) display;
            fdisp.setAutoShowControls(false);
            
            controls.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                  fdisp.showWidgetFrame();
                }
            });
            
            frame.pack();
           // frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
            frame.toFront();
                    
        }
        // TODO: Exception handling
        catch (Exception exe) {
            exe.printStackTrace();
            return;
        }
        
    }
    
    
    
    // Quick and dirty function to reshape a 1D float array into a 2D
    private static float[][] reshape(float[] arr, int m, int n)
    {
        float[][] newArr = new float[m][n];
        
        int index=0;
        for (int i = 0; i < n; i++)
        {
            for(int j = 0; j< m; j++)
            {
                newArr[j][i] = arr[index++];
            }
        }
        return newArr;
    }
    
    
    
    /////////////////////////////////////////////////////////
    // Shows a window that gives the opportunity to either define 
    // coordinate variables or specify corner points.
    //
    // Borrowed heavily from FlatFileChooser's makeNavigationPanel for style
    /////////////////////////////////////////////////////////
    private void showNavChooser()
    {
        JPanel midPanel = new JPanel();
        midPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Navigation"));

        GuiUtils.setListData(refComboBox, varNames);
        GuiUtils.setListData(latComboBox, varNames);
        GuiUtils.setListData(lonComboBox, varNames);
        
        McVGuiUtils.setComponentWidth(latComboBox, Width.QUADRUPLE);
        McVGuiUtils.setComponentWidth(lonComboBox, Width.QUADRUPLE);
        McVGuiUtils.setComponentWidth(refComboBox, Width.QUADRUPLE);
        
        //panelLatLonVars = McVGuiUtils.topBottom(latComboBox, lonComboBox, Prefer.NEITHER);
        panelLatLonVars = McVGuiUtils.topBottom(McVGuiUtils.makeLabeledComponent("Latitude:",latComboBox),
                                                McVGuiUtils.makeLabeledComponent("Longitude:",lonComboBox),
                                                Prefer.NEITHER);
        

        GuiUtils.buttonGroup(radioLatLonVars, radioLatLonBounds);

        // Images to make the bounds more clear
        IconPanel urPanel = new IconPanel("/edu/wisc/ssec/mcidasv/images/upper_right.gif");
        IconPanel llPanel = new IconPanel("/edu/wisc/ssec/mcidasv/images/lower_left.gif");
        
        McVGuiUtils.setComponentWidth(textLatUL);
        McVGuiUtils.setComponentWidth(textLonUL);
        McVGuiUtils.setComponentWidth(textLatLR);
        McVGuiUtils.setComponentWidth(textLonLR);
        panelLatLonBounds = McVGuiUtils.topBottom(
                McVGuiUtils.makeLabeledComponent("UL Lat/Lon:", GuiUtils.leftRight(GuiUtils.hbox(textLatUL, textLonUL), urPanel)),
                McVGuiUtils.makeLabeledComponent("LR Lat/Lon:", GuiUtils.leftRight(llPanel, GuiUtils.hbox(textLatLR, textLonLR))),
                Prefer.NEITHER);
        
        panelLatLonBounds = McVGuiUtils.topBottom(panelLatLonBounds, McVGuiUtils.makeLabeledComponent("Reference:", refComboBox), Prefer.NEITHER);

        
        McVGuiUtils.setComponentWidth(radioLatLonVars);
        McVGuiUtils.setComponentWidth(radioLatLonBounds);

        
        // Add a bit of a buffer to both
        panelLatLonVars = GuiUtils.inset(panelLatLonVars, 5);
        panelLatLonBounds = GuiUtils.inset(panelLatLonBounds, 5);
        
        GroupLayout layout = new GroupLayout(midPanel);
        midPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioLatLonVars)
                        .addGap(GAP_RELATED)
                        .addComponent(panelLatLonVars))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioLatLonBounds)
                        .addGap(GAP_RELATED)
                        .addComponent(panelLatLonBounds)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioLatLonVars)
                    .addComponent(panelLatLonVars))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioLatLonBounds)
                    .addComponent(panelLatLonBounds))
                .addPreferredGap(RELATED)
                .addContainerGap())
        );
 

        
        radioLatLonVars.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                checkSetLatLon();
            }
        });
        
        radioLatLonBounds.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                checkSetLatLon();
            }
        });
        
        
        JButton goBtn = new JButton("Go!");
        ActionListener goBtnAction = new ActionListener()
        {
            public void actionPerformed(ActionEvent ae)
            {
                boolean isVar = radioLatLonVars.isSelected();
                if (isVar)
                    navVarAction();
                else 
                    navCornersAction();
            }
        };
        goBtn.addActionListener(goBtnAction);
        
        
        JPanel wholePanel = McVGuiUtils.topBottom(midPanel, goBtn, Prefer.NEITHER);
            
        JFrame myWindow = GuiUtils.makeWindow("Pick Your Navigation!", GuiUtils.inset(wholePanel, 10), 0, 0);
        checkSetLatLon();
        myWindow.setVisible(true);
        myWindow.toFront();
    }
    
    
    /**
     * enable/disable widgets for navigation
     */
    private void checkSetLatLon() {
        boolean isVar = radioLatLonVars.isSelected();
        GuiUtils.enableTree(panelLatLonVars, isVar);
        GuiUtils.enableTree(panelLatLonBounds, !isVar);
    }
    
    
    /**
     * 
     * One of the two workhorses of our nav chooser, it alters the chosen 
     * (existing) variables so they can be used as lat/lon pairs.
     * 
     */
    private void navVarAction()
    {
        
    }
    
    

    /**
     * 
     * One of the two workhorses of our nav chooser, it creates new 
     * variables for lat/lon based on the specified cornerpoints and
     * reference variable (for dimensions.)
     * 
     */
    
    private void navCornersAction()
    {
        
    }
    
    public class BadNetCDFDialog extends JDialog {

        /**
         * Create the dialog.
         */
        public BadNetCDFDialog() {
            setTitle("Non-Compliant NetCDF Tool");
            setMinimumSize(new Dimension(705, 320));
            setBounds(100, 100, 705, 320);
            Container contentPane = getContentPane();
            
            JLabel headerLabel = new JLabel("McIDAS-V is unable to read your file.");
            headerLabel.setFont(UIManager.getFont("OptionPane.font"));
            headerLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
            
            JTextPane messageTextPane = new JTextPane();
            Font textPaneFont = UIManager.getFont("TextPane.font");
            String fontCss = String.format("style=\"font-family: '%s'; font-size: %d;\"", textPaneFont.getFamily(), textPaneFont.getSize());
            messageTextPane.setBackground(UIManager.getColor("Label.background"));
            messageTextPane.setContentType("text/html");
            messageTextPane.setDragEnabled(false);
            messageTextPane.setText("<html>\n<body "+fontCss +">To verify if your file is CF-compliant, you can run your file through an online compliance checker (<a href=\"http://titania.badc.rl.ac.uk/cgi-bin/cf-checker.pl\">example CF-compliance utility</a>).<br/><br/> \n\nIf the checker indicates that your file is not compliant you can attempt to fix it using the NcML Editor provided in this window.<br/><br/>\n\nIn a future release of McIDAS-V, this interface will present you with choices for the variables necessary for McIDAS-V to display your data.<br/></font></body></html>");
            messageTextPane.setEditable(false);
            messageTextPane.addHyperlinkListener(new HyperlinkListener() {
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
                        return;
                    }
                    String url = null;
                    if (e.getURL() == null) {
                        url = e.getDescription();
                    } else {
                        url = e.getURL().toString();
                    }
                    WebBrowser.browse(url);
                }
            });
            

            JSeparator separator = new JSeparator();
            // seems pretty dumb to have to do this sizing business in order to get the separator to appear, right?
            // check out: http://docs.oracle.com/javase/tutorial/uiswing/components/separator.html
            // "Separators have almost no API and are extremely easy to use as 
            // long as you keep one thing in mind: In most implementations, 
            // a vertical separator has a preferred height of 0, and a 
            // horizontal separator has a preferred width of 0. This means a 
            // separator is not visible unless you either set its preferred 
            // size or put it in under the control of a layout manager such as 
            // BorderLayout or BoxLayout that stretches it to fill its 
            // available display area."
            // WHO ON EARTH DECIDED THAT WAS SENSIBLE DEFAULT BEHAVIOR FOR A SEPARATOR WIDGET!?
            separator.setMinimumSize(new Dimension(1, 12));
            separator.setPreferredSize(new Dimension(1, 12));

            
            JLabel editorLabel = new JLabel("Open the file in the NcML editor:");
            
            JButton editorButton = new JButton("NcML Editor");
            editorButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    showNcMLEditor();
                }
            });
            
            JLabel viewLabel = new JLabel("I just want to view one of the variables:");
            
            JButton viewButton = new JButton("View Variable");
            viewButton.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    showVarPicker();
                }
            });
            
            JLabel noncompliantLabel = new JLabel("I have navigation variables, they just aren't CF-compliant: (FEATURE INCOMPLETE)");
            
            JButton noncompliantButton = new JButton("Choose Nav");
            noncompliantButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    showNavChooser();
                }
            });
            this.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    logger.trace("disposing of dialog");
                    BadNetCDFDialog.this.dispose();
                }
            });

            contentPane.setLayout(new MigLayout(
                "", 
                "[grow][]", 
                "[][grow][][][][][][]"));
            contentPane.add(headerLabel,        "spanx 2, alignx left, aligny top, wrap");
            contentPane.add(messageTextPane,    "spanx 2, grow, wrap");
            contentPane.add(separator,          "spanx 2, growx, aligny top, wrap");
            contentPane.add(editorLabel,        "alignx left, aligny baseline");
            contentPane.add(editorButton,       "growx, aligny baseline, wrap");
            contentPane.add(viewLabel,          "alignx left, aligny baseline");
            contentPane.add(viewButton,         "growx, aligny baseline, wrap");
            contentPane.add(noncompliantLabel,  "alignx left, aligny baseline");
            contentPane.add(noncompliantButton, "growx, aligny baseline, wrap");
            
        }
    }
}
