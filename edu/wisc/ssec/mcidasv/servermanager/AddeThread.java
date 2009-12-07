package edu.wisc.ssec.mcidasv.servermanager;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.servermanager.McservEvent.EventLevel;
import edu.wisc.ssec.mcidasv.servermanager.McservEvent.McservStatus;

/**
 * Thread that actually execs mcservl
 */
public class AddeThread extends Thread {

//    /** {@literal "Root"} local server directory. */
//    private final String ADDE_DIRECTORY;
//
//    /** Path to local server binaries. */
//    private final String ADDE_BIN;
//
//    /** Path to local server data. */
//    private final String ADDE_DATA;
//
//    /** Path to mcservl. */
    private final String ADDE_MCSERVL;
//
//    /** Path to the user's {@literal ".mcidasv"} directory. */
//    private final String USER_DIRECTORY;
//
//    /** Path to the user's {@literal "RESOLV.SRV"}. */
//    private final String ADDE_RESOLV;
//
//    /** */
//    private final String MCTRACE;
//
//    private final String[] ADDE_ENV;
//
////    private String[] addeCommands = { ADDE_MCSERVL, "-p", LOCAL_PORT, "-v" };
    private final String[] ADDE_COMMANDS;

    /** 
     * The letter of the drive where McIDAS-V lives. Only applicable to 
     * Windows.
     * 
     * @see McIDASV#getJavaDriveLetter()
     */
    private String javaDriveLetter = McIDASV.getJavaDriveLetter();

//    private String localPort = Constants.LOCAL_ADDE_PORT;

    /** */
    int result;

    /** */
    Process proc;

    //prepare buffers for process output and error streams
    /** STDERR buffer for mcservl. */
    private final StringBuffer err = new StringBuffer();

    /** STDOUT buffer for mcservl. */
    private final StringBuffer out = new StringBuffer();

    /** */
    private final EntryStore entryStore;

    private final String[] ADDE_ENV;

    public AddeThread(final EntryStore entryStore) {
        this.entryStore = entryStore;

        if (McIDASV.isWindows()) {
            ADDE_ENV = entryStore.getWindowsAddeEnv();
            ADDE_MCSERVL = System.getProperty("user.dir") + File.separator + "adde" + File.separator + "bin" + File.separator + "mcservl.exe";
        } else {
            ADDE_ENV = entryStore.getUnixAddeEnv();
            ADDE_MCSERVL = System.getProperty("user.dir") + File.separator + "adde" + File.separator + "bin" + File.separator + "mcservl";
        }
        ADDE_COMMANDS = entryStore.getAddeCommands();
//        if (McIDASV.isWindows()) {
//            ADDE_DIRECTORY = System.getProperty("user.dir") + "\\adde";
//            ADDE_BIN = ADDE_DIRECTORY + "\\bin";
//            ADDE_DATA = ADDE_DIRECTORY + "\\data";
//            ADDE_MCSERVL = ADDE_BIN + "\\mcservl.exe";
//            USER_DIRECTORY = entryStore.getUserDirectory();
//            ADDE_RESOLV = USER_DIRECTORY + "\\RESOLV.SRV";
//            MCTRACE = "0";
//            ADDE_ENV = getWindowsAddeEnv();
//        } else {
//            ADDE_DIRECTORY = System.getProperty("user.dir") + "/adde";
//            ADDE_BIN = ADDE_DIRECTORY + "/bin";
//            ADDE_DATA = ADDE_DIRECTORY + "/data";
//            ADDE_MCSERVL = ADDE_BIN + "/mcservl";
//            USER_DIRECTORY = entryStore.getUserDirectory();
//            ADDE_RESOLV = USER_DIRECTORY + "/RESOLV.SRV";
//            MCTRACE = "0";
//            ADDE_ENV = getUnixAddeEnv();
//        }
//
//        ADDE_COMMANDS = new String[] { ADDE_MCSERVL, "-p", localPort, "-v"};
    }

//    private String[] getWindowsAddeEnv() {
//        return new String[] {
//            "PATH=" + ADDE_BIN,
//            "MCPATH=" + USER_DIRECTORY + ":" + ADDE_DATA,
//            "MCNOPREPEND=1",
//            "MCTRACE=" + MCTRACE,
//            "MCJAVAPATH=" + System.getProperty("java.home"),
//            "MCBUFRJARPATH=" + ADDE_BIN,
//            "SYSTEMDRIVE=" + javaDriveLetter,
//            "SYSTEMROOT=" + javaDriveLetter + "\\Windows",
//            "HOMEDRIVE=" + javaDriveLetter,
//            "HOMEPATH=\\Windows"
//        };
//    }
//
//    private String[] getUnixAddeEnv() {
//        return new String[] {
//            "PATH=" + ADDE_BIN,
//            "MCPATH=" + USER_DIRECTORY + ":" + ADDE_DATA,
//            "LD_LIBRARY_PATH=" + ADDE_BIN,
//            "DYLD_LIBRARY_PATH=" + ADDE_BIN,
//            "MCNOPREPEND=1",
//            "MCTRACE=" + MCTRACE,
//            "MCJAVAPATH=" + System.getProperty("java.home"),
//            "MCBUFRJARPATH=" + ADDE_BIN
//        };
//    }

