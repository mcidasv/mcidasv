package edu.wisc.ssec.mcidasv.ui;

import java.*;
import java.awt.*;
import javax.swing.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

public class McIdasFrameDisplay extends JFrame {

	Image img;
	Canvas c;

	public McIdasFrameDisplay(int frameNumber, Image i) {
		
		super.setTitle("McIDAS-X Frame " + frameNumber);
		img = i;
		
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
		System.out.println("Image resolution: " + width + "x" + height);
		c = new Canvas() {
			public void update(Graphics g) {paint(g);}
			public void paint(Graphics g) {
				g.drawImage(img,0,0,this);
			}
		};
		c.setSize(width, height);
	    add(c);
	    
		pack();
	    super.setVisible(true);
	}
  
}