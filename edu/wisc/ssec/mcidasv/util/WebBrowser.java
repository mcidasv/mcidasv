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
package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JOptionPane;

import ucar.unidata.util.LogUtil;

import edu.wisc.ssec.mcidasv.McIDASV;

/**
 * A simple utility class for opening a web browser to a given link.
 */
public final class WebBrowser {

    /** Probe Unix-like systems for these browsers, in this order. */
    private static final List<String> unixBrowsers = 
        list("firefox", "chromium-browser", "google-chrome", "konqueror",
             "opera", "mozilla", "netscape");

    /** Do not create instances of {@code WebBrowser}. */
    private WebBrowser() { }

    /**
     * Attempts to use the system default browser to visit {@code url}. Tries
     * looking for and executing any browser specified by the IDV property 
     * {@literal "idv.browser.path"}. 
     * 
     * <p>If the property wasn't given or there 
     * was an error, try the new (as of Java 1.6) way of opening a browser. 
     * 
     * <p>If the previous attempts failed (or we're in 1.5), we finally try
     * some more primitive measures.
     * 
     * <p>Note: if you are trying to use this method with a 
     * {@link javax.swing.JTextPane} you may need to turn off editing via
     * {@link javax.swing.JTextPane#setEditable(boolean)}.
     * 
     * @param url URL to visit.
     * 
     * @see #tryUserSpecifiedBrowser(String)
     * @see #openNewStyle(String)
     * @see #openOldStyle(String)
     */
    public static void browse(final String url) {
        // if the user has taken the trouble to explicitly provide the path to 
        // a web browser, we should probably use it. 
        if (tryUserSpecifiedBrowser(url)) {
            return;
        }

        // determine whether or not we can use the 1.6 classes
        if (canAttemptNewStyle()) {
            if (openNewStyle(url)) {
                return;
            }
        }

        // if not, use the hacky stuff.
        openOldStyle(url);
    }

    /**
     * Use the functionality within {@link java.awt.Desktop} to try opening
     * the user's preferred web browser.
     *
     * @param url URL to visit.
     * 
     * @return Either {@code true} if things look ok, {@code false} if there 
     * were problems.
     */
    private static boolean openNewStyle(final String url) {
        boolean retVal = false;
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                    // well... the assumption is that there was not a problem
                    retVal = true;
                } catch (URISyntaxException e) {
                    LogUtil.logException("Bad syntax in URI: "+url, e);
                } catch (IOException e) {
                    LogUtil.logException("Problem accessing URI: "+url, e);
                }
            }
        }
        return retVal;
    }

    /**
     * Uses {@link Runtime#exec(String)} to launch the user's preferred web
     * browser. This method isn't really recommended unless you're stuck with
     * Java 1.5.
     * 
     * <p>Note that the browsers need to be somewhere in the PATH, as this 
     * method uses the {@code which} command (also needs to be in the PATH!).
     * 
     * @param url URL to visit.
     */
    private static void openOldStyle(final String url) {
        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (isMac()) {
                Runtime.getRuntime().exec("/usr/bin/open "+url);
            } else {
                for (String browser : unixBrowsers) {
                    if (Runtime.getRuntime().exec("which "+browser).waitFor() == 0) {
                        Runtime.getRuntime().exec(browser+' '+url);
                        return;
                    }
                }
                throw new IOException("Could not find a web browser to launch (tried "+unixBrowsers+')');
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Problem running web browser:\n" + e.getLocalizedMessage());
        }
    }

    /**
     * Attempts to launch the browser pointed at by 
     * the {@literal "idv.browser.path"} IDV property, if it has been set.
     * 
     * @param url URL to open.
     * 
     * @return Either {@code true} if the command-line was executed, {@code false} if
     * either the command-line wasn't launched or {@literal "idv.browser.path"}
     * was not set.
     */
    private static boolean tryUserSpecifiedBrowser(final String url) {
        McIDASV mcv = McIDASV.getStaticMcv();
        boolean retVal = false;
        if (mcv != null) {
            String browserPath = mcv.getProperty("idv.browser.path", (String)null);
            if (browserPath != null && !browserPath.trim().isEmpty()) {
                try {
                    Runtime.getRuntime().exec(browserPath+' '+url);
                    retVal = true;
                } catch (Exception e) {
                    LogUtil.logException("Executing browser: "+browserPath, e);
                }
            }
        }
        return retVal;
    }

    /**
     * There's supposedly a bug lurking that can hang the JVM on Linux if
     * {@code java.net.useSystemProxies} is enabled. Detect whether or not our
     * configuration may trigger the bug.
     * 
     * @return Either {@code true} if everything is ok, {@code false} 
     * otherwise.
     */
    private static boolean canAttemptNewStyle() {
        // remove this check if JDK's bug 6496491 is fixed or if we can
        // assume ORBit >= 2.14.2 and gnome-vfs >= 2.16.1
        return isUnix() && Boolean.valueOf(System.getProperty("java.net.useSystemProxies", "true"));
    }

    /**
     * Test for whether or not the current platform is Mac OS X.
     *
     * @return Are we shiny, happy OS X users?
     */
    private static boolean isMac() {
        return System.getProperty("os.name", "").startsWith("Mac OS");
    }

    /**
     * Test for whether or not the current platform is some form of
     * {@literal "unix"} (but not OS X!).
     *
     * @return Do we perhaps think that beards and suspenders are the height 
     * of fashion?
     */
    private static boolean isUnix() {
        return !isMac() && !isWindows();
    }

    /**
     * Test for whether or not the current platform is Windows.
     *
     * @return Are we running Windows??
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    public static void main(String[] args) {
        browse("http://www.haskell.org/"); // sassy!
    }
}
