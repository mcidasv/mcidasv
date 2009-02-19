package edu.wisc.ssec.mcidasv.supportform;

import java.util.Properties;
import java.util.Map.Entry;

// test/example only! use something else!
public class SimpleStateCollector implements StateCollector {

    public String getBundleAttachmentName() {
        return "empty.mcv";
    }

    public String getExtraAttachmentName() {
        return "system.properties";
    }

    public String getContentsAsString() {
        StringBuffer buf = new StringBuffer();
        Properties props = System.getProperties();
        for (Entry<Object, Object> entry : props.entrySet()) {
            buf.append(entry.getKey()+"="+entry.getValue()+"\n");
        }
        return buf.toString();
    }

    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[SimpleStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    public boolean canBundleState() {
        return false;
    }

    public byte[] getBundledState() {
        return "".getBytes();
    }

    public String getLogPath() {
        return "";
    }
}
