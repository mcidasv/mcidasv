package edu.wisc.ssec.mcidasv.supportform;

public interface StateCollector {
    public String getBundleAttachmentName();
    public String getExtraAttachmentName();
    public String getContentsAsString();
    public byte[] getContents();
    public String toString();
    public boolean canBundleState();
    public byte[] getBundledState();
    public String getLogPath();
}
