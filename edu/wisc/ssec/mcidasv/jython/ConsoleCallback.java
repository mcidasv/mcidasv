package edu.wisc.ssec.mcidasv.jython;


public interface ConsoleCallback {

    /**
     * Called after the console has run a block of Jython.
     * 
     * @param block The Jython block that was run.
     */
    public void ranBlock(final String block);
}
