package edu.wisc.ssec.mcidasv.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class McIdasFrameDisplay extends JFrame {

	private Integer frameNumber = 1;
	private Integer frameIndex = 0;
	private List frameNumbers;
	private Hashtable images;
	private Image theImage;
	private JPanelImage pi;
	private JComboBox cb;
	
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
		JComboBox newCb = new JComboBox(frameNames);
		newCb.setEditable(false);
    	newCb.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent e) {
        		if (e.getStateChange() == e.SELECTED) {
	        		JComboBox myself = (JComboBox)e.getItemSelectable();
	        		showIndexNumber(myself.getSelectedIndex());
        		}
        	}
        });
		newCb.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent ke) {
				char keyChar = ke.getKeyChar();
				if (ke.isAltDown()) {
					if (keyChar == (char)'a') showFrameNext();
					else if (keyChar == (char)'b') showFramePrevious();
				}
            }
        });
    	this.cb = newCb;
    	
    	Dimension d = new Dimension(100, 50);
    	
        JButton buttonPrevious = new JButton("Previous");
        buttonPrevious.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	showFramePrevious();
            }
        });
        buttonPrevious.setPreferredSize(d);
    	
        JButton buttonNext = new JButton("Next");
        buttonNext.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	showFrameNext();
            }
        });
        buttonNext.setPreferredSize(d);
		
		setTitle(title);
		setLayout(new BorderLayout());
		getContentPane().add(buttonPrevious, BorderLayout.WEST);
		getContentPane().add(cb, BorderLayout.NORTH);
		getContentPane().add(buttonNext, BorderLayout.EAST);
		getContentPane().add(pi);
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
	
	private void showIndexNumber(int inIndex) {
		if (inIndex < 0 || inIndex >= frameNumbers.size()) return;
		frameIndex = (Integer)inIndex;
		frameNumber = (Integer)frameNumbers.get(inIndex);
		cb.setSelectedIndex(frameIndex);
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
  
}