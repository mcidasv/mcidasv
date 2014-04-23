/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2014
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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.StringUtil;
import visad.VisADException;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.PatternFileFilter;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;

/**
 * McIDAS-V needs to handle a few command line flags/options that the IDV does
 * not. Only the ability to force the Aqua look and feel currently exists.
 * 
 * @author McIDAS-V Developers
 */
public class ArgumentManager extends ArgsManager {

    private static final Logger helpLogger = LoggerFactory.getLogger("mcvstdout");

    /** McIDAS-V flag that signifies everything that follows is a Jython argument. */
    public static final String ARG_JYTHONARGS = "-scriptargs";

    public static final String ARG_LOGPATH = "-logpath";

    /** usage message */
    public static final String USAGE_MESSAGE =
        "Usage: runMcV [OPTIONS] <bundle/script files, e.g., .mcv, .mcvz, .py>";

    /** Jython arguments, if any. */
    private List<String> jythonArguments;
    
    /** Jython script to execute, or {@literal "<none>"} if one was not given. */
    private String jythonScript;

    /**
     *  Given by the "-user" argument. Alternative user path for bundles,  resources, etc.
     */
    String defaultUserDirectory = StartupManager.getInstance().getPlatform().getUserDirectory();

    /**
     * Just bubblin' on up the inheritance hierarchy.
     * 
     * @param idv The IDV instance.
     * @param args The command line arguments that were given.
     */
    public ArgumentManager(IntegratedDataViewer idv, String[] args) {
        super(idv, args);
        jythonArguments = new ArrayList<String>();
        jythonScript = "<none>";
    }

