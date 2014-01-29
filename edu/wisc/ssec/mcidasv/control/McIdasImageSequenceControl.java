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

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import visad.VisADException;
import visad.georef.MapProjection;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.control.ControlWidget;
import ucar.unidata.idv.control.ImageSequenceControl;
import ucar.unidata.idv.control.WrapperWidget;
import ucar.unidata.ui.colortable.ColorTableManager;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import edu.wisc.ssec.mcidasv.data.FrameDirtyInfo;
import edu.wisc.ssec.mcidasv.data.McIdasFrame;
import edu.wisc.ssec.mcidasv.data.McIdasXDataSource;
import edu.wisc.ssec.mcidasv.data.McIdasXInfo;
import edu.wisc.ssec.mcidasv.ui.McIdasFrameDisplay;

/**
 * A DisplayControl for handling McIDAS-X image sequences
 */
public class McIdasImageSequenceControl extends ImageSequenceControl {
	
    private JLabel runningThreads;
    private JCheckBox navigatedCbx;
    private JPanel frameNavigatedContent;
    private McIdasFrameDisplay frameDisplay;
    private Dimension frameSize;
    private JTextField inputText;
    private JScrollPane outputPane;
    private StyledDocument outputText;
    private Font outputFont = new Font("Monospaced", Font.BOLD, 12);
    private ArrayList commandHistory = new ArrayList();
    private int commandHistoryIdx = -1;
    private boolean commandHistoryMode = true;
    
    /** McIDAS-X handles */
    private McIdasXInfo mcidasxInfo;
    private McIdasXDataSource mcidasxDS;

    private int threadCount = 0;

    private static DataChoice dc=null;
    private static Integer frmI;

    /** Holds frame component information */
    private FrameComponentInfo frameComponentInfo;
    private List frameDirtyInfoList = new ArrayList();
    private List frameNumbers = new ArrayList();

