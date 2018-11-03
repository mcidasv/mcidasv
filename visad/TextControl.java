//
// TextControl.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2018 Bill Hibbard, Curtis Rueden, Tom
Rink, Dave Glowacki, Steve Emmerson, Tom Whittaker, Don Murray, and
Tommy Jasmin.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
MA 02111-1307, USA
*/

package visad;

import java.awt.Font;
import java.rmi.RemoteException;
import java.text.NumberFormat;

import visad.util.HersheyFont;
import visad.util.Util;

/**
   TextControl is the VisAD class for controlling Text display scalars.<P>
*/
public class TextControl extends Control {

  private Object font = null;

  // abcd 5 February 2001
  //private boolean center = false;

  private double size = 1.0;

  private double factor = 1.0;

  private double autoSizeFactor = 1.0;

  // WLH 31 May 2000
  // draw on sphere surface
  private boolean sphere = false;

  private NumberFormat format = null;

  // abcd 1 February 2001
  // Rotation, in degrees, clockwise along positive x axis
  private double rotation = 0.0;
  // SL  15 March 2003
  private double characterRotation = 0.0;   // characterRotation
  private double scale = 1.0;  // Scaling factor
  private double[] offset = new double[]{0.0, 0.0, 0.0};

  // WLH 6 Aug 2001
  private boolean autoSize = false;
  private ProjectionControlListener pcl = null;

  /**
   * A class to represent the different types of justification.
   * Use a class so the user can't just pass in an arbitrary integer
   *
   * @author abcd 5 February 2001
   */
  public static class Justification {
    private String name;

    /** Predefined value for left justification */
    public static final Justification LEFT = new Justification("Left");

    /** Predefined value for center justification */
    public static final Justification CENTER = new Justification("Center");

    /** Predefined value for right justification */
    public static final Justification RIGHT = new Justification("Right");

    /** Predefined value for top justification */
    public static final Justification TOP = new Justification("Top");

    /** Predefined value for bottom justification */
    public static final Justification BOTTOM = new Justification("Bottom");

    /**
     * Constructor - simply store the name
     */
    public Justification(String newName)
    {
      name = newName;
    }
  }
  private Justification justification = Justification.LEFT;
  private Justification verticalJustification = Justification.BOTTOM;

  public TextControl(DisplayImpl d) {
    super(d);
  }

  public void setAutoSize(boolean auto)
         throws VisADException {
    if (auto == autoSize) return;
    DisplayImpl display = getDisplay();
    DisplayRenderer dr = display.getDisplayRenderer();
    MouseBehavior mouse = dr.getMouseBehavior();
    ProjectionControl pc = display.getProjectionControl();
    if (auto) {
      if (pcl == null) {
        pcl = new ProjectionControlListener(mouse, this, pc);
        pc.addControlListener(pcl);
      }
    }
    if (pcl != null) {
      pcl.setActive(auto);
    }
    /*
    else {
      pc.removeControlListener(pcl);
    }
    */
    autoSize = auto;
    try {
      changeControl(true);
    }
    catch (RemoteException e) {
    }
  }

  public boolean getAutoSize() {
    return autoSize;
  }

  public void nullControl() {
    try {
      setAutoSize(false);
    }
    catch (VisADException e) {
    }
    super.nullControl();
  }

  /** set the font; in the initial release this has no effect
  *
  * @param f is the java.awt.Font or the visad.util.HersheyFont
  */

  public void setFont(Object f)
         throws VisADException, RemoteException {
    if (f instanceof java.awt.Font ||
        f instanceof visad.util.HersheyFont ||
        f == null) {
      font = f;
      changeControl(true);
    } else {
      throw new VisADException("Font must be java.awt.Font or HersheyFont");
    }

  }

  /** return the java.awt.Font
  *
  * @return the java.awt.Font if the font is of that type; otherwise, null
  */
  public Font getFont() {
    if (font instanceof java.awt.Font) {
      return (Font) font;
    } else {
      return null;
    }
  }

  /** return the HersheyFont
  *
  * @return the visad.util.HersheyFont if the font is of
  * that type; otherwise, null
  */
  public HersheyFont getHersheyFont() {
    if (font instanceof visad.util.HersheyFont) {
      return (HersheyFont) font;
    } else {
      return null;
    }
  }

