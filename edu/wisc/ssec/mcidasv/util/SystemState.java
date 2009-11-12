package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;
import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.arrList;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.VirtualUniverse;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.util.ResourceCollection;
import ucar.visad.display.DisplayUtil;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;

public class SystemState {

//    private static List<Group> ordering = list(Group.VERSIONS, Group.MCVPROPS, Group.IDVPROPS, Group.SYSTEM, Group.MISC);
//
//    public enum Group {
//        IDVPROPS("IDV Properties"),
//        MCVPROPS("McIDAS-V Properties"),
//        VERSIONS("Version Information"),
//        MISC("Misc Properties"),
//        SYSTEM("Computer Properties");
//
//        private final String name;
//
//        private Group(final String name) {
//            this.name = name;
//        }
//
//        public String getName() {
//            return name;
//        }
//    };

    /** Reference to the McIDAS-V {@literal "god object."} */
    private final McIDASV mcv;

    /**
     * Creates a new {@code SystemState} that can be used to gather information
     * about the state of {@literal "this"} computer.
     * 
     * @param mcv Used to query McIDAS-V about its state. Should not be {@code null}.
     */
    public SystemState(final McIDASV mcv) {
        this.mcv = mcv;
    }

//    public static List<Group> getOrdering() {
//        return ordering;
//    }

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

    /**
     * Attempts to call methods belonging to 
     * {@code com.sun.management.OperatingSystemMXBean}. If successful, we'll
     * have the following information:
     * <ul>
     *   <li>opsys.memory.virtual.committed: virtual memory that is guaranteed to be available</li>
     *   <li>opsys.memory.swap.total: total amount of swap space in bytes</li>
     *   <li>opsys.memory.swap.free: free swap space in bytes</li>
     *   <li>opsys.cpu.time: CPU time used by the process (nanoseconds)</li>
     *   <li>opsys.memory.physical.free: free physical memory in bytes</li>
     *   <li>opsys.memory.physical.total: physical memory in bytes</li>
     *   <li>opsys.load: system load average for the last minute</li>
     * </ul>
     * 
     * @return Map of properties that contains interesting information about
     * the hardware McIDAS-V is using.
     */
    public Map<String, String> queryOpSysProps() {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        long committed = hackyMethodCall("getCommittedVirtualMemorySize", Long.MIN_VALUE);
        long freeMemory = hackyMethodCall("getFreePhysicalMemorySize", Long.MIN_VALUE);
        long freeSwap = hackyMethodCall("getFreeSwapSpaceSize", Long.MIN_VALUE);
        long cpuTime = hackyMethodCall("getProcessCpuTime", Long.MIN_VALUE);
        long totalMemory = hackyMethodCall("getTotalPhysicalMemorySize", Long.MIN_VALUE);
        long totalSwap = hackyMethodCall("getTotalSwapSpaceSize", Long.MIN_VALUE);
        double loadAvg = hackyMethodCall("getSystemLoadAverage", Double.NaN);

        properties.put("opsys.cpu.time", Long.toString(cpuTime));
        properties.put("opsys.load", Double.toString(loadAvg));
        properties.put("opsys.memory.virtual.committed", Long.toString(committed));
        properties.put("opsys.memory.physical.free", Long.toString(freeMemory));
        properties.put("opsys.memory.physical.total", Long.toString(totalMemory));
        properties.put("opsys.memory.swap.free", Long.toString(freeSwap));
        properties.put("opsys.memory.swap.total", Long.toString(totalSwap));

        return properties;
    }

