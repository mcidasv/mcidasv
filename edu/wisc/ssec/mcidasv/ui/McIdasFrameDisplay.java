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

package edu.wisc.ssec.mcidasv.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.ui.AnimatedGifEncoder;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.JpegImagesToMovie;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;

public class McIdasFrameDisplay extends JPanel implements ActionListener {
	
    /** Do we show the big icon */
    public static boolean bigIcon = false;
    
    /** The start/stop button */
    AbstractButton startStopBtn;
    
    /** stop icon */
    private static Icon stopIcon;

    /** start icon */
    private static Icon startIcon;
	
    /** Flag for changing the INDEX */
    public static final String CMD_INDEX = "CMD_INDEX";

    /** property for setting the widget to the first frame */
    public static final String CMD_BEGINNING = "CMD_BEGINNING";

    /** property for setting the widget to the loop in reverse */
    public static final String CMD_BACKWARD = "CMD_BACKWARD";

    /** property for setting the widget to the start or stop */
    public static final String CMD_STARTSTOP = "CMD_STARTSTOP";

    /** property for setting the widget to the loop forward */
    public static final String CMD_FORWARD = "CMD_FORWARD";

    /** property for setting the widget to the last frame */
    public static final String CMD_END = "CMD_END";
    
    /** hi res button */
    private static JRadioButton hiBtn;

    /** medium res button */
    private static JRadioButton medBtn;

    /** low res button */
    private static JRadioButton lowBtn;
    
    /** display rate field */
    private JTextField displayRateFld;

	private Integer frameNumber = 1;
	private Integer frameIndex = 0;
	private List frameNumbers;
	private Hashtable images;
	private Image theImage;
	private JPanelImage pi;
	private JComboBox indicator;
	private Dimension d;
	
    private Thread loopThread;
    private boolean isLooping = false;
    private int loopDwell = 500;
    
    private boolean antiAlias = false;
	
    public McIdasFrameDisplay(List frameNumbers) {
    	this(frameNumbers, new Dimension(640, 480));
    }
    
	public McIdasFrameDisplay(List frameNumbers, Dimension d) {
		if (frameNumbers.size()<1) return;
		this.frameIndex = 0;
		this.frameNumbers = frameNumbers;
		this.frameNumber = (Integer)frameNumbers.get(this.frameIndex);
		this.images = new Hashtable(frameNumbers.size());
		this.d = d;
		this.pi = new JPanelImage();
		this.pi.setFocusable(true);
		this.pi.setSize(this.d);
		this.pi.setPreferredSize(this.d);
		this.pi.setMinimumSize(this.d);
		this.pi.setMaximumSize(this.d);
		
		String[] frameNames = new String[frameNumbers.size()];
		for (int i=0; i<frameNumbers.size(); i++) {
			frameNames[i] = "Frame " + (Integer)frameNumbers.get(i);
		}
		indicator = new JComboBox(frameNames);
        indicator.setFont(new Font("Dialog", Font.PLAIN, 9));
        indicator.setLightWeightPopupEnabled(false);
        indicator.setVisible(true);
        indicator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	showIndexNumber(indicator.getSelectedIndex());
            }
        });
        
