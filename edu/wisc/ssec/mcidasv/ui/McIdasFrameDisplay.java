package edu.wisc.ssec.mcidasv.ui;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class McIdasFrameDisplay extends JFrame {

	private Integer frameNumber = 1;
	private List frameNumbers;
	private Hashtable images;
	private Image theImage;
	private JPanelImage p;
	
	public McIdasFrameDisplay(String title, List frameNumbers) {
		if (frameNumbers.size()<1) return;
		this.frameNumbers = frameNumbers;
		this.frameNumber = (Integer)frameNumbers.get(0);
		this.images = new Hashtable(frameNumbers.size());
		this.p = new JPanelImage();
		setTitle(title);
		getContentPane().add(p);
	}
	
	public void setFrameImage(int inFrame, Image inImage) {
		images.put("Frame " + inFrame, inImage);
	}
	
	public void showFrameNumber(int inFrame) {
		boolean inList = false;
		for (int i=0; i<frameNumbers.size(); i++) {
			Integer frameInt = (Integer)frameNumbers.get(i);
			if (frameInt.intValue() == inFrame) {
				inList = true;
				break;
			}
		}
		if (!inList) {
			System.err.println("showFrameNumber: " + inFrame + " is not a valid frame");
			return;
		}
		frameNumber = (Integer)inFrame;
		paintFrame();
		this.setSize(p.getWidth() + 20, p.getHeight() + 40);
//		pack();
		if (!isVisible()) {
			setVisible(true);
			toFront();
		}
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
		this.p.setSize(width, height);
		this.p.repaint();
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