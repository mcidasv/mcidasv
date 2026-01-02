/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

//
// UniverseBuilderJ3D.java
//

/*
   copied from Sun's Java 3D API Specification. version 1.0
*/

package visad.java3d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import javax.media.j3d.*;
import javax.vecmath.*;

public class UniverseBuilderJ3D extends Object {

    private static final Logger logger = LoggerFactory.getLogger(UniverseBuilderJ3D.class);

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
        universe.removeAllLocales();
      } catch (RuntimeException why) {
        // Apparently these calls throw NPEs sometimes. We're just going to
        // log any exceptions for now.
        logger.error("Exception occurred while destroying j3d comps", why);
      }

      canvas = null;
      universe = null;
      locale = null;
      vpTrans = null;
      view = null;
      vpRoot = null;
      vp = null;
    }
}