    public void run() {
        try {
            //start ADDE binary with "-p PORT" and set environment appropriately
            proc = Runtime.getRuntime().exec(ADDE_COMMANDS, ADDE_ENV);

            //create thread for reading inputStream (process' stdout)
            StreamReaderThread outThread = new StreamReaderThread(proc.getInputStream(),out);

            //create thread for reading errorStream (process' stderr)
            StreamReaderThread errThread = new StreamReaderThread(proc.getErrorStream(),err);

            //start both threads
            outThread.start();
            errThread.start();

            //wait for process to end
            result = proc.waitFor();

            //finish reading whatever's left in the buffers
            outThread.join();
            errThread.join();

            if (result != 0) {
                entryStore.stopLocalServer(entryStore.getRestarting());
                String errString = err.toString();

                /** If the server couldn't start for a known reason, try again on another port
                 *  Retry up to 10 times 
                 */
                if ((result==35584 || errString.indexOf("Error binding to port") >= 0) &&
                        Integer.parseInt(entryStore.getLocalPort()) < Integer.parseInt(Constants.LOCAL_ADDE_PORT) + 10) {
                    String oldPort = entryStore.getLocalPort();
                    entryStore.setLocalPort(entryStore.nextLocalPort());
                    entryStore.fireMcservEvent(McservStatus.NO_STATUS, EventLevel.DEBUG, "couldn't start on port "+ oldPort + ", trying " + entryStore.getLocalPort());
                    entryStore.startLocalServer(entryStore.getRestarting());
                } else {
                    entryStore.fireMcservEvent(McservStatus.NO_STATUS, EventLevel.DEBUG, "returned: "+errString);
                }
            } else {
                entryStore.fireMcservEvent(McservStatus.NO_STATUS, EventLevel.DEBUG, "went away...");
            }

        } catch (InterruptedException e) {
            McservStatus status = McservStatus.DIED;
            EventLevel level = EventLevel.ERROR;
            if (entryStore.getRestarting()) {
                status = McservStatus.RESTARTED;
                level = EventLevel.NORMAL;
            }
            entryStore.fireMcservEvent(status, level, "mcservl was interrupted");
        } catch (Exception e) {
            entryStore.fireMcservEvent(McservStatus.DIED, EventLevel.ERROR, "Error executing "+ADDE_MCSERVL);
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    public void stopProcess() {
        proc.destroy();
    }

//    /**
//     * 
//     */
//    public String toString() {
//        return String.format("[AddeThread@%x: ADDE_ENV=%s, ADDE_COMMANDS=%s]", hashCode(), ADDE_ENV, ADDE_COMMANDS);
//    }

    /**
     * Thread to read the stderr and stdout of mcservl
     */
    private static class StreamReaderThread extends Thread {
        /** */
        private final StringBuffer mOut;

        /** */
        private final InputStreamReader mIn;

        /** */
        public StreamReaderThread(final InputStream in, final StringBuffer out) {
            mOut = out;
            mIn = new InputStreamReader(in);
        }

        /** */
        public void run() {
            int ch;
            try {
                while (-1 != (ch = mIn.read())) {
                    mOut.append((char)ch);
                }
            } catch (Exception e) {
                mOut.append("\nRead error: "+e.getMessage());
            }
        }
    }
}