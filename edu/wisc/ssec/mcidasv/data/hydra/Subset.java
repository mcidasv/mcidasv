package edu.wisc.ssec.mcidasv.data.hydra;

public class Subset {

  private int[] start = null;
  private int[] count = null;
  private int[] stride = null;

  public Subset(int rank) {
    start = new int[rank];
    count = new int[rank];
    stride = new int[rank];

    for (int t=0; t<rank; t++) {
      start[t] = -1;
      count[t] = -1;
      stride[t] = -1;
    }
  }

  public int[] getStart() {
    return start;
  }

  public int[] getCount() {
    return count;
  }

  public int[] getStride() {
    return stride;
  }

}
