//
// VisADCanvasJ3D.java
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

package visad.java3d;


import visad.*;

import javax.media.j3d.*;

import java.awt.*;
import java.awt.image.BufferedImage;

import java.rmi.RemoteException;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.sun.j3d.utils.universe.SimpleUniverse;
import java.util.Iterator;


/**
   VisADCanvasJ3D is the VisAD extension of Canvas3D
*/
public class VisADCanvasJ3D extends Canvas3D {

  /**           */
  private DisplayRendererJ3D displayRenderer;

  /**           */
  private DisplayImplJ3D display;

  /**           */
  private Component component;

  /**           */
  Dimension prefSize = new Dimension(0, 0);

  /**           */
  boolean captureFlag = false;

  /**           */
  BufferedImage captureImage = null;

  // size of image for off screen rendering

  /**           */
  private int width;

  /**           */
  private int height;

  /**           */
  private static int textureWidthMax = 0;

  /**           */
  private static int textureHeightMax = 0;

  /**           */
  private final static int textureWidthMaxDefault = 1024;

  /**           */
  private final static int textureHeightMaxDefault = 1024;

  /**           */
  private boolean offscreen = false;

  /**           */
  private static GraphicsConfiguration defaultConfig = null;

  /**           */
  private GraphicsConfiguration myConfig = null;

  /** tracks whether stop has been called           */
  private boolean stopCalled = false;



  /**
   * Make the graphics configuration
   * @param offscreen  true if this is offscreen rendering (sets double
   *                   buffering to UNNECESSARY)
   * @return the graphics configuration
   */
  private static GraphicsConfiguration makeConfig(boolean offscreen) {
    GraphicsEnvironment e = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice d = e.getDefaultScreenDevice();
    // GraphicsConfiguration c = d.getDefaultConfiguration();
/* fix suggested by John Brecht from http://www.j3d.org/faq/running.html#flicker
    GraphicsConfigTemplate3D gct3d = new GraphicsConfigTemplate3D();
    GraphicsConfiguration c = gct3d.getBestConfiguration(d.getConfigurations());
*/
    GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
    if (offscreen) {
      template.setDoubleBuffer(GraphicsConfigTemplate3D.UNNECESSARY);
    }
    GraphicsConfiguration c = d.getBestConfiguration(template);
    return c;
  }

  /**
   * Set up the texture properties from the GraphicsConfiguration
   * @param c  GraphicsConfiguration
   */
  private void setTextureProperties() {
    //- see if we've already set this
    if (!((textureHeightMax == 0) && (textureWidthMax == 0))) return;
    //- determine textureWidthMax ---------------------------------------
    //- first check user-defined cmd-line specs:
    String prop = null;
    try {
      prop = System.getProperty("textureWidthMax");
    }
    catch (Exception exp) {
      prop = null;
    }
    textureWidthMax = (prop == null)
                      ? textureWidthMax
                      : Integer.parseInt(prop);

    // no user defined values, so query Java3D, or set to defaults
    if ((textureHeightMax == 0) && (textureWidthMax == 0)) {
      Integer wProp = null;
      Integer hProp = null;
      Canvas3D cnvs = new Canvas3D(myConfig);
      try {
        java.lang.reflect.Method method =
          cnvs.getClass().getMethod("queryProperties", (Class[])null);
        java.util.Map propertiesMap = (java.util.Map)method.invoke(cnvs,
                                        (Object[])null);
        wProp = (Integer)propertiesMap.get("textureWidthMax");
        hProp = (Integer)propertiesMap.get("textureHeightMax");
        /*
        Iterator iter = propertiesMap.keySet().iterator();
        System.out.println("----------------------------------------------------------");
        while (iter.hasNext()) {
           Object key = iter.next();
           System.out.println(key+":  "+propertiesMap.get(key));
           System.out.println("----------------------------------------------------------");
        }
        */
      }
      catch (Exception exc) {
      }

      if ((wProp == null) || (hProp == null)) {
        textureWidthMax = textureWidthMaxDefault;
        textureHeightMax = textureHeightMaxDefault;
        System.out.println(
          "This version of Java3D can't query \"textureWidthMax/textureHeightMax\"\n" +
          "so they are being assigned the default values: \n" +
          "textureWidthMax:  " + textureWidthMaxDefault + "\n" +
          "textureHeightMax:  " + textureHeightMaxDefault);
        System.out.println(
          "If images render as a 'grey-box', try setting these parameters\n" +
          "to a lower value, eg. 512, with '-DtextureWidthMax=512'\n" +
          "Otherwise check your graphics environment specifications");
      }
      else {
        textureWidthMax = wProp.intValue();
        textureHeightMax = hProp.intValue();
      }
    }

  }

