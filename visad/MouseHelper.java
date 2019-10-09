//
// MouseHelper.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2019 Bill Hibbard, Curtis Rueden, Tom
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

import java.awt.event.*;

import java.rmi.*;
import java.awt.*;

import visad.browser.Convert;

/**
   MouseHelper is the VisAD helper class for MouseBehaviorJ3D
   and MouseBehaviorJ2D.<p>

   MouseHelper is preferred by cats everywhere.<p>
*/
public class MouseHelper
  implements RendererSourceListener
{



  MouseBehavior behavior;

  /** DisplayRenderer for Display */
  DisplayRenderer display_renderer;

  /** Display */
  DisplayImpl display;

  /** ProjectionControl for Display */
  private ProjectionControl proj;

  /** DataRenderer for direct manipulation */
  protected DataRenderer direct_renderer = null;

  /** matrix from ProjectionControl when mousePressed1 (left) */
  protected double[] tstart;

  /** screen location when mousePressed1 or mousePressed3 */
  protected int start_x, start_y;
  protected double xmul, ymul;
  protected double xymul;
  //protected double[] xtrans = new double[3];
  //protected double[] ytrans = new double[3];

  /** mouse in window (not used) */
  private boolean mouseEntered;

  /** ((InputEvent) event).getModifiers() when mouse pressed */
  protected int mouseModifiers;

  /** flag for 2-D mode */
  private boolean mode2D;

// start of variables for table-driven mapping from mouse buttons and
//     keys to functons

  // index values for functions
  public static final int NONE = -1, ROTATE = 0, ZOOM = 1, TRANSLATE = 2,
    CURSOR_TRANSLATE = 3, CURSOR_ZOOM = 4, CURSOR_ROTATE = 5, DIRECT = 6;

  /* Number of functions */
  //protected int NFUNCTIONS = 7;

  // index values for mouse buttons
  public static final int LEFT = 0, CENTER = 1, RIGHT = 2;

  // actual mouse buttons pressed
  protected boolean[] actual_button = {false, false, false};

  // mouse button pressed accounting for combos
  protected int virtual_button = -1;

  // array of enables for functions
  protected boolean[] function = {false, false, false, false, false, false, false};

  // save previous function to compute function change
  protected boolean[] old_function = {false, false, false, false, false, false, false};

  // enable any two mouse buttons = the third
  private boolean enable_combos = true;

  // mapping from buttons/keys to function
  //   function_map[button][CTRL][SHIFT] where
  //   button = 0, 1, 2
  //   CTRL = 0, 1
  //   SHIFT = 0, 1
  // initialize with defaults
  int[][][] function_map =
    {{{ROTATE, ZOOM}, {TRANSLATE, NONE}},
     {{CURSOR_TRANSLATE, CURSOR_ZOOM}, {CURSOR_ROTATE, NONE}},
     {{DIRECT, DIRECT}, {DIRECT, DIRECT}}};

// end of variables for table-driven mapping from mouse buttons
//     and keys to functons

  public MouseHelper(DisplayRenderer r, MouseBehavior b) {
    behavior = b;
    display_renderer = r;
    display = display_renderer.getDisplay();
    proj = display.getProjectionControl();
    mode2D = display_renderer.getMode2D();

    // track Display's DataRenderers in case direct_renderer is removed
    display.addRendererSourceListener(this);

    //function =
    //  new boolean[] {false, false, false, false, false, false, false};
    //enable_combos = true;
  }

  /** 
   * Get the MouseBehavior for this Helper
   * @return MouseBehavior
   */
  public MouseBehavior getMouseBehavior() { return behavior; }

  /** 
   * Get the Display for this Helper
   * @return Display
   */
  public DisplayImpl getDisplay() { return display; }

  /** 
   * Get the DisplayRenderer for this Helper
   * @return DisplayRenderer
   */
  public DisplayRenderer getDisplayRenderer() { return display_renderer; }

  /** 
   * Get the ProjectionControl for this Helper
   * @return ProjectionControl
   */
  public ProjectionControl getProjectionControl() { return proj; }

  /** 
   * Get whether we're in 2D mode
   * @return true if 2D mode
   */
  public boolean getMode2D() { return mode2D; }

  /**
   * Process the given event treating it as a local event.
   * @param event event to process.
   */
  public void processEvent(AWTEvent event) {
    processEvent(event, VisADEvent.LOCAL_SOURCE);
  }

  /** 
   * Process the given event, treating it as coming from a remote source
   *  if remote flag is set.
   * @param event event to process.
   * @param remoteId  id of remote source.
   */
  public void processEvent(AWTEvent event, int remoteId) {

    if (getMouseBehavior() == null) return;

    if (event instanceof MouseEvent &&
        ((MouseEvent) event).getID() == MouseEvent.MOUSE_PRESSED) {
      start_x = 0;
      start_y = 0;
      setTranslationFactor(start_x, start_y);
    }

    if (!(event instanceof MouseEvent)) {
      System.out.println("MouseHelper.processStimulus: non-MouseEvent");
      return;
    }

    MouseEvent mouse_event = (MouseEvent) event;

    int event_id = event.getID();

    if (event_id == MouseEvent.MOUSE_ENTERED) {
      mouseEntered = true;
      try {
        DisplayEvent e = new DisplayEvent(getDisplay(),
          DisplayEvent.MOUSE_ENTERED, mouse_event, remoteId);
        getDisplay().notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_EXITED) {
      mouseEntered = false;
      try {
        DisplayEvent e = new DisplayEvent(getDisplay(),
          DisplayEvent.MOUSE_EXITED, mouse_event, remoteId);
        getDisplay().notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_MOVED) {
      try {
        DisplayEvent e = new DisplayEvent(getDisplay(),
          DisplayEvent.MOUSE_MOVED, mouse_event, remoteId);
        getDisplay().notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
      return;
    }
    else if (event_id == MouseEvent.MOUSE_PRESSED ||
             event_id == MouseEvent.MOUSE_RELEASED) {
      int m = ((InputEvent) event).getModifiers();
      int m1 = m & InputEvent.BUTTON1_MASK;
      int m2 = m & InputEvent.BUTTON2_MASK;
      int m3 = m & InputEvent.BUTTON3_MASK;
      int mctrl = m & InputEvent.CTRL_MASK;
      int mshift = m & InputEvent.SHIFT_MASK;

      if (event_id == MouseEvent.MOUSE_PRESSED) {
        getDisplay().updateBusyStatus();
        if (m1 != 0) {
          actual_button[LEFT] = true;
        }
        if (m2 != 0) {
          actual_button[CENTER] = true;
        }
        if (m3 != 0) {
          actual_button[RIGHT] = true;
        }
        mouseModifiers = m;
      }
      else { // event_id == MouseEvent.MOUSE_RELEASED
        getDisplay().updateBusyStatus();
        if (m1 != 0) {
          actual_button[LEFT] = false;
        }
        if (m2 != 0) {
          actual_button[CENTER] = false;
        }
        if (m3 != 0) {
          actual_button[RIGHT] = false;
        }
        mouseModifiers = 0;
      }

      // compute button combos
      int n = 0, sum = 0;
      for (int i=0; i<3; i++) {
        if (actual_button[i]) {
          n++;
          sum += i;
        }
      }
      if (n == 1) {
        virtual_button = sum;
      }
      else if (n == 2 && getEnableCombos()) {
        virtual_button = 3 - sum;
      }
      else { // n == 0 || n == 3 || (n == 2 && !getEnableCombos())
        virtual_button = -1;
      }
  
      // compute old and new functions
      for (int i=0; i<function.length; i++) {
        old_function[i] = function[i];
        function[i] = false;
      }

      int vctrl = (mctrl == 0) ? 0 : 1;
      int vshift = (mshift == 0) ? 0 : 1;
      int f = (virtual_button < 0) ? -1 :
              function_map[virtual_button][vctrl][vshift];

      if (f >= 0) function[f] = true;

      boolean cursor_off = enableFunctions((MouseEvent) event);

      if (event_id == MouseEvent.MOUSE_PRESSED) {
        try {
          DisplayEvent e = new DisplayEvent(getDisplay(),
            DisplayEvent.MOUSE_PRESSED, mouse_event, remoteId);
          getDisplay().notifyListeners(e);
        }
        catch (VisADException e) {
        }
        catch (RemoteException e) {
        }
        if (m1 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_PRESSED_LEFT, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m2 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_PRESSED_CENTER, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m3 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_PRESSED_RIGHT, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
      }
      else { // event_id == MouseEvent.MOUSE_RELEASED
        try {
          DisplayEvent e = new DisplayEvent(getDisplay(),
            DisplayEvent.MOUSE_RELEASED, mouse_event, remoteId);
          getDisplay().notifyListeners(e);
        }
        catch (VisADException e) {
        }
        catch (RemoteException e) {
        }
        if (m1 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_RELEASED_LEFT, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m2 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_RELEASED_CENTER, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
        if (m3 != 0) {
          try {
            DisplayEvent e = new DisplayEvent(getDisplay(),
              DisplayEvent.MOUSE_RELEASED_RIGHT, mouse_event, remoteId);
            getDisplay().notifyListeners(e);
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
      }
      if (cursor_off) getDisplayRenderer().setCursorOn(false);
    }
    else if (event_id == MouseEvent.MOUSE_DRAGGED) {
      handleMouseDragged(mouse_event, remoteId);
      try {
        DisplayEvent e = new DisplayEvent(getDisplay(),
          DisplayEvent.MOUSE_DRAGGED, mouse_event, remoteId);
        getDisplay().notifyListeners(e);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
    }

  }

  protected void handleMouseDragged(MouseEvent event, int remoteId) {
      MouseBehavior mouseBehavior = getMouseBehavior();
      boolean cursor = function[CURSOR_TRANSLATE] ||
                       function[CURSOR_ZOOM] ||
                       function[CURSOR_ROTATE];

      boolean matrix = function[ROTATE] ||
                       function[ZOOM] ||
                       function[TRANSLATE];

      if (cursor || matrix || function[DIRECT]) {
        getDisplay().updateBusyStatus();

        Dimension d = event.getComponent().getSize();
        int current_x = event.getX();
        int current_y = event.getY();

        if (matrix) {
          double[] t1 = null;
          double[] t2 = null;
          if (function[ZOOM]) {
            // current_y -> scale
            double scale =
              Math.exp((start_y-current_y) / (double) d.height);
            t1 = getMouseBehavior().make_matrix(0.0, 0.0, 0.0, scale, 0.0, 0.0, 0.0);
          }
          if (function[TRANSLATE]) {
            // current_x, current_y -> translate
            // WLH 9 Aug 2000
            double transx = xmul * (start_x - current_x);
            double transy = ymul * (start_y - current_y);
            // System.out.println("xmul = " + xmul + " ymul = " + ymul);
            // System.out.println("transx = " + transx + " transy = " + transy);
            t1 = getMouseBehavior().make_translate(-transx, -transy);
          }

          double [] myMatrix = tstart;
          if (function[ROTATE]) {
            if (getMode2D()) {
              double transx = xmul * (start_x - current_x);
              double transy = ymul * (start_y - current_y);
              t1 = getMouseBehavior().make_translate(-transx, -transy);
            }
            else {
              // don't do 3-D rotation in 2-D mode
              double angley =
                - (current_x - start_x) * 100.0 / (double) d.width;
              double anglex =
                - (current_y - start_y) * 100.0 / (double) d.height;
              double[] transA         = { 0.0, 0.0, 0.0 };
              double[] rotA           = { 0.0, 0.0, 0.0 };
              double[] scaleA         = { 0.0, 0.0, 0.0 };
              mouseBehavior.instance_unmake_matrix(rotA, scaleA, transA, myMatrix);

              if(display_renderer.getScaleRotation()) {
                  angley = angley/scaleA[0];
                  anglex = anglex/scaleA[0];
              }

              if(display_renderer.getRotateAboutCenter()) {
                  myMatrix = mouseBehavior.multiply_matrix(mouseBehavior.make_translate(-transA[0], -transA[1], -transA[2]), myMatrix);
                  t2 = mouseBehavior.make_translate(transA[0], transA[1], transA[2]);
              }

              t1 = mouseBehavior.make_matrix(anglex, angley,
                0.0, 1.0, 0.0, 0.0, 0.0);
            }
          }


          if (t1 != null) {
            t1 = mouseBehavior.multiply_matrix(t1, myMatrix);

            if(t2!=null) {
                t1 = mouseBehavior.multiply_matrix(t2,t1);
            }


            try {
              getProjectionControl().setMatrix(t1);
            } catch (VisADException e) {}
            catch (RemoteException e) {}
          }
          return;
        } // end if (matrix)



        if (function[CURSOR_ZOOM]) {
          if (!getMode2D()) {
            // don't do cursor Z in 2-D mode
            // current_y -> 3-D cursor Z
            float diff =
              (start_y - current_y) * 4.0f / (float) d.height;
            getDisplayRenderer().drag_depth(diff);
          }
        }
        if (function[CURSOR_ROTATE]) {
          if (!getMode2D()) {
            // don't do 3-D rotation in 2-D mode
            double angley =
              - (current_x - start_x) * 100.0 / (double) d.width;
            double anglex =
              - (current_y - start_y) * 100.0 / (double) d.height;
            double[] t1 = getMouseBehavior().make_matrix(anglex, angley,
              0.0, 1.0, 0.0, 0.0, 0.0);
            t1 = getMouseBehavior().multiply_matrix(t1, tstart);
            try {
              getProjectionControl().setMatrix(t1);
            }
            catch (VisADException e) {
            }
            catch (RemoteException e) {
            }
          }
        }
        if(function[CURSOR_TRANSLATE]) {
          // current_x, current_y -> 3-D cursor X and Y
          VisADRay cursor_ray = getMouseBehavior().findRay(current_x, current_y);
          if (cursor_ray != null) {
            getDisplayRenderer().drag_cursor(cursor_ray, false);
          }
        }

        if (function[DIRECT]) {
          if (direct_renderer != null) {
            VisADRay direct_ray = getMouseBehavior().findRay(current_x, current_y);
            if (direct_ray != null) {
              direct_renderer.setLastMouseModifiers(mouseModifiers);
              direct_renderer.drag_direct(direct_ray, false, mouseModifiers);
            }
          }
        }
      }
  }

  /**
   * Enable the functions for this mouse helper
   * @param event  The MouseEvent
   */
  protected boolean enableFunctions(MouseEvent event) {

    boolean cursor_off = false;

    if (event == null) {
      for (int i=0; i<function.length; i++) {
        old_function[i] = function[i];
        function[i] = false;
      }
    }

    // compute old and new cursor and matrix enables
    boolean cursor = function[CURSOR_TRANSLATE] ||
                     function[CURSOR_ZOOM] ||
                     function[CURSOR_ROTATE];
    boolean old_cursor = old_function[CURSOR_TRANSLATE] ||
                         old_function[CURSOR_ZOOM] ||
                         old_function[CURSOR_ROTATE];

    boolean matrix = function[ROTATE] ||
                     function[ZOOM] ||
                     function[TRANSLATE];
    boolean old_matrix = old_function[ROTATE] ||
                         old_function[ZOOM] ||
                         old_function[TRANSLATE];

    // disable functions
    if (old_cursor && !cursor) {
      // getDisplayRenderer().setCursorOn(false);
      cursor_off = true;
    }

    if (old_function[DIRECT] && !function[DIRECT]) {
      getDisplayRenderer().setDirectOn(false);
      if (direct_renderer != null) {
        direct_renderer.release_direct();
        direct_renderer = null;
      }

    }

    // enable functions
    if (matrix && !old_matrix) {

      start_x = ((MouseEvent) event).getX();
      start_y = ((MouseEvent) event).getY();
      tstart = getProjectionControl().getMatrix();
      
      setTranslationFactor(start_x, start_y);

    } // end if (matrix && !old_matrix)


    if (cursor && !old_cursor) {

      // turn cursor on whenever mouse button2 pressed
      getDisplayRenderer().setCursorOn(true);

      start_x = ((MouseEvent) event).getX();
      start_y = ((MouseEvent) event).getY();

      tstart = getProjectionControl().getMatrix();
    }

    if (function[CURSOR_TRANSLATE] && !old_function[CURSOR_TRANSLATE]) {
      VisADRay cursor_ray = getMouseBehavior().findRay(start_x, start_y);
      if (cursor_ray != null) {
        getDisplayRenderer().drag_cursor(cursor_ray, true);
      }
    }

    if (function[CURSOR_ZOOM] && !old_function[CURSOR_ZOOM]) {
      if (!getMode2D()) {
        // don't do cursor Z in 2-D mode
        // current_y -> 3-D cursor Z
        VisADRay cursor_ray =
          getMouseBehavior().cursorRay(getDisplayRenderer().getCursor());
        getDisplayRenderer().depth_cursor(cursor_ray);
      }
    }

    if (function[DIRECT] && !old_function[DIRECT]) {
      if (getDisplayRenderer().anyDirects()) {
        int current_x = ((MouseEvent) event).getX();
        int current_y = ((MouseEvent) event).getY();
        VisADRay direct_ray =
          getMouseBehavior().findRay(current_x, current_y);
        if (direct_ray != null) {
          direct_renderer =
            getDisplayRenderer().findDirect(direct_ray, mouseModifiers);
          if (direct_renderer != null) {
            getDisplayRenderer().setDirectOn(true);
            direct_renderer.setLastMouseModifiers(mouseModifiers);
            direct_renderer.drag_direct(direct_ray, true,
              mouseModifiers);
          }
        }
      }
    }
    return cursor_off;
  }

  /** 
   * Enable/disable the interpretation of any pair of mouse buttons
   * as the third button.
   * @param  e  enable/disable. If true (default), interpret any pair 
   *            of mouse buttons as the third button.
   */
  public void setEnableCombos(boolean e) {
    enable_combos = e;
    enableFunctions(null);
  }

  /**
   * Get whether the mouse button combos are enabled
   * @return true if any pair of mouse buttons is interpreted as the third
   */
  public boolean getEnableCombos() { return enable_combos; }

  /** 
   * Set mapping from (button, ctrl, shift) to function.
   *
   *  <pre>
   *  map[button][ctrl][shift] =
   *    MouseHelper.NONE               for no function
   *    MouseHelper.ROTATE             for box rotate
   *    MouseHelper.ZOOM               for box zoom
   *    MouseHelper.TRANSLATE          for box translate
   *    MouseHelper.CURSOR_TRANSLATE   for cursor translate
   *    MouseHelper.CURSOR_ZOOM        for cursor on Z axis (3-D only)
   *    MouseHelper.CURSOR_ROTATE      for box rotate with cursor
   *    MouseHelper.DIRECT             for direct manipulate
   *  where button = 0 (left), 1 (center), 2 (right)
   *  ctrl = 0 (CTRL key not pressed), 1 (CTRL key pressed)
   *  shift = 0 (SHIFT key not pressed), 1 (SHIFT key pressed)
   *
   *  Note some direct manipulation DataRenderers test the status of
   *  CTRL and SHIFT keys, so it is advisable that the DIRECT function
   *  be invariant to the state of ctrl and shift in the map array.
   *
   *  For example, to set the left mouse button for direct
   *  manipulation, and the center button for box rotation
   *  (only without shift or control):
   *  mouse_helper.setFunctionMap(new int[][][]
   *    {{{MouseHelper.DIRECT, MouseHelper.DIRECT},
   *      {MouseHelper.DIRECT, MouseHelper.DIRECT}},
   *     {{MouseHelper.ROTATE, MouseHelper.NONE},
   *      {MouseHelper.NONE, MouseHelper.NONE}},
   *     {{MouseHelper.NONE, MouseHelper.NONE},
   *      {MouseHelper.NONE, MouseHelper.NONE}}});
   *  </pre>
   * @param  map map of functions.  map must be int[3][2][2]
   * @throws VisADException  bad map
   */
  public void setFunctionMap(int[][][] map) throws VisADException {
    if (map == null || map.length != 3) {
      throw new DisplayException("bad map array");
    }
    for (int i=0; i<3; i++) {
      if (map[i] == null || map[i].length != 2) { 
        throw new DisplayException("bad map array");
      }
      for (int j=0; j<2; j++) {
        if (map[i][j] == null || map[i][j].length != 2) {
          throw new DisplayException("bad map array");
        }
        for (int k=0; k<2; k++) {
          if (map[i][j][k] >= function.length) {
            throw new DisplayException("bad map array value: " + map[i][j][k]);
          }
        }
      }
    }
    for (int i=0; i<3; i++) {
      for (int j=0; j<2; j++) {
        for (int k=0; k<2; k++) {
          function_map[i][j][k] = map[i][j][k];
        }
      }
    }
    enableFunctions(null);
  }
  
  public void setTranslationFactor(int start_x, int start_y) {
      // WLH 9 Aug 2000
      VisADRay start_ray = getMouseBehavior().findRay(start_x, start_y);
      VisADRay start_ray_x = getMouseBehavior().findRay(start_x + 1, start_y);
      VisADRay start_ray_y = getMouseBehavior().findRay(start_x, start_y + 1);

      double[] rot = new double[3];
      double[] scale = new double[3];
      double[] trans = new double[3];
      getMouseBehavior().instance_unmake_matrix(rot, scale, trans, tstart);
      double sts = scale[0];
      double[] trot = getMouseBehavior().make_matrix(rot[0], rot[1], rot[2],
                                           scale[0], scale[1], scale[2],
                                           0.0, 0.0, 0.0);

      // WLH 17 Aug 2000
      double[] xmat = getMouseBehavior().make_translate(
                         start_ray_x.position[0] - start_ray.position[0],
                         start_ray_x.position[1] - start_ray.position[1],
                         start_ray_x.position[2] - start_ray.position[2]);
      double[] ymat = getMouseBehavior().make_translate(
                         start_ray_y.position[0] - start_ray.position[0],
                         start_ray_y.position[1] - start_ray.position[1],
                         start_ray_y.position[2] - start_ray.position[2]);
      double[] xmatmul = getMouseBehavior().multiply_matrix(trot, xmat);
      double[] ymatmul = getMouseBehavior().multiply_matrix(trot, ymat);
      
      getMouseBehavior().instance_unmake_matrix(rot, scale, trans, xmatmul);
      xmul = trans[0];
      getMouseBehavior().instance_unmake_matrix(rot, scale, trans, ymatmul);
      ymul = trans[1];

      // horrible hack, WLH 17 Aug 2000
      if (getMouseBehavior() instanceof visad.java2d.MouseBehaviorJ2D) {
        double factor = xymul / Math.sqrt(xmul * xmul + ymul * ymul);
        xmul *= factor;
        ymul *= factor;

        xmul = Math.abs(xmul);
        ymul = -Math.abs(ymul);
      }     
  }

  /**
   * Print out a readable form of a matrix.  Useful for
   * debugging.
   * @param title  title to prepend to output.
   * @param m  matrix to print.
   */
  public void print_matrix(String title, double[] m) {
    if (getMouseBehavior() == null) return;
    double[] rot = new double[3];
    double[] scale = new double[3];
    double[] trans = new double[3];
    getMouseBehavior().instance_unmake_matrix(rot, scale, trans, m);
    StringBuffer buf = new StringBuffer(title);
    buf.append(" = (");
    buf.append(Convert.shortString(rot[0]));
    buf.append(", ");
    buf.append(Convert.shortString(rot[1]));
    buf.append(", ");
    buf.append(Convert.shortString(rot[2]));
    buf.append("), ");
    if (scale[0] == scale[1] && scale[0] == scale[2]) {
      buf.append(Convert.shortString(scale[0]));
      buf.append(", (");
    } else {
      buf.append("(");
      buf.append(Convert.shortString(scale[0]));
      buf.append(", ");
      buf.append(Convert.shortString(scale[1]));
      buf.append(", ");
      buf.append(Convert.shortString(scale[2]));
      buf.append("), (");
    }
    buf.append(Convert.shortString(trans[0]));
    buf.append(", ");
    buf.append(Convert.shortString(trans[1]));
    buf.append(", ");
    buf.append(Convert.shortString(trans[2]));
    buf.append(")");
    System.out.println(buf.toString());
  }

  /**
   * Implementation for RendererSourceListener.  Notifies that the
   * renderer has been deleted.
   * @param renderer DataRenderer that was deleted.
   */
  public void rendererDeleted(DataRenderer renderer)
  {
    if (direct_renderer != null) {
      if (direct_renderer == renderer || direct_renderer.equals(renderer)) {
        direct_renderer = null;
      }
    }
  }




}
