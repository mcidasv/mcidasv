package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTextField;

import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class TextOption extends AbstractOption {
    private String value = "";

    public TextOption(final String id, final String label, 
        final String defaultValue, final OptionPlatform optionPlatform,
        final Visibility optionVisibility) 
    {
        super(id, label, Type.TEXT, optionPlatform, optionVisibility);
        setValue(defaultValue);
    }

    public JComponent getComponent() {
        final JTextField tf = new JTextField(getValue(), 10);
        tf.addKeyListener(new KeyAdapter() {
            public void keyReleased(final KeyEvent e) {
                setValue(tf.getText());
            }
        });
        if (!onValidPlatform()) {
            tf.setEnabled(false);
        }
        return tf;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String newValue) {
        value = newValue;
    }

    public String toString() {
        return String.format("[TextOption@%x: optionId=%s, value=%s]", 
            hashCode(), getOptionId(), getValue());
    }
}
