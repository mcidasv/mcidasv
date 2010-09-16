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
package edu.wisc.ssec.mcidasv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.PatternFileFilter;

/**
 * McIDAS-V needs to handle a few command line flags/options that the IDV does
 * not. Only the ability to force the Aqua look and feel currently exists.
 * 
 * @author McIDAS-V Developers
 */
public class ArgumentManager extends ArgsManager {

    /** usage message */
    public static final String USAGE_MESSAGE =
        "Usage: runMcV <args> <bundle/script files, e.g., .mcv, .mcvz, .isl>";

    /**
     *  Given by the "-user" argument. Alternative user path for bundles,  resources, etc.
     */
    String defaultUserDirectory = StartupManager.INSTANCE.getPlatform().getUserDirectory();

    /**
     * Just bubblin' on up the inheritance hierarchy.
     * 
     * @param idv The IDV instance.
     * @param args The command line arguments that were given.
     */
    public ArgumentManager(IntegratedDataViewer idv, String[] args) {
        super(idv, args);
    }

    /**
     * Currently we're only handling the {@code -forceaqua} flag so we can
     * mitigate some overlay issues we've been seeing on OS X Leopard.
     * 
     * @param arg The current argument we're examining.
     * @param args The actual array of arguments.
     * @param idx The index of {@code arg} within {@code args}.
     * 
     * @return The idx of the last value in the args array we look at. i.e., 
     * if the flag arg does not require any further values in the args array 
     * then don't increment idx.  If arg requires one more value then 
     * increment idx by one. etc.
     * 
     * @throws Exception Throw bad things off to something that can handle 'em!
     */
    protected int parseArg(String arg, String[] args, int idx) 
        throws Exception {

        if (arg.equals("-forceaqua")) {
            // unfortunately we can't simply set the look and feel here. If I
            // were to do so, the loadLookAndFeel in the IdvUIManager would 
            // eventually get loaded and then set the look and feel to whatever
            // the preferences dictate.
            // instead I use the boolean toggle to signal to McV's 
            // UIManager.loadLookAndFeel that it should simply ignore the user's
            // preference is and load the Aqua L&F from there.
            McIDASV.useAquaLookAndFeel = true;
        } else if (arg.equals(ARG_HELP)) {
            System.err.println(USAGE_MESSAGE);
            System.err.println(getUsageMessage());
            ((McIDASV)getIdv()).exit(1);
        } else {
            return super.parseArg(arg, args, idx);
        }
        return idx;
    }

    /**
     * Print out the command line usage message and exit
     * 
     * @param err The usage message
     */
    @Override public void usage(String err) {
        String msg = USAGE_MESSAGE;
        msg = msg + "\n" + getUsageMessage();
        LogUtil.userErrorMessage(err + "\n" + msg);
        ((McIDASV)getIdv()).exit(1);
    }

    /**
     * Append some McIDAS-V specific command line options to the default IDV
     * usage message.
     *
     * @return Usage message.
     */
    protected String getUsageMessage() {
        return msg("-forceaqua", "Forces the Aqua look and feel on OS X")
               + super.getUsageMessage();
    }

    /**
     * @see ArgsManager#getBundleFileFilters()
     */
    @Override public List<PatternFileFilter> getBundleFileFilters() {
        List<PatternFileFilter> filters = new ArrayList<PatternFileFilter>(); 
        Collections.addAll(filters, getXidvFileFilter(), getZidvFileFilter(), FILTER_JNLP, FILTER_ISL, super.getXidvFileFilter(), super.getZidvFileFilter());
        return filters;
    }

    /**
     * Returns a list of {@link PatternFileFilter}s that can be used to determine
     * if a file is a bundle. 
     * 
     * <p>If {@code fromOpen} is {@code true}, the 
     * returned list will contain {@code PatternFileFilter}s for bundles as 
     * well as JNLP and ISL files. If {@code false}, the returned list will
     * only contain filters for XML and zipped bundles.
     * 
     * @param fromOpen Whether or not this has been called from an 
     * {@literal "open file"} dialog. 
     * 
     * @return Filters for bundles.
     */
    public List<PatternFileFilter> getBundleFilters(final boolean fromOpen) {
        List<PatternFileFilter> filters = new ArrayList<PatternFileFilter>();

        if (fromOpen)
            Collections.addAll(filters, getXidvZidvFileFilter(), FILTER_JNLP, FILTER_ISL, super.getXidvZidvFileFilter());
        else
            filters.addAll(getBundleFileFilters());

        return filters;
    }

    /**
     * @see ArgsManager#getXidvFileFilter()
     */
    @Override public PatternFileFilter getXidvFileFilter() {
        return Constants.FILTER_MCV;
    }

    /**
     * @see ArgsManager#getZidvFileFilter()
     */
    @Override public PatternFileFilter getZidvFileFilter() {
        return Constants.FILTER_MCVZ;
    }

    /**
     * @see ArgsManager#getXidvZidvFileFilter()
     */
    @Override public PatternFileFilter getXidvZidvFileFilter() {
        return Constants.FILTER_MCVMCVZ;
    }

    /*
     * There's some internal IDV file opening code that relies on this method.
     * We've gotta override if we want to use .zidv bundles.
     */
    @Override public boolean isZidvFile(final String name) {
        return isZippedBundle(name);
    }

    /* same story as isZidvFile! */
    @Override public boolean isXidvFile(final String name) {
        return isXmlBundle(name);
    }

    /**
     * Tests to see if <code>name</code> has a known XML bundle extension.
     * 
     * @param name Name of the bundle.
     * 
     * @return Whether or not <code>name</code> has an XML bundle suffix.
     */
    public static boolean isXmlBundle(final String name) {
        return IOUtil.hasSuffix(name, Constants.FILTER_MCV.getPreferredSuffix())
            || IOUtil.hasSuffix(name, Constants.FILTER_XIDV.getPreferredSuffix());
    }

    /**
     * Tests to see if <code>name</code> has a known zipped bundle extension.
     * 
     * @param name Name of the bundle.
     * 
     * @return Whether or not <code>name</code> has zipped bundle suffix.
     */
    public static boolean isZippedBundle(final String name) {
        return IOUtil.hasSuffix(name, Constants.FILTER_MCVZ.getPreferredSuffix())
               || IOUtil.hasSuffix(name, Constants.FILTER_ZIDV.getPreferredSuffix());
    }

    /**
     * Tests <code>name</code> to see if it has a known bundle extension.
     * 
     * @param name Name of the bundle.
     * 
     * @return Whether or not <code>name</code> has a bundle suffix.
     */
    public static boolean isBundle(final String name) {
        return (isXmlBundle(name) || isZippedBundle(name));
    }
}