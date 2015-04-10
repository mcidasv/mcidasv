package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.io.File;

import java.util.regex.Pattern;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
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
        String bundleDir =
            StartupManager.getInstance().getPlatform().getUserBundles();
        setValue(selectBundle(bundleDir));
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
            @Override
            public void actionPerformed(ActionEvent e) {
                browseButtonActionPerformed(e);
            }
        });
        return McVGuiUtils.sideBySide(bundleField, browseButton);
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
        return (value == null) ? defaultValue : '"' + value + '"';
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
        return value;
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