/*
		// Create the File menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
		fileMenu.add(GuiUtils.makeMenuItem("Print...", this,
                "doPrintImage", null, true));
        fileMenu.add(GuiUtils.makeMenuItem("Save image...", this,
                "doSaveImageInThread"));
        fileMenu.add(GuiUtils.makeMenuItem("Save movie...", this,
                "doSaveMovieInThread"));
        
		setTitle(title);
		setJMenuBar(menuBar);
*/
        
        JComponent controls = GuiUtils.hgrid(
        		GuiUtils.left(doMakeAntiAlias()), GuiUtils.right(doMakeVCR()));
        add(GuiUtils.vbox(controls, pi));
		
	}
	
	/**
	 * Make the UI for anti-aliasing controls
	 * 
	 * @return  UI as a Component
	 */
	private Component doMakeAntiAlias() {
    	JCheckBox newBox = new JCheckBox("Smooth images", antiAlias);
    	newBox.setToolTipText("Set to use anti-aliasing to smooth images when resizing to fit frame display");
    	newBox.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		JCheckBox myself = (JCheckBox)e.getItemSelectable();
        		antiAlias = myself.isSelected();
        		paintFrame();
        	}
        });
        return newBox;
	}
	
    /**
     * Make the UI for VCR controls.
     *
     * @return  UI as a Component
     */
    private JComponent doMakeVCR() {
        KeyListener listener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                char c    = e.getKeyChar();
				if (e.isAltDown()) {
					if (c == (char)'a') showFrameNext();
					else if (c == (char)'b') showFramePrevious();
					else if (c == (char)'l') toggleLoop(true);
				}
            }
        };
        List buttonList = new ArrayList();
        indicator.addKeyListener(listener);
        buttonList.add(GuiUtils.inset(indicator, new Insets(0, 0, 0, 2)));
        String[][] buttonInfo = {
            { "Go to first frame", CMD_BEGINNING, getIcon("Rewind") },
            { "One frame back", CMD_BACKWARD, getIcon("StepBack") },
            { "Run/Stop", CMD_STARTSTOP, getIcon("Play") },
            { "One frame forward", CMD_FORWARD, getIcon("StepForward") },
            { "Go to last frame", CMD_END, getIcon("FastForward") }
        };

        for (int i = 0; i < buttonInfo.length; i++) {
            JButton btn = GuiUtils.getImageButton(buttonInfo[i][2], getClass(), 2, 2);
            btn.setToolTipText(buttonInfo[i][0]);
            btn.setActionCommand(buttonInfo[i][1]);
            btn.addActionListener(this);
            btn.addKeyListener(listener);
            btn.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
            buttonList.add(btn);
            if (i == 2) {
                startStopBtn = btn;
            }
        }

        JComponent sbtn = makeSlider();
        sbtn.addKeyListener(listener);
        buttonList.add(sbtn);

        JComponent contents = GuiUtils.hflow(buttonList, 1, 0);

        updateRunButton();
        return contents;
    }
	
    /**
     * Get the correct icon name based on whether we are in big icon mode
     *
     * @param name base name
     *
     * @return Full path to icon
     */
    private String getIcon(String name) {
        return "/auxdata/ui/icons/" + name + (bigIcon
                ? "24"
                : "16") + ".gif";
    }
	
    /**
     * Public by implementing ActionListener.
     *
     * @param e  ActionEvent to check
     */
    public void actionPerformed(ActionEvent e) {
        actionPerformed(e.getActionCommand());
    }
    
    /**
     * Handle the action
     *
     * @param cmd The action
     */
    private void actionPerformed(String cmd) {
        if (cmd.equals(CMD_STARTSTOP)) {
        	toggleLoop(false);
        } else if (cmd.equals(CMD_FORWARD)) {
            showFrameNext();
        } else if (cmd.equals(CMD_BACKWARD)) {
            showFramePrevious();
        } else if (cmd.equals(CMD_BEGINNING)) {
            showFrameFirst();
        } else if (cmd.equals(CMD_END)) {
            showFrameLast();
        }
    }
    
    /**
     * Update the icon in the run button
     */
    private void updateRunButton() {
        if (stopIcon == null) {
            stopIcon  = Resource.getIcon(getIcon("Pause"), true);
            startIcon = Resource.getIcon(getIcon("Play"), true);
        }
        if (startStopBtn != null) {
        	if (isLooping) {
                startStopBtn.setIcon(stopIcon);
                startStopBtn.setToolTipText("Stop animation");
            } else {
                startStopBtn.setIcon(startIcon);
                startStopBtn.setToolTipText("Start animation");
            }
        }
    }
	
	public void setFrameImage(int inFrame, Image inImage) {
		images.put("Frame " + inFrame, inImage);
	}
	
	private int getIndexPrevious() {
		int thisIndex = frameIndex.intValue();
		if (thisIndex > 0)
			thisIndex--;
		else
			thisIndex = frameNumbers.size() - 1;
		return thisIndex;
	}
	
	private int getIndexNext() {
		int thisIndex = frameIndex.intValue();
		if (thisIndex < frameNumbers.size() - 1)
			thisIndex++;
		else
			thisIndex = 0;
		return thisIndex;
	}
	
	public void showFramePrevious() {
		showIndexNumber(getIndexPrevious());
	}
	
	public void showFrameNext() {
		showIndexNumber(getIndexNext());
	}
	
	public void showFrameFirst() {
		showIndexNumber(0);
	}
	
	public void showFrameLast() {
		showIndexNumber(frameNumbers.size() - 1);
	}
	
	public void toggleLoop(boolean goFirst) {
		if (isLooping) stopLoop(goFirst);
		else startLoop(goFirst);
	}
	
	public void startLoop(boolean goFirst) {
//		if (goFirst) showFrameFirst();
        loopThread = new Thread(new Runnable() {
            public void run() {
                runLoop();
            }
        });
        loopThread.start();
        isLooping = true;
        updateRunButton();
	}
	
	public void stopLoop(boolean goFirst) {
		loopThread = null;
		isLooping = false;
		if (goFirst) showFrameFirst();
		updateRunButton();
	}
	
    private void runLoop() {
        try {
            Thread myThread = Thread.currentThread();
            while (myThread == loopThread) {
                long sleepTime = (long)loopDwell;
                showFrameNext();
                //Make sure we're sleeping for a minimum of 100ms
                if (sleepTime < 100) {
                    sleepTime = 100;
                }
                Misc.sleep(sleepTime);
            }
        } catch (Exception e) {
            LogUtil.logException("Loop animation: ", e);
        }
    }
	
	private void showIndexNumber(int inIndex) {
		if (inIndex < 0 || inIndex >= frameNumbers.size()) return;
		frameIndex = (Integer)inIndex;
		frameNumber = (Integer)frameNumbers.get(inIndex);
		indicator.setSelectedIndex(frameIndex);
		paintFrame();
	}
	
	public void showFrameNumber(int inFrame) {
		int inIndex = -1;
		for (int i=0; i<frameNumbers.size(); i++) {
			Integer frameInt = (Integer)frameNumbers.get(i);
			if (frameInt.intValue() == inFrame) {
				inIndex = (Integer)i;
				break;
			}
		}
		if (inIndex >= 0)
			showIndexNumber(inIndex);
		else
			System.err.println("showFrameNumber: " + inFrame + " is not a valid frame");
	}
	
	public int getFrameNumber() {
		return frameNumber.intValue();
	}
	
	private void paintFrame() {
		theImage = (Image)images.get("Frame " + frameNumber);
		if (theImage == null) {
			System.err.println("paintFrame: Got a null image for frame " + frameNumber);
			return;
		}
		
		MediaTracker mediaTracker = new MediaTracker(this);
		mediaTracker.addImage(theImage, frameNumber);
		try {
			mediaTracker.waitForID(frameNumber);
		} catch (InterruptedException ie) {
			System.err.println("MediaTracker exception: " + ie);
		}

		this.pi.setImage(theImage);
		this.pi.repaint();
	}
		
    /**
     * Make the value slider
     *
     * @return The slider button
     */
    private JComponent makeSlider() {
        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slide = (JSlider) e.getSource();
                if (slide.getValueIsAdjusting()) {
                    //                      return;
                }
                loopDwell = slide.getValue() * 100;
            }
        };
        JComponent[] comps = GuiUtils.makeSliderPopup(1, 50, loopDwell / 100, listener);
        comps[0].setToolTipText("Change dwell rate");
        return comps[0];
    }

    /**
     * Print the image
     */
