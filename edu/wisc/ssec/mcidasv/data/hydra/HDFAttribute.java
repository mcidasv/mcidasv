/*
 * $Id$
 *
 * Copyright 2007-2008
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison,
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 *
 * http://www.ssec.wisc.edu/mcidas
 *
 * This file is part of McIDAS-V.
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
 * along with this program.  If not, see http://www.gnu.org/licenses
 */

package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Iterator;

public class HDFAttribute {
  final Class type;
  Iterator attrIter;


  public HDFAttribute(String str) {
    this(HDFArray.make(new String[] {str}));
  }

  public HDFAttribute(String[] str_s) {
    this(HDFArray.make(str_s));
  }

  public HDFAttribute(final HDFArray array) {
    type = array.getType();

    attrIter = new Iterator() {
      int index = 0;
      int length = array.length;

      public boolean hasNext() {
        if (index < length) {
          return true;
        }
        else {
          return false;
        }
      }

      public Object next() {
        if (type == Short.class) {
           return new Short(((short[])array.getArray())[index++]);
        }
        else if (type == Integer.class) {
           return new Integer(((int[])array.getArray())[index++]);
        } 
        else if (type == Float.class) {
           return new Float(((float[])array.getArray())[index++]);
        }
        else if (type == Double.class) {
           return new Double(((double[])array.getArray())[index++]);
        }
        else if (type == String.class) {
           return ((String[])array.getArray())[index++];
        }

        return null;
      }

      public void remove() {
      }
    };

  }

  public Iterator getAttribute() {
    return attrIter;
  }

}