    /**
     * Default ctor; sets the attribute flags
     */
    public McIdasImageSequenceControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT);
        initFrameComponentInfo();
        this.mcidasxInfo = null;
        this.mcidasxDS = null;
        
        setDisplayId("bridgecontrol");
        setHelpUrl("idv.controls.bridgecontrol");
        
        setExpandedInTabs(true);
    }

    /**
     * Creates, if needed, the frameComponentInfo member.
     */
    private void initFrameComponentInfo() {
        if (this.frameComponentInfo == null) {
            this.frameComponentInfo = new FrameComponentInfo(true, true, true, false, true, false);
        }
    }
    
    /**
     * Initializes the frameDirtyInfoList member.
     */
    private void initFrameDirtyInfoList() {
    	this.frameDirtyInfoList.clear();
    	Integer frameNumber;
    	FrameDirtyInfo frameDirtyInfo;
    	for (int i=0; i<this.frameNumbers.size(); i++) {
    		frameNumber = (Integer)this.frameNumbers.get(i);
    		frameDirtyInfo = new FrameDirtyInfo(frameNumber.intValue(), false, false, false);
    		this.frameDirtyInfoList.add(frameDirtyInfo);
    	}
    }
    
    /**
     * Sets the frameDirtyInfoList member based on frame number
     */
    private void setFrameDirtyInfoList(int frameNumber, boolean dirtyImage, boolean dirtyGraphics, boolean dirtyColorTable) {
    	FrameDirtyInfo frameDirtyInfo;
    	for (int i=0; i<this.frameDirtyInfoList.size(); i++) {
    		frameDirtyInfo = (FrameDirtyInfo)frameDirtyInfoList.get(i);
    		if (frameDirtyInfo.getFrameNumber() == frameNumber) {
    			frameDirtyInfo.setDirtyImage(dirtyImage);
    			frameDirtyInfo.setDirtyGraphics(dirtyGraphics);
    			frameDirtyInfo.setDirtyColorTable(dirtyColorTable);
    			this.frameDirtyInfoList.set(i, frameDirtyInfo);
    		}
    	}
    }
    
    /**
     * Override the base class method that creates request properties
     * and add in the appropriate frame component request parameters.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(McIdasComponents.IMAGE, new Boolean(this.frameComponentInfo.getIsImage()));
        props.put(McIdasComponents.GRAPHICS, new Boolean(this.frameComponentInfo.getIsGraphics()));
        props.put(McIdasComponents.COLORTABLE, new Boolean(this.frameComponentInfo.getIsColorTable()));
        props.put(McIdasComponents.ANNOTATION, new Boolean(this.frameComponentInfo.getIsAnnotation()));
        props.put(McIdasComponents.FAKEDATETIME, new Boolean(this.frameComponentInfo.getFakeDateTime()));
        props.put(McIdasComponents.DIRTYINFO, this.frameDirtyInfoList);
        return props;
    }

    /**
     * A helper method for constructing the ui.
     * This fills up a list of {@link ControlWidget}
     * (e.g., ColorTableWidget) and creates a gridded
     * ui  with them.
     *
     * @return The ui for the widgets
     */
    protected JComponent doMakeWidgetComponent() {

        JPanel framePanel = new JPanel();
        try {
        	framePanel = GuiUtils.top(doMakeFramePanel());
        } catch (Exception e) {
        	System.err.println("doMakeContents exception: " + e);
        }
        
        JComponent settingsPanel = GuiUtils.top(super.doMakeWidgetComponent());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Frames", framePanel);
        tabbedPane.add("Settings", settingsPanel);

        return tabbedPane;
 
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
    	
        // Navigated checkbox
        navigatedCbx = new JCheckBox("Display data in main 3D panel", false);
        navigatedCbx.setToolTipText("Set to send navigated data to the main 3D display in addition to this 2D display");
        navigatedCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	JCheckBox myself = (JCheckBox)e.getItemSelectable();
            	GuiUtils.enableTree(frameNavigatedContent, myself.isSelected());
            	updateVImage();
            }
         });
        JPanel frameNavigatedCbx =
        	GuiUtils.hflow(Misc.newList(navigatedCbx), 2, 0);
        controlWidgets.add(
        		new WrapperWidget( this, GuiUtils.rLabel("Data:"), frameNavigatedCbx));
    	
        // Navigated options
        JPanel frameComponentsPanel =
            GuiUtils.hflow(Misc.newList(doMakeImageBox(), doMakeGraphicsBox(), doMakeAnnotationBox()), 2, 0);
        JPanel frameOrderPanel =
        	GuiUtils.hflow(Misc.newList(doMakeFakeDateTimeBox()), 2, 0);
        JPanel frameProjectionPanel =
        	GuiUtils.hflow(Misc.newList(doMakeResetProjectionBox()), 2, 0);
        frameNavigatedContent = 
        	GuiUtils.vbox(frameComponentsPanel, frameOrderPanel, frameProjectionPanel);
        GuiUtils.enableTree(frameNavigatedContent, false);
        controlWidgets.add(
        		new WrapperWidget( this, GuiUtils.rLabel(""), frameNavigatedContent));

    }
    
    /**
     * Get frame control widgets specific to this control.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private JPanel doMakeFramePanel() 
    	throws VisADException, RemoteException {
    	frameSize = new Dimension(640, 480);
    	
    	JPanel framePanel = new JPanel(new GridBagLayout());
    	GridBagConstraints c = new GridBagConstraints();
    	c.gridwidth = GridBagConstraints.REMAINDER;
    	
        frmI = new Integer(0);
        ControlContext controlContext = getControlContext();
        List dss = ((IntegratedDataViewer)controlContext).getDataSources();
        for (int i=0; i<dss.size(); i++) {
        	DataSourceImpl ds = (DataSourceImpl)dss.get(i);
        	if (ds instanceof McIdasXDataSource) {
        		frameNumbers.clear();
        		ds.setProperty(DataSource.PROP_AUTOCREATEDISPLAY, false);
        		mcidasxDS = (McIdasXDataSource)ds;
        		DataContext dataContext = mcidasxDS.getDataContext();
        		ColorTableManager colorTableManager = 
        			((IntegratedDataViewer)dataContext).getColorTableManager();
        		ColorTable ct = colorTableManager.getColorTable("McIDAS-X");
        		setColorTable(ct);
        		this.mcidasxInfo = mcidasxDS.getMcIdasXInfo();
        		this.dc = getDataChoice();
        		String choiceStr = this.dc.toString();
        		if (choiceStr.equals("Frame Sequence")) {
        			frameNumbers = mcidasxDS.getFrameNumbers();
        		} else {
        			StringTokenizer tok = new StringTokenizer(choiceStr);
        			String str = tok.nextToken();
        			if (str.equals("Frame")) {
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
        initFrameDirtyInfoList();
        
        // McIDAS-X frame display
        frameDisplay = new McIdasFrameDisplay(frameNumbers, frameSize);
        for (int i=0; i<frameNumbers.size(); i++) {
        	updateXImage((Integer)frameNumbers.get(i));
        	if (i==0) showXImage((Integer)frameNumbers.get(i));
        }
        framePanel.add(GuiUtils.hflow(Misc.newList(frameDisplay)), c);
        
        // McIDAS-X text stuff
        outputPane = doMakeOutputText();
        inputText = doMakeCommandLine();
    	JPanel commandLinePanel =
    		GuiUtils.vbox(outputPane, doMakeSpacer(), inputText);
    	commandLinePanel.setBorder(BorderFactory.createLineBorder(Color.black));
        framePanel.add(GuiUtils.hflow(Misc.newList(commandLinePanel)), c);
        
        // McIDAS-X commands that are running
    	runningThreads = GuiUtils.lLabel("Running: " + this.threadCount);
        framePanel.add(GuiUtils.hflow(Misc.newList(runningThreads)), c);
       
        // Create a sensible title and tell McIDAS-X to stop looping and go to the first frame
        String title = "";
        if (frameNumbers.size() == 1) {
        	title = "McIDAS-X Frame " + (Integer)frameNumbers.get(0);
        }
        else {
        	Integer first = (Integer)frameNumbers.get(0);
        	Integer last = (Integer)frameNumbers.get(frameNumbers.size() - 1);
        	if (last - first == frameNumbers.size() - 1) {
        		title = "McIDAS-X Frames " + first + "-" + last;
        	}
        	else {
        		title = "McIDAS-X Frames " + (Integer)frameNumbers.get(0);
        		for (int i=1; i<frameNumbers.size(); i++) {
        			title += ", " + (Integer)frameNumbers.get(i);
        		}
        	}
        }
        sendCommandLine("TERM L OFF; SF " + (Integer)frameNumbers.get(0), false);
	   
        setNameFromUser(title);

        // Give inputText the focus whenever anything is clicked on...
    	framePanel.addMouseListener(new MouseAdapter() {
    		public void mouseClicked(MouseEvent me) {
    			if (me.getButton() == me.BUTTON1) {
    				inputText.requestFocus();
    			}
    		}
    	});
        
        return framePanel;
    }

    /**
     * Make the frame component check boxes.
     * @return Check box for Images
     */
	protected Component doMakeImageBox() {
    	JCheckBox newBox = new JCheckBox("Image", frameComponentInfo.getIsImage());
    	newBox.setToolTipText("Set to import image data");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setIsImage(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}

    /**
     * Make the frame component check boxes.
     * @return Check box for Graphics
     */
    protected Component doMakeGraphicsBox() {
    	JCheckBox newBox = new JCheckBox("Graphics", frameComponentInfo.getIsGraphics());
    	newBox.setToolTipText("Set to import graphics data");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setIsGraphics(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}

    /**
     * Make the frame component check boxes.
     * @return Check box for Color table
     */
    protected Component doMakeColorTableBox() {
    	JCheckBox newBox = new JCheckBox("Color table", frameComponentInfo.getIsColorTable());
    	newBox.setToolTipText("Set to import color table data");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setIsColorTable(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}
    
    /**
     * Make the frame component check boxes.
     * @return Check box for Annotation line
     */
    protected Component doMakeAnnotationBox() {
    	JCheckBox newBox = new JCheckBox("Annotation line", frameComponentInfo.getIsAnnotation());
    	newBox.setToolTipText("Set to import annotation line");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setIsAnnotation(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}
    
    /**
     * Make the frame behavior check boxes.
     * @return Check box for Fake date/time
     */
    protected Component doMakeFakeDateTimeBox() {
    	JCheckBox newBox =
    		new JCheckBox("Use McIDAS-X frame order to override data time with frame number",
    			frameComponentInfo.getFakeDateTime());
    	newBox.setToolTipText("Set to preserve frame order");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setFakeDateTime(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}
    
    /**
     * Make the frame behavior check boxes.
     * @return Check box for Projection reset
     */
    protected Component doMakeResetProjectionBox() {
    	JCheckBox newBox =
    		new JCheckBox("Use McIDAS-X data projection",
    			frameComponentInfo.getResetProjection());
    	newBox.setToolTipText("Set to reset projection when data is refreshed");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		frameComponentInfo.setResetProjection(myself.isSelected());
        		updateVImage();
        	}
        });
        return newBox;
	}
    
    private void resetCommandHistory() {
    	commandHistory = new ArrayList();
    	resetCommandHistoryIdx();
    }
    
    private void resetCommandHistoryIdx() {
    	commandHistoryIdx = -1;
    }
    
    protected JTextField doMakeCommandLine() {
		final JTextField commandLine = new JTextField(0);
		commandLine.setFont(outputFont);
		commandLine.setBackground(Color.black);
		commandLine.setForeground(Color.cyan);
		commandLine.setCaretColor(Color.cyan);
		
		FontMetrics metrics = commandLine.getFontMetrics(outputFont);
    	Dimension d = new Dimension(frameSize.width, metrics.getHeight());
    	commandLine.setSize(d);
    	commandLine.setPreferredSize(d);
    	commandLine.setMinimumSize(d);
    	commandLine.setMaximumSize(d);
    	
    	resetCommandHistory();
		
		commandLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String line = commandLine.getText().trim();
				if (line.equals("")) return;
				commandLine.setText("");			
				sendCommandLineThread(line, true);
				
				// Add it to the head of commandHistory list
				commandHistory.add(0, line);
				resetCommandHistoryIdx();
			}
		});
		commandLine.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
				char keyChar = ke.getKeyChar();
				if (commandLine.getText().trim().equals("") && commandHistory.size() > 0) {
					commandHistoryMode = true;
				}
				if (commandHistoryMode) {
					if (keyChar == '&') {
						commandHistoryIdx = Math.min(commandHistoryIdx+1,commandHistory.size()-1);
						if (commandHistoryIdx < 0) {
							resetCommandHistoryIdx();
							commandLine.setText("");
						}
						else {
							commandLine.setText((String)commandHistory.get(commandHistoryIdx));
						}
						ke.consume();
					}
					else if (keyChar == '^') {
						commandHistoryIdx--;
						if (commandHistoryIdx < 0) {
							resetCommandHistoryIdx();
							commandLine.setText("");
						}
						else {
							commandLine.setText((String)commandHistory.get(commandHistoryIdx));
						}
						ke.consume();
					}
					else {
						commandHistoryMode = false;
					}
				}
            	if (Character.isLowerCase(keyChar))
            		keyChar = Character.toUpperCase(keyChar);
            	else
            		keyChar = Character.toLowerCase(keyChar);
            	ke.setKeyChar(keyChar);
            }
        });
		commandLine.setBorder(new EmptyBorder(0,0,0,0));
		return commandLine;
    }
    
    private Component doMakeSpacer() {
    	JPanel spacer = new JPanel();
    	Color backgroundColor = new Color(0, 128, 128);
    	spacer.setBackground(backgroundColor);
		FontMetrics metrics = inputText.getFontMetrics(outputFont);
    	Dimension d = new Dimension(frameSize.width, metrics.getHeight());
    	spacer.setSize(d);
    	spacer.setPreferredSize(d);
    	spacer.setMinimumSize(d);
    	spacer.setMaximumSize(d);
    	spacer.setBorder(new EmptyBorder(0,0,0,0));
    	return spacer;
    }
    
    protected JScrollPane doMakeOutputText() {   	
    	JTextPane outputPane = new JTextPane() {
    		public void setSize(Dimension d) {
				if (d.width < getParent().getSize().width)
					d.width = getParent().getSize().width;
				super.setSize(d);
			}
			public boolean getScrollableTracksViewportWidth() {
				return false;
			}
    	};
    	outputPane.setFont(outputFont);
        outputPane.setEditable(false);
        outputPane.setBackground(Color.black);
        outputPane.setForeground(Color.lightGray);
        JScrollPane outputScrollPane = new JScrollPane(outputPane,
        		ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
		FontMetrics metrics = outputPane.getFontMetrics(outputFont);
    	Dimension d = new Dimension(frameSize.width, metrics.getHeight() * 8);
    	outputScrollPane.setSize(d);
    	outputScrollPane.setPreferredSize(d);
    	outputScrollPane.setMinimumSize(d);
    	outputScrollPane.setMaximumSize(d);
    	
    	outputText = (StyledDocument)outputPane.getDocument();
    	outputScrollPane.setBorder(new EmptyBorder(0,0,0,0));
        
    	return outputScrollPane;
    }

    /**
     * Send the given commandline to McIDAS-X over the bridge
     * @param line
     * @param showprocess
     */
	private void sendCommandLine(String line, boolean showprocess) {
		
    	// The user might have moved to another frame...
     	// Ask the image display which frame we are on
		int frameCur = 1;
		if (frameDisplay != null)
			frameCur = frameDisplay.getFrameNumber();
		
        line = line.trim();
        if (line.length() < 1) return;
        String encodedLine = line;
        try {
        	encodedLine = URLEncoder.encode(line,"UTF-8");
        } catch (Exception e) {
        	System.out.println("sendCommandLine URLEncoder exception: " + e);
        }
        
        DataInputStream inputStream = mcidasxInfo.getCommandInputStream(encodedLine, frameCur);
        if (!showprocess) {
            try { inputStream.close(); }
            catch (Exception e) {}
        	return;
        }
//        appendTextLineItalics(line);
        appendTextLineCommand(line);
        try {
        	BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        	String responseType = null;
        	String ULine = null;
        	StringTokenizer tok;
        	boolean inList = false;
        	boolean doUpdate = false;
        	boolean dirtyImage, dirtyGraphics, dirtyColorTable;
        	// Burn the session key header line
        	String lineOut = br.readLine();
           	lineOut = br.readLine();
        	while (lineOut != null) {
//        		System.out.println("sendCommandLine processing: " + lineOut);
        		tok = new StringTokenizer(lineOut, " ");
        		responseType = tok.nextToken();
        		if (responseType.equals("U")) {
            		Integer frameInt = (Integer)Integer.parseInt(tok.nextToken());
            		inList = false;
            		dirtyImage = dirtyGraphics = dirtyColorTable = false;
            		// Don't pay attention to anything outside of our currently loaded frame list
    				for (int i=0; i<frameNumbers.size(); i++) {
    					if (frameInt.compareTo((Integer)frameNumbers.get(i)) == 0) inList = true;
    				}
    				if (inList) {
    					ULine = tok.nextToken();
//    					System.out.println("  Frame " + frameInt + " status line: " + ULine);
    					if (Integer.parseInt(ULine.substring(1,2)) != 0) dirtyImage = true;
    					if (Integer.parseInt(ULine.substring(3,4)) != 0) dirtyGraphics = true;
    					if (Integer.parseInt(ULine.substring(5,6)) != 0) dirtyColorTable = true;
    					if (dirtyImage || dirtyGraphics || dirtyColorTable) {
    						doUpdate = true;
    						updateXImage(frameInt);
    					}
    					setFrameDirtyInfoList(frameInt, dirtyImage, dirtyGraphics, dirtyColorTable);
    				}
        		} else if (responseType.equals("C")) {
        			appendTextLineCommand(lineOut.substring(6));
                } else if (responseType.equals("T")) {
//                	appendTextLine("   " + lineOut.substring(6));
                	appendTextLineNormal(lineOut.substring(6));
                	
                } else if (responseType.equals("M") ||
                           responseType.equals("S")) {
//                	appendTextLine(" * " + lineOut.substring(6));
                	appendTextLineError(lineOut.substring(6));
                	
                } else if (responseType.equals("R")) {
//                	appendTextLine(" ! " + lineOut.substring(6));
                	appendTextLineError(lineOut.substring(6));
                } else if (responseType.equals("V")) {
//                	System.out.println("Viewing frame status line: " + lineOut);
                	frameCur = Integer.parseInt(tok.nextToken());
        		} else if (responseType.equals("H") ||
     				       responseType.equals("K")) {
        			/* Don't do anything with these response types */
        		} else {
        			/* Catch any unparsed line... */
        			System.err.println("Could not parse bridge response: " + lineOut);
        		}
        		lineOut = br.readLine();
        	}
        	showXImage(frameCur);
			if (doUpdate) {
				updateVImage();
			}
			
	    } catch (Exception e) {
	        System.out.println("sendCommandLine exception: " + e);
            try { inputStream.close(); }
            catch (Exception ee) {}
	    }

	}

/*
	private void appendTextLine(String line) {
		outputText.append(line + "\n");
		outputText.setCaretPosition(outputText.getDocument().getLength());
	}
*/
	
	private void appendTextLineNormal(String line) {
        Style style = outputText.addStyle("Normal", null);
        StyleConstants.setForeground(style, Color.lightGray);
        try {
        	outputText.insertString(outputText.getLength(), line + "\n", style);
        } catch (BadLocationException e) { }
        scrollTextLineToBottom();
	}
	
	private void appendTextLineCommand(String line) {
        Style style = outputText.addStyle("Command", null);
        StyleConstants.setForeground(style, Color.green);
        try {
        	outputText.insertString(outputText.getLength(), line + "\n", style);
        } catch (BadLocationException e) { }
        scrollTextLineToBottom();
	}
	
	private void appendTextLineError(String line) {
        Style style = outputText.addStyle("Error", null);
        StyleConstants.setForeground(style, Color.yellow);
        try {
        	outputText.insertString(outputText.getLength(), line + "\n", style);
        } catch (BadLocationException e) { }
        scrollTextLineToBottom();
	}

	private void scrollTextLineToBottom() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JScrollBar vBar = outputPane.getVerticalScrollBar();
				vBar.setValue(vBar.getMaximum());
			}
		});
	}
	
	private void updateXImage(int inFrame) {
		if (mcidasxDS == null || frameDisplay == null) return;
		try {
			McIdasFrame frm = mcidasxDS.getFrame(inFrame);
			Image imageGIF = frm.getGIF();
			frameDisplay.setFrameImage(inFrame, imageGIF);
		} catch (Exception e) {
			System.out.println("updateXImage exception: " + e);
		}
	}
	
	private void showXImage(int inFrame) {
		if (frameDisplay == null) return;
		try {
			frameDisplay.showFrameNumber(inFrame);
		} catch (Exception e) {
			System.out.println("showXImage exception: " + e);
		}
	}
	
    private void updateVImage() {
        try {
        	getRequestProperties();
            resetData();
        } catch (Exception e) {
            System.out.println("updateVImage exception: " + e);
        }
    }

    public boolean init(DataChoice choice)
			throws VisADException, RemoteException {
    	setShowProgressBar(false);
    	boolean ret = super.init(choice, false);
    	return ret;
    }
    
    /**
     * This gets called when the control has received notification of a
     * dataChange event.
     * 
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected void resetData() throws VisADException, RemoteException {
    	// Do not attempt to load any data unless the checkbox is set...
    	if (!navigatedCbx.isSelected()) return;
    	
        super.resetData();

    	if (frameComponentInfo.getResetProjection()) {
    		MapProjection mp = getDataProjection();
    		if (mp != null) {
    			MapViewManager mvm = getMapViewManager();
        		mvm.setMapProjection(mp, false); 
        	}
        }
    }
    
    /**
     * Try my hand at creating a thread
     */
    private class McIdasCommandLine implements Runnable {
    	private String line;
    	private boolean showprocess;
    	public McIdasCommandLine() {
    		this.line = "";
    		this.showprocess = true;
    	}
    	public McIdasCommandLine(String line, boolean showprocess) {
    		this.line = line;
    		this.showprocess = showprocess;
    	}
        public void run() {
        	notifyThreadStart();
        	sendCommandLine(this.line, this.showprocess);
        	notifyThreadStop();
        }
    }
    
    /**
     * Threaded sendCommandLine
     * @param line
     * @param showprocess
     */
    private void sendCommandLineThread(String line, boolean showprocess) {
    	McIdasCommandLine mcCmdLine = new McIdasCommandLine(line, showprocess);
    	Thread t = new Thread(mcCmdLine);
        t.start();
    }
    
    private void notifyThreadStart() {
    	this.threadCount++;
    	notifyThreadCount();
    }
    private void notifyThreadStop() {
    	this.threadCount--;
    	notifyThreadCount();
    }
    private void notifyThreadCount() {
    	runningThreads.setText("Running: " + this.threadCount);
    }

}
