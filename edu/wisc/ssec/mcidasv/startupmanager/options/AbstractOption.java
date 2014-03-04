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
import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.QUOTE_CHAR;
import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.QUOTE_STRING;
import static edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.SET_PREFIX;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import edu.wisc.ssec.mcidasv.startupmanager.Platform;
import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public abstract class AbstractOption implements Option {

    /**
     * A unique identifier for an option. Should be the same as the 
     * startup variable name found in the startup preference file.
     */
    private final String optionId;

    /** 
     * Brief description of the option. It will appear as the option's 
     * label in the GUI.
     */
    private final String label;

    /** @see Type */
    private final Type optionType;

    /** @see OptionPlatform */
    private final OptionPlatform optionPlatform;

    /** @see Visibility */
    private final Visibility optionVisibility;

    /**
     * Creates an option that can hold a specified sort of data and that
     * applies to a given platform.
     * 
     * @param id ID used to refer to this option.
     * @param label Text that'll be used as the GUI label for this option
     * @param optionType Type of data this option will represent.
     * @param optionPlatform Platform(s) where this option is applicable.
     * @param optionVisibility Visibility behavior of this option.
     */
    public AbstractOption(final String id, final String label, 
        final Type optionType, final OptionPlatform optionPlatform, 
        final Visibility optionVisibility) 
    {
        this.optionId = id;
        this.label = label;
        this.optionType = optionType;
        this.optionPlatform = optionPlatform;
        this.optionVisibility = optionVisibility;
    }

    /**
     * Determines if the option applies to the current platform.
     * 
     * @return {@code true} if this option is applicable, {@code false} 
     * otherwise.
     */
    protected boolean onValidPlatform() {
        OptionPlatform platform = getOptionPlatform();
        if (platform == OptionPlatform.ALL) {
            return true;
        }
        if (platform == OptionMaster.getInstance().convertToOptionPlatform()) {
            return true;
        }
        return false;
    }

    /**
     * Tests the specified string to see if it's valid for the current 
     * platform. Currently strings that contain {@literal "SET "} 
     * <b>[ note the space ]</b> are considered to be Windows-only, while 
     * strings lacking {@literal "SET "} are considered Unix-like.
     * 
     * @param text The string to test.
     * 
     * @return Whether or not the string is valid.
     */
    protected boolean isValidPrefFormat(final String text) {
        assert text != null;
        boolean hasSet = text.contains(SET_PREFIX);
        boolean isWin = StartupManager.getInstance().getPlatform() == Platform.WINDOWS;
        return isWin == hasSet;
    }

    /**
     * Returns this option's type.
     * 
     * @return Option's type.
     * 
     * @see Type
     */
    public Type getOptionType() {
        return optionType;
    }

    /**
     * Returns the platform(s) to which this option applies.
     * 
     * @return Option's platform.
     * 
     * @see OptionPlatform
     */
    public OptionPlatform getOptionPlatform() {
        return optionPlatform;
    }

    /**
     * Returns whether or not this option represents a visible UI element.
     * 
     * @return The option's visibility.
     * 
     * @see Visibility
     */
    public Visibility getOptionVisibility() {
        return optionVisibility;
    }

    /**
     * Returns the ID used when referring to this option.
     * 
     * @return Option's ID.
     */
    public String getOptionId() {
        return optionId;
    }

    /**
     * Returns a brief description of this option. Mostly useful for 
     * providing a GUI label.
     * 
     * @return Option's label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Initializes the current option using a relevant variable from the 
     * startup script.
     * 
     * @param text Line from the startup script that represents the current
     * option.
     * 
     * @throws IllegalArgumentException if {@code text} is not in the proper
     * format for the current platform.
     */
    public void fromPrefsFormat(final String text) {
        if (!isValidPrefFormat(text)) {
            throw new IllegalArgumentException("Incorrect syntax for this platform: " + text);
        }
        String copy = new String(text);
        if (StartupManager.getInstance().getPlatform() == Platform.WINDOWS) {
            copy = copy.replace(SET_PREFIX, EMPTY_STRING);
        }
        String[] chunks = copy.split("=");
        if (chunks.length == 2 && chunks[0].equals(optionId)) {
            setValue(chunks[1]);
        } else {
            setValue(EMPTY_STRING);
        }
    }

    /**
     * Returns a string representation of the current option that is suitable 
     * for use in the startup script.
     * 
     * @return Current value of this option as a startup script variable. The 
     * formatting changes slightly between {@literal "Unix-like"} platforms 
     * and Windows.
     * 
     * @see #isValidPrefFormat(String)
     */
    public String toPrefsFormat() {
        Platform platform = StartupManager.getInstance().getPlatform();
        StringBuilder str = new StringBuilder(optionId);
        String value = getValue();
        if (platform == Platform.WINDOWS) {
            str.insert(0, SET_PREFIX);
        }
        str.append('=');
        if ((platform != Platform.WINDOWS) && value.contains(" ")) {
            str.append(QUOTE_CHAR).append(value).append(QUOTE_CHAR);
        } else {
            str.append(value);
        }
        return str.toString();
    }

    /**
     * Returns the GUI component that represents the option. 
     * {@link BooleanOption BooleanOptions} are represented by a
     * {@link JCheckBox}, while {@link TextOption TextOptions} appear as a
     * {@link JTextField}.
     * 
     * @return The GUI representation of this option.
     */
    public abstract JComponent getComponent();

    /**
     * Returns the value of the option. Note that
     * {@link BooleanOption BooleanOptions} return either {@literal "0"} or
     * {@literal "1"}.
     * 
     * @return The current value of the option.
     */
    public abstract String getValue();

    /**
     * Forces the value of the option to the data specified. Note that 
     * {@link BooleanOption BooleanOptions} accept either {@literal "0"}, or
     * {@literal "1"}.
     * 
     * @param value New value to use.
     */
    public abstract void setValue(final String value);

    /**
     * Friendly string representation of the option.
     * 
     * @return {@code String} containing relevant info about the option.
     * 
     * @see TextOption#toString()
     * @see BooleanOption#toString()
     */
    public abstract String toString();
}
