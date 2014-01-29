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

package edu.wisc.ssec.mcidasv.control;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import visad.ConstantMap;
import visad.Data;
import visad.Real;
import visad.VisADException;
import visad.georef.MapProjection;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.visad.display.DisplayMaster;

import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.McIDASV;
import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import edu.wisc.ssec.mcidasv.data.hydra.MultiDimensionSubset;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralData;
import edu.wisc.ssec.mcidasv.data.hydra.MultiSpectralDataSource;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay;
import edu.wisc.ssec.mcidasv.display.hydra.MultiSpectralDisplay.DragLine;
import edu.wisc.ssec.mcidasv.jython.Console;
import edu.wisc.ssec.mcidasv.jython.ConsoleCallback;

public class LinearCombo extends HydraControl implements ConsoleCallback {

    /** Trusty logging object. */
    private static final Logger logger = LoggerFactory.getLogger(LinearCombo.class);

    /** Help topic identifier. */
    public static final String HYDRA_HELP_ID = 
        "idv.controls.hydra.linearcombinationcontrol";

    /** 
     * Path to the Jython source code that allows for interaction with a 
     * linear combination display control.
     */
    public static final String HYDRA_SRC = 
        "/edu/wisc/ssec/mcidasv/resources/python/linearcombo/hydra.py";

    /** Name used in Jython namespace to refer to the {@literal "IDV god object"}. */
    public static final String CONSOLE_IDV_OBJECT = "idv";

    /** 
     * Name used in Jython namespace to refer back to an instantiation of a 
     * linear combination control.
     */
    public static final String CONSOLE_CONTROL_OBJECT = "_linearCombo";

    public static final String CONSOLE_OBJECT = "_jythonConsole";

    public static final String CONSOLE_DATA_OBJECT = "_data";

    private Console console;

    private MultiSpectralDisplay display;

    private DisplayMaster displayMaster;

    private String sourceFile = "";

    private ComboDataChoice comboChoice;

    private MultiSpectralDataSource source;

    private List<String> jythonHistory;

    private Map<String, Selector> selectorMap;

    private Map<String, Selector> jythonMap;

    private DataChoice dataChoice = null;

    /**
     * 
     */
    public LinearCombo() {
        super();
        setHelpUrl(HYDRA_HELP_ID);
        jythonHistory = new ArrayList<String>();
        selectorMap = new HashMap<String, Selector>();
        jythonMap = new HashMap<String, Selector>();
    }

    @Override public boolean init(final DataChoice choice) throws VisADException, RemoteException {
        List<DataSource> sources = new ArrayList<DataSource>();
        choice.getDataSources(sources);
        dataChoice = choice;

        ((McIDASV)getIdv()).getMcvDataManager().setHydraControl(choice, this);

        source = ((MultiSpectralDataSource)sources.get(0));
        sourceFile = source.getDatasetName();

        MultiSpectralData data = source.getMultiSpectralData(choice);

        Float fieldSelectorChannel = (Float)getDataSelection().getProperty(Constants.PROP_CHAN);
        if (fieldSelectorChannel == null)
            fieldSelectorChannel = data.init_wavenumber;

        console = new Console();
        console.setCallbackHandler(this);

        console.injectObject(CONSOLE_IDV_OBJECT, getIdv());
        console.injectObject(CONSOLE_CONTROL_OBJECT, this);
        console.injectObject(CONSOLE_OBJECT, console);
        console.injectObject(CONSOLE_DATA_OBJECT, source.getMultiSpectralData(choice));

        console.runFile("__main__", "/edu/wisc/ssec/mcidasv/resources/python/console_init.py");
        console.runFile("__main__", HYDRA_SRC);

        display = new MultiSpectralDisplay((DirectDataChoice)choice);
        display.setWaveNumber(fieldSelectorChannel);
        display.setDisplayControl(this);
        ((McIDASV)getIdv()).getMcvDataManager().setHydraDisplay(choice, display);
        return true;
    }

    @Override public void initDone() {
        MapViewManager viewManager = (MapViewManager)getViewManager();
        MapProjectionDisplay dispMaster = 
            (MapProjectionDisplay)viewManager.getMaster();
        
        try {
            dispMaster.setMapProjection(getDataProjection());
        } catch (Exception e) {
            logException("problem setting MapProjection", e);
        }

        getIdv().getIdvUIManager().showDashboard();
        console.queueBatch("history", jythonHistory);
        jythonHistory.clear();
    }

    public List<String> getJythonHistory() {
        return console.getHistory();
    }

