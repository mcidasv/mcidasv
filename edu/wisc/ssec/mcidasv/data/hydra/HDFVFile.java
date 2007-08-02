package edu.wisc.ssec.mcidasv.data.hydra;

public class HDFVFile {
  int fId;
  HDF hdf;

  HDFVFile(HDF hdf, String filename) throws Exception {
    this.hdf = hdf;
    fId = hdf.hOpen(filename);
    int rc = hdf.vStart(fId);
  }

  public int getId() {
    return fId;
  }

  public int find(String data_name) throws Exception {
    return hdf.vsFind(fId, data_name);
  }

  public HDFVData select(String name) throws Exception {
    int v_ref = find(name);
    return new HDFVData(name, fId, v_ref, hdf);
  }
}
