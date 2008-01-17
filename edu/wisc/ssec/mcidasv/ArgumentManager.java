package edu.wisc.ssec.mcidasv;

import java.util.Vector;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import ucar.unidata.idv.ArgsManager;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.TwoFacedObject;

/**
 * McIDAS-V needs to handle a few command line flags/options that the IDV does
 * not. Only the ability to force the Aqua look and feel currently exists.
 * 
 * @author McIDAS-V Developers
 */
public class ArgumentManager extends ArgsManager {

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
	 * Currently we're only handling the <code>-forceaqua</code> flag so we can
	 * mitigate some overlay issues we've been seeing on OS X Leopard.
	 * 
	 * @param arg The current argument we're examining.
	 * @param args The actual array of arguments.
	 * @param idx The index of <code>arg</code> within <code>args</code>.
	 *
	 * @return The idx of the last value in the args array we look at.
	 *         i.e., if the flag arg does not require any further values
	 *         in the args array then don't increment idx.  If arg requires
	 *         one more value then increment idx by one. etc.
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
			return idx;
		} else {
			return super.parseArg(arg, args, idx);
		}

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

}