    public void setJythonHistory(final List<String> persistedHistory) {
        jythonHistory = persistedHistory;
    }

    @Override public MapProjection getDataProjection() {
        MapProjection mp = null;
        HashMap subset = null;
        Hashtable table = dataChoice.getProperties();
        MultiDimensionSubset dataSel =
           (MultiDimensionSubset)table.get(MultiDimensionSubset.key);
        
        if (dataSel != null) {
            subset = dataSel.getSubset();
        }
        mp = source.getDataProjection(subset);
        return mp;
    }

    @Override public Container doMakeContents() {
        JTabbedPane pane = new JTabbedPane();
        pane.add("Console", GuiUtils.inset(getConsoleTab(), 5));
        GuiUtils.handleHeavyWeightComponentsInTabs(pane);
        return pane;
    }

    private JComponent getConsoleTab() {
        JPanel consolePanel = console.getPanel();
        consolePanel.setPreferredSize(new Dimension(500, 150));
        return GuiUtils.topCenter(display.getDisplayComponent(), consolePanel);
    }

    @Override public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
    }

    @Override public String toString() {
        return "[LinearCombo@" + Integer.toHexString(hashCode()) + 
            ": sourceFile=" + sourceFile + ']';
    }

    public void moveSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id)) {
            return;
        }
        display.updateControlSelector(id, wavenum);
    }

    public void updateSelector(final String id, final float wavenum) {
        if (!selectorMap.containsKey(id)) {
            return;
        }

        selectorMap.get(id).setWaveNumber(wavenum);
        String cmd = new StringBuilder("_linearCombo.moveSelector('")
            .append(id)
            .append("', ")
            .append(wavenum)
            .append(')')
            .toString();

        console.addPretendHistory(cmd);
    }

    protected void addSelector(final Selector selector) throws Exception {
        ConstantMap[] mapping = selector.getColor();
        float r = Double.valueOf(mapping[0].getConstant()).floatValue();
        float g = Double.valueOf(mapping[1].getConstant()).floatValue();
        float b = Double.valueOf(mapping[2].getConstant()).floatValue();
        Color javaColor = new Color(r, g, b);
        display.createSelector(selector.getId(), javaColor);
        display.setSelectorValue(selector.getId(), selector.getWaveNumber());
        selectorMap.put(selector.getId(), selector);
        logger.trace("added selector={}", selector);
    }

    protected MultiSpectralDisplay getMultiSpectralDisplay() {
        return display;
    }

    protected int getSelectorCount() {
        return selectorMap.size();
    }

    private Set<String> getSelectorIds(final Map<String, Object> objMap) {
        assert objMap != null : objMap;

        Set<String> ids = new HashSet<String>();
        Collection<Object> jython = objMap.values();

        for (Iterator<Object> i = jython.iterator(); i.hasNext();) {
            Object obj = i.next();
            if (!(obj instanceof Selector)) {
                continue;
            }

            String selectorId = ((Selector)obj).getId();
            ids.add(selectorId);
        }

        return ids;
    }

    /**
     * Return a mapping of names to their {@link edu.wisc.ssec.mcidasv.control.LinearCombo.Selector Selectors}.
     * 
     * @param objMap {@code Map} of objects.
     * 
     * @return Map of name to {@code Selector}.
     */
    private Map<String, Selector> mapNamesToThings(final Map<String, Object> objMap) {
        assert objMap != null : objMap;

        Map<String, Selector> nameMap = new HashMap<String, Selector>(objMap.size());
        Set<Selector> seen = new LinkedHashSet<Selector>();
        for (Map.Entry<String, Object> entry : objMap.entrySet()) {
            Object obj = entry.getValue();
            if (!(obj instanceof Selector)) {
                continue;
            }

            String name = entry.getKey();
            Selector selector = (Selector)obj;
            if (!seen.contains(selector)) {
                seen.add(selector);
                selector.clearNames();
            }
            nameMap.put(name, selector);
            selector.addName(name);
        }
        return nameMap;
    }

    public float getInitialWavenumber() {
        return display.getMultiSpectralData().init_wavenumber;
    }

    public PyDictionary getBandNameMappings() {
        PyDictionary map = new PyDictionary();
        MultiSpectralData data = display.getMultiSpectralData();
        if (!data.hasBandNames())
           return map;

        for (Entry<String, Float> entry : data.getBandNameMap().entrySet()) {
            map.__setitem__(entry.getKey(), new PyFloat(entry.getValue()));
        }

        return map;
    }

    public void addCombination(final String name, final Data combo) {
        source.addChoice(name, combo);
    }

