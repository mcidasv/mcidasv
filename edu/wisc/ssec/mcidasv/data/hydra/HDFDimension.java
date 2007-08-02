package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFDimension {
   String name;
   int size;
   int type;
   int n_attrs;

   public HDFDimension(String name, int size, int type, int n_attrs) {
     this.name = name;
     this.size = size;
     this.type = type;
     this.n_attrs = n_attrs;
   }


}
