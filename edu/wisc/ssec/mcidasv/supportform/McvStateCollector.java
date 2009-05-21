/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */
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

    /**
     * Builds a state collector that knows how to query McIDAS-V for specific
     * information.
     * 
     * @param mcv The McIDAS-V reference that we'll interrogate.
     */
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
     * @return Set of properties that contains interesting information about
     * the hardware McIDAS-V is using.
     */
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

    /**
     * Polls Java for information about the user's machine. We're specifically
     * after memory statistics, number of processors, and display information.
     * 
     * @return Set of properties that describes the user's machine.
     */
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

    /**
     * Polls Java 3D for information about its environment. Specifically, we 
     * call {@link VirtualUniverse#getProperties()} and 
     * {@link Canvas3D#queryProperties()}.
     * 
     * @return As much information as Java 3D can provide.
     */
    @SuppressWarnings("unchecked") // casting to Object, so this should be fine.
    private Properties queryJava3d() {
        Properties props = new Properties();
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
     * What should the name of the bundled version of McIDAS-V's current state 
     * be named?
     * 
     * @return Filename to use as an email attachment. Note that this file is
     * created specifically for the support request and will not exist otherwise.
     */
    public String getBundleAttachmentName() {
        return BUNDLE;
    }

    /**
     * What should the set of McIDAS-V system properties be named?
     * 
     * @return Filename to use as an email attachment. Again, this file does
     * not actually exist outside of the support request.
     */
    public String getExtraAttachmentName() {
        return EXTRA;
    }

    public String getLogPath() {
        String logPath = System.getProperty("user.home");
        if (System.getProperty("os.name", "").startsWith("Windows"))
            logPath += "\\.mcidasv\\mcidasv.log";
        else
            logPath += "/.mcidasv/mcidasv.log";
        return logPath;
    }

    /**
     * Builds the McIDAS-V system properties and returns the results as a 
     * {@code String}.
     * 
     * @return The McIDAS-V system properties in the following format: 
     * {@code KEY=VALUE\n}. This is so we kinda-sorta conform to the standard
     * {@link Properties} file format.
     */
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

    /**
     * The results of {@link #getContentsAsString()} as an array of {@code byte}s.
     * This makes for a particularly easy way to attach to a {@code HTTP POST}.
     */
    public byte[] getContents() {
        return getContentsAsString().getBytes();
    }

    public String toString() {
        return String.format("[McvStateCollector@%x: canBundleState=%s, bundle=%s, extra=%s]", hashCode(), canBundleState(), getBundleAttachmentName(), getExtraAttachmentName());
    }

    /**
     * Whether or not this {@link StateCollector} allows for attaching current
     * McIDAS-V state as a bundle.
     */
    public boolean canBundleState() {
        return true;
    }

    /**
     * Current McIDAS-V state as an XML bundle named by 
     * {@link #getBundleAttachmentName()}.
     */
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
