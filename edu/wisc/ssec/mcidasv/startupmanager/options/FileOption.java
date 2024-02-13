/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.sideBySide;
import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.topBottom;

import java.io.File;

import java.nio.file.Paths;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.util.MakeToString;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

/**
 * Represents a file selection.
 */
public final class FileOption extends AbstractOption {

    /** Label for {@link #browseButton}. */
    private static final String BUTTON_LABEL = "Browse...";

    /** Label for {@link #enableCheckBox}. */
    private static final String CHECKBOX_LABEL = "Specify default bundle:";

    /** System property that points to the McIDAS-V user path. */
    private static final String USERPATH = "mcv.userpath";

    /** Name of the {@literal "bundle"} subdirectory of the user path. */
    private static final String BUNDLE_DIR = "bundles";

    /** Constant that represents string version of the {@code 1} boolean. */
    private static final String TRUE_STRING = "1";

    /** Constant that represents string version of the {@code 0} boolean. */
    private static final String FALSE_STRING = "0";
    
    /** Used to ensure that no quote marks are present. */
    private static final String QUOTE = "\"";
    
    /** Tool tip used by {@link #bundleField}. */
    public static final String BUNDLE_FIELD_TIP =
        "Path to default bundle. An empty path signifies that there is no"
        + " default bundle in use.";

    /** Default state of {@link #enableCheckBox}. */
    private final boolean defaultCheckBox;

    /** Default path for {@link #bundleField}. */
    private final String defaultBundle;

    /**
     * Shows current default bundle. Empty means there isn't one. May be
     * {@code null}!
     */
    private JTextField bundleField;

    /** Used to pop up a {@link JFileChooser}. May be {@code null}! */
    private JButton browseButton;

    /**
     * Whether or not the default bundle should be used. May be {@code null}!
     */
    private JCheckBox enableCheckBox;

    /** Current state of {@link #enableCheckBox}. */
    private boolean checkbox;

    /** Current contents of {@link #bundleField}. Value may be {@code null}! */
    private String path;

    /**
     * Create a new {@literal "file option"} that allows the user to select
     * a file.
     *
     * @param id Option ID.
     * @param label Option label (used in GUI).
     * @param defaultValue Default option value.
     * @param platform Platform restrictions for the option.
     * @param visibility Visibility restrictions for the option.
     */
    public FileOption(
        final String id,
        final String label,
        final String defaultValue,
        final OptionMaster.OptionPlatform platform,
        final OptionMaster.Visibility visibility)
    {
        super(id, label, OptionMaster.Type.DIRTREE, platform, visibility);
        String[] defaults = parseFormat(defaultValue);
        this.defaultCheckBox = booleanFromFormat(defaults[0]);
        this.defaultBundle = defaults[1];
        setValue(defaultValue);
    }

    /**
     * Handles the user clicking on the {@link #browseButton}.
     *
     * @param event Currently ignored.
     */
    private void browseButtonActionPerformed(ActionEvent event) {
        String defaultPath =
            StartupManager.getInstance().getPlatform().getUserBundles();
        String userPath = System.getProperty(USERPATH, defaultPath);
        String bundlePath = Paths.get(userPath, BUNDLE_DIR).toString();
        setValue(selectBundle(bundlePath));
    }

