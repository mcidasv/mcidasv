package edu.wisc.ssec.mcidasv.servermanager;

import java.io.InputStream;
import java.io.InputStreamReader;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;

/**
 * Thread that actually execs mcservl
 */
public class AddeThread extends Thread {

//    String[] addeCommands = { addeMcservl, "-p", LOCAL_PORT, "-v" };

    String[] addeEnvUnix = {
//            "PATH=" + addeBin,
//            "MCPATH=" + userDirectory + ":" + addeData,
//            "LD_LIBRARY_PATH=" + addeBin,
//            "DYLD_LIBRARY_PATH=" + addeBin,
//            "MCNOPREPEND=1",
//            "MCTRACE=" + MCTRACE,
//            "MCJAVAPATH=" + System.getProperty("java.home"),
//            "MCBUFRJARPATH=" + addeBin
    };

    String javaDriveLetter = System.getProperty("java.home").substring(0,2);
    String[] addeEnvWindows = {
//            "PATH=" + addeBin,
//            "MCPATH=" + userDirectory + ":" + addeData,
//            "MCNOPREPEND=1",
//            "MCTRACE=" + MCTRACE,
//            "MCJAVAPATH=" + System.getProperty("java.home"),
//            "MCBUFRJARPATH=" + addeBin,
//            "SYSTEMDRIVE=" + javaDriveLetter,
//            "SYSTEMROOT=" + javaDriveLetter + "\\Windows",
//            "HOMEDRIVE=" + javaDriveLetter,
//            "HOMEPATH=\\Windows"
    };

    int result;
    Process proc;

    //prepare buffers for process output and error streams
    private final StringBuffer err = new StringBuffer();
    private final StringBuffer out = new StringBuffer();

    private final EntryStore entryStore;

    public AddeThread(final EntryStore entryStore) {
        this.entryStore = entryStore;
    }

    public void run() {
//        try {
//            //start ADDE binary with "-p PORT" and set environment appropriately
//            if (McIDASV.isUnixLike()) {
//                proc = Runtime.getRuntime().exec(addeCommands, addeEnvUnix);
//            } else {
//                proc = Runtime.getRuntime().exec(addeCommands, addeEnvWindows);
//            }
//
//            //create thread for reading inputStream (process' stdout)
//            StreamReaderThread outThread = new StreamReaderThread(proc.getInputStream(),out);
//
//            //create thread for reading errorStream (process' stderr)
//            StreamReaderThread errThread = new StreamReaderThread(proc.getErrorStream(),err);
//
//            //start both threads
//            outThread.start();
//            errThread.start();
//
//            //wait for process to end
//            result=proc.waitFor();
//
//            //finish reading whatever's left in the buffers
//            outThread.join();
//            errThread.join();
//
//            if (result!=0) {
//                stopLocalServer();
//                String errString = err.toString();
//
//                /** If the server couldn't start for a known reason, try again on another port
//                 *  Retry up to 10 times 
//                 */
//                if ((result==35584 || errString.indexOf("Error binding to port") >= 0) &&
//                        Integer.parseInt(entryStore.getLocalPort()) < Integer.parseInt(Constants.LOCAL_ADDE_PORT) + 10) {
//                    String oldPort = entryStore.getLocalPort();
//                    setLocalPort(entryStore.nextLocalPort());
//                    System.err.println(addeMcservl + " couldn't start on port "+ oldPort + ", trying " + LOCAL_PORT);
//                    startLocalServer();
//                } else {
//                    System.err.println(addeMcservl + " returned: " + result);
//                    System.err.println("  " + errString);
//                }
//            } else {
//                System.err.println(addeMcservl + " went away...");
//            }
//
//        } catch (InterruptedException e) {
////          System.err.println(addeMcservl + " was interrupted");
//        } catch (Exception e) {
//            System.err.println("Error executing " + addeMcservl);
//            e.printStackTrace();
//        }
    }
    
    public void stopProcess() {
        proc.destroy();
    }

    /**
     * Thread to read the stderr and stdout of mcservl
     */
    private static class StreamReaderThread extends Thread {
        private final StringBuffer mOut;

        private final InputStreamReader mIn;

        public StreamReaderThread(final InputStream in, final StringBuffer out) {
            mOut = out;
            mIn = new InputStreamReader(in);
        }

        public void run() {
            int ch;
            try {
                while(-1 != (ch=mIn.read()))
                    mOut.append((char)ch);
            } catch (Exception e) {
                mOut.append("\nRead error: "+e.getMessage());
            }
        }
    }
}