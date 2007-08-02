package edu.wisc.ssec.mcidasv.data.hydra;

import java.util.Iterator;

public class HDFFileInfo {

    public int num_SDdatasets;
    public int num_globalAttrs;

    public HDFFileInfo(int num_SDdatasets, int num_globalAttrs) {
        this.num_SDdatasets  = num_SDdatasets;
        this.num_globalAttrs = num_globalAttrs;
    }
}
