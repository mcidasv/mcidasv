package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Resource;

public class McIdasFrameDisplay extends JFrame implements ActionListener {
	
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

	private Integer frameNumber = 1;
	private Integer frameIndex = 0;
	private List frameNumbers;
	private Hashtable images;
	private Image theImage;
	private JPanelImage pi;
	private JComboBox indicator;
	
    private Thread loopThread;
    private boolean isLooping = false;
    private int loopDwell = 500;
	
	public McIdasFrameDisplay(String title, List frameNumbers) {
		if (frameNumbers.size()<1) return;
		this.frameIndex = 0;
		this.frameNumbers = frameNumbers;
		this.frameNumber = (Integer)frameNumbers.get(this.frameIndex);
		this.images = new Hashtable(frameNumbers.size());
		this.pi = new JPanelImage();
		this.pi.setFocusable(true);
		
		String[] frameNames = new String[frameNumbers.size()];
		for (int i=0; i<frameNumbers.size(); i++) {
			frameNames[i] = "Frame " + (Integer)frameNumbers.get(i);
		}
		indicator = new JComboBox(frameNames);
        indicator.setFont(new Font("Dialog", Font.PLAIN, 9));
        indicator.setLightWeightPopupEnabled(false);
        // set to non-visible until items are added
        indicator.setVisible(true);
        indicator.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	showIndexNumber(indicator.getSelectedIndex());
            }
        });
        
		setTitle(title);
		setLayout(new BorderLayout());
		getContentPane().add(GuiUtils.center(doMakeContents()), BorderLayout.NORTH);
		getContentPane().add(pi);
	}
	
    /**
     * Make the UI for this widget.
     *
     * @return  UI as a Component
     */
    private JComponent doMakeContents() {
        KeyListener listener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                char c    = e.getKeyChar();
				if (e.isAltDown()) {
					if (c == (char)'a') showFrameNext();
					else if (c == (char)'b') showFramePrevious();
					else if (c == (char)'l') toggleLoop();
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
        	toggleLoop();
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
	
	public void toggleLoop() {
		if (isLooping) stopLoop();
		else startLoop();
	}
	
	public void startLoop() {
        loopThread = new Thread(new Runnable() {
            public void run() {
                runLoop();
            }
        });
        loopThread.start();
        isLooping = true;
        updateRunButton();
	}
	
	public void stopLoop() {
		loopThread = null;
		isLooping = false;
		showFrameFirst();
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
		pack();
		if (!isVisible()) {
			setVisible(true);
			toFront();
		}
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

		int width = theImage.getWidth(null);
		int height = theImage.getHeight(null);
		Dimension d = new Dimension(width, height);
		this.pi.setSize(d);
		this.pi.setPreferredSize(d);
		this.pi.repaint();
	}
	
	public class JPanelImage extends JPanel {
		public JPanelImage() { }
		public void update(Graphics g) {
			paint(g);
		}
		public void paint(Graphics g) {
			g.drawImage(theImage, 0, 0, null);
		}
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
        comps[0].setToolTipText("Change the dwell rate");
        return comps[0];
    }
  
}