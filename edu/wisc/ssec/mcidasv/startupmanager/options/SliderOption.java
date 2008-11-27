package edu.wisc.ssec.mcidasv.startupmanager.options;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ucar.unidata.util.GuiUtils;

import edu.wisc.ssec.mcidasv.startupmanager.StartupManager;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;

public class SliderOption extends AbstractOption {
    private final int minValue = 10;
    private final int maxValue = 80;
    private final int total;

    private int sliderValue = minValue;

    public SliderOption(final String id, final String label, 
            final String defaultValue, final OptionPlatform optionPlatform, 
            Visibility optionVisibility) 
    {
        super(id, label, Type.SLIDER, optionPlatform, optionVisibility);
        total = StartupManager.INSTANCE.getPlatform().getAvailableMemory();
        setValue(defaultValue);
    }

    private ChangeListener makeChangeListener(final JLabel sliderLabel) {
        return new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                JSlider src = (JSlider)evt.getSource();
                int value = src.getValue();
                sliderLabel.setText("Use "+value+"% ");
                setValue(Integer.toString(value));
            }
        };
    }

    public JComponent getComponent() {
        JLabel sliderLabel = new JLabel("Use "+sliderValue+"% ");
        JLabel postLabel = new JLabel(" of available memory ("+total+"mb)");

        ChangeListener listener = makeChangeListener(sliderLabel);

        JComponent[] sliderComps = GuiUtils.makeSliderPopup(minValue, maxValue+1, sliderValue, listener);
        JSlider slider = (JSlider)sliderComps[1];
        slider.setMinorTickSpacing(5);
        slider.setMajorTickSpacing(10);
        slider.setSnapToTicks(true);
        slider.setExtent(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        sliderComps[0].setToolTipText("Set maximum memory by percent");

        JPanel sliderPanel = GuiUtils.hbox(sliderLabel, sliderComps[0], postLabel);
        return sliderPanel;
    }

    public String getValue() {
        return Integer.toString(sliderValue)+"P";
    }

    public void setValue(final String newValue) {
        int endIndex = newValue.length();
        if (newValue.endsWith("P"))
            endIndex = endIndex - 1;
        int test = Integer.parseInt(newValue.substring(0, endIndex));
        if (test >= 0)
            sliderValue = test;
    }

    public String toString() {
        return String.format("[SliderOption@%x: optionId=%s, value=%s]", hashCode(), getOptionId(), getValue());
    }

}
