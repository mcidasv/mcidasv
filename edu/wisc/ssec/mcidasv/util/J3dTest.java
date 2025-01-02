/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
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

package edu.wisc.ssec.mcidasv.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;

import com.sun.j3d.utils.geometry.ColorCube;
import com.sun.j3d.utils.universe.SimpleUniverse;

import javax.media.j3d.Alpha;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.RotationInterpolator;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;

/**
 * Display a simple rotating cube.
 * 
 * <p>The intent here is verify that Java3D can put something on the screen,
 * so no McIDAS-V stuff should be happening in here.</p>
 */
public class J3dTest {
    J3dTest() {
        GraphicsConfiguration config =
            SimpleUniverse.getPreferredConfiguration();
        Canvas3D canvas = new Canvas3D(config);
        
        SimpleUniverse univ = new SimpleUniverse(canvas);
        univ.getViewingPlatform().setNominalViewingTransform();
        univ.getViewer().getView().setMinimumFrameCycleTime(5);
        
        BranchGroup objRoot = new BranchGroup();
        TransformGroup transformGroup = new TransformGroup();
        transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRoot.addChild(transformGroup);
        transformGroup.addChild(new ColorCube(0.4));
        RotationInterpolator rotator =
            new RotationInterpolator(
                new Alpha(-1, 4000),
                transformGroup,
                new Transform3D(),
                0.0f,
                (float)Math.PI * 2.0f);
        rotator.setSchedulingBounds(new BoundingSphere(new Point3d(0.0,0.0,0.0), 100.0));
        objRoot.addChild(rotator);
        objRoot.compile();
        
        univ.addBranchGraph(objRoot);
        
        JFrame jFrame = new JFrame();
        jFrame.setLayout(new BorderLayout());
        jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jFrame.setTitle("Java3D Test");
        jFrame.getContentPane().add(canvas, BorderLayout.CENTER);
        jFrame.setPreferredSize(new Dimension(250, 250));
        jFrame.pack();
        jFrame.setVisible(true);
    }
    
    public static void main(String... args) {
        SwingUtilities.invokeLater(J3dTest::new);
    }
}