  /**
   * Get the default configuration.
   * @return the default configuration
   */
  public static GraphicsConfiguration getDefaultConfig() {
    return defaultConfig;
  }

  /**
   * Create the canvase for the renderer.
   * @param renderer  the renderer for this canvas
   */
  public VisADCanvasJ3D(DisplayRendererJ3D renderer) {
    this(renderer, null);
  }

  /**
   * Create the canvase for the renderer with the specified configuration.
   * @param renderer  the renderer for this canvas
   * @param config  GraphicsConfiguration (may be null - in which case
   *                a default configuration is used)
   */
  public VisADCanvasJ3D(DisplayRendererJ3D renderer,
                        GraphicsConfiguration config) {
    super(config == null
          ? defaultConfig = (defaultConfig == null
                             ? makeConfig(false)
                             : defaultConfig)
          : config);
    myConfig = (config == null)
               ? defaultConfig
               : config;

    setTextureProperties();
    displayRenderer = renderer;
    display = (DisplayImplJ3D)renderer.getDisplay();
    component = null;
  }

  /**
   * Set the component for this canvas.
   * @param c Component to use
   */
  void setComponent(Component c) {
    component = c;
  }

  /**           */
  private static final double METER_RATIO = (0.0254 / 90.0); // from Java3D docs

  /**
   * Constructor for offscreen rendering.
   * @param renderer   renderer to use
   * @param w          width of canvas
   * @param h          height of canvas
   *
   * @throws VisADException 
   */
  public VisADCanvasJ3D(DisplayRendererJ3D renderer, int w, int h)
          throws VisADException {

// to disable off screen rendering (if you have lower than Java3D
// version 1.2.1 installed), uncomment out the following six lines (the
// super and throw statements)

    /**
     * super(defaultConfig);
     * throw new VisADException("\n\nFor off screen rendering in Java3D\n" +
     *      "please edit visad/java3d/VisADCanvasJ3D.java as follows:\n" +
     *      "remove or comment-out \"super(defaultConfig);\" and the\n" +
     *      "  throw statement for this Exception,\n" +
     *      "and un-comment the body of this constructor\n");
     */
// AND comment out the rest of this constructor,
    super(defaultConfig = (defaultConfig == null
                           ? makeConfig(true)
                           : defaultConfig), true);
    myConfig = defaultConfig;
    setTextureProperties();
    displayRenderer = renderer;
    display = (DisplayImplJ3D)renderer.getDisplay();
    component = null;
    offscreen = true;
    width = w;
    height = h;
    BufferedImage image = new BufferedImage(width, height,
                                            BufferedImage.TYPE_INT_RGB);
    ImageComponent2D image2d =
      new ImageComponent2D(ImageComponent2D.FORMAT_RGB, image);
    setOffScreenBuffer(image2d);
    Screen3D screen = getScreen3D();
    int screen_width = 1280;
    int screen_height = 1024;
    screen.setSize(screen_width, screen_height);
    double width_in_meters = screen_width * METER_RATIO;
    double height_in_meters = screen_height * METER_RATIO;
    screen.setPhysicalScreenWidth(width_in_meters);
    screen.setPhysicalScreenHeight(height_in_meters);
  }

  /**
   * Set the display associated with this canvas (from renderer)
   */
  void setDisplay() {
    if (display == null) {
      display = (DisplayImplJ3D)displayRenderer.getDisplay();
    }
  }

  /**
   * See if this is an offscreen rendering.
   * @return true if offscreen.
   */
  public boolean getOffscreen() {
    return offscreen;
  }

  /**
   * Render the readout for the field at index.
   * @param i index.
   */
  public void renderField(int i) {
    displayRenderer.drawCursorStringVector(this);
  }

