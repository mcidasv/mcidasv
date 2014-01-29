/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
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
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
// Shawn E. Gano
/**
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 * 
 * This file is part of JSatTrak.
 * 
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
 */

package edu.wisc.ssec.mcidasv.data.adde.sgp4;

/**
 * Various Math functions; many of them for vector and matrix operations for 3 dimensions.
 */
public class MathUtils
{

//      vector 2-norm
    /**
     * vector 2-norm
     *
     * @param a vector of length 3
     * @return norm(a)
     */
        public static double norm(double[] a)
        {
                double c = 0.0;

                for(int i=0;i<a.length;i++)
                {
                        c += a[i]*a[i];
                }

                return Math.sqrt(c);
        }

        // dot product for 3D vectors
    /**
     * dot product for 3D vectors
     *
     * @param a 3x1 vector
     * @param b 3x1 vector
     * @return a dot b
     */
        public static double dot(double[] a, double[] b)
        {
                double c =0;;

                for (int i = 0; i < 3; i++) // row
                {
                        c += a[i]*b[i];
                }

                return c;

        } // mult 3x3 matrices

    /**
     * multiply two matrices 3x3
     *
     * @param a 3x3 matrix
     * @param b 3x3 matrix
     * @return a x b
     */
        public static double[][] mult(double[][] a, double[][] b)
        {
                double[][] c = new double[3][3];

                for (int i = 0; i < 3; i++) // row
                {
                        for (int j = 0; j < 3; j++) // col
                        {
                                c[i][j] = 0.0;
                                for (int k = 0; k < 3; k++)
                                {
                                        c[i][j] += a[i][k] * b[k][j];
                                }
                        }
                }

                return c;

        } // mult 3x3 matrices

    /**
     * vector subtraction
     *
     * @param a vector of length 3
     * @param b vector of length 3
     * @return a-b
     */
        public static double[] sub(double[] a, double[] b)
        {
                double[] c = new double[3];
                for(int i=0;i<3;i++)
                {
                        c[i] = a[i] - b[i];
                }

                return c;
        }
}
