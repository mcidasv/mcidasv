package edu.wisc.ssec.mcidasv.supportform;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.VirtualUniverse;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.ResourceCollection;
import ucar.visad.display.DisplayUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;

public class McvStateCollector implements StateCollector {

    /** Reference used to query McIDAS-V's application state. */
    private final McIDASV mcv;

    /** Name of the attachment used for the system state bundle. */
    private static final String BUNDLE = "bundle" + Constants.SUFFIX_MCV;

    /** Name of the attachment used for system properties. */
    private static final String EXTRA = "mcv.properties";

    public McvStateCollector(final McIDASV mcv) {
        if (mcv == null)
            throw new NullPointerException(); // TODO: message
        this.mcv = mcv;
    }

    /**
     * Attempt to invoke {@code OperatingSystemMXBean.methodName} via 
     * reflection.
     * 
     * @param <T> Either {@code Long} or {@code Double}.
     * @param methodName The method to invoke. Must belong to 
     * {@code com.sun.management.OperatingSystemMXBean}.
     * @param defaultValue Default value to return, must be in 
     * {@literal "boxed"} form.
     * 
     * @return Either the result of the {@code methodName} call or 
     * {@code defaultValue}.
     */
    private <T> T hackyMethodCall(final String methodName, final T defaultValue) {
        assert methodName != null : "Cannot invoke a null method name";
        assert methodName.length() > 0: "Cannot invoke an empty method name";
        OperatingSystemMXBean osBean = 
            ManagementFactory.getOperatingSystemMXBean();
        T result = defaultValue;
        try {
            Method m = osBean.getClass().getMethod(methodName);
            m.setAccessible(true);
            // don't suppress warnings because we cannot guarantee that this
            // cast is correct.
            result = (T)m.invoke(osBean);
        } catch (Exception e) {
            // do nothing for right now
        }
        return result;
    }

    private Properties queryOpSysProps() {
        Properties properties = new Properties();
        long committed = hackyMethodCall("getCommittedVirtualMemorySize", Long.MIN_VALUE);
        long freeMemory = hackyMethodCall("getFreePhysicalMemorySize", Long.MIN_VALUE);
        long freeSwap = hackyMethodCall("getFreeSwapSpaceSize", Long.MIN_VALUE);
        long cpuTime = hackyMethodCall("getProcessCpuTime", Long.MIN_VALUE);
        long totalMemory = hackyMethodCall("getTotalPhysicalMemorySize", Long.MIN_VALUE);
        long totalSwap = hackyMethodCall("getTotalSwapSpaceSize", Long.MIN_VALUE);
        double loadAvg = hackyMethodCall("getSystemLoadAverage", Double.NaN);

        properties.setProperty("opsys.cpu.time", Long.toString(cpuTime));
        properties.setProperty("opsys.load", Double.toString(loadAvg));
        properties.setProperty("opsys.memory.virtual.committed", Long.toString(committed));
        properties.setProperty("opsys.memory.physical.free", Long.toString(freeMemory));
        properties.setProperty("opsys.memory.physical.total", Long.toString(totalMemory));
        properties.setProperty("opsys.memory.swap.free", Long.toString(freeSwap));
        properties.setProperty("opsys.memory.swap.total", Long.toString(totalSwap));

        return properties;
    }

    private Properties queryMachine() {
        Properties props = new Properties();

        // cpu count and whatnot
        int processors = Runtime.getRuntime().availableProcessors();
        props.setProperty("opsys.cpu.count", Integer.toString(processors));

        // memory: available, used, etc
        props.putAll(queryOpSysProps());

        // screen: count, resolution(s)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int displayCount = ge.getScreenDevices().length;

        for (int i = 0; i < displayCount; i++) {
            String baseId = "opsys.display."+i+".";
            GraphicsDevice dev = ge.getScreenDevices()[i];
            DisplayMode mode = dev.getDisplayMode();
            props.setProperty(baseId+"name", dev.getIDstring());
            props.setProperty(baseId+"depth", Integer.toString(mode.getBitDepth()));
            props.setProperty(baseId+"width", Integer.toString(mode.getWidth()));
            props.setProperty(baseId+"height", Integer.toString(mode.getHeight()));
        }

        return props;
    }

    private Properties queryJava3d() {
        Properties props = new Properties();
        VirtualUniverse universe = new VirtualUniverse();
        Map<String, Object> universeProps = 
            (Map<String, Object>)universe.getProperties();
        props.putAll(universeProps);

        GraphicsConfiguration config = 
            DisplayUtil.getPreferredConfig(null, true, false);
        Map<String, Object> c3dMap = new Canvas3D(config).queryProperties();
        props.putAll(c3dMap);
        return props;
    }

    private Properties queryMcvState() {
        Properties props = new Properties();

        ArgsManager args = mcv.getArgsManager();
        props.put("mcv.state.islinteractive", args.getIslInteractive());
        props.put("mcv.state.offscreen", args.getIsOffScreen());
        props.put("mcv.state.initcatalogs", args.getInitCatalogs());
        props.put("mcv.state.actions", mcv.getActionHistory());
        props.put("mcv.plugins.installed", args.installPlugins);
        props.put("mcv.state.commandline", mcv.getCommandLineArgs());

        // loop through resources
        List<IdvResource> resources = mcv.getResourceManager().getResources();
        for (IdvResource resource : resources) {
            String id = resource.getId();
            props.put(id+".description", resource.getDescription());
            if (resource.getPattern() == null)
                props.put(id+".pattern", "null");
            else
                props.put(id+".pattern", resource.getPattern());

            ResourceCollection rc = mcv.getResourceManager().getResources(resource);
            List specified = new ArrayList();
            List valid = new ArrayList();
            for (int i = 0; i < rc.size(); i++) {
                specified.add(rc.get(i));
                if (rc.isValid(i))
                    valid.add(rc.get(i));
            }

            props.put(id+".specified", specified);
            props.put(id+".existing", valid);
        }
        return props;
    }

    public String getBundleAttachmentName() {
        return BUNDLE;
    }

    public String getExtraAttachmentName() {
        return EXTRA;
    }

    public String getContentsAsString() {
        Properties props = new Properties();
        // get machine properties
        props.putAll(queryMachine());

        // get java system properties
        props.putAll(System.getProperties());

        // get java3d/jogl properties
        props.putAll(queryJava3d());

        // get idv/mcv properties
        props.putAll(queryMcvState());

        StringBuffer buf = new StringBuffer();
        for (Entry<Object, Object> entry : props.entrySet())
            buf.append(entry.getKey()+"="+entry.getValue()+"\n");

        return buf.toString();
    }

    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[McvStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    public boolean canBundleState() {
        return true;
    }

    public byte[] getBundledState() {
        String data = "";
        try {
            data = mcv.getPersistenceManager().getBundleXml(true);
        } catch (Exception e) {
            LogUtil.logException("Error saving state for support request", e);
        }
        return data.getBytes();
    }
}
