/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2015
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

import java.io.File;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
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

    /**
     * Regular expression pattern for ensuring that no quote marks are present
     * in {@link #value}.
     */
    private static final Pattern CLEAN_VALUE_REGEX = Pattern.compile("\"");

    /** Formatting string used by {@link #toString()}. */
    private static final String FORMAT =
        "[FileOption@%x: optionId=%s, value=%s]";

    /** Tool tip used by {@link #bundleField}. */
    public static final String BUNDLE_FIELD_TIP =
        "Path to default bundle. An empty path signifies that there is no"
        + " default bundle in use.";

    /** Default option value. See {@link OptionMaster#blahblah}. */
    private final String defaultValue;

    /** Shows current default bundle. Empty means there isn't one. */
    private JTextField bundleField;

    /** Used to pop up a {@link JFileChooser}. */
    private JButton browseButton;

    /** Whether or not the default bundle should be used. */
    private JCheckBox enableCheckBox;

    /** Current value of this option. */
    private String value;

    /**
     * Create a new {@literal "file option"} that allows the user to select
     * a file.
     *
     * @param id Option ID.
     * @param label Option label (used in GUI).
     * @param defaultValue Default option value.
     * @param optionPlatform Platform restrictions for the option.
     * @param optionVisibility Visibility restrictions for the option.
     */
    public FileOption(
        final String id,
        final String label,
        final String defaultValue,
        final OptionMaster.OptionPlatform optionPlatform,
        final OptionMaster.Visibility optionVisibility)
    {
        super(id, label, OptionMaster.Type.DIRTREE, optionPlatform, optionVisibility);
        this.defaultValue = defaultValue;
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
        if ((value != null) && !value.isEmpty()) {
            fileChooser.setSelectedFile(new File(value));
        } else {
            fileChooser.setCurrentDirectory(new File(bundleDirectory));
        }
        String result = defaultValue;
        switch (fileChooser.showOpenDialog(null)) {
            case JFileChooser.APPROVE_OPTION:
                result = fileChooser.getSelectedFile().getAbsolutePath();
                break;
        }
        return result;
    }

    /**
     * Returns the GUI component that represents the option.
     *
     * @return GUI representation of this option.
     */
    @Override public JComponent getComponent() {
        bundleField = new JTextField(value);
        bundleField.setColumns(30);
        bundleField.setToolTipText(BUNDLE_FIELD_TIP);

        browseButton = new JButton(BUTTON_LABEL);
        browseButton.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                browseButtonActionPerformed(e);
            }
        });

        enableCheckBox = new JCheckBox(CHECKBOX_LABEL, true);
        enableCheckBox.addActionListener(new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                boolean status = enableCheckBox.isSelected();
                bundleField.setEnabled(status);
                browseButton.setEnabled(status);
            }
        });
        JPanel bottom = McVGuiUtils.sideBySide(bundleField, browseButton);
        return McVGuiUtils.topBottom(enableCheckBox, bottom, McVGuiUtils.Prefer.NEITHER);
    }

    /**
     * Returns the value of the option. <b>THE RESULT WILL HAVE QUOTE MARKS
     * AT THE BEGINNING AND END</b>. This was done to make handling paths with
     * spaces a bit more safe.
     *
     * <p>If you need the unquoted value, please try
     * {@link #getUnquotedValue()} instead.</p>
     *
     * @return The current value of the option.
     */
    @Override public String getValue() {
        return '"' + getUnquotedValue() + '"';
    }

    /**
     * Forces the value of the option to the data specified.
     *
     * @param newValue New value to use.
     */
    @Override public void setValue(final String newValue) {
        value = CLEAN_VALUE_REGEX.matcher(newValue).replaceAll("");
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                // defaultValue check is to avoid blanking out the field
                // when the user hits cancel
                if ((bundleField != null) && !defaultValue.equals(value)) {
                    bundleField.setText(value);
                }
            }
        });
    }

    /**
     * Returns {@link #value} without quotation marks. Since the values being
     * returned will be paths, you may want to consider using
     * {@link #getValue()} instead.
     *
     * @return This option's current {@literal "value"}. May be {@code null}
     * or empty.
     */
    public String getUnquotedValue() {
        String result = defaultValue;
        if ((enableCheckBox != null) && enableCheckBox.isSelected()) {
            result = value;
        }
        return result;
    }

    /**
     * Friendly string representation of the option.
     *
     * @return {@code String} containing relevant info about the option.
     */
    @Override public String toString() {
        return String.format(FORMAT, hashCode(), getOptionId(), getValue());
    }
}