  /** set the centering flag; if true, text will be centered at
      mapped locations; if false, text will be to the right
      of mapped locations */
  public void setCenter(boolean c)
         throws VisADException, RemoteException {
    // abcd 5 February 2001
    justification = Justification.CENTER;
    //center = c;
    changeControl(true);
  }

// TODO: Deprecate this?
  /** return the centering flag */
  public boolean getCenter() {
    // abcd 5 February 2001
    //return center;
    if (justification == Justification.CENTER) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Set the justification flag
   *
   * Possible values are TextControl.Justification.LEFT,
   * TextControl.Justification.CENTER and TextControl.Justification.RIGHT
   */
  // abcd 5 February 2001
  public void setJustification(Justification newJustification)
         throws VisADException, RemoteException
  {
    // Store the new value
    justification = newJustification;

    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Return the justification value
   */
  // abcd 5 February 2001
  public Justification getJustification()
  {
    return justification;
  }

  /**
   * Set the vertical justification flag
   *
   * Possible values are TextControl.Justification.TOP,
   * TextControl.Justification.CENTER and TextControl.Justification.BOTTOM
   * 
   */
  public void setVerticalJustification(Justification newJustification)
         throws VisADException, RemoteException
  {
    // Store the new value
    verticalJustification = newJustification;

    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Return the vertical justification value
   *
   */
  public Justification getVerticalJustification()
  {
    return verticalJustification;
  }

  /** set the size of characters; the default is 1.0 */
  public void setSize(double s)
         throws VisADException, RemoteException {
    factor = s;
    size = factor*autoSizeFactor;
    changeControl(true);
  }

  private void setSizeForAuto(double autoSizeFactor)
          throws VisADException, RemoteException {
    this.autoSizeFactor = autoSizeFactor;
    size = factor*autoSizeFactor;
    changeControl(true);
  }
  
  /** return the size */
  public double getSize() {
    return size;
  }

  // WLH 31 May 2000
  public void setSphere(boolean s)
         throws VisADException, RemoteException {
    sphere = s;
    changeControl(true);
  }

  // WLH 31 May 2000
  public boolean getSphere() {
    return sphere;
  }

  // WLH 16 June 2000
  public void setNumberFormat(NumberFormat f)
         throws VisADException, RemoteException {
    format = f;
    changeControl(true);
  }

  // WLH 16 June 2000
  public NumberFormat getNumberFormat() {
    return format;
  }

  private boolean fontEquals(Object newFont)
  {
    if (font == null) {
      if (newFont != null) {
        return false;
      }
    } else if (newFont == null) {
      return false;
    } else if (!font.equals(newFont)) {
      return false;
    }

    return true;
  }

  // WLH 16 June 2000
  private boolean formatEquals(NumberFormat newFormat)
  {
    if (format == null) {
      if (newFormat != null) {
        return false;
      }
    } else if (newFormat == null) {
      return false;
    } else if (!format.equals(newFormat)) {
      return false;
    }

    return true;
  }

  /**
   * Set the rotation
   *
   * abcd 1 February 2001
   */
  public void setRotation(double newRotation)
         throws VisADException, RemoteException
  {
    // Store the new rotation
    rotation = newRotation;
    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Get the rotation
   *
   * abcd 1 February 2001
   */
  public double getRotation()
  {
    return rotation;
  }

  /** get a string that can be used to reconstruct this control later */
  public String getSaveString() {
    return null;
  }

  /** reconstruct this control using the specified save string */
  public void setSaveString(String save)
    throws VisADException, RemoteException
  {
    throw new UnimplementedException(
      "Cannot setSaveString on this type of control");
  }

  /** copy the state of a remote control to this control */
  public void syncControl(Control rmt)
    throws VisADException
  {
    if (rmt == null) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with null Control object");
    }

    if (!(rmt instanceof TextControl)) {
      throw new VisADException("Cannot synchronize " + getClass().getName() +
                               " with " + rmt.getClass().getName());
    }

    TextControl tc = (TextControl )rmt;

    boolean changed = false;

    if (!fontEquals(tc.font)) {
      changed = true;
      font = tc.font;
    }

    // abcd 5 February 2001
    //if (center != tc.center) {
    //  changed = true;
    //  center = tc.center;
    //}
    if (justification != tc.justification) {
      changed = true;
      justification = tc.justification;
    }
    // abcd 19 February 2003
    if (verticalJustification != tc.verticalJustification) {
      changed = true;
      verticalJustification = tc.verticalJustification;
    }

    if (!Util.isApproximatelyEqual(size, tc.size)) {
      changed = true;
      size = tc.size;
    }

    // WLH 31 May 2000
    if (sphere != tc.sphere) {
      changed = true;
      sphere = tc.sphere;
    }

    // WLH 16 June 2000
    if (!formatEquals(tc.format)) {
      changed = true;
      format = tc.format;
    }

    // abcd 1 February 2001
    if (!Util.isApproximatelyEqual(rotation, tc.rotation)) {
      changed = true;
      rotation = tc.rotation;
    }

    // SL 16 July 2003
    if (!Util.isApproximatelyEqual(characterRotation, tc.characterRotation)) {
      changed = true;
      characterRotation = tc.characterRotation;
    }

    // WLH 6 Aug 2001
    if (autoSize != tc.autoSize) {
      // changed = true;
      setAutoSize(tc.autoSize);
    }

    // SL 16 July 2003
    if (!Util.isApproximatelyEqual(scale, tc.scale)) {
      changed = true;
      scale = tc.scale;
    }

    // SL 16 July 2003
    for (int i=0; i<3; i++) {
      if (!Util.isApproximatelyEqual(offset[i], tc.offset[i])) {
        changed = true;
        offset[i] = tc.offset[i];
      }
    }

    if (changed) {
      try {
        changeControl(true);
      } catch (RemoteException re) {
        throw new VisADException("Could not indicate that control" +
                                 " changed: " + re.getMessage());
      }
    }
  }

  public boolean equals(Object o)
  {
    if (!super.equals(o)) {
      return false;
    }

    TextControl tc = (TextControl )o;

    if (!fontEquals(font)) {
      return false;
    }

    // abcd 5 February 2001
    //if (center != tc.center) {
    if (justification != tc.justification) {
      return false;
    }
    // abcd 19 March 2003
    if (verticalJustification != tc.verticalJustification) {
      return false;
    }

    // WLH 31 May 2000
    if (sphere != tc.sphere) {
      return false;
    }

    // WLH 16 June 2000
    if (!formatEquals(tc.format)) {
      return false;
    }

    // abcd 1 February 2001
    if (!Util.isApproximatelyEqual(rotation, tc.rotation)) {
      return false;
    }

    if (!Util.isApproximatelyEqual(size, tc.size)) {
      return false;
    }

    // WLH 6 Aug 2001
    if (autoSize != tc.autoSize) {
      return false;
    }

    // SL 18 July 2003
    if (!Util.isApproximatelyEqual(characterRotation, tc.characterRotation)) {
      return false;
    }

    // SL 18 July 2003
    if (!Util.isApproximatelyEqual(scale, tc.scale)) {
      return false;
    }

    // SL 18 July 2003
    for (int i=0; i<3; i++) {
      if (!Util.isApproximatelyEqual(offset[i], tc.offset[i])) {
        return false;
      }
    }

    return true;
  }

  /**
   * Gets the value of characterRotation
   *
   * @return the value of characterRotation
   */
  public double getCharacterRotation()  {
    return this.characterRotation;
  }

  /**
   * Sets the value of characterRotation
   *
   * @param argCharacterRotation Value to assign to this.characterRotation
   */
  public void setCharacterRotation(double argCharacterRotation) throws
    VisADException, RemoteException
  {
    this.characterRotation = argCharacterRotation;
    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Gets the value of scale
   *
   * @return the value of scale
   */
  public double getScale()  {
    return this.scale;
  }

  /**
   * Sets the value of scale
   *
   * @param argScale Value to assign to this.scale
   */
  public void setScale(double argScale)
    throws VisADException, RemoteException {
    this.scale = argScale;
    // Tell the control it's changed
    changeControl(true);
  }

  /**
   * Gets the value of offset
   *
   * @return the value of offset
   */
  public double[] getOffset()  {
    double[] aOffset = new double[]{
      this.offset[0], this.offset[1], this.offset[2]};
    return aOffset;
  }

  /**
   * Sets the value of offset
   *
   * @param argOffset Value to assign to this.offset
   */
  public void setOffset(double[] argOffset)
    throws VisADException, RemoteException {
    this.offset[0] = argOffset[0];
    this.offset[1] = argOffset[1];
    this.offset[2] = argOffset[2];
    // Tell the control it's changed
    changeControl(true);
  }

  class ProjectionControlListener implements ControlListener {
    private boolean pfirst = true;
    private MouseBehavior mouse;
    private ProjectionControl pcontrol;
    private TextControl text_control;
    private double base_scale = 1.0;
    private float last_cscale = 1.0f;
    private boolean active = false;

    ProjectionControlListener(MouseBehavior m, TextControl t,
                              ProjectionControl p) {
      mouse = m;
      text_control = t;
      pcontrol = p;
    }
    
    public void setActive(boolean onoroff) {
      active = onoroff;
    }

    public void controlChanged(ControlEvent e)
           throws VisADException, RemoteException {
      if (!active) return;
      double[] matrix = pcontrol.getMatrix();
      double[] rot = new double[3];
      double[] scale = new double[3];
      double[] trans = new double[3];
      mouse.instance_unmake_matrix(rot, scale, trans, matrix);

      if (pfirst) {
        pfirst = false;
        base_scale = scale[2];
        last_cscale = 1.0f;
      }
      else {
        float cscale = (float) (base_scale / scale[2]);
        float ratio = cscale / last_cscale;
        if (ratio < 0.95f || 1.05f < ratio) {
          last_cscale = cscale;
          text_control.setSizeForAuto(cscale);
        }
      }
    }
  }
}
