//
// DisplayEvent.java
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

import java.awt.Component;
import java.awt.event.*;
import javax.swing.*;

/**
   DisplayEvent is the VisAD class for Events from Display
   objects.  They are sourced by Display objects and
   received by DisplayListener objects.<P>
*/
public class DisplayEvent extends VisADEvent {

  // If you add more events, be sure to add them to the Javadoc for
  // DisplayEvent.getId(), DisplayImpl.enableEvent(), and
  // DisplayImpl.disableEvent() and DisplayImpl.eventStatus. Also 
  // update DisplayEvent.main()'s ids array.

  /**
   * The "mouse pressed" event.  This event occurs when any
   * of the mouse buttons is pressed inside the display.  Other
   * MOUSE_PRESSED event positions (LEFT, CENTER, RIGHT) are based
   * on a right-handed mouse configuration.
   */
  public final static int MOUSE_PRESSED = 1;

  /** The "transform done" event. This even occurs when a DisplayImpl
      finishes transforming Data into depictions. */
  public final static int TRANSFORM_DONE = 2;

  /** The "frame done" event. This even occurs when Data depictions
      have been rendered to the screen by the graphics API. */
  public final static int FRAME_DONE = 3;

  /**
   * The "center mouse button pressed" event.  This event occurs when
   * the center mouse button is pressed inside the display.
   */
  public final static int MOUSE_PRESSED_CENTER = 4;

  /**
   * The "left mouse button pressed" event.  This event occurs when
   * the left mouse button is pressed inside the display.
   */
  public final static int MOUSE_PRESSED_LEFT = 5;

  /**
   * The "right mouse button pressed" event.  This event occurs when
   * the right mouse button is pressed inside the display.
   */
  public final static int MOUSE_PRESSED_RIGHT = 6;

  /**
   * The "mouse released" event.  This event occurs when any
   * of the mouse buttons is released after one of the MOUSE_PRESSED
   * events. Other MOUSE_RELEASED event positions (LEFT, CENTER, RIGHT)
   * are based on a right-handed mouse configuration.
   */
  public final static int MOUSE_RELEASED = 7;

  /**
   * The "center mouse button released" event.  This event occurs when
   * the center mouse button is released after a MOUSE_PRESSED or
   * MOUSE_PRESSED_CENTER event.
   */
  public final static int MOUSE_RELEASED_CENTER = 8;

  /**
   * The "left mouse button released" event.  This event occurs when
   * the left mouse button is released after a MOUSE_PRESSED or
   * MOUSE_PRESSED_LEFT event.
   */
  public final static int MOUSE_RELEASED_LEFT = 9;

  /**
   * The "right mouse button released" event.  This event occurs when
   * the right mouse button is released after a MOUSE_PRESSED or
   * MOUSE_PRESSED_RIGHT event.
   */
  public final static int MOUSE_RELEASED_RIGHT = 10;

  /**
   * The "map added" event.  This event occurs when
   * a ScalarMap is added to the display.
   */
  public final static int MAP_ADDED = 11;

  /**
   * The "maps cleared" event.  This event occurs when
   * all ScalarMaps are removed from the display.
   */
  public final static int MAPS_CLEARED = 12;

  /**
   * The "reference added" event.  This event occurs when
   * a DataReference is added to the display.
   */
  public final static int REFERENCE_ADDED = 13;

  /**
   * The "reference removed" event.  This event occurs when
   * a DataReference is removed from the display.
   */
  public final static int REFERENCE_REMOVED = 14;

  /**
   * The "display destroyed" event.  This event occurs when
   * a display's destroy() method is called.
   */
  public final static int DESTROYED = 15;

  /**
   * The "key pressed" event.  This event occurs when the display
   * has the focus and a key on the keyboard is pressed.
   *
   * Note that a KeyboardBehavior must be attached to the display
   * before this type of event will be reported.
   */
  public final static int KEY_PRESSED = 16;

  /**
   * The "key released" event.  This event occurs when the display
   * has the focus and a key on the keyboard is released.
   *
   * Note that a KeyboardBehavior must be attached to the display
   * before this type of event will be reported.
   */
  public final static int KEY_RELEASED = 17;

  /**
   * The "mouse dragged" event.  This event occurs when
   * the mouse is dragged across the display.
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.MOUSE_DRAGGED)
   * to enable reporting of this type of event.
   */
  public final static int MOUSE_DRAGGED = 18;

  /**
   * The "mouse entered" event.  This event occurs when
   * the mouse cursor enters the region of the display.
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.MOUSE_ENTERED)
   * to enable reporting of this type of event.
   */
  public final static int MOUSE_ENTERED = 19;

  /**
   * The "mouse exited" event.  This event occurs when
   * the mouse cursor leaves the region of the display.
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.MOUSE_EXITED)
   * to enable reporting of this type of event.
   */
  public final static int MOUSE_EXITED = 20;

