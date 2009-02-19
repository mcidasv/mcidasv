package edu.wisc.ssec.mcidasv.supportform;

import edu.wisc.ssec.mcidasv.Constants;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.LogUtil;

public class IdvStateCollector implements StateCollector {

    private IntegratedDataViewer idv;

    private static final String BUNDLE_FILENAME = "bundle" + Constants.SUFFIX_MCV;
    private static final String EXTRA_FILENAME = "extra.html";

    public IdvStateCollector(IntegratedDataViewer idv) {
        this.idv = idv;
    }

    public String getBundleAttachmentName() {
        return BUNDLE_FILENAME;
    }

    public String getExtraAttachmentName() {
        return EXTRA_FILENAME;
    }

    public String getContentsAsString() {
        return "";
    }

    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[IdvStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    public boolean canBundleState() {
        return true;
    }

    public byte[] getBundledState() {
        String data = "";
        try {
            data = idv.getPersistenceManager().getBundleXml(true);
        } catch (Exception e) {
            LogUtil.logException("Error saving state for support request", e);
        }
        return data.getBytes();
    }

    public String getLogPath() {
        return "";
    }
}
