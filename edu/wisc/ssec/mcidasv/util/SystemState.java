/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2011
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
package edu.wisc.ssec.mcidasv.util;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import java.awt.DisplayMode;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.VirtualUniverse;

import org.python.core.Py;
import org.python.core.PySystemState;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IdvResourceManager.IdvResource;
import ucar.unidata.util.ResourceCollection;
import ucar.visad.display.DisplayUtil;

import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;

/**
 * Utility methods for querying the state of the user's machine.
 */
public class SystemState {

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
    private static <T> T hackyMethodCall(final String methodName, final T defaultValue) {
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
     * Returns the contents of Jython's registry (basically just Jython-specific
     * properties) as well as some of the information from Python's 
     * {@literal "sys"} module. 
     * 
     * @return Jython's configuration settings. 
     */
    public static Map<Object, Object> queryJythonProps() {
        Map<Object, Object> properties = 
            new LinkedHashMap<Object, Object>(PySystemState.registry);
        properties.put("sys.argv", Py.getSystemState().argv.toString());
        properties.put("sys.builtin_module_names", PySystemState.builtin_module_names.toString());
        properties.put("sys.byteorder", PySystemState.byteorder);
        properties.put("sys.isPackageCacheEnabled", PySystemState.isPackageCacheEnabled());
        properties.put("sys.path", Py.getSystemState().path);
        properties.put("sys.platform", PySystemState.platform.toString());
        properties.put("sys.version", PySystemState.version);
        properties.put("sys.version_info", PySystemState.version_info);
        return properties;
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
    public static Map<String, String> queryOpSysProps() {
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
     * @return {@link Map} of properties that describes the user's machine.
     */
    public static Map<String, String> queryMachine() {
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
            String baseId = "opsys.display."+i+'.';
            GraphicsDevice dev = ge.getScreenDevices()[i];
            DisplayMode mode = dev.getDisplayMode();
            props.put(baseId+"name", dev.getIDstring());
            props.put(baseId+"depth", Integer.toString(mode.getBitDepth()));
            props.put(baseId+"width", Integer.toString(mode.getWidth()));
            props.put(baseId+"height", Integer.toString(mode.getHeight()));
            props.put(baseId+"refresh", Integer.toString(mode.getRefreshRate()));
        }
        return props;
    }

    /**
     * Returns a mapping of display number to a {@link java.awt.Rectangle} 
     * that represents the {@literal "bounds"} of the display.
     */
    public static Map<Integer, Rectangle> getDisplayBounds() {
        Map<Integer, Rectangle> map = new LinkedHashMap<Integer, Rectangle>();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        int idx = 0;
        for (GraphicsDevice dev : ge.getScreenDevices()) {
            for (GraphicsConfiguration config : dev.getConfigurations()) {
                map.put(idx++, config.getBounds());
            }
        }
        return map;
    }

    
    
    // TODO(jon): this should really be a polygon
    public static Rectangle getVirtualDisplayBounds() {
        Rectangle virtualBounds = new Rectangle();
        for (Rectangle bounds : getDisplayBounds().values()) {
            virtualBounds = virtualBounds.union(bounds);
        }
        return virtualBounds;
    }

    /**
     * Polls Java 3D for information about its environment. Specifically, we 
     * call {@link VirtualUniverse#getProperties()} and 
     * {@link Canvas3D#queryProperties()}.
     * 
     * @return As much information as Java 3D can provide.
     */
    @SuppressWarnings("unchecked") // casting to Object, so this should be fine.
    public static Map<String, Object> queryJava3d() {
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
    // need: argsmanager, resource manager
    public static Map<String, Object> queryMcvState(final McIDASV mcv) {
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
            if (resource.getPattern() == null) {
                props.put(id+".pattern", "null");
            } else {
                props.put(id+".pattern", resource.getPattern());
            }

            ResourceCollection rc = mcv.getResourceManager().getResources(resource);
            List<String> specified = new ArrayList<String>();
            List<String> valid = new ArrayList<String>();
            for (int i = 0; i < rc.size(); i++) {
                specified.add((String)rc.get(i));
                if (rc.isValid(i)) {
                    valid.add((String)rc.get(i));
                }
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
    public static String getStateAsString(final McIDASV mcv) {
        return getStateAsString(mcv, false);
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
    public static String getStateAsString(final McIDASV mcv, final boolean firehose) {
        StringBuilder buf = new StringBuilder(20000);

        Map<String, String> versions = ((StateManager)mcv.getStateManager()).getVersionInfo();
        Properties sysProps = System.getProperties();
        Map<String, Object> j3dProps = queryJava3d();
        Map<String, String> machineProps = queryMachine();
        Map<Object, Object> jythonProps = queryJythonProps();
        Map<String, Object> mcvProps = queryMcvState(mcv);

        buf.append("Software Versions:")
            .append("\nMcIDAS-V: ").append(versions.get("mcv.version.general")).append(" (").append(versions.get("mcv.version.build")).append(')')
            .append("\nIDV:      ").append(versions.get("idv.version.general")).append(" (").append(versions.get("idv.version.build")).append(')')
            .append("\n\nOperating System:")
            .append("\nName:         ").append(sysProps.getProperty("os.name"))
            .append("\nVersion:      ").append(sysProps.getProperty("os.version"))
            .append("\nArchitecture: ").append(sysProps.getProperty("os.arch"))
            .append("\n\nJava:")
            .append("\nVersion: ").append(sysProps.getProperty("java.version"))
            .append("\nVendor:  ").append(sysProps.getProperty("java.vendor"))
            .append("\nHome:    ").append(sysProps.getProperty("java.home"))
            .append("\n\nJava 3D:")
            .append("\nRenderer: ").append(j3dProps.get("j3d.renderer"))
            .append("\nPipeline: ").append(j3dProps.get("j3d.pipeline"))
            .append("\nVendor:   ").append(j3dProps.get("j3d.vendor"))
            .append("\nVersion:  ").append(j3dProps.get("j3d.version"))
            .append("\n\nJython:")
            .append("\nVersion:     ").append(jythonProps.get("sys.version_info"))
            .append("\npython.home: ").append(jythonProps.get("python.home"));

        if (firehose) {
            buf.append("\n\n\nFirehose:\n\n# SOFTWARE VERSIONS\n");
            for (String key : (new TreeSet<String>(versions.keySet()))) {
                buf.append(key).append('=').append(versions.get(key)).append('\n');
            }

            buf.append("\n# MACHINE PROPERTIES\n");
            for (String key : (new TreeSet<String>(machineProps.keySet()))) {
                buf.append(key).append('=').append(machineProps.get(key)).append('\n');
            }

            buf.append("\n# JAVA SYSTEM PROPERTIES\n");
            for (Object key : (new TreeSet<Object>(sysProps.keySet()))) {
                buf.append(key).append('=').append(sysProps.get(key)).append('\n');
            }

            buf.append("\n# JAVA3D/JOGL PROPERTIES\n");
            for (String key : (new TreeSet<String>(j3dProps.keySet()))) {
                buf.append(key).append('=').append(j3dProps.get(key)).append('\n');
            }

            buf.append("\n# JYTHON PROPERTIES\n");
            for (Object key : (new TreeSet<Object>(jythonProps.keySet()))) {
                buf.append(key).append('=').append(jythonProps.get(key)).append('\n');
            }

            // get idv/mcv properties
            buf.append("\n# IDV AND MCIDAS-V PROPERTIES\n");
            for (String key : (new TreeSet<String>(mcvProps.keySet()))) {
                buf.append(key).append('=').append(mcvProps.get(key)).append('\n');
            }
        }
        return buf.toString();
    }
    

}
