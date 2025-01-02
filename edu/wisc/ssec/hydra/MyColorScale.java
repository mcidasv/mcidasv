/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

package edu.wisc.ssec.hydra;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import ucar.unidata.util.Misc;

import static ucar.visad.display.ColorScale.DEFAULT_LABEL_COLOR;
import static ucar.visad.display.ColorScale.HORIZONTAL_ORIENT;
import static ucar.visad.display.ColorScale.PRIMARY;

import ucar.visad.display.DisplayMaster;
import visad.DisplayImpl;
import visad.LocalDisplay;
import visad.PlotText;
import visad.ShadowType;
import visad.TextControl;
import visad.Unit;
import visad.VisADException;
import visad.VisADGeometryArray;
import visad.VisADLineArray;
import visad.VisADTriangleArray;
import visad.util.HersheyFont;

public class MyColorScale implements PropertyChangeListener {

    boolean isVisible = true;
    MyShapeDisplayable clrBar;
    MyShapeDisplayable lbls;

    float start_x = -1f;
    float start_y = 1f;
    float width = 2f;
    float height = 0.05f;

    float[][] clrTable;
    DisplayMaster dspMaster;


    public MyColorScale(DisplayMaster dspMaster, float[][] clrTable,
                        float lowRange, float highRange, float incr) throws VisADException, RemoteException {
        this.dspMaster = dspMaster;

        LocalDisplay dspImpl = dspMaster.getDisplay();
        java.awt.Rectangle r = dspMaster.getScreenBounds();

        double[] xyz = ucar.visad.Util.getVWorldCoords((DisplayImpl) dspImpl, r.x, r.y, null);

        clrBar = new MyShapeDisplayable("clrBar",
                createColorBar(-1f, 1f, 2f, 0.05f, clrTable));
        dspMaster.addDisplayable(clrBar);

        if (!Double.isNaN(lowRange)) {
            lbls = new MyShapeDisplayable("lbls",
                    (createColorBarLabels(-1f, 1f, 2f, 0.05f, new double[]{lowRange, highRange}, new float[]{1f, 1f, 1f}))[0]);
            dspMaster.addDisplayable(lbls);
        }
    }

    public void setVisible(boolean isVisible) throws VisADException, RemoteException {
        this.isVisible = isVisible;
        clrBar.setVisible(isVisible);
        lbls.setVisible(isVisible);
    }

