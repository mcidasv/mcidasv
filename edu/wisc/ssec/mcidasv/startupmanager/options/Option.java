package edu.wisc.ssec.mcidasv.startupmanager.options;

import javax.swing.JComponent;

import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public interface Option {

    public void fromPrefsFormat(final String text);

    public JComponent getComponent();

    public String getLabel();

    public String getOptionId();

    public OptionPlatform getOptionPlatform();

    public Type getOptionType();

    public Visibility getOptionVisibility();

    public String getValue();

    public void setValue(final String newValue);

    public String toPrefsFormat();

    public String toString();
}