    /**
     * Show a {@code JFileChooser} dialog that allows the user to select a
     * bundle.
     *
     * @param bundleDirectory Initial directory for the {@code JFileChooser}.
     *
     * @return Either the path to the user's chosen bundle, or
     * {@link #defaultValue} if the user cancelled.
     */
    private String selectBundle(final String bundleDirectory) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        if ((path != null) && !path.isEmpty()) {
            fileChooser.setSelectedFile(new File(path));
        } else {
            fileChooser.setCurrentDirectory(new File(bundleDirectory));
        }
        String result;
        switch (fileChooser.showOpenDialog(null)) {
            case JFileChooser.APPROVE_OPTION:
                result = fileChooser.getSelectedFile().getAbsolutePath();
                break;
            default:
                result = path;
                break;
        }
        return '"' + getCheckBoxValue() + ';' + result + '"';
    }

    /**
     * Returns the GUI component that represents the option.
     *
     * @return GUI representation of this option.
     */
    @Override public JComponent getComponent() {
        bundleField = new JTextField(path);
        bundleField.setColumns(30);
        bundleField.setToolTipText(BUNDLE_FIELD_TIP);
        bundleField.setEnabled(checkbox);

        browseButton = new JButton(BUTTON_LABEL);
        browseButton.setEnabled(checkbox);
        browseButton.addActionListener(this::browseButtonActionPerformed);

        enableCheckBox = new JCheckBox(CHECKBOX_LABEL, checkbox);
        enableCheckBox.addActionListener(e -> {
            boolean status = enableCheckBox.isSelected();
            bundleField.setEnabled(status);
            browseButton.setEnabled(status);
        });
        JPanel bottom = sideBySide(bundleField, browseButton);
        return topBottom(enableCheckBox, bottom, McVGuiUtils.Prefer.NEITHER);
    }

    /**
     * Returns a string containing the state of {@link #enableCheckBox} and
     * {@link #bundleField}.
     *
     * <p>Results should look like {@code 0;/path/to/bundle.mcv}.</p>
     *
     * @return Current value of the option.
     */
    @Override public String getValue() {
        return '"' + getCheckBoxValue() + ';' + getBundlePath() + '"';
    }

    /**
     * Returns a string representation of {@link #enableCheckBox}.
     *
     * @return Either {@code 1} or {@code 0} depending upon the state of
     * {@link #enableCheckBox}.
     */
    public String getCheckBoxValue() {
        boolean status = defaultCheckBox;
        if (enableCheckBox != null) {
            status = enableCheckBox.isSelected();
        }
        return status ? TRUE_STRING : FALSE_STRING;
    }

    /**
     * Returns a string representating the path to the startup bundle.
     *
     * @return If {@link #bundleField} is {@code null}, {@link #defaultBundle}
     * is returned. Otherwise the contents of the text field are returned.
     */
    public String getBundlePath() {
        String result = defaultBundle;
        if (bundleField != null) {
            result = bundleField.getText();
        }
        return result;
    }

    /**
     * Forces the value of the option to the data specified.
     *
     * @param newValue New value to use.
     */
    @Override public void setValue(final String newValue) {
        String[] results = parseFormat(newValue);
        checkbox = booleanFromFormat(results[0]);
        path = results[1];
        SwingUtilities.invokeLater(() -> {
            String[] results1 = parseFormat(newValue);
            checkbox = booleanFromFormat(results1[0]);
            path = results1[1];
            if (enableCheckBox != null) {
                enableCheckBox.setSelected(checkbox);
            }

            // defaultValue check is to avoid blanking out the field
            // when the user hits cancel
            if ((bundleField != null) && !defaultBundle.equals(path)) {
                bundleField.setEnabled(checkbox);
                bundleField.setText(path);
            }

            if (browseButton != null) {
                browseButton.setEnabled(checkbox);
            }
        });
    }

    /**
     * Friendly string representation of the option.
     *
     * @return {@code String} containing relevant info about the option.
     */
    @Override public String toString() {
        return MakeToString.fromInstance(this)
                           .add("optionId", getOptionId())
                           .add("value", getValue()).toString();
    }

    /**
     * Attempt to extract something sensible from the value given in
     * {@literal "runMcV-Prefs"}.
     *
     * <p>Expected format is something like {@code "0;/path/to/bundle.mcv"} or
     * {@code "1;"}. The first example would signify that
     * {@link #enableCheckBox} is not selected, and the contents of
     * {@link #bundleField} are {@code /path/to/bundle.mcv}. The second
     * example would signify that {@link #enableCheckBox} is selected, and the
     * contents of {@link #bundleField} should be an empty string.</p>
     *
     * @param format See method description for details. {@code null} not
     * allowed.
     *
     * @return Two element array where the first element is the state of
     * {@link #enableCheckBox} and the second is the bundle path. Note that
     * the bundle path may be empty.
     *
     * @see #booleanFromFormat(String)
     */
    public static String[] parseFormat(String format) {
        if (format.startsWith(QUOTE) || format.endsWith(QUOTE)) {
            format = format.replace(QUOTE, "");
        }
        String checkBox = TRUE_STRING;
        String path;
        int splitAt = format.indexOf(';');
        if (splitAt == -1) {
            // string was something like "/path/goes/here.mcv"
            path = format;
        } else if (splitAt == 0) {
            // string was something like ";/path/goes/here.mcv"
            path = format.substring(1);
        } else {
            // string was something like "1;/path/goes/here.mcv"
            checkBox = format.substring(0, splitAt);
            path = format.substring(splitAt + 1);
        }
        if (path.isEmpty()) {
            checkBox = FALSE_STRING;
        }
        return new String[] { checkBox, path };
    }

    /**
     * Convert the strings {@code 1} and {@code 0} to their corresponding
     * boolean values.
     *
     * @param value String to convert. {@code null} or empty strings accepted.
     *
     * @return Returns {@code true} if {@code value} is {@code 1}. Otherwise
     * returns {@code false}.
     */
    public static boolean booleanFromFormat(String value) {
        boolean result = false;
        if (TRUE_STRING.equals(value)) {
            result = true;
        }
        return result;
    }
}
