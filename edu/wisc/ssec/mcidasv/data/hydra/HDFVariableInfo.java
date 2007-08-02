package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVariableInfo {
   String var_name;
   int rank;
   int[] dim_lengths;
   int data_type;
   int n_attrs;

   public HDFVariableInfo(String var_name, int rank, int[] dim_lengths, int data_type, int n_attrs) {
     this.var_name = var_name;
     this.rank = rank;
     this.dim_lengths = dim_lengths;
     this.n_attrs = n_attrs;
   }


}
