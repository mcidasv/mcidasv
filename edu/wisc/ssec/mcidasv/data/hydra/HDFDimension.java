package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFDimension {
   public String name;
   public int size;
   public int type;
   public int n_attrs;

   public HDFDimension(String name, int size, int type, int n_attrs) {
     this.name = name;
     this.size = size;
     this.type = type;
     this.n_attrs = n_attrs;
   }


}