    private static List<String> extractJythonArgs(int index, String[] args) {
        List<String> jythonArgs = new ArrayList<String>(args.length);
        for (int i = index; i < args.length; i++) {
            jythonArgs.add(args[i]);
        }
        return jythonArgs;
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

        if ("-forceaqua".equals(arg)) {
            // unfortunately we can't simply set the look and feel here. If I
            // were to do so, the loadLookAndFeel in the IdvUIManager would 
            // eventually get loaded and then set the look and feel to whatever
            // the preferences dictate.
            // instead I use the boolean toggle to signal to McV's 
            // UIManager.loadLookAndFeel that it should simply ignore the user's
            // preference is and load the Aqua L&F from there.
            McIDASV.useAquaLookAndFeel = true;
        } else if (ARG_HELP.equals(arg)) {
            helpLogger.info(USAGE_MESSAGE);
            helpLogger.info(getUsageMessage());
            ((McIDASV)getIdv()).exit(1);
        } else if (checkArg(arg, "-script", args, idx, 1) || checkArg(arg, "-pyfile", args, idx, 1)) {
            String scriptArg = args[idx++];
            jythonScript = scriptArg;
            scriptingFiles.add(scriptArg);
            if (!getIslInteractive()) {
                setIsOffScreen(true);
            }
        } else if ("-console".equals(arg)) {
            System.err.println("*** WARNING: console flag is likely to go away soon!");
        } else if (ARG_JYTHONARGS.equals(arg)) {
            if (scriptingFiles.isEmpty()) {
                System.err.println("*** WARNING: Jython script arguments will be ignored unless you provide a Jython script to execute!");
            } else {
                jythonArguments.addAll(extractJythonArgs(idx, args));
                
                // jump to end of args to halt further idv processing.
                return args.length;
            }
        } else if (checkArg(arg, ARG_LOGPATH, args, idx, 1)) {
            String argValue = args[idx++];
            persistentCommandLineArgs.add(ARG_LOGPATH);
            persistentCommandLineArgs.add(argValue);
        } else {
            if (ARG_ISLINTERACTIVE.equals(arg) || ARG_B64ISL.equals(arg) || ARG_ISLFILE.equals(arg) || isIslFile(arg)) {
                System.err.println("*** WARNING: ISL is being deprecated!");
            } else if (arg.startsWith("-D")) {
                List<String> l = StringUtil.split(arg.substring(2), "=");
                if (l.size() == 2) {
                    System.setProperty(l.get(0), l.get(1));
                }
            }
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
        msg = msg + '\n' + getUsageMessage();
        LogUtil.userErrorMessage(err + '\n' + msg);
        ((McIDASV)getIdv()).exit(1);
    }

    /**
     * Append some McIDAS-V specific command line options to the default IDV
     * usage message.
     *
     * @return Usage message.
     */
    protected String getUsageMessage() {
        return msg(ARG_HELP, "(this message)")
            + msg("-forceaqua", "Forces the Aqua look and feel on OS X")
            + msg(ARG_PROPERTIES, "<property file>")
            + msg("-Dpropertyname=value", "(Define the property value)")
            + msg(ARG_INSTALLPLUGIN, "<plugin jar file or url to install>")
            + msg(ARG_PLUGIN, "<plugin jar file, directory, url for this run>")
            + msg(ARG_NOPLUGINS, "Don't load plugins")
            + msg(ARG_CLEARDEFAULT, "(Clear the default bundle)")
            + msg(ARG_NODEFAULT, "(Don't read in the default bundle file)")
            + msg(ARG_DEFAULT, "<.mcv/.mcvz file>")
            + msg(ARG_BUNDLE, "<bundle file or url>")
            + msg(ARG_B64BUNDLE, "<base 64 encoded inline bundle>")
            + msg(ARG_SETFILES, "<datasource pattern> <semi-colon delimited list of files> (Use the list of files for the bundled datasource)")
            + msg(ARG_ONEINSTANCEPORT, "<port number> (Check if another version of McIDAS-V is running. If so pass command line arguments to it and shutdown)")
            + msg(ARG_NOONEINSTANCE, "(Don't do the one instance port)")
            + msg(ARG_NOPREF, "(Don't read in the user preferences)")
            + msg(ARG_USERPATH, "<user directory to use>")
            + msg(ARG_LOGPATH, "<path to log file>")
            + msg(ARG_SITEPATH, "<url path to find site resources>")
            + msg(ARG_NOGUI, "(Don't show the main window gui)")
            + msg(ARG_DATA, "<data source> (Load the data source)")
            + msg(ARG_DISPLAY, "<parameter> <display>")
//            + msg("<scriptfile.isl>", "(Run the IDV script in batch mode)")
            + msg("-script", "<jython script file to evaluate>")
            + msg("-pyfile", "<jython script file to evaluate>")
            + msg(ARG_JYTHONARGS, "All arguments after this flag will be considered Jython arguments.")
//            + msg(ARG_B64ISL, "<base64 encoded inline isl> This will run the isl in interactive mode")
//            + msg(ARG_ISLINTERACTIVE, "run any isl files in interactive mode")
            + msg(ARG_IMAGE, "<image file name> (create a jpeg image and then exit)")
            + msg(ARG_MOVIE, "<movie file name> (create a quicktime movie and then exit)")
            + msg(ARG_IMAGESERVER, "<port number or .properties file> (run McIDAS-V in image generation server mode. Support http requests on the given port)")
            + msg(ARG_CATALOG, "<url to a chooser catalog>")
            + msg(ARG_CONNECT, "<collaboration hostname to connect to>")
            + msg(ARG_SERVER, "(Should McIDAS-V run in collaboration server mode)")
            + msg(ARG_PORT, "<Port number collaboration server should listen on>")
            + msg(ARG_CHOOSER, "(show the data chooser on start up) ")
            + msg(ARG_PRINTJNLP, "(Print out any embedded bundles from jnlp files)")
            + msg(ARG_CURRENTTIME, "<dttm> (Override current time for background processing)")
//            + msg(ARG_CURRENTTIME, "<dttm> (Override current time for ISL processing)")
            + msg(ARG_LISTRESOURCES, "<list out the resource types")
            + msg(ARG_DEBUG, "(Turn on debug print)")
            + msg(ARG_MSG_DEBUG, "(Turn on language pack debug)")
            + msg(ARG_MSG_RECORD, "<Language pack file to write missing entries to>")
            + msg(ARG_TRACE, "(Print out trace messages)")
            + msg(ARG_NOERRORSINGUI, "(Don't show errors in gui)")
            + msg(ARG_TRACEONLY, "<trace pattern> (Print out trace messages that match the pattern)")
            + msg("-console", "[ fix for getting the console functionality in install4j launcher ]");
    }
    
    /**
     * Determine whether or not the user has provided any arguments for a 
     * Jython script.
     * 
     * @return {@code true} if the user has provided Jython arguments, 
     * {@code false} otherwise.
     */
    public boolean hasJythonArguments() {
        return !jythonArguments.isEmpty();
    }
    
    /**
     * Returns Jython arguments. <b>Note:</b> this does not include the Jython
     * script that will be executed.
     * 
     * @return Either a {@link List} of {@link String Strings} containing the
     * arguments or an empty {@code List} if there were no arguments given.
     */
    public List<String> getJythonArguments() {
        return jythonArguments;
    }
    
    /**
     * Returns the name of the Jython script the user has provided.
     * 
     * @return Either the path to a Jython file or {@literal "<none>"} if the
     * user did not provide a script.
     */
    public String getJythonScript() {
        return jythonScript;
    }
    
    /**
     * Gets called by the IDV to process the set of initial files, e.g.,
     * default bundles, command line bundles, jnlp files, etc.
     * 
     * <p>Overridden by McIDAS-V to remove bundle file paths that are zero
     * characters long. This was happening because {@code runMcV.bat} was
     * always passing {@literal '-bundle ""'} on the command line (for Windows). 
     * 
     * @throws VisADException When something untoward happens
     * @throws RemoteException When something untoward happens
     */
    @Override protected void processInitialBundles()
            throws VisADException, RemoteException 
    {
        for (int i = 0; i < argXidvFiles.size(); i++) {
            String path = (String)argXidvFiles.get(i);
            if (path.isEmpty()) {
                argXidvFiles.remove(i);
            }
        }
        super.processInitialBundles();
    }
    
    /**
     * @see ArgsManager#getBundleFileFilters()
     */
    @Override public List<PatternFileFilter> getBundleFileFilters() {
        List<PatternFileFilter> filters = new ArrayList<PatternFileFilter>(); 
        Collections.addAll(filters, getXidvFileFilter(), getZidvFileFilter(), FILTER_JNLP);
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
        if (fromOpen) {
            Collections.addAll(filters, getXidvZidvFileFilter(), FILTER_JNLP, FILTER_ISL, super.getXidvZidvFileFilter());
        } else {
            filters.addAll(getBundleFileFilters());
        }
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