    public void update(float[][] clrTable) {
        try {
            VisADTriangleArray ta = createColorBar(start_x, start_y, width, height, clrTable);
            clrBar.setMarker(ta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(double[] range) throws Exception {
        if (lbls == null) {
            lbls = new MyShapeDisplayable("lbls",
                    (createColorBarLabels(-1f, 1f, 2f, 0.05f, range, new float[]{1f, 1f, 1f}))[0]);
            dspMaster.addDisplayable(lbls);
        } else {
            VisADGeometryArray ta = createColorBarLabels(start_x, start_y, width, height, range, new float[]{1f, 1f, 1f})[0];
            lbls.setMarker(ta);
        }
    }

    public void remove() {
        try {
            dspMaster.removeDisplayable(clrBar);
            dspMaster.removeDisplayable(lbls);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static VisADTriangleArray createColorBar(float start_x, float start_y, float width, float height, float[][] colorPalette) throws VisADException {
        return createColorBar(start_x, start_y, width, height, start_x, start_x + width, colorPalette);
    }

    public static VisADTriangleArray createColorBar(float start_x, float start_y, float width, float height,
                                                    float fillFrom, float fillTo, float[][] colorPalette)
            throws VisADException {
        int HORIZONTAL_ORIENT = 1;
        int VERTICAL_ORIENT = 0;
        int orient = HORIZONTAL_ORIENT;
        boolean useAlpha = false;

        // Get the color table used by the signalStrength scalar map.
        int numColours = colorPalette[0].length;
        boolean hasAlpha = (colorPalette.length == 4) && useAlpha;

        float delta = ((orient == HORIZONTAL_ORIENT)
                ? width
                : height) / (float) numColours;
        int numPointsPerTriangle = 3;
        int numValuesPerPoint = 3;
        int numColoursPerPoint = hasAlpha
                ? 4
                : 3;
        int numTriangles = numColours * 2;
        numTriangles += 4;
        float[] triangles =
                new float[numTriangles * numPointsPerTriangle * numValuesPerPoint];
        byte[] colors =
                new byte[numTriangles * numPointsPerTriangle * numColoursPerPoint];

        int index = 0;
        int colorIndex = 0;

        float delta_a = start_x - fillFrom;

        // Left fill triangles
        if (delta_a > 0) {
            float start_a = fillFrom;
            int ii = 0;

            // First point in triangle
            triangles[index++] = delta_a * ii + start_a;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Second point in triangle
            triangles[index++] = delta_a * (ii + 1) + start_a;  // delta*i + delta;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Third point in triangle
            triangles[index++] = delta_a * (ii + 1) + start_a;  // delta*i + delta;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;

            // First point in triangle
            triangles[index++] = delta_a * ii + start_a;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Second point in triangle
            triangles[index++] = delta_a * (ii + 1) + start_a;  // delta*i + delta;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;

            // Third point in triangle
            triangles[index++] = delta_a * ii + start_a;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;

            byte rr = (byte) (colorPalette[0][0] * 255);
            byte gg = (byte) (colorPalette[1][0] * 255);
            byte bb = (byte) (colorPalette[2][0] * 255);
            byte aa = (byte) 0;
            if (hasAlpha) {
                aa = (byte) (colorPalette[3][0] * 255);
            }
            for (int n = 0; n < 6; n++) {  // set colors for the 6 points
                colors[colorIndex++] = rr;
                colors[colorIndex++] = gg;
                colors[colorIndex++] = bb;
                if (hasAlpha) {
                    colors[colorIndex++] = aa;
                }
            }
        }

        for (int i = 0; i < numColours; ++i) {

            byte red = (byte) (colorPalette[0][i] * 255);
            byte green = (byte) (colorPalette[1][i] * 255);
            byte blue = (byte) (colorPalette[2][i] * 255);
            byte alpha = (byte) 0;
            if (hasAlpha) {
                alpha = (byte) (colorPalette[3][i] * 255);
            }


            // The right-half triangle
            // 1     2
            // .......
            //  .    .
            //   .   .
            //    .  .
            //     . .
            //      ..
            //       .
            //       3

            // HORIZONTAL goes left->right, VERTICAL goes bottom->top
            if (orient == HORIZONTAL_ORIENT) {

                // First point in triangle
                triangles[index++] = delta * i + start_x;
                triangles[index++] = 0.0f + start_y;
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = delta * (i + 1) + start_x;  // delta*i + delta;
                triangles[index++] = 0.0f + start_y;
                triangles[index++] = 0.0f;

                // Third point in triangle
                triangles[index++] = delta * (i + 1) + start_x;  // delta*i + delta;
                triangles[index++] = height + start_y;
                triangles[index++] = 0.0f;

            } else {                                   // VERTICAL_ORIENT

                // First point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Third point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;
            }

            // The left-half triangle
            // 1
            // .
            // ..
            // . .
            // .  .
            // .   .
            // .    .
            // .......
            // 3     2

            if (orient == HORIZONTAL_ORIENT) {

                // First point in triangle
                triangles[index++] = delta * i + start_x;
                triangles[index++] = 0.0f + start_y;
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = delta * (i + 1) + start_x;  // delta*i + delta;
                triangles[index++] = height + start_y;
                triangles[index++] = 0.0f;


                // Third point in triangle
                triangles[index++] = delta * i + start_x;
                triangles[index++] = height + start_y;
                triangles[index++] = 0.0f;

            } else {  // VERTICAL_ORIENT

                // First point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * (i + 1);
                triangles[index++] = 0.0f;

                // Second point in triangle
                triangles[index++] = width;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;


                // Third point in triangle
                triangles[index++] = 0.0f;
                triangles[index++] = -height + delta * i;
                triangles[index++] = 0.0f;
            }

            for (int n = 0; n < 6; n++) {  // set colors for the 6 points
                colors[colorIndex++] = red;
                colors[colorIndex++] = green;
                colors[colorIndex++] = blue;
                if (hasAlpha) {
                    colors[colorIndex++] = alpha;
                }
            }
        } // for (i<numColours)

        // Right fill triangles
        float delta_b = fillTo - (start_x + width);
        if (delta_b > 0) {
            float start_b = start_x + width;
            int ii = 0;
            // First point in triangle
            triangles[index++] = delta_b * ii + start_b;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Second point in triangle
            triangles[index++] = delta_b * (ii + 1) + start_b;  // delta*i + delta;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Third point in triangle
            triangles[index++] = delta_b * (ii + 1) + start_b;  // delta*i + delta;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;

            // First point in triangle
            triangles[index++] = delta_b * ii + start_b;
            triangles[index++] = 0.0f + start_y;
            triangles[index++] = 0.0f;

            // Second point in triangle
            triangles[index++] = delta_b * (ii + 1) + start_b;  // delta*i + delta;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;


            // Third point in triangle
            triangles[index++] = delta_b * ii + start_b;
            triangles[index++] = height + start_y;
            triangles[index++] = 0.0f;

            byte rr = (byte) (colorPalette[0][255] * 255);
            byte gg = (byte) (colorPalette[1][255] * 255);
            byte bb = (byte) (colorPalette[2][255] * 255);
            byte aa = (byte) 0;
            if (hasAlpha) {
                aa = (byte) (colorPalette[3][255] * 255);
            }
            for (int n = 0; n < 6; n++) {  // set colors for the 6 points
                colors[colorIndex++] = rr;
                colors[colorIndex++] = gg;
                colors[colorIndex++] = bb;
                if (hasAlpha) {
                    colors[colorIndex++] = aa;
                }
            }
        }


        // Set all the normal vectors to (0,0,1) for each
        // vertex of each triangle.
        float[] normals =
                new float[numTriangles * numPointsPerTriangle * numValuesPerPoint];

        index = 0;
        for (int i = 0; i < numTriangles; ++i) {

            // First point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

            // Second point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

            // Third point in triangle.
            normals[index++] = 0.0f;
            normals[index++] = 0.0f;
            normals[index++] = 1.0f;

        }  // for (i<numTriangles)

        VisADTriangleArray triangleArray = new VisADTriangleArray();
        triangleArray.coordinates = triangles;
        triangleArray.normals = normals;
        triangleArray.colors = colors;
        triangleArray.vertexCount = numTriangles * numPointsPerTriangle;

        return triangleArray;

    }  // end createTriangles()

    public static VisADGeometryArray[] createColorBarLabels(float startx, float starty, float width, float height, double[] clrRange, float[] foregrndClr) throws VisADException, RemoteException {
        int orient = 1; // Horizontal
        double lowRange = clrRange[0];
        double highRange = clrRange[1];
        Object labelFont = null;
        int labelSize = 10;
        Hashtable labelTable = new Hashtable();
        double increment = (highRange - lowRange) / 6;
        Unit unit = null;
        boolean unitVisible = false;
        Color labelColor = Color.WHITE;
        int labelSide = 0; // primary

        // compute graphics positions
        // these are {x, y, z} vectors
        double[] base = null;  // vector from one character to another
        double[] up = null;  // vector from bottom of character to top
        double[] startn = null;  // -1.0 position along axis
        double[] startp = null;  // +1.0 position along axis
        // by default, all labels rendered centered
        TextControl.Justification justification = (orient
                == HORIZONTAL_ORIENT)
                ? TextControl.Justification.CENTER
                : TextControl.Justification.LEFT;

        Vector lineArrayVector = new Vector();  // vector for line drawings
        Vector labelArrayVector = new Vector();
        double ONE = (lowRange > highRange)
                ? -1.0
                : 1.0;
        double min = Math.min(lowRange, highRange);
        double max = Math.max(lowRange, highRange);
        double range = Math.abs(max - min);
        double majorTickSpacing = Misc.computeTickSpacing(min, max);
        double firstValue = lowRange;
        double SCALE;
        double fontScale = 1.0;
        if ((labelFont != null) && (labelFont instanceof Font)) {
            fontScale = ((Font) labelFont).getSize() / 12.;
        }

        if (orient == HORIZONTAL_ORIENT) {
            SCALE = height * .8 * fontScale;
            if (labelSide == PRIMARY) {
                base = new double[]{SCALE, 0.0, 0.0};
                up = new double[]{0.0, SCALE, 0.0};
                startn = new double[]{0, -height - SCALE * .5, 0};
                startp = new double[]{width, -height - SCALE * .5, 0};
            } else {
                base = new double[]{SCALE, 0.0, 0.0};
                up = new double[]{0.0, SCALE, 0.0};
                startn = new double[]{0, height + SCALE * .5, 0};
                startp = new double[]{width, height + SCALE * .5, 0};
            }
        } else {  // VERTICAL_ORIENT
            SCALE = width * .8 * fontScale;
            if (labelSide == PRIMARY) {
                base = new double[]{SCALE, 0.0, 0.0};
                up = new double[]{0.0, SCALE, 0.0};
                startn = new double[]{width + SCALE * .5, -height, 0};
                startp = new double[]{width + SCALE * .5, 0, 0};
            } else {
                base = new double[]{SCALE, 0.0, 0.0};
                up = new double[]{0.0, SCALE, 0.0};
                startn = new double[]{-SCALE * .5, -height, 0};
                startp = new double[]{-SCALE * .5, 0, 0};
                justification = TextControl.Justification.RIGHT;
            }
        }

        double dist =  // dist from the color bar in up direction
                (orient == HORIZONTAL_ORIENT)
                        ? SCALE + SCALE / 10.
                        : width + SCALE / 10.;
        double[] updir = up;

        // Draw the labels.  If user hasn't defined their own, make defaults.
        /*
        if ( !userLabels) {
            createStandardLabels(max, min, min, (labelAllTicks == false)
                    ? (range)
                    : majorTickSpacing, false);
        }
        */
        labelTable = Misc.createLabelTable(max, min, min, increment);
        if (labelTable.isEmpty()) {
            return null;
        }

        double val_unit = Math.abs(highRange - lowRange) / (range);
        //Fudge factor to move unit slightly beyond color bar. 
        val_unit = val_unit + (val_unit * 5 / 100);
        double[] point_unit = new double[3];
        for (int j = 0; j < 3; j++) {
            point_unit[j] = (1.0 - val_unit) * startn[j]
                    + val_unit * startp[j] - dist * up[j];
        }
        if ((unit != null) && unitVisible) {
            if (labelFont == null) {
                VisADLineArray label = PlotText.render_label(unit + "",
                        point_unit, base, updir,
                        justification);
                lineArrayVector.add(label);
            } else if (labelFont instanceof Font) {
                VisADTriangleArray label = PlotText.render_font(unit + "",
                        (Font) labelFont, point_unit,
                        base, updir, justification);
                labelArrayVector.add(label);
            } else if (labelFont instanceof HersheyFont) {
                VisADLineArray label = PlotText.render_font(unit + "",
                        (HersheyFont) labelFont,
                        point_unit, base, updir,
                        justification);
                lineArrayVector.add(label);
            }
        }

        for (Enumeration e = labelTable.keys(); e.hasMoreElements(); ) {
            Double Value;
            try {
                Value = (Double) e.nextElement();
            } catch (ClassCastException cce) {
                throw new VisADException("Invalid keys in label hashtable");
            }
            double test = Value.doubleValue();
            if ((test > max) || (test < min)) {
                continue;  // don't draw labels beyond range
            }

            double val = Math.abs(test - lowRange) / (range);  // pos along the scale

            double[] point = new double[3];
            for (int j = 0; j < 3; j++) {
                point[j] = (1.0 - val) * startn[j] + val * startp[j]
                        - dist * up[j];
            }

            point[0] += startx;
            point[1] += starty;


            //System.out.println("For label = " + Value.doubleValue() + "(" + val + "), point is (" + point[0] + "," + point[1] + "," + point[2] + ")");


            if (labelFont == null) {
                VisADLineArray label =
                        PlotText.render_label((String) labelTable.get(Value),
                                point, base, updir, justification);
                lineArrayVector.add(label);
            } else if (labelFont instanceof Font) {
                VisADTriangleArray label =
                        PlotText.render_font((String) labelTable.get(Value),
                                (Font) labelFont, point, base,
                                updir, justification);
                labelArrayVector.add(label);

            } else if (labelFont instanceof HersheyFont) {
                VisADLineArray label =
                        PlotText.render_font((String) labelTable.get(Value),
                                (HersheyFont) labelFont, point,
                                base, updir, justification);
                lineArrayVector.add(label);
            }
        }

        // merge the line arrays
        VisADLineArray lineLabels = null;
        VisADTriangleArray triLabels = null;
        if (!lineArrayVector.isEmpty()) {
            VisADLineArray[] arrays =
                    (VisADLineArray[]) lineArrayVector.toArray(
                            new VisADLineArray[lineArrayVector.size()]);
            lineLabels = VisADLineArray.merge(arrays);
            // set the color for the label arrays
            float[] rgb;
            if (labelColor == null) {
                if (foregrndClr != null) {
                    rgb = foregrndClr;
                } else {
                    rgb = DEFAULT_LABEL_COLOR.getColorComponents(null);
                }
            } else {
                rgb = labelColor.getColorComponents(null);
            }
            byte red = ShadowType.floatToByte(rgb[0]);
            byte green = ShadowType.floatToByte(rgb[1]);
            byte blue = ShadowType.floatToByte(rgb[2]);
            int n = 3 * lineLabels.vertexCount;
            byte[] colors = new byte[n];
            for (int i = 0; i < n; i += 3) {
                colors[i] = red;
                colors[i + 1] = green;
                colors[i + 2] = blue;
            }
            lineLabels.colors = colors;
        }

        // merge the label arrays
        if (!(labelArrayVector.isEmpty())) {
            VisADTriangleArray[] labelArrays =
                    (VisADTriangleArray[]) labelArrayVector.toArray(
                            new VisADTriangleArray[labelArrayVector.size()]);
            triLabels = VisADTriangleArray.merge(labelArrays);
            // set the color for the label arrays
            float[] rgb = labelColor.getColorComponents(null);
            byte red = ShadowType.floatToByte(rgb[0]);
            byte green = ShadowType.floatToByte(rgb[1]);
            byte blue = ShadowType.floatToByte(rgb[2]);
            int n = 3 * triLabels.vertexCount;
            byte[] colors = new byte[n];
            for (int i = 0; i < n; i += 3) {
                colors[i] = red;
                colors[i + 1] = green;
                colors[i + 2] = blue;
            }
            triLabels.colors = colors;
        }
        return new VisADGeometryArray[]{lineLabels, triLabels};
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Depiction depict = ((DepictionControl) evt.getSource()).getDepiction();
        if (!depict.getVisible()) { // Don't update the single ColorScale unless source depiction is visible
            return;
        }
        try {
            String evtName = evt.getPropertyName();
            switch (evtName) {
                case "ColorScale":
                    float[][] clrTbl = (float[][]) evt.getNewValue();
                    update(clrTbl);
                    break;

                case "range":
                    double[] rng = (double[]) evt.getNewValue();
                    update(rng);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
