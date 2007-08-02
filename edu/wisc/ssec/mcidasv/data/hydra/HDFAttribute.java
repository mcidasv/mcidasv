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