//    public void addRealCombination(final String name, final Combination combo) {
//        source.addRealCombo(name, combo, console);
//    }
//
//    public Console getConsole() {
//        return console;
//    }

    /**
     * Called after Jython's internals have finished processing {@code line}
     * (and before control is given back to the user).
     * 
     * <p>This is where {@code LinearCombo} controls map Jython names to Java
     * objects.
     */
    public void ranBlock(final String line) {
        List<DragLine> dragLines = display.getSelectors();
        Map<String, Object> javaObjects = console.getJavaInstances();
        Set<String> ids = getSelectorIds(javaObjects);
        for (DragLine dragLine : dragLines) {
            String lineId = dragLine.getControlId();
            if (!ids.contains(lineId)) {
                display.removeSelector(lineId);
                selectorMap.remove(lineId);
            }
        }

        jythonMap = mapNamesToThings(javaObjects);
        logger.trace("ranBlock: javaObjs={}", javaObjects);
    }

//    public void saveJythonThings() {
//        // well, only selectors so far...
//        for (Map.Entry<String, Selector> entry : jythonMap.entrySet()) {
//            String cmd = String.format("%s.setWaveNumber(%f)", entry.getKey(), entry.getValue().getWaveNumber());
//            System.err.println("saving: "+cmd);
//            console.addMetaCommand(cmd);
//        }
//    }

    public static abstract class JythonThing {
        protected Set<String> jythonNames = new LinkedHashSet<String>();
        public JythonThing() { }
        public abstract Data getData();

        public static String colorString(final ConstantMap[] color) {
            if (color == null) {
                return "[null]";
            }
            if (color.length != 3) {
                return "[invalid color string]";
            }

            double r = color[0].getConstant();
            double g = color[1].getConstant();
            double b = color[2].getConstant();
            return String.format("[r=%.3f; g=%.3f; b=%.3f]", r, g, b);
        }

        private static Data extractData(final Object other) throws VisADException, RemoteException {
            if (other instanceof JythonThing) {
                return ((JythonThing)other).getData();
            }
            if (other instanceof PyFloat) {
                return new Real(((PyFloat)other).getValue());
            }
            if (other instanceof PyInteger) {
                return new Real(((PyInteger)other).getValue());
            }
            if (other instanceof Double) {
                return new Real((Double)other);
            }
            if (other instanceof Integer) {
                return new Real((Integer)other);
            }
            if (other instanceof Data) {
                return (Data)other;
            }
            throw new IllegalArgumentException("Can't figure out what to do with " + other);
        }

        private static String extractName(final Object other) {
            if (other instanceof JythonThing) {
                return ((JythonThing)other).getName();
            }
            if (other instanceof PyInteger) {
                return ((PyInteger)other).toString();
            }
            if (other instanceof PyFloat) {
                return ((PyFloat)other).toString();
            }
            if (other instanceof Double) {
                return ((Double)other).toString();
            }
            if (other instanceof Integer) {
                return ((Integer)other).toString();
            }
            throw new IllegalArgumentException("UGH: "+other);
        }

        public abstract boolean removeName(final String name);
        public abstract boolean addName(final String name);
        public abstract String getName();
        public abstract Collection<String> getNames();

        public Combination __add__(final Object other) throws VisADException, RemoteException {
            return new AddCombination(this, other);
        }
        public Combination __sub__(final Object other) throws VisADException, RemoteException {
            return new SubtractCombination(this, other);
        }
        public Combination __mul__(final Object other) throws VisADException, RemoteException {
            return new MultiplyCombination(this, other);
        }
        public Combination __div__(final Object other) throws VisADException, RemoteException {
            return new DivideCombination(this, other);
        }
        public Combination __pow__(final Object other) throws VisADException, RemoteException {
            return new ExponentCombination(this, other);
        }
        public Combination __mod__(final Object other) throws VisADException, RemoteException {
            return new ModuloCombination(this, other);
        }
        public Combination __radd__(final Object other) throws VisADException, RemoteException {
            return new AddCombination(other, this);
        }
        public Combination __rsub__(final Object other) throws VisADException, RemoteException {
            return new SubtractCombination(other, this);
        }
        public Combination __rmul__(final Object other) throws VisADException, RemoteException {
            return new MultiplyCombination(other, this);
        }
        public Combination __rdiv__(final Object other) throws VisADException, RemoteException {
            return new DivideCombination(other, this);
        }
        public Combination __rpow__(final Object other) throws VisADException, RemoteException {
            return new ExponentCombination(other, this);
        }
        public Combination __rmod__(final Object other) throws VisADException, RemoteException {
            return new ModuloCombination(other, this);
        }
        public Combination __neg__() throws VisADException, RemoteException {
            return new NegateCombination(this);
        }
    }

    /**
     * 
     */
    public static class Selector extends JythonThing {

        /** */
        private final String ID;

        /** */
        private float waveNumber;

        /** */
        private ConstantMap[] color;

        /** */
        private Console console;

        /** */
        private HydraControl control;

        /** */
        private Data data;

        /** */
        private MultiSpectralDisplay display;

        /**
         * 
         * 
         * @param waveNumber 
         * @param color 
         * @param control 
         * @param console 
         */
        public Selector(final float waveNumber, final ConstantMap[] color, final HydraControl control, final Console console) {
            super();
            this.ID = hashCode() + "_jython";
            this.waveNumber = waveNumber;
            this.control = control;
            this.console = console;
            this.display = control.getMultiSpectralDisplay();

            this.color = new ConstantMap[color.length];
            for (int i = 0; i < this.color.length; i++) {
                ConstantMap mappedColor = (ConstantMap)color[i];
                this.color[i] = (ConstantMap)mappedColor.clone();
            }

            if (control instanceof LinearCombo) {
                LinearCombo lc = (LinearCombo)control;
                try {
                    lc.addSelector(this);
                } catch (Exception e) {
                    // TODO(jon): no way jose
                    System.err.println("Could not create selector: "+e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        /**
         * Attempts removal of a known name for the current Selector.
         * 
         * @param name Name (within Jython namespace) to remove.
         * 
         * @return {@code true} if removal was successful, {@code false} 
         * otherwise.
         */
        public boolean removeName(final String name) {
            return jythonNames.remove(name);
        }

        /**
         * Returns the known Jython names associated with this Selector.
         * 
         * @return {@literal "Names"} (aka variables) in a Jython namespace that
         * refer to this Selector. Collection may be empty, but never 
         * {@code null}.
         */
        public Collection<String> getNames() {
            return new LinkedHashSet<String>(jythonNames);
        }

        /**
         * Resets the known names of a Selector.
         */
        public void clearNames() {
            jythonNames.clear();
        }

        /**
         * Attempts to associate a Jython {@literal "variable"/"name"} with 
         * this Selector.
         * 
         * @param name Name used within the Jython namespace. Cannot be 
         * {@code null}.
         * 
         * @return {@code true} if {@code name} was successfully added, 
         * {@code false} otherwise.
         */
        public boolean addName(final String name) {
            return jythonNames.add(name);
        }

        /**
         * Returns a Jython name associated with this Selector. Consider using
         * {@link #getNames()} instead.
         * 
         * @return Either a blank {@code String} if there are no associated 
         * names, or the {@literal "first"} (iteration-order) name.
         * 
         * @see #getNames()
         */
        public String getName() {
            if (jythonNames.isEmpty()) {
                return "";
            } else {
                return jythonNames.iterator().next();
            }
        }

        /**
         * Changes the {@literal "selected"} wave number to the given value.
         * 
         * <p><b>WARNING:</b>no bounds-checking is currently being performed,
         * but this is expected to change in the near future.</p>
         * 
         * @param newWaveNumber New wave number to associate with the current
         * Selector.
         */
        public void setWaveNumber(final float newWaveNumber) {
            waveNumber = newWaveNumber;
            try {
                display.setSelectorValue(ID, waveNumber);
            } catch (Exception e) {
                LogUtil.logException("Selector.setWaveNumber", e);
            }
        }

        /**
         * Returns the {@literal "selected"} wave number associated with this
         * {@code Selector}.
         * 
         * @return Wave number currently selected by this {@code Selector}.
         */
        public float getWaveNumber() {
            return waveNumber;
        }

        /**
         * Returns the color associated with this {@code Selector}.
         * 
         * @return {@literal "Color"} for this {@code Selector}.
         */
        public ConstantMap[] getColor() {
            return color;
        }

        /**
         * Returns the data selected by the location of this {@code Selector}.
         * 
         * @return Data selected by this {@code Selector}.
         */
        public Data getData() {
            return control.getMultiSpectralDisplay().getImageDataFrom(waveNumber);
        }

        /**
         * Returns an identifier for this {@code Selector}.
         * 
         * @return ID for this {@code Selector}.
         */
       public String getId() {
            return ID;
        }

       /**
        * Returns a {@code String} representation of the relevant information
        * {@literal "stored"} by this {@code Selector}.
        *
        * @return {@code String} representation of this {@code Selector}.
        */
       @Override public String toString() {
           int hashLen = 0;
           int idLen = 0;
           int waveLen = 0;
           int colorLen = 0;
           int namesLen = 0;
           return String.format("[Selector@%x: id=%s, waveNumber=%f, color=%s, jythonNames=%s]",
               hashCode(), ID, waveNumber, colorString(color), jythonNames);
           
       }
    }

    public static abstract class Combination extends JythonThing {
        private final Object left;
        private final Object right;

        private final String leftName;
        private final String rightName;

        private final Data leftData;
        private final Data rightData;

        private Data operationData;

        public Combination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            left = lhs;
            right = rhs;

            leftName = extractName(left);
            rightName = extractName(right);

            leftData = extractData(left);
            rightData = extractData(right);
        }

        private static Data extractData(final Object obj) throws VisADException, RemoteException {
            if (obj instanceof JythonThing) {
                return ((JythonThing)obj).getData();
            }
            if (obj instanceof PyFloat) {
                return new Real(((PyFloat)obj).getValue());
            }
            if (obj instanceof PyInteger) {
                return new Real(((PyInteger)obj).getValue());
            }
            if (obj instanceof Double) {
                return new Real((Double)obj);
            }
            if (obj instanceof Integer) {
                return new Real((Integer)obj);
            }
            if (obj instanceof Data) {
                return (Data)obj;
            }
            throw new IllegalArgumentException("Can't figure out what to do with " + obj);
        }

        protected static String extractName(final Object obj) {
            if (obj instanceof JythonThing) {
                return ((JythonThing)obj).getName();
            }
            if (obj instanceof PyFloat) {
                return ((PyFloat)obj).toString();
            }
            if (obj instanceof PyInteger) {
                return ((PyInteger)obj).toString();
            }
            if (obj instanceof Double) {
                return ((Double)obj).toString();
            }
            if (obj instanceof Integer) {
                return ((Integer)obj).toString();
            }
            throw new IllegalArgumentException("UGH: "+obj);
        }

        protected void setOperationData(final Data opData) {
            operationData = opData;
        }

        protected Data getOperationData() {
            return operationData;
        }

        //public Data 
        
        public Object getLeft() {
            return left;
        }

        public Object getRight() {
            return right;
        }

        public String getLeftName() {
            return leftName;
        }

        public String getRightName() {
            return rightName;
        }

        public Data getLeftData() {
            return leftData;
        }

        public Data getRightData() {
            return rightData;
        }

        public boolean removeName(final String name) {
            return true;
        }

        public boolean addName(final String name) {
            return true;
        }

        public String getName() {
            return getFriendlyString();
        }

        public Data getData() {
            return operationData;
        }

        public Collection<String> getNames() {
            Set<String> set = new LinkedHashSet<String>(1);
            set.add(getFriendlyString());
            return set;
        }

        public abstract String getFriendlyString();
        public abstract String getPersistableString();
        public abstract String toString();
    }

    private static class AddCombination extends Combination {
        public AddCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().add(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s + %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[AddCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class SubtractCombination extends Combination {
        public SubtractCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().subtract(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s - %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[SubtractCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class MultiplyCombination extends Combination {
        public MultiplyCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().multiply(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s * %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[MultiplyCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class DivideCombination extends Combination {
        public DivideCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().divide(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s / %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[DivideCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class ExponentCombination extends Combination {
        public ExponentCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().pow(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s**%s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[ExponentCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class ModuloCombination extends Combination {
        public ModuloCombination(final Object lhs, final Object rhs) throws VisADException, RemoteException {
            super(lhs, rhs);
            setOperationData(getLeftData().remainder(getRightData()));
        }
        public String getFriendlyString() {
            return String.format("(%s %% %s)", getLeftName(), getRightName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[ModuloCombo@%x: leftName=%s, rightName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getRightName(), getFriendlyString(), getPersistableString());
        }
    }
    private static class NegateCombination extends Combination {
        public NegateCombination(final Object lhs) throws VisADException, RemoteException {
            super(lhs, null);
            setOperationData(getLeftData().negate());
        }
        public String getFriendlyString() {
            return String.format("(-%s)", getLeftName());
        }
        public String getPersistableString() {
            return getFriendlyString();
        }
        public String toString() {
            return String.format("[NegateCombo@%x: leftName=%s, friendlyString=%s, persistableString=%s]", 
                hashCode(), getLeftName(), getFriendlyString(), getPersistableString());
        }
    }
}