/*
    public void doPrintImage() {
        try {
            toFront();
            PrinterJob printJob = PrinterJob.getPrinterJob();
            printJob.setPrintable(
                ((DisplayImpl) getMaster().getDisplay()).getPrintable());
            if ( !printJob.printDialog()) {
                return;
            }
            printJob.print();
        } catch (Exception exc) {
            logException("There was an error printing the image", exc);
        }
    }
*/
    
    /**
     * User has requested saving display as an image. Prompt
     * for a filename and save the image to it.
     */
    public void doSaveImageInThread() {
        Misc.run(this, "doSaveImage");
    }
    
    /**
     * Save the image
     */
    public void doSaveImage() {

        SecurityManager backup = System.getSecurityManager();
        System.setSecurityManager(null);
        try {
            if (hiBtn == null) {
                hiBtn  = new JRadioButton("High", true);
                medBtn = new JRadioButton("Medium", false);
                lowBtn = new JRadioButton("Low", false);
                GuiUtils.buttonGroup(hiBtn, medBtn).add(lowBtn);
            }
            JPanel qualityPanel = GuiUtils.vbox(new JLabel("Quality:"),
                                      hiBtn, medBtn, lowBtn);

            JComponent accessory = GuiUtils.vbox(Misc.newList(qualityPanel));

            List filters = Misc.newList(FileManager.FILTER_IMAGE);

            String filename = FileManager.getWriteFile(filters,
                                  FileManager.SUFFIX_JPG,
                                  GuiUtils.top(GuiUtils.inset(accessory, 5)));

            if (filename != null) {
                if (filename.endsWith(".pdf")) {
                    ImageUtils.writePDF(
                        new FileOutputStream(filename), this.pi);
                    System.setSecurityManager(backup);
                    return;
                }
                float quality = 1.0f;
                if (medBtn.isSelected()) {
                    quality = 0.6f;
                } else if (lowBtn.isSelected()) {
                    quality = 0.2f;
                }
                ImageUtils.writeImageToFile(theImage, filename, quality);
            }
        } catch (Exception e) {
        	System.err.println("doSaveImage exception: " + e);
        }
        // for webstart
        System.setSecurityManager(backup);

    }
    
    /**
     * User has requested saving display as a movie. Prompt
     * for a filename and save the images to it.
     */
    public void doSaveMovieInThread() {
        Misc.run(this, "doSaveMovie");
    }
    
    /**
     * Save the movie
     */
    public void doSaveMovie() {

        try {
        	Dimension size = new Dimension();
        	List theImages = new ArrayList(frameNumbers.size());
        	for (int i=0; i<frameNumbers.size(); i++) {
        		Integer frameInt = (Integer)frameNumbers.get(i);
        		theImages.add((Image)images.get("Frame " + frameInt));
        		if (size == null) {
            		int width = theImage.getWidth(null);
            		int height = theImage.getHeight(null);
            		size = new Dimension(width, height);
        		}
        	}
        	
        	//TODO: theImages should actually be a list of filenames that we have already saved
        	
            if (displayRateFld == null) {
            	displayRateFld = new JTextField("2", 3);
            }
            if (hiBtn == null) {
                hiBtn  = new JRadioButton("High", true);
                medBtn = new JRadioButton("Medium", false);
                lowBtn = new JRadioButton("Low", false);
                GuiUtils.buttonGroup(hiBtn, medBtn).add(lowBtn);
            }
            JPanel qualityPanel = GuiUtils.vbox(new JLabel("Quality:"),
                                      hiBtn, medBtn, lowBtn);
            JPanel ratePanel = GuiUtils.vbox(new JLabel("Frames per second:"),
                                      displayRateFld);

            JComponent accessory = GuiUtils.vbox(Misc.newList(qualityPanel,
            		new JLabel(" "), ratePanel));
            
            List filters = Misc.newList(FileManager.FILTER_MOV,
                    FileManager.FILTER_AVI, FileManager.FILTER_ANIMATEDGIF);

            String filename = FileManager.getWriteFile(filters,
                                  FileManager.SUFFIX_MOV,
                                  GuiUtils.top(GuiUtils.inset(accessory, 5)));
            
        	double displayRate =
                (new Double(displayRateFld.getText())).doubleValue();

            if (filename.toLowerCase().endsWith(".gif")) {
                double rate = 1.0 / displayRate;
                AnimatedGifEncoder.createGif(filename, theImages,
                        AnimatedGifEncoder.REPEAT_FOREVER,
                        (int) (rate * 1000));
            } else if (filename.toLowerCase().endsWith(".avi")) {
                ImageUtils.writeAvi(theImages, displayRate,
                                    new File(filename));
            } else {
                SecurityManager backup = System.getSecurityManager();
                System.setSecurityManager(null);
                JpegImagesToMovie.createMovie(filename, size.width,
                        size.height, (int) displayRate,
                        new Vector(theImages));
                System.setSecurityManager(backup);
            }
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad number format");
            return;
        } catch (IOException ioe) {
            LogUtil.userErrorMessage("Error writing movie: " + ioe);
            return;
        }

    }
  
}
