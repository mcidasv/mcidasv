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

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.KEY_RENDERING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC;
import static java.awt.RenderingHints.VALUE_RENDER_QUALITY;
import static java.awt.geom.AffineTransform.getScaleInstance;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * Extend JPanel to draw an optionally anti-aliased image filling the panel.
 */
public class JPanelImage extends JPanel {

    /** Image to draw within the {@code JPanel}. May be {@code null}. */
    private Image theImage;

    /** Whether or not anti-aliasing is being used. */
    private boolean antiAlias;

    /**
     * Create a {@code JPanelImage} without specifying an {@link Image}. Note
     * that this sets {@link #antiAlias} to {@code true}.
     */
    public JPanelImage() {
        antiAlias = true;
    }

    /**
     * Create a {@code JPanelImage} that contains the specified {@link Image}.
     * Note that this sets {@link #antiAlias} to {@code true}.
     *
     * @param panelImage {@code Image} to use within the created panel.
     * {@code null} is allowed.
     */
    public JPanelImage(Image panelImage) {
        antiAlias = true;
        theImage = panelImage;
    }

    /**
     * Determine whether or not this {@code JPanelImage} is using
     * anti-aliasing.
     *
     * @return {@code true} if anti-aliasing is enabled, {@code false}
     * otherwise. Default value is {@code true}.
     */
    public boolean getAntiAlias() {
        return antiAlias;
    }

    /**
     * Set whether or not this {@code JPanelImage} should use anti-aliasing.
     *
     * @param setAA {@code true} if anti-aliasing should be enabled,
     * {@code false} otherwise.
     */
    public void setAntiAlias(boolean setAA) {
        antiAlias = setAA;
    }

    /**
     * Return the {@link Image} being used within this {@code JPanelImage}.
     *
     * @return {@code Image} being used, or {@code null} if the {@code Image}
     * has not been provided.
     */
    public Image getImage() {
        return theImage;
    }

    /**
     * Set the {@link Image} to use within this {@code JPanelImage}.
     *
     * @param panelImage {@code Image} to use. {@code null} is permitted.
     */
    public void setImage(Image panelImage) {
        theImage = panelImage;
    }

    /**
     * Update the panel after a change. Note that this is the same as calling
     * {@link #paint(java.awt.Graphics)}.
     *
     * @param g Specified graphics window. Should not be {@code null}.
     */
    @Override public void update(Graphics g) {
        paint(g);
    }

    /**
     * Paint the {@code JPanelImage}.
     *
     * @param g Specified graphics window. Should not be {@code null}.
     */
    @Override public void paint(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        if (antiAlias) {
            BufferedImage newImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            double scaleX = theImage.getWidth(null) / (double)width;
            double scaleY = theImage.getHeight(null) / (double)height;
            double scaleXY = 1.0 / Math.max(scaleX, scaleY);
            Graphics2D g2d = newImage.createGraphics();
            g2d.setBackground(Color.black);
            g2d.clearRect(0, 0, width, height);
            RenderingHints hints = new RenderingHints(null);
            hints.put(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
            hints.put(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
            hints.put(KEY_RENDERING, VALUE_RENDER_QUALITY);
            g2d.setRenderingHints(hints);
            g2d.drawImage(theImage, getScaleInstance(scaleXY, scaleXY), null);
            g.drawImage(newImage, 0, 0, null);
        } else {
            g.drawImage(theImage, 0, 0, width, height, null);
        }
    }
}