    /**
     * Polls Java for information about the user's machine. We're specifically
     * after memory statistics, number of processors, and display information.
     * 
     * @return Map of properties that describes the user's machine.
     */
    public Map<String, String> queryMachine() {
        Map<String, String> props = new LinkedHashMap<String, String>();

        // cpu count and whatnot
        int processors = Runtime.getRuntime().availableProcessors();
        props.put("opsys.cpu.count", Integer.toString(processors));

        // memory: available, used, etc
        props.putAll(queryOpSysProps());

        // screen: count, resolution(s)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int displayCount = ge.getScreenDevices().length;

        for (int i = 0; i < displayCount; i++) {
            String baseId = "opsys.display."+i+".";
            GraphicsDevice dev = ge.getScreenDevices()[i];
            DisplayMode mode = dev.getDisplayMode();
            props.put(baseId+"name", dev.getIDstring());
            props.put(baseId+"depth", Integer.toString(mode.getBitDepth()));
            props.put(baseId+"width", Integer.toString(mode.getWidth()));
            props.put(baseId+"height", Integer.toString(mode.getHeight()));
        }

        return props;
    }

    /**
     * Polls Java 3D for information about its environment. Specifically, we 
     * call {@link VirtualUniverse#getProperties()} and 
     * {@link Canvas3D#queryProperties()}.
     * 
     * @return As much information as Java 3D can provide.
     */
    @SuppressWarnings("unchecked") // casting to Object, so this should be fine.
    public Map<String, Object> queryJava3d() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();
        Map<String, Object> universeProps = 
            (Map<String, Object>)VirtualUniverse.getProperties();
        props.putAll(universeProps);

