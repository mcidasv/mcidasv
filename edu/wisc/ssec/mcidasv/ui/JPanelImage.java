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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Extend JPanel to draw an optionally anti-aliased image filling the panel
 */
public class JPanelImage extends JPanel {
	
	// The image to draw in the panel
	private Image theImage;
	
	// Use anti-aliasing
	private boolean aa = true;
	
	public JPanelImage() {}
	
	public JPanelImage(Image panelImage) {
		theImage = panelImage;
	}
	
	public boolean getAntiAlias() {
		return aa;
	}
	
	public void setAntiAlias(boolean setAA) {
		aa = setAA;
	}
	
	public Image getImage() {
		return theImage;
	}
	
	public void setImage(Image panelImage) {
		theImage = panelImage;
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	
	@Override
	public void paint(Graphics g) {
		if (aa) {
			BufferedImage newImage = new BufferedImage(
					this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_RGB);
			double scaleX = (double)theImage.getWidth(null) / (double)this.getWidth();
			double scaleY = (double)theImage.getHeight(null) / (double)this.getHeight();
			double scaleXY = 1.0 / (Math.max(scaleX, scaleY));
			Graphics2D g2d = newImage.createGraphics();
			g2d.setBackground(Color.black);
			g2d.clearRect(0, 0, this.getWidth(), this.getHeight());

			RenderingHints hints = new RenderingHints(null);
			hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			hints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHints(hints);

			g2d.drawImage(theImage, AffineTransform.getScaleInstance(scaleXY, scaleXY), null);
			g.drawImage(newImage, 0, 0, null);
		}
		else {
			g.drawImage(theImage, 0, 0, this.getWidth(), this.getHeight(), null);
		}
	}
}
