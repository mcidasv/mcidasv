
//
// UniverseBuilderJ3D.java
//

/*
   copied from Sun's Java 3D API Specification. version 1.0
*/

package visad.java3d;

import java.lang.reflect.Method;

import javax.media.j3d.*;
import javax.vecmath.*;

public class UniverseBuilderJ3D extends Object {

    // User-specified canvas
    private Canvas3D canvas;

    // Scene graph elements that the user may want access to
    private VirtualUniverse universe;
    private Locale locale;
    TransformGroup vpTrans;
    View view;
    private BranchGroup vpRoot;
    private ViewPlatform vp;

    public UniverseBuilderJ3D(Canvas3D c) {
      canvas = c;

      // Establish a virtual universe, with a single hi-res Locale
      universe = new VirtualUniverse();
      locale = new Locale(universe);

      // Create a PhysicalBody and Physical Environment object
      PhysicalBody body = new PhysicalBody();
      PhysicalEnvironment environment = new PhysicalEnvironment();

      // Create a View and attach the Canvas3D and the physical
      // body and environment to the view.
      view = new View();
      view.addCanvas3D(c);
      view.setPhysicalBody(body);
      view.setPhysicalEnvironment(environment);
      if(minimumFrameCycleTime!=0) {
          view.setMinimumFrameCycleTime(minimumFrameCycleTime);
      }

      // Create a branch group node for the view platform
      vpRoot = new BranchGroup();
      vpRoot.setCapability(BranchGroup.ALLOW_DETACH);
      vpRoot.setCapability(Group.ALLOW_CHILDREN_READ);

      // Create a ViewPlatform object, and its associated
      // TransformGroup object, and attach it to the root of the
      // subgraph.  Attach the view to the view platform.
      Transform3D t = new Transform3D();
      t.set(new Vector3f(0.0f, 0.0f, 2.0f));
      vp = new ViewPlatform();
      vpTrans = new TransformGroup(t);
      vpTrans.setCapability(Group.ALLOW_CHILDREN_READ);
      vpTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
      vpTrans.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

      vpTrans.addChild(vp);
      vpRoot.addChild(vpTrans);

      view.attachViewPlatform(vp);

      // Attach the branch graph to the universe, via the Locale.
      // The scene graph is now live!
      locale.addBranchGraph(vpRoot);
    }

    private static long minimumFrameCycleTime=0;

    /**
     * This lets client code set the framecycletime on the View
     *
     * @param ms The frame cycle time in milliseconds
    */
    public static void setMinimumFrameCycleTime(long ms) {
        minimumFrameCycleTime = ms;        
    }



    public void addBranchGraph(BranchGroup bg) {
      if (locale != null) locale.addBranchGraph(bg);
    }

    /**
     * Clean up resources according to 
     * http://wiki.java.net/bin/view/Javadesktop/Java3DApplicationDevelopment#Releasing_Canvas3D_View_and_Virt
     */
    public void destroy() {
    	
      // clean up resources in a way compatible back to Java3D 1.2
      for (int idx = 0; idx < view.numCanvas3Ds(); idx++) {
    	Canvas3D cvs = view.getCanvas3D(idx);
    	if (cvs.isOffScreen()) {
    		cvs.setOffScreenBuffer(null);
    	}
        view.removeCanvas3D(cvs);
      }
      try {
        view.attachViewPlatform(null);
      } catch (RuntimeException why) {
    	// Apparently this might throw a NPE.
    	// Ignore because we're just trying to conform to best practice.  
      }
      universe.removeAllLocales();

      canvas = null;
      universe = null;
      locale = null;
      vpTrans = null;
      view = null;
      vpRoot = null;
      vp = null;
    }
}

