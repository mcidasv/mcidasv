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
package edu.wisc.ssec.mcidasv.startupmanager.options;

import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.EMPTY_STRING;
import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.QUOTE_STRING;
import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.SET_PREFIX;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.wisc.ssec.mcidasv.startupmanager.Platform;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class TextOption extends AbstractOption {

    /** Logging object. */
//    private static final Logger logger = LoggerFactory.getLogger(TextOption.class);

    /** Text field value. */
    private String value;

    /**
     * Create a startup option that allows the user to supply arbitrary text.
     *
     * <p><b>NOTE:</b> {@code null} is not a permitted value for any of this
     * constructor's parameters.</p>
     *
     * @param id Identifier for this startup option.
     * @param label Brief description suitable for a GUI label.
     * @param defaultValue Default value for this startup option.
     * @param optionPlatform Platforms where this option may be applied.
     * @param optionVisibility Whether or not the option is presented via the GUI.
     */
    public TextOption(final String id, final String label, 
        final String defaultValue, final OptionPlatform optionPlatform,
        final Visibility optionVisibility) 
    {
        super(id, label, Type.TEXT, optionPlatform, optionVisibility);
        value = defaultValue;
    }

    /**
     * Builds a {@link JTextField} containing the text value specified in the
     * constructor.
     *
     * @return {@code JTextField} to present to the user.
     */
    public JTextField getComponent() {
        final JTextField tf = new JTextField(value, 10);
        tf.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(final KeyEvent e) {
                setValue(tf.getText());
            }
        });
        if (!onValidPlatform()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() {
                    tf.setEnabled(false);
                }
            });
        }
        return tf;
    }

    /**
     * Returns the user's input (or the default value).
     *
     * @return Input or default value.
     */
    public String getValue() {
//        logger.trace("returning value='{}'", value);
        return value;
    }

    /**
     * Stores the user's input.
     *
     * @param newValue User input. Should not be {@code null}; use a zero
     * length {@code String} to specify an empty value.
     */
    public void setValue(final String newValue) {
//        logger.trace("overwrite value='{}' with newValue='{}'", value, newValue);
        value = newValue;
    }

    /**
     * Initializes the current option using a relevant variable from the
     * startup script.
     *
     * @param text Line from the startup script that represents the current
     * option. {@code null} is not allowed.
     *
     * @throws IllegalArgumentException if {@code text} is not in the proper
     * format for the current platform.
     */
    @Override public void fromPrefsFormat(final String text) {
        if (!isValidPrefFormat(text)) {
            throw new IllegalArgumentException("Incorrect syntax for this platform: " + text);
        }
        String copy = new String(text);
        if (StartupManager.getInstance().getPlatform() == Platform.WINDOWS) {
            copy = copy.replace(SET_PREFIX, EMPTY_STRING);
        }
        int splitAt = copy.indexOf('=');
        if (splitAt >= 0) {
            setValue(removeOutermostQuotes(copy.substring(splitAt + 1)));
        } else {
            setValue(EMPTY_STRING);
        }
    }

    /**
     * {@code String} representation of this {@code TextOption}.
     *
     * @return {@code String} that looks something like
     * {@literal "[TextOption@7825114a: optionId=BLAH value=USER INPUT]"}.
     */
    public String toString() {
        return String.format("[TextOption@%x: optionId=%s, value=%s]",
            hashCode(), getOptionId(), getValue());
    }

    /**
     * If the given {@code String} begins and ends with
     * {@link OptionMaster#QUOTE_STRING}, this method will return the given
     * {@code String} without the {@literal "outermost"} quote pair. Otherwise
     * the {@code String} is returned without modification.
     *
     * @param value {@code String} from which the outermost pair of quotes
     * should be removed. Cannot be {@code null}.
     *
     * @return Either {@code value} with the outermost pair of quotes removed,
     * or {@code value}, unmodified.
     */
    public static String removeOutermostQuotes(final String value) {
        String returnValue = value;
        if (value.startsWith(QUOTE_STRING) && value.endsWith(QUOTE_STRING)) {
            returnValue = value.substring(1, value.length() - 1);
        }
        return returnValue;
    }
}
