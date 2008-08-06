package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import org.python.core.PyJavaInstance;
import org.python.core.PyObject;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;
import ucar.visad.display.DisplayMaster;
import visad.Data;
import visad.VisADException;
import visad.georef.MapProjection;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Combination;
import edu.wisc.ssec.mcidasv.control.LinearCombo.Selector;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;
import edu.wisc.ssec.mcidasv.jython.Runner;

public class HydraCombo extends HydraControl {

    private static final String PARAM = "BrightnessTemp";

    private String sourceFile = "";

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private CombinationPanel comboPanel;

    private static final int DEFAULT_FLAGS =
        FLAG_COLORTABLE | FLAG_SELECTRANGE | FLAG_ZPOSITION;

    private ComboDataChoice comboChoice;
    
    public HydraCombo() {
        super();
    }

    @Override public boolean init(final DataChoice choice)
        throws VisADException, RemoteException {
        
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        sourceFile = ((MultiSpectralDataSource)sources.get(0)).getDatasetName();
        comboChoice = ((MultiSpectralDataSource)sources.get(0)).getComboDataChoice();

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = MultiSpectralData.init_wavenumber;

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);

        addDisplayable(display.getImageDisplay(), DEFAULT_FLAGS);

        addViewManager(display.getViewManager());

        setAttributeFlags(DEFAULT_FLAGS);

