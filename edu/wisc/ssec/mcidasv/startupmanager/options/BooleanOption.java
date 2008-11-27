package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

public class BooleanOption extends AbstractOption {
    private String value = "0";

    public BooleanOption(final String id, final String label, 
        final String defaultValue, 
        final OptionMaster.OptionPlatform optionPlatform,
        final OptionMaster.Visibility optionVisibility) 
    {
        super(id, label, OptionMaster.Type.BOOLEAN, optionPlatform, optionVisibility);
        setValue(defaultValue);
    }

    public JComponent getComponent() {
        final JCheckBox cb = new JCheckBox();
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                setValue(cb.isSelected() ? "1" : "0");
            }
        });
        boolean booleanValue = false;
        if (value.equals("1"))
            booleanValue = true;
        cb.setSelected(booleanValue);
        if (!onValidPlatform()) {
            cb.setEnabled(false);
        }
        return cb;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String newValue) {
        if (newValue.equals("0") || newValue.equals("1"))
            value = newValue;
    }

    public String toString() {
        return String.format("[BooleanOption@%x: optionId=%s, value=%s]", 
            hashCode(), getOptionId(), getValue());
    }
}
