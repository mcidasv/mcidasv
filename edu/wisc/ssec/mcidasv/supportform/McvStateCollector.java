/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2010
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import ucar.unidata.util.LogUtil;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.StateManager;
import edu.wisc.ssec.mcidasv.util.SystemState;

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
        if (mcv == null) {
            throw new NullPointerException(); // TODO: message
        }
        this.mcv = mcv;
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
        return mcv.getUserFile("mcidasv.log");
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
//        Map<Object, Object> props = new LinkedHashMap<Object, Object>();

        Map<String, String> versions = ((StateManager)mcv.getStateManager()).getVersionInfo();
        Properties sysProps = System.getProperties();
        Map<String, Object> j3dProps = SystemState.queryJava3d();
        Map<String, String> machineProps = SystemState.queryMachine();
        Map<String, Object> mcvProps = SystemState.queryMcvState(mcv);

        StringBuffer buf = new StringBuffer(20000);
        buf.append("# Software Versions:")
            .append("\n# McIDAS-V: ").append(versions.get("mcv.version.general")).append(" (").append(versions.get("mcv.version.build")).append(')')
            .append("\n# IDV:      ").append(versions.get("idv.version.general")).append(" (").append(versions.get("idv.version.build")).append(')')
            .append("\n\n# Operating System:")
            .append("\n# Name:         ").append(sysProps.getProperty("os.name"))
            .append("\n# Version:      ").append(sysProps.getProperty("os.version"))
            .append("\n# Architecture: ").append(sysProps.getProperty("os.arch"))
            .append("\n\n# Java:")
            .append("\n# Version: ").append(sysProps.getProperty("java.version"))
            .append("\n# Vendor:  ").append(sysProps.getProperty("java.vendor"))
            .append("\n# Home:    ").append(sysProps.getProperty("java.home"))
            .append("\n\n# Java 3D:")
            .append("\n# Renderer: ").append(j3dProps.get("j3d.renderer"))
            .append("\n# Pipeline: ").append(j3dProps.get("j3d.pipeline"))
            .append("\n# Vendor:   ").append(j3dProps.get("j3d.vendor"))
            .append("\n# Version:  ").append(j3dProps.get("j3d.version"));

        // get software versions
        for (Entry<String, String> entry : versions.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

        // get machine properties
        for (Entry<String, String> entry : machineProps.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

        // get java system properties
        for (Entry<Object, Object> entry : sysProps.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

        // get java3d/jogl properties
        for (Entry<String, Object> entry : j3dProps.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

        // get idv/mcv properties
        for (Entry<String, Object> entry : mcvProps.entrySet()) {
            buf.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
        }

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