        GraphicsConfiguration config = 
            DisplayUtil.getPreferredConfig(null, true, false);
        Map<String, Object> c3dMap = new Canvas3D(config).queryProperties();
        props.putAll(c3dMap);
        return props;
    }

    /**
     * Queries McIDAS-V for information about its state. There's not a good way
     * to characterize what we're interested in, so let's leave it at 
     * {@literal "whatever seems useful"}.
     * 
     * @return Information about the state of McIDAS-V.
     */
    public Map<String, Object> queryMcvState() {
        Map<String, Object> props = new LinkedHashMap<String, Object>();

        ArgsManager args = mcv.getArgsManager();
        props.put("mcv.state.islinteractive", args.getIslInteractive());
        props.put("mcv.state.offscreen", args.getIsOffScreen());
        props.put("mcv.state.initcatalogs", args.getInitCatalogs());
        props.put("mcv.state.actions", mcv.getActionHistory());
        props.put("mcv.plugins.installed", args.installPlugins);
        props.put("mcv.state.commandline", mcv.getCommandLineArgs());

        // loop through resources
        @SuppressWarnings("unchecked") // older-style Unidata code; only ever uses IdvResources.
        List<IdvResource> resources = mcv.getResourceManager().getResources();
        for (IdvResource resource : resources) {
            String id = resource.getId();
            props.put(id+".description", resource.getDescription());
            if (resource.getPattern() == null)
                props.put(id+".pattern", "null");
            else
                props.put(id+".pattern", resource.getPattern());

            ResourceCollection rc = mcv.getResourceManager().getResources(resource);
            List<String> specified = new ArrayList<String>();
            List<String> valid = new ArrayList<String>();
            for (int i = 0; i < rc.size(); i++) {
                specified.add((String)rc.get(i));
                if (rc.isValid(i))
                    valid.add((String)rc.get(i));
            }

            props.put(id+".specified", specified);
            props.put(id+".existing", valid);
        }
        return props;
    }

    /**
     * Builds a (filtered) subset of the McIDAS-V system properties and returns
     * the results as a {@code String}.
     * 
     * @return The McIDAS-V system properties in the following format: 
     * {@code KEY=VALUE\n}. This is so we kinda-sorta conform to the standard
     * {@link Properties} file format.
     * 
     * @see #getStateAsString(boolean)
     */
    public String getStateAsString() {
        return getStateAsString(false);
    }

    /**
     * Builds the McIDAS-V system properties and returns the results as a 
     * {@code String}.
     * 
     * @param firehose If {@code true}, enables {@literal "unfiltered"} output.
     * 
     * @return The McIDAS-V system properties in the following format: 
     * {@code KEY=VALUE\n}. This is so we kinda-sorta conform to the standard
     * {@link Properties} file format.
     */
    public String getStateAsString(final boolean firehose) {
        StringBuffer buf = new StringBuffer();

        Map<String, String> versions = ((StateManager)mcv.getStateManager()).getVersionInfo();
        Properties sysProps = System.getProperties();
        Map<String, Object> j3dProps = queryJava3d();
        Map<String, String> machineProps = queryMachine();
        Map<String, Object> mcvProps = queryMcvState();

        buf.append("Software Versions:\n");
        buf.append("McIDAS-V: "+versions.get("mcv.version.general")+" ("+versions.get("mcv.version.build")+")\n");
        buf.append("IDV:      "+versions.get("idv.version.general")+" ("+versions.get("idv.version.build")+")\n\n");

        buf.append("Operating System:\n");
        buf.append("Name:         "+sysProps.getProperty("os.name")+"\n");
        buf.append("Version:      "+sysProps.getProperty("os.version")+"\n");
        buf.append("Architecture: "+sysProps.getProperty("os.arch")+"\n\n");

        buf.append("Java:\n");
        buf.append("Version: "+sysProps.getProperty("java.version")+"\n");
        buf.append("Vendor:  "+sysProps.getProperty("java.vendor")+"\n");
        buf.append("Home:    "+sysProps.getProperty("java.home")+"\n\n");

        buf.append("Java 3D:\n");
        buf.append("Version:  "+j3dProps.get("j3d.version")+"\n");
        buf.append("Vendor:   "+j3dProps.get("j3d.vendor")+"\n");
        buf.append("Renderer: "+j3dProps.get("renderer")+"\n");

        if (firehose) {
            buf.append("\n\nFirehose:\n");
            // get software versions
            for (Entry<String, String> entry : versions.entrySet())
                buf.append(entry.getKey()+"="+entry.getValue()+"\n");

            // get machine properties
            for (Entry<String, String> entry : queryMachine().entrySet())
                buf.append(entry.getKey()+"="+entry.getValue()+"\n");

            // get java system properties
            for (Entry<Object, Object> entry : System.getProperties().entrySet())
                buf.append(entry.getKey()+"="+entry.getValue()+"\n");

            // get java3d/jogl properties
            for (Entry<String, Object> entry : queryJava3d().entrySet())
                buf.append(entry.getKey()+"="+entry.getValue()+"\n");

            // get idv/mcv properties
            for (Entry<String, Object> entry : queryMcvState().entrySet())
                buf.append(entry.getKey()+"="+entry.getValue()+"\n");
        }
        return buf.toString();
    }
    
    // each entry should have:
    // group
    // "human" name
    // property id
    // short description
    // value

//    public static class SystemStateProperty {
//        private final Group grouping;
//        private final String property;
//        private final String name;
//        private final String description;
//        private final String value;
//
//        private SystemStateProperty(final Builder builder) {
//            this.grouping = builder.grouping;
//            this.property = builder.property;
//            this.name = (builder.name.length() == 0) ? this.property : builder.name;
//            this.description = builder.description;
//            this.value = builder.value;
//        }
//    }
//
//    public static class Builder {
//        private final String property;
//        private final String value;
//
//        private Group grouping = Group.MISC;
//        private String name = "";
//        private String description = "";
//
//        public Builder(final String property, final String value) {
//            this.property = property;
//            this.value = value;
//        }
//
//        public Builder group(final Group grouping) {
//            this.grouping = grouping;
//            return this;
//        }
//
//        public Builder name(final String name) {
//            this.name = name;
//            return this;
//        }
//
//        public Builder desc(final String description) {
//            this.description = description;
//            return this;
//        }
//
//        public SystemStateProperty build() {
//            return new SystemStateProperty(this);
//        }
//    }
}
