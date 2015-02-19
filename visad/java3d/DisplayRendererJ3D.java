//
// DisplayRendererJ3D.java
//

/*
VisAD system for interactive analysis and visualization of numerical
data.  Copyright (C) 1996 - 2015 Bill Hibbard, Curtis Rueden, Tom
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

package visad.java3d;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import javax.media.j3d.Appearance;
import javax.media.j3d.Background;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.GraphicsContext3D;
import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.SceneGraphObject;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4d;

import visad.AxisScale;
import visad.ColorAlphaControl;
import visad.ColorControl;
import visad.ContourControl;
import visad.Control;
import visad.ControlEvent;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayException;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.DisplayRenderer;
import visad.Flow1Control;
import visad.Flow2Control;
import visad.GraphicsModeControl;
import visad.MouseBehavior;
import visad.KeyboardBehavior;
import visad.PlotText;
import visad.ProjectionControl;
import visad.RangeControl;
import visad.RealType;
import visad.RendererControl;
import visad.RendererSourceListener;
import visad.ScalarMap;
import visad.ShapeControl;
import visad.TextControl;
import visad.VisADException;
import visad.VisADLineArray;
import visad.VisADRay;
import visad.VisADTriangleArray;
import visad.util.Util;

/**
 * <CODE>DisplayRendererJ3D</CODE> is the VisAD abstract super-class for
 * background and metadata rendering algorithms.  These complement
 * depictions of <CODE>Data</CODE> objects created by
 * <CODE>DataRenderer</CODE> objects.<P>
 *
 * <CODE>DisplayRendererJ3D</CODE> also manages the overall relation of
 * <CODE>DataRenderer</CODE> output to Java3D and manages the scene graph.<P>
 *
 * It creates the binding between <CODE>Control</CODE> objects and scene
 * graph <CODE>Behavior</CODE> objects for direct manipulation of
 * <CODE>Control</CODE> objects.<P>
 *
 * <CODE>DisplayRendererJ3D</CODE> is not <CODE>Serializable</CODE> and
 * should not be copied between JVMs.<P>
*/
public abstract class DisplayRendererJ3D
  extends DisplayRenderer
  implements RendererSourceListener
{

  /**
   * Set the name of a <code>SceneGraphObject</code>.
   * If <code>SceneGraphObject</code> does not have a <code>setName</code>
   * (J3D pre v1.4) this is a no-op.
   * @param name
   */
  public static void setSceneGraphObjectName(SceneGraphObject obj, String name) {
    Util.setName(obj, name);
  }

  private Object not_destroyed = new Object();

  // for screen locked
  private OrderedGroup screen_locked = null;
  private TransformGroup locked_trans = null;

  /** View associated with this VirtualUniverse */
  private View view;
  /** VisADCanvasJ3D associated with this VirtualUniverse */
  private VisADCanvasJ3D canvas;

  /** root BranchGroup of scene graph under Locale */
  private BranchGroup root = null;
  /** single TransformGroup between root and BranchGroups for all
   *  Data depictions */
  private TransformGroup trans = null;
  /** BranchGroup between trans and all direct manipulation
   *  Data depictions */
  // WLH 13 March 2000
  // private BranchGroup direct = null;

  // WLH 10 March 2000
  private OrderedGroup non_direct = null;

  /** TransformGroup for ViewPlatform */
  private TransformGroup vpTrans = null;

  /** MouseBehaviorJ3D */
  private MouseBehaviorJ3D mouse = null;

  /** KeyboardBehaviorJ3D */
  private KeyboardBehaviorJ3D keyboard = null;

  /** color of box and cursor */
  private ColoringAttributes box_color = null;
  private ColoringAttributes cursor_color = null;

  /** background attached to root */
  private Background background = null;

  /** TransformGroup between trans and cursor */
  private TransformGroup cursor_trans = null;
  /** single Switch between cursor_trans and cursor */
  private Switch cursor_switch = null;
  /** children of cursor_switch */
  private BranchGroup cursor_on = null, cursor_off = null;
  /** on / off state of cursor */
  private boolean cursorOn = false;
  /** on / off state of direct manipulation location display */
  private boolean directOn = false;

  /** single Switch between trans and box */
  private Switch box_switch = null;
  /** children of box_switch */
  private BranchGroup box_on = null, box_off = null;
  /** on / off state of box */
  private boolean boxOn = false;

  /** single Switch between trans and scales */
  private Switch scale_switch = null;
  /** children of scale_switch */
  private BranchGroup scale_on = null, scale_off = null;
  /** Vector of screen based AxisScales */
  private Vector axis_vector = new Vector();

  /** on / off state of cursor in GraphicsModeControl */

  /** Vector of DirectManipulationRenderers */
  private Vector directs = new Vector();

  /** cursor location */
  private float cursorX, cursorY, cursorZ;
  /** normalized direction perpendicular to current cursor plane */
  private float line_x, line_y, line_z;
  /** start value for cursor */
  private float point_x, point_y, point_z;

  /** ModelClip stuff, done by reflection */
  private Method modelClipSetEnable = null;
  private Method modelClipSetPlane = null;
  private Method modelClipAddScope = null;
  private Object modelClip = null;
  private boolean[] modelClipEnables =
    {false, false, false, false, false, false};

  public DisplayRendererJ3D () {
    super();
  }

  // WLH 17 Dec 2001
  public void destroy() {
    not_destroyed = null;

    if (canvas != null) canvas.stop();
    if (mouse != null) mouse.destroy();
    if (root != null) {
      root.detach();
      root = null;
    }

    axis_vector.removeAllElements();
    directs.removeAllElements();

    screen_locked = null;
    locked_trans = null;

    trans = null;
    vpTrans = null;
    non_direct = null;
    view = null;
    canvas = null;
    mouse = null;
    box_color = null;
    cursor_color = null;
    background = null;
    cursor_trans = null;
    cursor_switch = null;
    cursor_on = null; 
    cursor_off = null;
    box_switch = null;
    box_on = null; 
    box_off = null;
    scale_switch = null;
    scale_on = null; 
    scale_off = null;
  }

  /**
   * Specify <CODE>DisplayImpl</CODE> to be rendered.
   * @param dpy <CODE>Display</CODE> to render.
   * @exception VisADException If a <CODE>DisplayImpl</CODE> has already
   *                           been specified.
   */
  public void setDisplay(DisplayImpl dpy)
    throws VisADException
  {
    if (not_destroyed == null) return;
    super.setDisplay(dpy);
    dpy.addRendererSourceListener(this);
    boxOn = getRendererControl().getBoxOn();
  }

  public View getView() {
    return view;
  }

  public TransformGroup getViewTrans() {
    return vpTrans;
  }

  /**
   * Get the canvas for this renderer
   * @return  <CODE>VisADCanvasJ3D</CODE> that this renderer uses.
   */
  public VisADCanvasJ3D getCanvas() {
    return canvas;
  }

  /**
   * Capture the display rendition as an image.
   * @return  image of the display.
   */
  
  public BufferedImage getImage() {
    if (not_destroyed == null) return null;
    BufferedImage image = null;
    canvas.captureImage = null;
    ProjectionControl proj = getDisplay().getProjectionControl();
    double[] matrix= proj.getMatrix();
    while (image == null) {
      try {
        synchronized (this) {
          canvas.setDoubleBufferEnable(false);
          canvas.captureFlag = true;
          hasNotifyBeenCalled = false;
          if (canvas.getOffscreen()) {
              try {
              Method renderMethod =
                Canvas3D.class.getMethod("renderOffScreenBuffer",
                                         new Class[] {});
              renderMethod.invoke(canvas, new Object[] {});
              /*        Method waitMethod =
                Canvas3D.class.getMethod("waitForOffScreenRendering", new Class[] {});
                waitMethod.invoke(canvas, new Object[] {});*/
            }
            catch (NoSuchMethodException e) {}
            catch (IllegalAccessException e) {}
            catch (InvocationTargetException e) {}
          }
          try {
              proj.setMatrix(matrix);
          } catch (RemoteException e) { 
              e.printStackTrace();
          } catch (VisADException e) { 
              e.printStackTrace();
          }
          //Make sure the notify has not been called. There is the possbility that the above renderOffScreenBuffer call
          //gets completed before we get to this wait, resulting in a starvation lockup here because the canvas already 
          //notifies the display renderer and when we get to the wait nothing is going to notify this object.
          image = canvas.captureImage;
          if(image == null && !hasNotifyBeenCalled) {
              waitingOnImageCapture = true;
              wait();
              waitingOnImageCapture = false;
          } 
        }
      } catch(InterruptedException e) {
        // note notify generates a normal return from wait rather
        // than an Exception - control doesn't normally come here
        canvas.setDoubleBufferEnable(true); //- just in case
        e.printStackTrace();
      }
      if(image==null) {
          image = canvas.captureImage;
      }
      canvas.captureImage = null;
      canvas.setDoubleBufferEnable(true);
      if(image == null) {
          //What do we do here?
          //          break?;
      }
    }
    return image;
  }

  /** used for doing offscreen capture to prevent a starvation lockup */
  private boolean hasNotifyBeenCalled = false;

  /** used for doing offscreen capture to prevent the extra notify */
  private boolean waitingOnImageCapture = false;

  void notifyCapture() {
      hasNotifyBeenCalled = true;
      if(waitingOnImageCapture) {
         waitingOnImageCapture = false;
         synchronized (this) {
             notify(); 
         }
      } else {
      }
      //    }
  }

  public BranchGroup getRoot() {
    return root;
  }

  /**
   * Internal method used to initialize newly created
   * <CODE>RendererControl</CODE> with current renderer settings
   * before it is actually connected to the renderer.  This
   * means that changes will not generate <CODE>MonitorEvent</CODE>s.
   * @param ctl RendererControl to initialize
   */
  public void initControl(RendererControl ctl)
  {
    if (not_destroyed == null) return;
    Color3f c3f = new Color3f();

    // initialize box colors
    if (box_color != null) {
      box_color.getColor(c3f);
      try {
        ctl.setBoxColor(c3f.x, c3f.y, c3f.z);
      } catch (Throwable t) {
        // ignore any initialization problems
      }
    }

    // initialize cursor colors
    if (cursor_color != null) {
      cursor_color.getColor(c3f);
      try {
        ctl.setCursorColor(c3f.x, c3f.y, c3f.z);
      } catch (Throwable t) {
        // ignore any initialization problems
      }
    }

    // initialize background colors
    if (background != null) {
      background.getColor(c3f);
      try {
        ctl.setBackgroundColor(c3f.x, c3f.y, c3f.z);
      } catch (Throwable t) {
        // ignore any initialization problems
      }
    }

    // initialize box visibility
    try {
      ctl.setBoxOn(boxOn);
    } catch (Throwable t) {
      // ignore any initialization problems
    }
  }

  /**
   * Update internal values from those in the <CODE>RendererControl</CODE>.
   * @param evt <CODE>ControlEvent</CODE> generated by a change to the
   *            <CODE>RendererControl</CODE>
   */
  public void controlChanged(ControlEvent evt)
  {
    if (not_destroyed == null) return;
    RendererControl ctl = (RendererControl )evt.getControl();

    float[] ct;
    Color3f c3f = new Color3f();

    // update box colors
    if (box_color != null) {
      ct = ctl.getBoxColor();
      box_color.getColor(c3f);
      if (!Util.isApproximatelyEqual(ct[0], c3f.x) ||
          !Util.isApproximatelyEqual(ct[1], c3f.y) ||
          !Util.isApproximatelyEqual(ct[2], c3f.z))
      {
        box_color.setColor(ct[0], ct[1], ct[2]);
      }
    }

    // update cursor colors
    if (cursor_color != null) {
      ct = ctl.getCursorColor();
      cursor_color.getColor(c3f);
      if (!Util.isApproximatelyEqual(ct[0], c3f.x) ||
          !Util.isApproximatelyEqual(ct[1], c3f.y) ||
          !Util.isApproximatelyEqual(ct[2], c3f.z))
      {
        cursor_color.setColor(ct[0], ct[1], ct[2]);
      }
    }

    // update background colors
    ct = ctl.getBackgroundColor();
    background.getColor(c3f);
    if (!Util.isApproximatelyEqual(ct[0], c3f.x) ||
        !Util.isApproximatelyEqual(ct[1], c3f.y) ||
        !Util.isApproximatelyEqual(ct[2], c3f.z))
    {
      background.setColor(ct[0], ct[1], ct[2]);
    }

    // update box visibility
    boolean on = ctl.getBoxOn();
    if (on != boxOn) {
      boxOn = on;
      box_switch.setWhichChild(boxOn ? 1 : 0);
    }
  }

  public TransformGroup getTrans() {
    return trans;
  }

  public BranchGroup getCursorOnBranch() {
    return cursor_on;
  }

  public BranchGroup getBoxOnBranch() {
    return box_on;
  }

  /**
   * Toggle the cursor in the display
   * @param  on   true to display the cursor, false to hide it.
   */
  public void setCursorOn(boolean on) {
    if (not_destroyed == null) return;
    cursorOn = on;
    if (on) {
      cursor_switch.setWhichChild(1); // set cursor on
      setCursorStringVector();
    }
    else {
      cursor_switch.setWhichChild(0); // set cursor off
      setCursorStringVector(null);
    }
  }

  /**
   * Set the flag for direct manipulation
   * @param  on  true for enabling direct manipulation, false to disable
   */
  public void setDirectOn(boolean on) {
    if (not_destroyed == null) return;
    directOn = on;
    if (!on) {
      setCursorStringVector(null);
    }
  }

/* WLH 13 March 2000
  public BranchGroup getDirect() {
    return direct;
  }
*/

  /**
   * Create scene graph root, if none exists, with Transform
   * and direct manipulation root;
   * create special graphics (e.g., 3-D box, SkewT background),
   * any lights, any user interface embedded in scene.
   * @param v
   * @param vpt
   * @param c
   * @return Scene graph root.
   */
  public abstract BranchGroup createSceneGraph(View v, TransformGroup vpt,
                                               VisADCanvasJ3D c);

  /** @deprecated use createBasicSceneGraph(View v, TransformGroup vpt,
         VisADCanvasJ3D c, MouseBehaviorJ3D m, ColoringAttributes bc,
         ColoringAttributes cc)
      instead */
  public BranchGroup createBasicSceneGraph(View v, TransformGroup vpt,
         VisADCanvasJ3D c, MouseBehaviorJ3D m) {
    if (not_destroyed == null) return null;
    box_color = new ColoringAttributes();
    box_color.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
    box_color.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
    cursor_color = new ColoringAttributes();
    cursor_color.setCapability(ColoringAttributes.ALLOW_COLOR_READ);
    cursor_color.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
    return createBasicSceneGraph(v, vpt, c, m, box_color, cursor_color);
  }

  /**
   * Create scene graph root, if none exists, with Transform
   * and direct manipulation root.
   * @param v
   * @param vpt
   * @param c
   * @param m
   * @return Scene graph root.
   */
  public BranchGroup createBasicSceneGraph(View v, TransformGroup vpt,
         VisADCanvasJ3D c, MouseBehaviorJ3D m, ColoringAttributes bc,
         ColoringAttributes cc) {
    if (root != null) return root;
    if (not_destroyed == null) return null;

    mouse = m;
    view = v;
    vpTrans = vpt;
    box_color = bc;
    cursor_color = cc;

    // WLH 14 April 98
    v.setDepthBufferFreezeTransparent(false);
    canvas = c;
    // Create the root of the branch graph
    root = new BranchGroup();
    setSceneGraphObjectName(root, "Root");
    root.setCapability(BranchGroup.ALLOW_DETACH);
    root.setCapability(Group.ALLOW_CHILDREN_READ);
    root.setCapability(Group.ALLOW_CHILDREN_WRITE);
    root.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    // create the TransformGroup that is the parent of
    // Data object Group objects
    setTransform3D(null);
    root.addChild(trans);

    // create background
    background = new Background();
    setSceneGraphObjectName(background, "Background");
    background.setCapability(Background.ALLOW_COLOR_WRITE);
    background.setCapability(Background.ALLOW_COLOR_READ);
    float[] ctlBg = getRendererControl().getBackgroundColor();
    background.setColor(ctlBg[0], ctlBg[1], ctlBg[2]);
    BoundingSphere bound2 = new BoundingSphere(new Point3d(0.0,0.0,0.0),2000000.0);
    background.setApplicationBounds(bound2);
    root.addChild(background);

/* WLH 13 April 99 - does nothing
    BoundingBox boundingbox =
      new BoundingBox(new Point3d(-1.0, -1.0, -1.0),
                      new Point3d(1.0, 1.0, 1.0));
    trans.addChild(new BoundingLeaf(boundingbox));
*/

/* WLH 13 Macrh 2000
    // create the BranchGroup that is the parent of direct
    // manipulation Data object BranchGroup objects
    direct = new BranchGroup();
    direct.setCapability(Group.ALLOW_CHILDREN_READ);
    direct.setCapability(Group.ALLOW_CHILDREN_WRITE);
    direct.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    direct.setCapability(Node.ENABLE_PICK_REPORTING);
    trans.addChild(direct);
*/

    // WLH 10 March 2000
    non_direct = new OrderedGroup();
    setSceneGraphObjectName(non_direct, "NonDirect");
    non_direct.setCapability(Group.ALLOW_CHILDREN_READ);
    non_direct.setCapability(Group.ALLOW_CHILDREN_WRITE);
    non_direct.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    non_direct.setCapability(Node.ENABLE_PICK_REPORTING);
    trans.addChild(non_direct);

    cursor_trans = new TransformGroup();
    setSceneGraphObjectName(cursor_trans, "CursorTrans");
    cursor_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
    cursor_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
    cursor_trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    trans.addChild(cursor_trans);
    cursor_switch = new Switch();
    setSceneGraphObjectName(cursor_switch, "CursorSwitch");
    cursor_switch.setCapability(Switch.ALLOW_SWITCH_READ);
    cursor_switch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    cursor_switch.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_trans.addChild(cursor_switch);
    cursor_on = new BranchGroup();
    setSceneGraphObjectName(cursor_on, "CursorOn");
    cursor_on.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_on.setCapability(Group.ALLOW_CHILDREN_WRITE);
    cursor_off = new BranchGroup();
    setSceneGraphObjectName(cursor_off, "CursorOff");
    cursor_off.setCapability(Group.ALLOW_CHILDREN_READ);
    cursor_switch.addChild(cursor_off);
    cursor_switch.addChild(cursor_on);
    cursor_switch.setWhichChild(0); // initially off
    cursorOn = false;

    box_switch = new Switch();
    setSceneGraphObjectName(box_switch, "BoxSwitch");
    box_switch.setCapability(Switch.ALLOW_SWITCH_READ);
    box_switch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    box_switch.setCapability(Group.ALLOW_CHILDREN_READ);
    trans.addChild(box_switch);
    box_on = new BranchGroup();
    setSceneGraphObjectName(box_on, "BoxOn");
    box_on.setCapability(Group.ALLOW_CHILDREN_READ);
    box_on.setCapability(Group.ALLOW_CHILDREN_WRITE);
    box_off = new BranchGroup();
    setSceneGraphObjectName(box_off, "BoxOff");
    box_off.setCapability(Group.ALLOW_CHILDREN_READ);
    box_switch.addChild(box_off);
    box_switch.addChild(box_on);
    box_switch.setWhichChild(1); // initially on
    try {
      setBoxOn(true);
    } catch (Exception e) {
    }

    scale_switch = new Switch();
    setSceneGraphObjectName(scale_switch, "ScaleSwitch");
    scale_switch.setCapability(Switch.ALLOW_SWITCH_READ);
    scale_switch.setCapability(Switch.ALLOW_SWITCH_WRITE);
    scale_switch.setCapability(Group.ALLOW_CHILDREN_READ);
    trans.addChild(scale_switch);
    scale_on = new BranchGroup();
    setSceneGraphObjectName(scale_on, "ScaleOn");
    scale_on.setCapability(Group.ALLOW_CHILDREN_READ);
    scale_on.setCapability(Group.ALLOW_CHILDREN_WRITE);
    scale_on.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    scale_off = new BranchGroup();
    setSceneGraphObjectName(scale_off, "ScaleOff");
    scale_off.setCapability(Group.ALLOW_CHILDREN_READ);
    scale_switch.addChild(scale_off);
    scale_switch.addChild(scale_on);
    scale_switch.setWhichChild(0); // initially off

    // WLH 23 Oct 2001
    try {
      Class modelClipClass = Class.forName("javax.media.j3d.ModelClip");
      Class[] param = new Class[] {};
      Constructor modelClipConstructor = modelClipClass.getConstructor(param);
      param = new Class[] {int.class, boolean.class};
      modelClipSetEnable = modelClipClass.getMethod("setEnable", param);
      param = new Class[] {int.class, javax.vecmath.Vector4d.class};
      modelClipSetPlane = modelClipClass.getMethod("setPlane", param);
      param = new Class[] {javax.media.j3d.Group.class};
      modelClipAddScope = modelClipClass.getMethod("addScope", param);
      param = new Class[] {int.class};
      Method modelClipSetCapability =
        modelClipClass.getMethod("setCapability", param);
      param = new Class[] {javax.media.j3d.Bounds.class};
      Method modelClipSetInfluencingBounds =
        modelClipClass.getMethod("setInfluencingBounds", param);
      modelClip = modelClipConstructor.newInstance(new Object[] {});
      int ALLOW_PLANE_WRITE =
        modelClipClass.getField("ALLOW_PLANE_WRITE").getInt(modelClip);
      modelClipSetCapability.invoke(modelClip,
                         new Object[] {new Integer(ALLOW_PLANE_WRITE)});
      int ALLOW_ENABLE_WRITE =
        modelClipClass.getField("ALLOW_ENABLE_WRITE").getInt(modelClip);
      modelClipSetCapability.invoke(modelClip,
                         new Object[] {new Integer(ALLOW_ENABLE_WRITE)});
      Boolean f = new Boolean(false);
      for (int i=0; i<6; i++) {
        modelClipSetEnable.invoke(modelClip, new Object[] {new Integer(i), f});
      }
      BoundingSphere bound3 =
        new BoundingSphere(new Point3d(0.0,0.0,0.0),2000000.0);
      modelClipSetInfluencingBounds.invoke(modelClip, new Object[] {bound3});
      background.setApplicationBounds(bound2);
      modelClipAddScope.invoke(modelClip, new Object[] {non_direct});
      setSceneGraphObjectName(((Node) modelClip), "ModelClip");
      trans.addChild((Node) modelClip);
    }
    catch (ClassNotFoundException e) {
    }
    catch (NoSuchMethodException e) {
    }
    catch (InstantiationException e) {
    }
    catch (IllegalAccessException e) {
    }
    catch (InvocationTargetException e) {
    }
    catch (NoSuchFieldException e) {
    }

    return root;
  }

  // WLH 23 Oct 2001
  /** 
   * Define a clipping plane in (XAxis, YAxis, ZAxis) space.  Allows
   * up to 6 arbitrary planes.  Each clip plane is defined by the equation:
   * <PRE>
   *      aX + bY + cZ + d <= 0
   * </PRE>
   * <p>Example useage:</p>
   * To clip to the usual VisAD cube (i.e., x, y and z values in the 
   * range -1.0 to +1.0) (see Test35.java), call:
   * <PRE>
   *    DisplayRendererJ3D dr = 
   *             (DisplayRendererJ3D) display.getDisplayRenderer();
   *    dr.setClip(0, true,  1.0f,  0.0f,  0.0f, -1.01f);  // X_POS face
   *    dr.setClip(1, true, -1.0f,  0.0f,  0.0f, -1.01f);  // X_NEG face
   *    dr.setClip(2, true,  0.0f,  1.0f,  0.0f, -1.01f);  // Y_POS face
   *    dr.setClip(3, true,  0.0f, -1.0f,  0.0f, -1.01f);  // Y_NEG face
   *    dr.setClip(4, true,  0.0f,  0.0f,  1.0f, -1.01f);  // Z_POS face
   *    dr.setClip(5, true,  0.0f,  0.0f, -1.0f, -1.01f);  // Z_NEG face
   * </PRE>
   * <b>Note:</b> d value is slightly less than -1.0 so items in the plane
   *              are not clipped.
   * @param  plane  plane number must be in (0, ..., 5)).
   * @param  enable true to enable clipping on this plane, false to disable
   * @param  a      x coefficent
   * @param  b      y coefficent
   * @param  c      z coefficent
   * @param  d      constant
   * @throws  VisADException  illegal plane argument or 
   *                          unsupported (&lt; 1.2) version of Java 3D
   */
  public void setClip(int plane, boolean enable, float a, float b, float c, float d)
         throws VisADException {
    if (not_destroyed == null) return;
    if (plane < 0 || 5 < plane) {
      throw new DisplayException("plane must be in 0,...,5 range " + plane);
    }
    if (modelClip == null ||
        modelClipSetEnable == null ||
        modelClipSetPlane == null) {
      throw new DisplayException("model clipping not supported in this " +
                                 "version of Java3D");
    }
    Vector4d vect = new Vector4d((double) a, (double) b, (double) c, (double) d);
    try {
      Object[] params = {new Integer(plane), new Boolean(enable)};
      modelClipSetEnable.invoke(modelClip, params);
      params = new Object[] {new Integer(plane), vect};
      modelClipSetPlane.invoke(modelClip, params);
      modelClipEnables[plane] = enable;
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  private void clipOff() {
    if (not_destroyed == null) return;
    try {
      for (int i=0; i<6; i++) {
        if (modelClipEnables[i]) {
          Object[] params = {new Integer(i), new Boolean(false)};
          modelClipSetEnable.invoke(modelClip, params);
        }
      }
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  private void clipOn() {
    if (not_destroyed == null) return;
    try {
      for (int i=0; i<6; i++) {
        if (modelClipEnables[i]) {
          Object[] params = {new Integer(i), new Boolean(true)};
          modelClipSetEnable.invoke(modelClip, params);
        }
      }
    }
    catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the <CODE>MouseBehavior</CODE> associated with this renderer.
   * @return  the <CODE>MouseBehavior</CODE> used by this renderer to handle
   *          mouse events.
   */
  public MouseBehavior getMouseBehavior() {
    return mouse;
  }

  /**
   * Get the <CODE>KeyboardBehavior</CODE> associated with this renderer.
   * 
   * @return the <CODE>KeyboardBehavior</CODE> used by this renderer to handle
   *         mouse events.
   */
  public KeyboardBehavior getKeyboardBehavior() {
    return keyboard;
  }

  public void addSceneGraphComponent(Group group) {
    if (not_destroyed == null) return;
    non_direct.addChild(group);
  }

  public void addLockedSceneGraphComponent(Group group) {
    if (not_destroyed == null || screen_locked == null) return;
    screen_locked.addChild(group);
  }

  //- TDR, Hydra stuff
  public void addLockedSceneGraphComponent(Group group, boolean initWithProj) {
    if (not_destroyed == null || screen_locked == null) return;
    if (initWithProj) {
      ProjectionControl proj = getDisplay().getProjectionControl();
      locked_trans.setTransform(new Transform3D(proj.getMatrix()));
    }
    screen_locked.addChild(group);
  }
                                                                                                                                         
  public void updateLockedTrans(double[] matrix) {
    if (locked_trans != null) {
      locked_trans.setTransform(new Transform3D(matrix));
    }
  }


  public void addDirectManipulationSceneGraphComponent(Group group,
                         DirectManipulationRendererJ3D renderer) {
    if (not_destroyed == null) return;
    // WLH 13 March 2000
    // direct.addChild(group);
    non_direct.addChild(group);
    directs.addElement(renderer);
  }


  public void clearScene(DataRenderer renderer) {
    if (not_destroyed == null) return;
    directs.removeElement(renderer);
  }

  /** 
   * Get the cusor location.
   * @return  cursor location as an array of x, y, and z values
   */
  public double[] getCursor() {
    double[] cursor = new double[3];
    cursor[0] = cursorX;
    cursor[1] = cursorY;
    cursor[2] = cursorZ;
    return cursor;
  }

  public void depth_cursor(VisADRay ray) {
    line_x = (float) ray.vector[0];
    line_y = (float) ray.vector[1];
    line_z = (float) ray.vector[2];
    point_x = cursorX;
    point_y = cursorY;
    point_z = cursorZ;
  }

  public void drag_depth(float diff) {
    if (not_destroyed == null) return;
    cursorX = point_x + diff * line_x;
    cursorY = point_y + diff * line_y;
    cursorZ = point_z + diff * line_z;
    setCursorLoc();
  }

  public void drag_cursor(VisADRay ray, boolean first) {
    if (not_destroyed == null) return;
    float o_x = (float) ray.position[0];
    float o_y = (float) ray.position[1];
    float o_z = (float) ray.position[2];
    float d_x = (float) ray.vector[0];
    float d_y = (float) ray.vector[1];
    float d_z = (float) ray.vector[2];
/*
    Point3d origin = new Point3d();
    Vector3d direction = new Vector3d();
    ray.get(origin, direction);
    float o_x = (float) origin.x;
    float o_y = (float) origin.y;
    float o_z = (float) origin.z;
    float d_x = (float) direction.x;
    float d_y = (float) direction.y;
    float d_z = (float) direction.z;
*/
    if (first) {
      line_x = d_x;
      line_y = d_y;
      line_z = d_z;
    }
    float dot = (cursorX - o_x) * line_x +
                (cursorY - o_y) * line_y +
                (cursorZ - o_z) * line_z;
    float dot2 = d_x * line_x + d_y * line_y + d_z * line_z;
    if (dot2 == 0.0) return;
    dot = dot / dot2;
    // new cursor location is intersection
    cursorX = o_x + dot * d_x;
    cursorY = o_y + dot * d_y;
    cursorZ = o_z + dot * d_z;
    setCursorLoc();
  }

  private void setCursorLoc() {
    if (not_destroyed == null) return;
    Transform3D t = new Transform3D();
    t.setTranslation(new Vector3f(cursorX, cursorY, cursorZ));
    cursor_trans.setTransform(t);
    if (cursorOn) {
      setCursorStringVector();
    }
  }

  /**
   * Set the cursor location
   * @param  x  x location
   * @param  y  y location
   * @param  z  z location
   */
  public void setCursorLoc(float x, float y, float z) {
    if (not_destroyed == null) return;
    Transform3D t = new Transform3D();
    t.setTranslation(new Vector3f(x, y, z));
    cursor_trans.setTransform(t);
    if (cursorOn) {
      setCursorStringVector();
    }
  }

  /**
   * Whenever <CODE>cursorOn</CODE> or <CODE>directOn</CODE> is true,
   * display Strings in cursorStringVector.
   * @param canvas
   */
  
  public void drawCursorStringVector(VisADCanvasJ3D canvas) {
    
	if (not_destroyed == null) return;

    GraphicsContext3D graphics = canvas.getGraphicsContext3D();
    // graphics.setModelClip(null);
    // causes NullPointerException at GraphicsContext3D.java:689

    // set cursor color, if possible
    try {
      float[] c3 = getCursorColor();
      Appearance appearance = new Appearance();
      ColoringAttributes color = new ColoringAttributes();
      color.setColor(new Color3f(c3));
      appearance.setColoringAttributes(color);
      graphics.setAppearance(appearance);
    } catch (Exception e) {
    }

    Point3d position1 = new Point3d();
    Point3d position2 = new Point3d();
    Point3d position3 = new Point3d();
    canvas.getPixelLocationInImagePlate(1, 10, position1);
    canvas.getPixelLocationInImagePlate(10, 10, position2);
    canvas.getPixelLocationInImagePlate(1, 1, position3);

    DisplayImpl display = getDisplay();
    if (display != null && display.getGraphicsModeControl() != null) {
      // hack to move text closer to eye
      if (getDisplay().getGraphicsModeControl().getProjectionPolicy() ==
          View.PERSPECTIVE_PROJECTION) {
        Point3d left_eye = new Point3d();
        Point3d right_eye = new Point3d();
        canvas.getLeftEyeInImagePlate(left_eye);
        canvas.getRightEyeInImagePlate(right_eye);
        Point3d eye = new Point3d((left_eye.x + right_eye.x)/2.0,
                                  (left_eye.y + right_eye.y)/2.0,
                                  (left_eye.z + right_eye.z)/2.0);
        double alpha = 0.3;
        position1.x = alpha * position1.x + (1.0 - alpha) * eye.x;
        position1.y = alpha * position1.y + (1.0 - alpha) * eye.y;
        position1.z = alpha * position1.z + (1.0 - alpha) * eye.z;
        position2.x = alpha * position2.x + (1.0 - alpha) * eye.x;
        position2.y = alpha * position2.y + (1.0 - alpha) * eye.y;
        position2.z = alpha * position2.z + (1.0 - alpha) * eye.z;
        position3.x = alpha * position3.x + (1.0 - alpha) * eye.x;
        position3.y = alpha * position3.y + (1.0 - alpha) * eye.y;
        position3.z = alpha * position3.z + (1.0 - alpha) * eye.z;
      }
    }
// end of hack to move text closer to eye

    Transform3D t = new Transform3D();
    canvas.getImagePlateToVworld(t);
    t.transform(position1);
    t.transform(position2);
    t.transform(position3);

    // draw cursor strings in upper left corner of screen
    double[] start = {(double) position1.x,
                      (double) position1.y,
                      (double) position1.z};
    double[] base =  {(double) (position2.x - position1.x),
                      (double) (position2.y - position1.y),
                      (double) (position2.z - position1.z)};
    double[] up =    {(double) (position3.x - position1.x),
                      (double) (position3.y - position1.y),
                      (double) (position3.z - position1.z)};
    if (cursorOn || directOn) {
    	Enumeration strings = getCursorStringVector().elements();
    	while (strings.hasMoreElements()) {
    		String string = (String) strings.nextElement();
    		if ((string != null) && (! string.trim().isEmpty())) {
    			try {
    				VisADLineArray array =
    						PlotText.render_label(string, start, base, up, false);
    				graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
    				start[1] -= 1.2 * up[1];
    			}
    			catch (VisADException e) {
    			}
    		}
    	}
    }

    // draw Exception strings in lower left corner of screen
    double[] startl = {(double) position3.x,
                       (double) -position3.y,
                       (double) position3.z};
    Vector rendererVector = getDisplay().getRendererVector();
    Enumeration renderers = rendererVector.elements();
    while (renderers.hasMoreElements()) {
      DataRenderer renderer = (DataRenderer) renderers.nextElement();
      Vector exceptionVector = renderer.getExceptionVector();
      Enumeration exceptions = exceptionVector.elements();
      while (exceptions.hasMoreElements()) {
        Exception error = (Exception) exceptions.nextElement();
        String string = error.getMessage();
        try {
          VisADLineArray array =
            PlotText.render_label(string, startl, base, up, false);
          graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
          startl[1] += 1.2 * up[1];
        }
        catch (VisADException e) {
        }
      }
    }

    // draw wait flag in lower left corner of screen
    if (getWaitFlag() && getWaitMessageVisible()) {
      try {
        VisADLineArray array =
          PlotText.render_label("please wait . . .", startl, base, up, false);
        graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
        startl[1] += 1.2 * up[1];
      }
      catch (VisADException e) {
      }
    }

    // draw Animation string in lower right corner of screen
    String[] animation_string = getAnimationString();
    if (animation_string[0] != null) {
      int nchars = animation_string[0].length();
      if (nchars < 12) nchars = 12;
      double[] starta = {(double) (-position2.x - nchars *
                                        (position2.x - position1.x)),
                         (double) -position3.y + 1.2 * up[1],
                         // (double) position2.y, WLH 30 April 99
                         (double) position2.z};
      try {
        VisADLineArray array =
          PlotText.render_label(animation_string[0], starta, base, up, false);
        graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
        starta[1] -= 1.2 * up[1];
        if (animation_string[1] != null) {
          array =
            PlotText.render_label(animation_string[1], starta, base, up, false);
          graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));
          starta[1] -= 1.2 * up[1];
        }
      }
      catch (VisADException e) {
      }
    }

    if (scale_switch != null && scale_switch.getWhichChild() == 1) {
      Dimension d = canvas.getSize();
      int w = d.width;
      int h = d.height;

      double MUL = 3.0 * w / 256.0;
      double XMAX = Math.abs(MUL * position2.x - (MUL - 1.0) * position3.x);
      double YMAX = Math.abs(MUL * position2.y - (MUL - 1.0) * position3.y);
      double XMIN = -XMAX;
      double YMIN = -YMAX;

      TransformGroup trans = getTrans();
      Transform3D tt = new Transform3D();
      trans.getTransform(tt);
      tt.invert();
      Point3d positionx = new Point3d(XMAX, YMAX, 0.0);
      Point3d positionn = new Point3d(XMIN, YMIN, 0.0);
      tt.transform(positionx);
      tt.transform(positionn);

      double XTMAX = positionx.x;
      double YTMAX = positionx.y;
      double XTMIN = positionn.x;
      double YTMIN = positionn.y;

      Enumeration axes = axis_vector.elements();
      while (axes.hasMoreElements()) {
        AxisScale axisScale = (AxisScale) axes.nextElement();
        try {
          boolean success =
            axisScale.makeScreenBasedScale(XMIN, YMIN, XMAX, YMAX,
                                           XTMIN, YTMIN, XTMAX, YTMAX);
          if (success) {
// System.out.println("makeScreenBasedScale success");
            int axis = axisScale.getAxis();
            int axis_ordinal = axisScale.getAxisOrdinal();
            VisADLineArray array = axisScale.getScaleArray();
            VisADTriangleArray labels = axisScale.getLabelArray();
            float[] scale_color = axisScale.getColor().getColorComponents(null);
  
            // set cursor color, if possible
            Appearance appearance = new Appearance();
            ColoringAttributes color = new ColoringAttributes();
            color.setColor(new Color3f(scale_color));
            appearance.setColoringAttributes(color);
            graphics.setAppearance(appearance);
            graphics.draw(((DisplayImplJ3D) getDisplay()).makeGeometry(array));

            if (labels != null) {
              GeometryArray labelGeometry = 
                ((DisplayImplJ3D) getDisplay()).makeGeometry(labels);
              Appearance labelAppearance =
                ShadowTypeJ3D.staticMakeAppearance(
                    getDisplay().getGraphicsModeControl(), null, null, 
                    labelGeometry, true);
              graphics.setAppearance(labelAppearance);
              graphics.draw(labelGeometry);
            }
          }
          else {
//  System.out.println("makeScreenBasedScale fail");
          }
        } catch (Exception e) {
        }
      }
    }
    // graphics.flush(true); doesn't help
    // clipOn(); doesn't work
  }

  /**
   * Find the <CODE>DataRenderer</CODE> that is closest to the ray and
   * uses the specified mouse modifiers for direct manipulation.
   * @param  ray  position to check
   * @param  mouseModifiers  modifiers for mouse clicks
   * @return  closest DataRenderer that uses the specified mouse click
   *          modifiers for direct manipulation or null if there is none.
   */
  public DataRenderer findDirect(VisADRay ray, int mouseModifiers) {
    if (not_destroyed == null) return null;
    DirectManipulationRendererJ3D renderer = null;
    float distance = Float.MAX_VALUE;
    Enumeration renderers = ((Vector) directs.clone()).elements();
    while (renderers.hasMoreElements()) {
      DirectManipulationRendererJ3D r =
        (DirectManipulationRendererJ3D) renderers.nextElement();
      if (r.getEnabled()) {
        r.setLastMouseModifiers(mouseModifiers);
        float d = r.checkClose(ray.position, ray.vector);
        if (d < distance) {
          distance = d;
          renderer = r;
        }
      }
    }
    if (distance < getPickThreshhold()) {
      return renderer;
    }
    else {
      return null;
    }
  }

  /**
   * Check to see if there are any <CODE>DirectManipulationRenderer</CODE>s
   * in this display.
   * @return  true if there are any
   */
  public boolean anyDirects() {
    if (not_destroyed == null) return false;
    return !directs.isEmpty();
  }

  /**
   * Set the scales on.
   * @param  on   turn on if true, otherwise turn them off
   */
  public void setScaleOn(boolean on) {
    if (not_destroyed == null) return;
    if (on) {
      scale_switch.setWhichChild(1); // on
    }
    else {
      scale_switch.setWhichChild(0); // off
    }
  }

  /**
   * Set the scale for the appropriate axis.
   * @param  axisScale  AxisScale for this scale
   * @throws  VisADException  couldn't set the scale
   */
  public void setScale(AxisScale axisScale)
         throws VisADException {
    if (not_destroyed == null) return;
    if (axisScale.getScreenBased() && getMode2D()) {
      if (!axis_vector.contains(axisScale)) {
        axis_vector.addElement(axisScale);

        clearScale(axisScale);
	/*  Replaced by clearScale()  2001-08-08  DRM
        // eliminate any non-screen-based scale for this AxisScale
        int axis = axisScale.getAxis();
        int axis_ordinal = axisScale.getAxisOrdinal();
        int dim = getMode2D() ? 2 : 3;
        synchronized (scale_on) {
          int n = scale_on.numChildren();
          int m = dim * axis_ordinal + axis;
          if (m >= n) {
            for (int i=n; i<=m; i++) {
              BranchGroup empty = new BranchGroup();
              empty.setCapability(BranchGroup.ALLOW_DETACH);
              empty.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
              scale_on.addChild(empty);
            }
          }
          BranchGroup empty = new BranchGroup();
          empty.setCapability(BranchGroup.ALLOW_DETACH);
          empty.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
          scale_on.setChild(empty, m);
        }
	*/
      }
    }
    else {
      setScale(axisScale.getAxis(),
               axisScale.getAxisOrdinal(),
               axisScale.getScaleArray(),
               axisScale.getLabelArray(),
               axisScale.getColor().getColorComponents(null));
    }
  }

  /**
   * Set the scale for the appropriate axis.
   * @param  axis  axis for this scale (0 = XAxis, 1 = YAxis, 2 = ZAxis)
   * @param  axis_ordinal  position along the axis
   * @param  array   <CODE>VisADLineArray</CODE> representing the scale plot
   * @param  scale_color   array (dim 3) representing the red, green and blue
   *                       color values.
   * @throws  VisADException  couldn't set the scale
   */
  public void setScale(int axis, int axis_ordinal,
              VisADLineArray array, float[] scale_color)
         throws VisADException {
    if (not_destroyed == null) return;
    setScale(axis, axis_ordinal, array, null, scale_color);
  }

  /**
   * Set the scale for the appropriate axis.
   * @param  axis  axis for this scale (0 = XAxis, 1 = YAxis, 2 = ZAxis)
   * @param  axis_ordinal  position along the axis
   * @param  array   <CODE>VisADLineArray</CODE> representing the scale plot
   * @param  labels  <CODE>VisADTriangleArray</CODE> representing the labels
   *                 created using a font (can be null)
   * @param  scale_color   array (dim 3) representing the red, green and blue
   *                       color values.
   * @throws  VisADException  couldn't set the scale
   */
  public void setScale(int axis, int axis_ordinal,
              VisADLineArray array, VisADTriangleArray labels,
              float[] scale_color)
         throws VisADException {
    if (not_destroyed == null) return;
// DisplayImpl.printStack("setScale");
    // add array to scale_on
    // replace any existing at axis, axis_ordinal
    DisplayImplJ3D display = (DisplayImplJ3D) getDisplay();
    GeometryArray geometry = display.makeGeometry(array);
    GraphicsModeControl mode = display.getGraphicsModeControl();
    ColoringAttributes color = new ColoringAttributes();
    color.setColor(scale_color[0], scale_color[1], scale_color[2]);
    Appearance appearance =
      ShadowTypeJ3D.staticMakeAppearance(mode, null, color, geometry, false);
    Shape3D shape = new Shape3D(geometry, appearance);
    shape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
    shape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
    BranchGroup group = new BranchGroup();
    group.setCapability(BranchGroup.ALLOW_DETACH);
    group.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
    group.addChild(shape);
    if (labels != null) {
      GeometryArray labelGeometry = display.makeGeometry(labels);
      Appearance labelAppearance =
        ShadowTypeJ3D.staticMakeAppearance(mode, null, null,
                                           labelGeometry, true);
      Shape3D labelShape = new Shape3D(labelGeometry, labelAppearance);
      labelShape.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
      labelShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
      
      group.addChild(labelShape);


      if (labels instanceof VisADTriangleArray) {
        GeometryArray labelGeometry2 = display.makeGeometry(labels);
        Appearance labelAppearance2 =
          ShadowTypeJ3D.staticMakeAppearance(mode, null, null,
                                             labelGeometry2, true);

        // LineAttributes la = labelAppearance2.getLineAttributes();
        // better without anti-aliasing
        // la.setLineAntialiasingEnable(true);

        PolygonAttributes pa = labelAppearance2.getPolygonAttributes();
        pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
        Shape3D labelShape2 = new Shape3D(labelGeometry2, labelAppearance2);
        labelShape2.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
        labelShape2.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
        group.addChild(labelShape2);
      }


    }
    // may only add BranchGroup to 'live' scale_on
    int dim = getMode2D() ? 2 : 3;
    synchronized (scale_on) {
      int n = scale_on.numChildren();
      int m = dim * axis_ordinal + axis;
      if (m >= n) {
        for (int i=n; i<=m; i++) {
          BranchGroup empty = new BranchGroup();
          empty.setCapability(BranchGroup.ALLOW_DETACH);
          empty.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
          scale_on.addChild(empty);
        }
      }
      scale_on.setChild(group, m);
    }
  }

  /**
   * Remove all the scales being rendered.
   */
  public void clearScales() {
    if (not_destroyed == null) return;
    if (scale_on != null) {
      synchronized (scale_on) {
        int n = scale_on.numChildren();
        for (int i=n-1; i>=0; i--) {
          scale_on.removeChild(i);
        }
      }
    }
    axis_vector.removeAllElements();
  }

  /**
   * Remove a particular scale being rendered.
   * @param axisScale  AxisScale to remove
   */
  
  public void clearScale(AxisScale axisScale) {
    if (not_destroyed == null) return;
    // eliminate any non-screen-based scale for this AxisScale
    int axis = axisScale.getAxis();
    int axis_ordinal = axisScale.getAxisOrdinal();
    int dim = getMode2D() ? 2 : 3;
    synchronized (scale_on) {
      int n = scale_on.numChildren();
      int m = dim * axis_ordinal + axis;
      if (m >= n) {
        for (int i=n; i<=m; i++) {
          BranchGroup empty = new BranchGroup();
          empty.setCapability(BranchGroup.ALLOW_DETACH);
          empty.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
          scale_on.addChild(empty);
        }
      }
      BranchGroup empty = new BranchGroup();
      empty.setCapability(BranchGroup.ALLOW_DETACH);
      empty.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
      scale_on.setChild(empty, m);
    }
  }

  public void setTransform3D(Transform3D t) {
    if (not_destroyed == null) return;
    if (trans == null) {
      trans = new TransformGroup();
      setSceneGraphObjectName(trans, "Trans");
      trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
      trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
      trans.setCapability(Group.ALLOW_CHILDREN_READ);
      trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
      trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
    }
    if (t != null) {
      trans.setTransform(t);
      if (locked_trans == null && root != null) {
        locked_trans = new TransformGroup();
        setSceneGraphObjectName(locked_trans, "LockedTrans");
        locked_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
        locked_trans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        locked_trans.setCapability(Group.ALLOW_CHILDREN_READ);
        locked_trans.setCapability(Group.ALLOW_CHILDREN_WRITE);
        locked_trans.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        locked_trans.setTransform(t);
        screen_locked = new OrderedGroup();
        setSceneGraphObjectName(screen_locked, "ScreenLocked");
        screen_locked.setCapability(Group.ALLOW_CHILDREN_READ);
        screen_locked.setCapability(Group.ALLOW_CHILDREN_WRITE);
        screen_locked.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        screen_locked.setCapability(Node.ENABLE_PICK_REPORTING);
        locked_trans.addChild(screen_locked);
        BranchGroup bgroup = new BranchGroup();
        setSceneGraphObjectName(bgroup, "LockedGroup");
        bgroup.setCapability(Group.ALLOW_CHILDREN_READ);
        bgroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
        bgroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
        bgroup.addChild(locked_trans);
        root.addChild(bgroup);
      }
    }
  }

  /**
   * Factory for constructing a subclass of <CODE>Control</CODE>
   * appropriate for the graphics API and for this
   * <CODE>DisplayRenderer</CODE>; invoked by <CODE>ScalarMap</CODE>
   * when it is <CODE>addMap()</CODE>ed to a <CODE>Display</CODE>.
   * @param map The <CODE>ScalarMap</CODE> for which a <CODE>Control</CODE>
   *            should be built.
   * @return The appropriate <CODE>Control</CODE>.
   */
  public Control makeControl(ScalarMap map) {
    if (not_destroyed == null) return null;
    DisplayRealType type = map.getDisplayScalar();
    DisplayImplJ3D display = (DisplayImplJ3D) getDisplay();
    if (type == null) return null;
    if (type.equals(Display.XAxis) ||
        type.equals(Display.YAxis) ||
        type.equals(Display.ZAxis) ||
        type.equals(Display.Latitude) ||
        type.equals(Display.Longitude) ||
        type.equals(Display.Radius)) {
      return (ProjectionControlJ3D) display.getProjectionControl();
    }
    else if (type.equals(Display.RGB) ||
             type.equals(Display.HSV) ||
             type.equals(Display.CMY)) {
      return new ColorControl(display);
    }
    else if (type.equals(Display.RGBA)) {
      return new ColorAlphaControl(display);
    }
    else if (type.equals(Display.Animation)) {
      // note only one RealType may be mapped to Animation
      // so control must be null
      Control control = display.getControl(AnimationControlJ3D.class);
      if (control != null) return control;
      else return new AnimationControlJ3D(display, (RealType) map.getScalar());
    }
    else if (type.equals(Display.SelectValue)) {
      return new ValueControlJ3D(display);
    }
    else if (type.equals(Display.SelectRange)) {
      return new RangeControl(display);
    }
    else if (type.equals(Display.IsoContour)) {
      return new ContourControl(display);
    }
    else if (type.equals(Display.Flow1X) ||
             type.equals(Display.Flow1Y) ||
             type.equals(Display.Flow1Z) ||
             type.equals(Display.Flow1Elevation) ||
             type.equals(Display.Flow1Azimuth) ||
             type.equals(Display.Flow1Radial)) {
      Control control = display.getControl(Flow1Control.class);
      if (control != null) return control;
      else return new Flow1Control(display);
    }
    else if (type.equals(Display.Flow2X) ||
             type.equals(Display.Flow2Y) ||
             type.equals(Display.Flow2Z) ||
             type.equals(Display.Flow2Elevation) ||
             type.equals(Display.Flow2Azimuth) ||
             type.equals(Display.Flow2Radial)) {
      Control control = display.getControl(Flow2Control.class);
      if (control != null) return control;
      else return new Flow2Control(display);
    }
    else if (type.equals(Display.Shape)) {
      return new ShapeControl(display);
    }
    else if (type.equals(Display.Text)) {
      return new TextControl(display);
    }
    else {
      return null;
    }
  }

  /**
   * Create the default <CODE>DataRenderer</CODE> for this type of 
   * <CODE>DisplayRenderer</CODE>
   * @return  new default renderer
   */
  public DataRenderer makeDefaultRenderer() {
    return new DefaultRendererJ3D();
  }

  /**
   * Check if the <CODE>DataRenderer</CODE> in question is legal for this
   * <CODE>DisplayRenderer</CODE>
   * @param renderer  <CODE>DataRenderer</CODE> to check
   * @return  true if renderer is a subclass of <CODE>RendererJ3D</CODE>
   */
  public boolean legalDataRenderer(DataRenderer renderer) {
    return (renderer instanceof RendererJ3D);
  }

  public void rendererDeleted(DataRenderer renderer)
  {
    if (not_destroyed == null) return;
    clearScene(renderer);
  }

  public void setLineWidth(float width) {
  }

  /**
   * Add a <CODE>KeyboardBehavior</CODE> for keyboard control of rotation,
   * translation and zoom.  
   * @param  behavior  keyboard behavior to add
   */
  public void addKeyboardBehavior(KeyboardBehaviorJ3D behavior)
  {
    if (not_destroyed == null) return;
    BranchGroup bg = new BranchGroup();
    bg.setCapability(Group.ALLOW_CHILDREN_READ);
    bg.addChild(behavior);
    trans.addChild(bg);
  }

  public void render_trigger() {
    ProjectionControl proj = getDisplay().getProjectionControl();
    try {
      if (proj != null) proj.setMatrix(proj.getMatrix());
    }
    catch (VisADException e) { }
    catch (RemoteException e) { }
  }

  public void setWaitFlag(boolean b) {
    if (not_destroyed == null) return;
    boolean old = getWaitFlag();
    super.setWaitFlag(b);
    if (b != old) {
      render_trigger();
    }
  }

  public int getTextureWidthMax() {
    return VisADCanvasJ3D.getTextureWidthMax();
  }

  public int getTextureHeightMax() {
    return VisADCanvasJ3D.getTextureWidthMax();
  }

}

