package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVariableInfo {
   public String var_name;
   public int rank;
   public int[] dim_lengths;
   public int data_type;
   public int n_attrs;

   public HDFVariableInfo(String var_name, int rank, int[] dim_lengths, int data_type, int n_attrs) {
     this.var_name = var_name;
     this.rank = rank;
     this.dim_lengths = dim_lengths;
     this.data_type = data_type;
     this.n_attrs = n_attrs;
   }


}
