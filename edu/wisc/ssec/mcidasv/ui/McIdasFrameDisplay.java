package edu.wisc.ssec.mcidasv.ui;

import java.*;
import java.awt.*;
import javax.swing.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

public class McIdasFrameDisplay extends JFrame {

	private Integer frameNumber;
	private Image img;
	private Canvas c;

	public McIdasFrameDisplay(int frame, Image i) {
		
		frameNumber = (Integer)frame;
		img = i;
		
		super.setTitle("McIDAS-X Frame " + frameNumber);
		paintImage();
		
	    super.setVisible(true);
	}
	
	private void paintImage() {
		MediaTracker mediaTracker = new MediaTracker(this);
		mediaTracker.addImage(img, 0);
		try
		{
			mediaTracker.waitForID(0);
		}
		catch (InterruptedException ie)
		{
			System.err.println(ie);
			System.exit(1);
		}

		int width = img.getWidth(null);
		int height = img.getHeight(null);
		c = new Canvas() {
			public void update(Graphics g) {paint(g);}
			public void paint(Graphics g) {
				g.drawImage(img,0,0,this);
			}
		};
		c.setSize(width, height);
	    add(c);
	    
		pack();
	}
	
	public int getFrameNumber() {
		return frameNumber.intValue();
	}
	
	public Image getImage() {
		return img;
	}
	
	public void setImage(Image i) {
		img = i;
		paintImage();
	}
  
}