  /**
   * The "mouse moved" event.  This event occurs when
   * the mouse is moved across the display.
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.MOUSE_MOVED)
   * to enable reporting of this type of event.
   */
  public final static int MOUSE_MOVED = 21;

  /**
   * The "display wait on" event.  This event occurs when
   * a display's renderer is told to "wait"
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.WAIT_ON)
   * to enable reporting of this type of event.
   */
  public final static int WAIT_ON = 22;

  /**
   * The "display wait off" event.  This event occurs when
   * a display's renderer is told to "stop waiting"
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.WAIT_OFF)
   * to enable reporting of this type of event.
   */
  public final static int WAIT_OFF = 23;

  /**
   * The "map removed" event.  This event occurs when
   * a ScalarMap is removed from the display.
   */
  public final static int MAP_REMOVED = 24;

  /**
   * The "component resized" event.  This event occurs when
   * a the display's component is resized.
   *
   * Note that you must call
   * DisplayImpl.enableEvent(DisplayEvent.COMPONENT_RESIZED)
   * to enable reporting of this type of event.
   */
  public final static int COMPONENT_RESIZED = 25;

  private final static String[] ids = {
      "?", "MOUSE_PRESSED", "TRANSFORM_DONE", "FRAME_DONE",
      "MOUSE_PRESSED_CENTER", "MOUSE_PRESSED_LEFT",  "MOUSE_PRESSED_RIGHT",
      "MOUSE_RELEASED", "MOUSE_RELEASED_CENTER", "MOUSE_RELEASED_LEFT",
      "MOUSE_RELEASED_RIGHT", "MAP_ADDED", "MAPS_CLEARED", "REFERENCE_ADDED",
      "REFERENCE_REMOVED", "DESTROYED", "KEY_PRESSED", "KEY_RELEASED",
      "MOUSE_DRAGGED", "MOUSE_ENTERED", "MOUSE_EXITED", "MOUSE_MOVED",
      "WAIT_ON", "WAIT_OFF", "MAP_REMOVED", "COMPONENT_RESIZED"
    };

  /** Dummy AWT component. */
  // private static final Component DUMMY = new JPanel();
  private static Component DUMMY = null;
  static {
    try {
      DUMMY = new JPanel();
    }
    catch (Throwable t) {
    }
  }

  private int id = 0;

  /** InputEvent corresponding to the DisplayEvent, if any */
  private InputEvent input_event = null;

  /** source of event */
  private Display display;

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * and type of event.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   */
  public DisplayEvent(Display d, int id_d) {
    this(d, id_d, LOCAL_SOURCE);
  }

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * and type of event.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   * @param  remoteId  ID of remote source
   */
  public DisplayEvent(Display d, int id_d, int remoteId) {
    // don't pass display as the source, since source
    // is transient inside Event
    super(null, 0, null, remoteId);
    display = d;
    id = id_d;
  }

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * type of event, and mouse positions where event occurred.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   * @param  x  the horizontal x coordinate for the mouse location in
   *            the display component
   * @param  y  the vertical y coordinate for the mouse location in
   *            the display component
   */
  public DisplayEvent(Display d, int id_d, int x, int y) {
    this(d, id_d, x, y, LOCAL_SOURCE);
  }

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * type of event, and mouse event describing mouse details.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   * @param  e  the InputEvent describing this MOUSE type DisplayEvent
   */
  public DisplayEvent(Display d, int id_d, InputEvent e) {
    this(d, id_d, e, LOCAL_SOURCE);
  }

  /**
   * get the AWT (including Swing) Component of a Display
   * @param d the Display
   * @return the Component of d (may be null
   */
  protected static Component getDisplayComponent(Display d) {
    if (!(d instanceof DisplayImpl)) return DUMMY;
    DisplayImpl di = (DisplayImpl) d;
    Component c = di.getComponent();
    return c == null ? DUMMY : c;
  }

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * type of event, mouse positions where event occurred, and
   * remote flag indicating whether event came from a remote source.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   * @param  x  the horizontal x coordinate for the mouse location in
   *            the display component
   * @param  y  the vertical y coordinate for the mouse location in
   *            the display component
   * @param  remoteId  ID of remote source
   */
  public DisplayEvent(Display d, int id_d, int x, int y, int remoteId) {
    this(d, id_d, makeInputEvent(d, x, y), remoteId);
    // this(d, id_d, new MouseEvent(getDisplayComponent(d), 0,
    //   System.currentTimeMillis(), 0, x, y, 1, false), remoteId);
  }

  /**
   * Constructs a DisplayEvent object with the specified source display,
   * type of event, mouse event describing mouse details, and remote
   * flag indicating whether event came from a remote source.
   *
   * @param  d  display that sends the event
   * @param  id_d  type of DisplayEvent that is sent
   * @param  e  the InputEvent describing this MOUSE type DisplayEvent
   * @param  remoteId  ID of remote source
   */
  public DisplayEvent(Display d, int id_d, InputEvent e, int remoteId) {
    // don't pass display as the source, since source
    // is transient inside Event
    super(null, 0, null, remoteId);
    display = d;
    id = id_d;
    input_event = e;
  }