  /**
   * Override base class method for grabbing image.
   */
  public void postSwap() {
    // make sure stop() wasn't called before callback completed
    if (display == null) return;

    if (captureFlag || display.hasSlaves()) {
      // WLH 18 March 99 - SRP suggests that in some implementations
      // this may need to be in postRender (invoked before buffer swap)
      captureFlag = false;

      int width = getSize().width;
      int height = getSize().height;
      GraphicsContext3D ctx = getGraphicsContext3D();
      Raster ras = new Raster();
      ras.setType(Raster.RASTER_COLOR);
      ras.setSize(width, height);
      ras.setOffset(0, 0);
      BufferedImage image = new BufferedImage(width, height,
                              BufferedImage.TYPE_INT_RGB);
      ImageComponent2D image2d =
        new ImageComponent2D(ImageComponent2D.FORMAT_RGB, image);
      ras.setImage(image2d);

      ctx.readRaster(ras);

      // Now strip out the image info
      ImageComponent2D img_src = ras.getImage();
      if (captureImage != null) captureImage.flush();
      captureImage = img_src.getImage();
      displayRenderer.notifyCapture();

      // CTR 21 Sep 99 - send BufferedImage to any attached slaved displays
      if (display.hasSlaves()) display.updateSlaves(captureImage);
    }
    // WLH 15 March 99
    if (offscreen) {
      Runnable notify = new Runnable() {
        public void run() {
          try {
            //Check if the display is null. We get this when doing off screen
            //image capture from the IDV
            DisplayImplJ3D tmpDisplay = display;
            if (tmpDisplay != null) {
              tmpDisplay.notifyListeners(DisplayEvent.FRAME_DONE, 0, 0);
            }
          }
          catch (VisADException e) {
          }
          catch (RemoteException e) {
          }
        }
      };
      Thread t = new Thread(notify);
      t.start();
    }
    else {
      try {
        display.notifyListeners(DisplayEvent.FRAME_DONE, 0, 0);
      }
      catch (VisADException e) {
      }
      catch (RemoteException e) {
      }
    }
  }

  /**
   * Get the preferred size of this canvas
   * @return the preferred size
   */
  public Dimension getPreferredSize() {
    return prefSize;
  }

  /**
   * Set the preferred size of this canvas
   * @param size the preferred size
   */
  public void setPreferredSize(Dimension size) {
    prefSize = size;
  }

  /**
   * Get the maximum texture width supported by this display
   * @return the maximum texture width
   */
  public static int getTextureWidthMax() {
    return textureWidthMax;
  }

  /**
   * Get the maximum texture height supported by this display
   * @return the maximum texture height
   */
  public static int getTextureHeightMax() {
    return textureHeightMax;
  }

  /**
   * Method to test this class
   *
   * @param args 
   *
   * @throws RemoteException 
   * @throws VisADException 
   */
  public static void main(String[] args)
          throws RemoteException, VisADException {
    DisplayImplJ3D display = new DisplayImplJ3D("offscreen", 300, 300);

    RealType[] types = {RealType.Latitude, RealType.Longitude};
    RealTupleType earth_location = new RealTupleType(types);
    RealType vis_radiance = RealType.getRealType("vis_radiance");
    RealType ir_radiance = RealType.getRealType("ir_radiance");
    RealType[] types2 = {vis_radiance, ir_radiance};
    RealTupleType radiance = new RealTupleType(types2);
    FunctionType image_tuple = new FunctionType(earth_location, radiance);

    int size = 32;
    FlatField imaget1 = FlatField.makeField(image_tuple, size, false);

    display.addMap(new ScalarMap(RealType.Latitude, Display.YAxis));
    display.addMap(new ScalarMap(RealType.Longitude, Display.XAxis));
    display.addMap(new ScalarMap(vis_radiance, Display.RGB));
    display.addMap(new ScalarMap(vis_radiance, Display.IsoContour));

    DataReferenceImpl ref_imaget1 = new DataReferenceImpl("ref_imaget1");
    ref_imaget1.setData(imaget1);
    display.addReference(ref_imaget1, null);

    JFrame jframe1 = new JFrame("test off screen");
    jframe1.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
    panel1.setAlignmentY(JPanel.TOP_ALIGNMENT);
    panel1.setAlignmentX(JPanel.LEFT_ALIGNMENT);
    jframe1.setContentPane(panel1);
    jframe1.pack();
    jframe1.setSize(300, 300);
    jframe1.setVisible(true);

    while(true) {
      Graphics gp = panel1.getGraphics();
      BufferedImage image = display.getImage();
      gp.drawImage(image, 0, 0, panel1);
      System.out.println("drawImage");
      gp.dispose();
      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
      }
    }

  }



  /**
   * Stop the applet
   */
  public void stop() {
    //If we have already been called then return
    if (stopCalled) {
      return;
    }
    stopCalled = true;

    if (!offscreen) {
      stopRenderer();
    }
    display = null;
    displayRenderer = null;
    if (component != null) {
      try {
        if (component instanceof DisplayPanelJ3D) {
          ((DisplayPanelJ3D)component).destroy();
        }
        else if (component instanceof DisplayAppletJ3D) {
          ((DisplayAppletJ3D)component).destroy();
        }
      }
      catch (Exception exc) {
        //jeffmc: we kept getting these exceptions so for now
        //just print out the error cond continue on
        System.err.println("Error destroying java3d component");
        exc.printStackTrace();
      }
      component = null; // WLH 17 Dec 2001
    }
    captureImage = null;
    myConfig = null;
  }

}

