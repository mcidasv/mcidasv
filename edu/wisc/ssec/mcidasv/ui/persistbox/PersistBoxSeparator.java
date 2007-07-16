package edu.wisc.ssec.mcidasv.ui.persistbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * A PersistBoxSeparator is an item that is placed inside a {@link PersistBox},
 * much in the same way as a {@link PersistBoxItem} or 
 * {@link PersistBoxCustomize} object. The separators differ in that 
 * JComponent's <code>paintComponent</code> method is overridden and will draw 
 * a line in the separator's combo box position.
 * 
 * @author Jonathan Beavers, SSEC
 */
public class PersistBoxSeparator extends PersistBoxItem {

	/** The (current) preferred size of the current separator.*/
	private Dimension preferredSize = new Dimension(5, 7);
	
	/** The color of the line drawn by <code>paintComponent</code>. */
	private Color color = Color.GRAY;

	/** The y coordinate for the line drawn by <code>paintComponent</code>. */
	private final int POSITION = 4;
	
	/** 
	 * This is an object that occupies an entry within a given PersistBox and 
	 * that displays a line ("separator") when called to paint itself in
	 * PersistBox.createRenderer(). setSeparator() is called so that the 
	 * instance immediately knows what it is.
	 * 
	 * @param box The PersistBox that this instance belongs to.
	 * @param idx The index of the current instance within the given PersistBox.
	 */
	public PersistBoxSeparator(PersistBox box, int idx) {
		index = idx;
		value = this; // TODO: maybe go back to "LINESEP"
		setOpaque(false);
		
		setSeparator();
	}
	
	/**
	 * Here's where the separator magic happens. It's amazing, honestly. It
	 * sets the color of the incoming Graphics object, and then DRAWS A LINE...
	 * 
	 * @param g The graphics context to paint.
	 */
	public void paintComponent(Graphics g) {
		g.setColor(color);
		g.drawLine(0, POSITION, getWidth(), POSITION);
	}
	
	/**
	 * @return The current preferred size for this separator.
	 */
	public Dimension getPreferredSize() {
		return preferredSize;
	}

	/**
	 * @param newSize The new preferred size of this separator.
	 */
	public void setPreferredSize(Dimension newSize) {
		preferredSize = newSize;
	}
	
	/**
	 * @return The current color of this separator.
	 */
	public Color getColor() {
		return color;
	}
	
	/**
	 * @param c The new color for this separator.
	 */
	public void setColor(Color c) {
		color = c;
	}
}


