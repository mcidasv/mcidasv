package edu.wisc.ssec.mcidasv.util;

import static edu.wisc.ssec.mcidasv.util.CollectionHelpers.list;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;

import javax.swing.JOptionPane;

public final class WebBrowser {

    /** Probe Unix-like systems for these browsers, in this order. */
    private static final List<String> unixBrowsers = 
        list("firefox", "konqueror", "opera", "mozilla", "netscape");

    /** None shall instantiate WebBrowser!! */
    private WebBrowser() { }

    /**
     * Attempts to use the system default browser to visit {@code url}. Tries
     * the new (as of Java 1.6) way of opening a browser first, and falls back
     * on more primitive measures should the nice stuff fail (or we're running
     * in 1.5).
     * 
     * @param url URL to visit.
     */
    public static void browse(final String url) {
        // determine whether or not we can use the 1.6 classes
        if (canAttemptNewStyle())
            if (openNewStyle(url))
                return;

        // if not, use the hacky stuff.
        openOldStyle(url);
    }

    /**
     * Uses the new functionality in {@link java.awt.Desktop} to try opening
     * the browser. Because McIDAS-V does not yet require Java 1.6, and 
     * {@code Desktop} was introduced in 1.6, we have to jump through some
     * reflection hoops.
     * 
     * @param url URL to visit.
     * 
     * @return {@code true} if things look ok, {@code false} if there were
     * problems.
     */
    private static boolean openNewStyle(final String url) {
        boolean retVal = true;
        try {
            Class desktop = Class.forName("java.awt.Desktop");
            Method isDesktopSupported = desktop.getMethod("isDesktopSupported", null);
            Boolean b = (Boolean)isDesktopSupported.invoke(null, null);
            if (b.booleanValue()) {
                final Object desktopInstance = desktop.getMethod("getDesktop", null).invoke(null, null);
                Class desktopAction = Class.forName("java.awt.Desktop$Action");
                Method isSupported = desktop.getMethod("isSupported", new Class[] { desktopAction });
                Object browseConst = desktopAction.getField("BROWSE").get(null);
                b = (Boolean) isSupported.invoke(desktopInstance, new Object[] {browseConst});
                if (b.booleanValue()) {
                    final Method browse = desktop.getMethod("browse", new Class[]{ URI.class });
                    browse.invoke(desktopInstance, new Object[] { new URI(url) });
                    retVal = true;
                } else {
                    retVal = false;
                }
            } else {
                retVal = false;
            }
        } catch (ClassNotFoundException e) {
            // JDK 5, ignore
            retVal = false;
        } catch (Exception e) {
            retVal = false;
        }
        return retVal;
    }

    /**
     * Uses {@link Runtime#exec(String)} to launch the user's preferred web
     * browser. This method isn't really recommended.
     * 
     * @param url URL to visit.
     */
    private static void openOldStyle(final String url) {
        try {
            if (isWindows()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                System.err.println("windows launching="+url);
            } else if (isMac()) {
                Runtime.getRuntime().exec("/usr/bin/open "+url);
                System.err.println("mac launching="+url);
            } else {
                for (String browser : unixBrowsers) {
                    if (Runtime.getRuntime().exec("which "+browser).waitFor() == 0) {
                        Runtime.getRuntime().exec(browser+" "+url);
                        System.err.println("unix launching="+browser);
                        return;
                    }
                }
                throw new IOException("Could not find a web browser to launch (tried "+unixBrowsers+")");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Problem running web browser:\n" + e.getLocalizedMessage());
        }
    }

    /**
     * There's supposedly a bug lurking that can hang the JVM on Linux if
     * {@code java.net.useSystemProxies} is enabled. Detect whether or not our
     * configuration may trigger the bug.
     * 
     * @return {@code true} if everything is ok, {@code false} otherwise.
     */
    private static boolean canAttemptNewStyle() {
        if (Boolean.getBoolean("java.net.useSystemProxies") && isUnix()) {
            // remove this check if JDK's bug 6496491 is fixed or if we can 
            // assume ORBit >= 2.14.2 and gnome-vfs >= 2.16.1
            return false;
        } 
        return true;
    }

    /**
     * @return Are we shiny, happy OS X users?
     */
    private static boolean isMac() {
        return System.getProperty("os.name", "").startsWith("Mac OS");
    }

    /**
     * @return Do we perhaps think that beards and suspenders are the height 
     * of fashion?
     */
    private static boolean isUnix() {
        return !isMac() && !isWindows();
    }

    /**
     * @return Are we running Windows??
     */
    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    public static void main(String[] args) {
        browse("http://www.haskell.org/"); // sassy!
    }
}