        comboPanel = new CombinationPanel(this);
        return true;
    }

    @Override public void initDone() {
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        Rectangle2D rect = MultiSpectralData.getLonLatBoundingBox(display.getImageData());
        try {
            mp = new LambertAEA(rect);
        } catch (Exception e) {
            logException("LinearCombo.getDataProjection", e);
        }
        return mp;
    }

    @Override protected Range getInitialRange() throws VisADException, RemoteException {
        return getDisplayConventions().getParamRange(PARAM, null);
    }

    @Override protected ColorTable getInitialColorTable() {
        return getDisplayConventions().getParamColorTable(PARAM);
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Channel Combination Tool", GuiUtils.inset(getComboTab(), 5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);
        return pane;
    }

    public void updateComboPanel(final String id, final float value) {
        if (comboPanel == null)
            return;
        comboPanel.updateSelector(id, value);
    }

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    private JComponent getComboTab() {
        JButton button = new JButton("MAKE COMPUTER GO NOW");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                comboPanel.queueCombination();
            }
        });
        JPanel tmp = GuiUtils.topCenterBottom(display.getDisplayComponent(), comboPanel.getPanel(), button);
        return tmp;
    }

    public void setComboChoice(final Data combination) {
        if (combination != null && comboChoice != null)
            comboChoice.setData(combination);
    }

    public static class CombinationPanel implements ConsoleCallback {
        private final SelectorWrapper a;
        private final SelectorWrapper b;
        private final SelectorWrapper c;
        private final SelectorWrapper d;

        private final OperationXY ab;
        private final OperationXY cd;

        private final CombineOperations abcd;

        private final MultiSpectralDisplay display;

        private final HydraCombo control;

        private final Console console;

        private final Map<String, Selector> selectorMap = new HashMap<String, Selector>();
        private final Map<String, SelectorWrapper> wrapperMap = new HashMap<String, SelectorWrapper>();

        public CombinationPanel(final HydraCombo control) {
            this.control = control;
            display = control.getMultiSpectralDisplay();
            if (display == null)
                throw new NullPointerException("Display hasn't been initialized");

            this.console = new Console();
            console.setCallbackHandler(this);

            a = makeWrapper("a", Color.RED);
            b = makeWrapper("b", Color.GREEN);
            c = makeWrapper("c", Color.BLUE);
            d = makeWrapper("d", Color.MAGENTA);

            enableSelector(a);

            ab = new OperationXY(this, a, b);
            cd = new OperationXY(this, c, d);

            abcd = new CombineOperations(this, ab, cd);
        }

        public void ranBlock(final String line) {
            PyObject jythonObj = console.getJythonObject("combo");
            if (jythonObj instanceof PyJavaInstance) {
                Object combination = jythonObj.__tojava__(Object.class);
                if (combination instanceof Combination) {
                    ((HydraCombo)control).setComboChoice(((Combination)combination).getData());
                } 
            }
        }

        public void updateSelector(final String id, final float channel) {
            if (!selectorMap.containsKey(id))
                return;

            Selector selector = selectorMap.get(id);
            selector.setWaveNumber(channel);
            wrapperMap.get(id).update();
        }

        protected void addSelector(final Selector selector, final boolean enabled) throws Exception {
            String id = selector.getId();
            if (enabled) {
                display.createSelector(id, selector.getColor());
                display.setSelectorValue(id, selector.getWaveNumber());
            }
            selectorMap.put(id, selector);
        }

        protected void disableSelector(final SelectorWrapper wrapper) {
            wrapper.disable();
            try {
                display.removeSelector(wrapper.getSelector().getId());
            } catch (Exception e) {
                LogUtil.logException("HydraCombo.disableSelector", e);
            }
        }

        protected void enableSelector(final SelectorWrapper wrapper) {
            wrapper.enable();
            try {
                Selector selector = wrapper.getSelector();
                String id = selector.getId();
                display.createSelector(id, selector.getColor());
                display.setSelectorValue(id, selector.getWaveNumber());
            } catch (Exception e) {
                LogUtil.logException("HydraCombo.disableSelector", e);
            }
        }

        private SelectorWrapper makeWrapper(final String var, final Color color) {
            SelectorWrapper tmp = new SelectorWrapper(var, color, control, console);
            try {
                addSelector(tmp.getSelector(), false);
                wrapperMap.put(tmp.getSelector().getId(), tmp);
                console.injectObject(var, new PyJavaInstance(tmp.getSelector()));
                return tmp;
            } catch (Exception e) { 
                LogUtil.logException("HydraCombo.makeWrapper", e);
            }
            return null;
        }

        public JPanel getPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.add(new JLabel("("));
            panel.add(a.getPanel());
            panel.add(ab.getBox());
            panel.add(b.getPanel());
            panel.add(new JLabel(")"));
            panel.add(abcd.getBox());
            panel.add(new JLabel("("));
            panel.add(c.getPanel());
            panel.add(cd.getBox());
            panel.add(d.getPanel());
            panel.add(new JLabel(")"));
            return panel;
        }

        public void queueCombination() {
            String jy = abcd.getJython();
            String statement = "exec 'combo=" + jy + "'";
            System.err.println("jython=" + statement);
            console.queueLine(statement);
        }
    }

    private static class OperationXY {
        private static final String[] OPERATIONS = { "+", "-", "/", "*", " "};
        private static final String INVALID_OP = " ";
        private final JComboBox combo = new JComboBox(OPERATIONS);

        private CombinationPanel comboPanel;
        private SelectorWrapper x;
        private SelectorWrapper y;

        public OperationXY(final CombinationPanel comboPanel, final SelectorWrapper x, final SelectorWrapper y) {
            this.x = x;
            this.y = y;
            this.comboPanel = comboPanel;
            combo.setSelectedItem(" ");
            combo.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (getOperation().equals(" ")) {
                        comboPanel.disableSelector(y);
                    } else {
                        comboPanel.enableSelector(y);
                    }
                }
            });
        }

        public void disable() {
            comboPanel.disableSelector(x);
            combo.setSelectedItem(INVALID_OP);
            comboPanel.disableSelector(y);
        }

        public void enable() {
            comboPanel.enableSelector(x);
        }

        public String getOperation() {
            return (String)combo.getSelectedItem();
        }

        public void setOperation(final String operation) {
            combo.setSelectedItem(operation);
        }

        public JComboBox getBox() {
            return combo;
        }

        public String getJython() {
            String operation = getOperation();
            if (operation.equals(INVALID_OP))
                operation = "";

            String jython = x.getJython() + operation + y.getJython();
            if (x.isValid() && y.isValid())
                return "(" + jython + ")";
            return jython;
        }
    }

    private static class CombineOperations {
        private static final String[] OPERATIONS = { "+", "-", "/", "*", " "};
        private static final String INVALID_OP = " ";
        private final JComboBox combo = new JComboBox(OPERATIONS);

        private CombinationPanel comboPanel;
        private OperationXY x;
        private OperationXY y;

        public CombineOperations(final CombinationPanel comboPanel, final OperationXY x, final OperationXY y) {
            this.comboPanel = comboPanel;
            this.x = x;
            this.y = y;
            combo.setSelectedItem(INVALID_OP);
            combo.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    if (getOperation().equals(" ")) {
                        y.disable();
                    } else {
                        y.enable();
                    }
                }
            });
        }

        public String getOperation() {
            return (String)combo.getSelectedItem();
        }

        public void setOperation(final String operation) {
            combo.setSelectedItem(operation);
        }

        public JComboBox getBox() {
            return combo;
        }

        public String getJython() {
            String operation = getOperation();
            if (operation.equals(INVALID_OP))
                operation = "";

            String jython = x.getJython() + operation + y.getJython();
            return jython;
        }
    }

    private static class SelectorWrapper {
        private static final String BLANK = "--------";
        private final String variable;
        private final Color color;
        private final Selector selector;
        private final JTextField scale = new JTextField(String.format("%3.1f", 1.0), 4);
        private final JTextField channel;

        public SelectorWrapper(final String variable, final Color color, final HydraCombo control, final Console console) {
            this.variable = variable;
            this.color = color;
            this.selector = new Selector(MultiSpectralData.init_wavenumber, color, control, console);
            channel = new JTextField(BLANK);
            channel.setBorder(new LineBorder(color, 2));
        }

        public Selector getSelector() {
            return selector;
        }

        public JPanel getPanel() {
            JPanel panel = new JPanel(new FlowLayout());
            panel.add(scale);
            panel.add(channel);
            return panel;
        }

        public String getJython() {
            if (!isValid())
                return "";
            return "(" + scale.getText() + "*" + variable + ")";
        }

        public boolean isValid() {
            return !channel.getText().equals(BLANK);
        }

        public void enable() {
            String fmt = String.format("%7.3f", selector.getWaveNumber());
            channel.setText(fmt);
        }

        public void disable() {
            channel.setText(BLANK);
        }

        public void update() {
            String fmt = String.format("%7.3f", selector.getWaveNumber());
            channel.setText(fmt);
        }
    }
}