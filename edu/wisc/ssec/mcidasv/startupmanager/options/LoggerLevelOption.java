package edu.wisc.ssec.mcidasv.startupmanager.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.OptionPlatform;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Type;
import edu.wisc.ssec.mcidasv.startupmanager.options.OptionMaster.Visibility;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils;

public class LoggerLevelOption extends AbstractOption {

    private static final String TRACE = "TRACE";
    
    private static final String DEBUG = "DEBUG";
    
    private static final String INFO = "INFO";
    
    private static final String WARN = "WARN";
    
    private static final String ERROR = "ERROR";
    
    private static final String OFF = "OFF";
    
    private JComboBox comboBox;
    
    private String currentChoice;
    
    public LoggerLevelOption(String id, String label, String defaultValue,
            OptionPlatform optionPlatform, Visibility optionVisibility) {
        super(id, label, Type.LOGLEVEL, optionPlatform, optionVisibility);
        if (!isValidValue(defaultValue)) {
            throw new IllegalArgumentException("Default value '"+defaultValue+"' is not one of: TRACE, DEBUG, INFO, WARN, ERROR, or OFF.");
        }
        currentChoice = defaultValue;
    }
    
    public JComboBox getComponent() {
        comboBox = new JComboBox(new String[] { TRACE, DEBUG, INFO, WARN, ERROR, OFF });
        comboBox.setSelectedItem(currentChoice);
        comboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                setValue(comboBox.getSelectedItem().toString());
            }
        });
        
        McVGuiUtils.setComponentWidth(comboBox, McVGuiUtils.Width.ONEHALF);
        return comboBox;
    }
    
    public String getValue() {
        return currentChoice;
    }
    
    public void setValue(String value) {
        if (!isValidValue(value)) {
            throw new IllegalArgumentException("Value '"+value+"' is not one of: TRACE, DEBUG, INFO, WARN, ERROR, or OFF.");
        }
        currentChoice = value;
        Logger rootLogger = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(stringToLogback(value));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (comboBox != null) {
                    comboBox.setSelectedItem(currentChoice);
                }
            }
        });
    }
    
    private static Level stringToLogback(final String value) {
        Level level;
        if (TRACE.equalsIgnoreCase(value)) {
            level = Level.TRACE;
        } else if (DEBUG.equalsIgnoreCase(value)) {
            level = Level.DEBUG;
        } else if (INFO.equalsIgnoreCase(value)) {
            level = Level.INFO;
        } else if (WARN.equalsIgnoreCase(value)) {
            level = Level.WARN;
        } else if (ERROR.equalsIgnoreCase(value)) {
            level = Level.ERROR;
        } else if (OFF.equalsIgnoreCase(value)) {
            level = Level.OFF;
        } else {
            throw new IllegalArgumentException();
        }
        return level;
    }
    
    private static boolean isValidValue(final String value) {
        return (TRACE.equalsIgnoreCase(value) || 
                DEBUG.equalsIgnoreCase(value) || 
                INFO.equalsIgnoreCase(value) || 
                WARN.equalsIgnoreCase(value) || 
                ERROR.equalsIgnoreCase(value) || 
                OFF.equalsIgnoreCase(value));
    }
    
    public String toString() {
        return String.format("[LoggerLevelOption@%x: currentChoice=%s",
                hashCode(), currentChoice);
    }
}