  private static InputEvent makeInputEvent(Display d, int x, int y) {
    Component c = getDisplayComponent(d);
    if (c != null) {
      return new MouseEvent(c, 0, System.currentTimeMillis(), 0, x, y, 1, false);
    }
    else {
      return null;
    }
  }

  /**
   * @return a new DisplayEvent which is a copy of this event,
   *         but which uses the specified source display
   */
  public DisplayEvent cloneButDisplay(Display dpy)
  {
    return new DisplayEvent(dpy, id, input_event, getRemoteId());
  }

  /**
   * @return the DisplayImpl that sent this DisplayEvent (or
   *         a RemoteDisplay reference to it if the Display was
   *         on a different JVM)
   */
  public Display getDisplay() {
    return display;
  }

  /**
   * Get the ID type of this event
   *
   * @return  DisplayEvent type.  Valid types are:
   *          <UL>
   *          <LI>DisplayEvent.FRAME_DONE
   *          <LI>DisplayEvent.TRANSFORM_DONE
   *          <LI>DisplayEvent.MOUSE_PRESSED
   *          <LI>DisplayEvent.MOUSE_PRESSED_LEFT
   *          <LI>DisplayEvent.MOUSE_PRESSED_CENTER
   *          <LI>DisplayEvent.MOUSE_PRESSED_RIGHT
   *          <LI>DisplayEvent.MOUSE_RELEASED_LEFT
   *          <LI>DisplayEvent.MOUSE_RELEASED_CENTER
   *          <LI>DisplayEvent.MOUSE_RELEASED_RIGHT
   *          <LI>DisplayEvent.MAP_ADDED
   *          <LI>DisplayEvent.MAPS_CLEARED
   *          <LI>DisplayEvent.REFERENCE_ADDED
   *          <LI>DisplayEvent.REFERENCE_REMOVED
   *          <LI>DisplayEvent.DESTROYED
   *          <LI>DisplayEvent.KEY_PRESSED
   *          <LI>DisplayEvent.KEY_RELEASED
   *          <LI>DisplayEvent.MOUSE_DRAGGED
   *          <LI>DisplayEvent.MOUSE_ENTERED
   *          <LI>DisplayEvent.MOUSE_EXITED
   *          <LI>DisplayEvent.MOUSE_MOVED
   *          <LI>DisplayEvent.WAIT_ON
   *          <LI>DisplayEvent.WAIT_OFF
   *          <LI>DisplayEvent.MAP_REMOVED
   *          <LI>DisplayEvent.COMPONENT_RESIZED
   *          </UL>
   */
  public int getId() {
    return id;
  }

  /**
   * Get the horizontal x coordinate for the mouse location.  Only valid
   * for MOUSE type events.
   *
   * @return  horizontal x coordinate for the mouse location in
   *          the display component, or -1 if not a mouse event
   */
  public int getX() {
    return input_event == null || !(input_event instanceof MouseEvent) ?
      -1 : ((MouseEvent) input_event).getX();
  }

  /**
   * Get the vertical y coordinate for the mouse location.  Only valid
   * for MOUSE type events.
   *
   * @return  vertical y coordinate for the mouse location in
   *          the display component, or -1 if not a mouse event
   */
  public int getY() {
    return input_event == null || !(input_event instanceof MouseEvent) ?
      -1 : ((MouseEvent) input_event).getY();
  }

  /**
   * Get the key code for the pressed or released key.  Only valid
   * for KEY type events.
   *
   * @return  key code for key pressed or released in the
   *          display component, or -1 if not a key event
   */
  public int getKeyCode() {
    return input_event == null || !(input_event instanceof KeyEvent) ?
      -1 : ((KeyEvent) input_event).getKeyCode();
  }

  /**
   * Get the keyboard modifiers (such as whether SHIFT or CTRL was
   * being held during the event).  Only valid for MOUSE and KEY type events.
   *
   * @return  keyboard modifier bit field, or -1 if not a mouse event
   */
  public int getModifiers() {
    return input_event == null ? -1 : input_event.getModifiers();
  }

  /**
   * Get the InputEvent associated with this DisplayEvent.
   * Only valid for MOUSE and KEY type events.
   *
   * @return  associated InputEvent, or null if not a mouse event
   */
  public InputEvent getInputEvent() { return input_event; }

  /**
   * String representation of the event.
   *
   * @return descriptive info about the event
   */
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("DisplayEvent: ");
    try {
      String display = getDisplay().getName();
      buf.append("Display=");
      buf.append(display);
      buf.append(", ");
    } catch (Exception ve) { }
    buf.append("Id=");
    buf.append(ids[getId()]);
    buf.append(", X=");
    buf.append(getX());
    buf.append(", Y=");
    buf.append(getY());
    buf.append(", remoteId=");
    buf.append(getRemoteId());
    return buf.toString();
  }

}

