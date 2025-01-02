/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2025
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * https://www.ssec.wisc.edu/mcidas/
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
 * along with this program.  If not, see https://www.gnu.org/licenses/.
 */

package ucar.unidata.idv.control.drawing;


import org.w3c.dom.Element;

import ucar.unidata.idv.control.DrawingControl;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import ucar.visad.*;
import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocationTuple;

import java.awt.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * Class HighLowGlyph Draws text
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.11 $
 */
public class HighLowGlyph extends DrawingGlyph {

    /** xgrf xml attribute */
    public static final String ATTR_PRESSURE = "pressure";

    /** The pressure */
    private double pressure;

    /** The text drawn */
    private boolean high = true;
    
    /**
     * {@code true} if we're showing {@literal "high/low/etc"}.
     * {@code false} if we're showing {@literal "alto/bajo/etc"}.
     */
    private boolean useEnglish = true;
    
    /** type for the symbol */
    private TextType textType1;

    /** type for the pressure */
    private TextType textType2;

    /** Text displayable */
    private TextDisplayable textDisplayable;

    /** Pressure displayable */
    private TextDisplayable pressureDisplayable;

    /** for the properties dialog */
    JTextField pressureFld;
    
    /** Button group for language convention selection. May be {@code null}. */
    ButtonGroup languageGroup;
    
    /** English conventions button. May be {@code null}. */
    JRadioButton englishBtn;
    
    /** Spanish conventions button. May be {@code null}. */
    JRadioButton spanishBtn;


    /**
     * ctor
     */
    public HighLowGlyph() {}

    /**
     * ctor
     *
     * @param high Is this a high pressure symbol
     */
    public HighLowGlyph(boolean high) {
        this.high = high;
    }


    /**
     * ctor
     *
     * @param control The control I'm in
     * @param event The event when I was created
     * @param high Is this a high pressure symbol
     */
    public HighLowGlyph(DrawingControl control, DisplayEvent event,
                        boolean high) {
        super(control, event);
        this.high = high;
    }



    /**
     * Get the extra descripition used in the JTable listing
     *
     * @return extra description
     */
    public String getExtraDescription() {
        return (high
                ? getHighText() + ' ' + pressure
                : getLowText() + ' ' + pressure);
    }




    /**
     * Make a field maps all of the time values to the given data as the range
     *
     * @param data The range
     *
     * @return time field
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Data getTimeField(Data data)
            throws VisADException, RemoteException {
        return Util.makeTimeRangeField(data, (isFrontDisplay()
                ? getTimeValues()
                : new ArrayList()));
    }


    /**
     * Initialize after creation
     *
     * @param event The event
     *
     * @return this
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public DrawingGlyph handleCreation(DisplayEvent event)
            throws VisADException, RemoteException {
        super.handleCreation(event);
        points = Misc.newList(getPoint(event));
        updateLocation();
        return null;
    }


    /**
     * User created me.
     *
     * @param control The control I'm in
     * @param event The event
     *
     * @return ok
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public boolean initFromUser(DrawingControl control, DisplayEvent event)
            throws VisADException, RemoteException {
        String pressureString = GuiUtils.getInput("Please enter a pressure",
                                    "Pressure:", "1000");
        if (pressureString == null) {
            return false;
        }
        pressureString = pressureString.trim();
        if (pressureString.length() == 0) {
            pressure = Double.NaN;
        } else {
            pressure = Misc.parseNumber(pressureString);
        }

        return super.initFromUser(control, event);
    }



    /**
     * Xml created me
     *
     *
     * @param control The control
     * @param node The xml
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void initFromXml(DrawingControl control, Element node)
            throws VisADException, RemoteException {
        super.initFromXml(control, node);
        this.pressure = XmlUtil.getAttribute(node, ATTR_PRESSURE, 0.0);
    }




    /**
     * The tag to use in the xml
     *
     * @return Xml tag name
     */
    public String getTagName() {
        return (high
                ? TAG_HIGH
                : TAG_LOW);
    }

    /**
     * Populate the xml node with attrs
     *
     * @param e Xml node
     */
    protected void addAttributes(Element e) {
        super.addAttributes(e);
        e.setAttribute(ATTR_PRESSURE, "" + pressure);
    }


    /**
     * utility to set the animation set on the displayable
     *
     * @param d displayable
     * @param timeValues times
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected static void setAnimationSet(Displayable d, List timeValues)
            throws VisADException, RemoteException {
        List animationSet = null;
        if ((timeValues != null) && (timeValues.size() == 2)) {
            DateTime startTime = (DateTime) timeValues.get(0);
            DateTime endTime   = (DateTime) timeValues.get(1);
            double newTime = startTime.getValue()
                             + (endTime.getValue() - startTime.getValue())
                               / 2.0;
            animationSet = new ArrayList();
            animationSet.add(new DateTime(newTime, startTime.getUnit()));
        }
        d.setOverrideAnimationSet(animationSet);
    }

    /**
     * Do the final initialization
     *
     * @return Successful
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean initFinalInner()
            throws VisADException, RemoteException {
        if ( !super.initFinalInner()) {
            return false;
        }


        setCoordType(COORD_LATLON);
        textType1 = TextType.getTextType("HighLowGlyph_" + (typeCnt++));
        textDisplayable = new TextDisplayable("HighLowGlyph_" + (typeCnt++),
                textType1);
        textDisplayable.setJustification(TextControl.Justification.CENTER);
        textDisplayable.setVerticalJustification(
            TextControl.Justification.BOTTOM);

        textDisplayable.setColor((high
                                  ? Color.BLUE
                                  : Color.RED));
        Font font1 = new Font("times", Font.PLAIN, 24);
        textDisplayable.setFont(font1);
        textDisplayable.setTextSize(control.getDisplayScale()
                                    * (font1.getSize() / 12.0f));
        textDisplayable.setSphere(control.inGlobeDisplay());

        addDisplayable(textDisplayable);
        if (isFrontDisplay()) {
            setAnimationSet(textDisplayable, getTimeValues());
        }

        textType2 = TextType.getTextType("HighLowGlyph_" + (typeCnt++));
        pressureDisplayable = new TextDisplayable("HighLowGlyph_"
                + (typeCnt++), textType2);
        pressureDisplayable.setJustification(TextControl.Justification.RIGHT);
        pressureDisplayable.setVerticalJustification(
            TextControl.Justification.TOP);
        pressureDisplayable.setColor((high
                                      ? Color.BLUE
                                      : Color.RED));

        Font font2 = new Font("times", Font.PLAIN, 14);
        pressureDisplayable.setFont(font2);
        pressureDisplayable.setTextSize(control.getDisplayScale()
                                        * (font2.getSize() / 12.0f));
        pressureDisplayable.setSphere(control.inGlobeDisplay());


        addDisplayable(pressureDisplayable);
        if (isFrontDisplay()) {
            setAnimationSet(pressureDisplayable, getTimeValues());
        }

        return true;
    }


    /**
     * Override the set color method so we don't set the color
     *
     * @param displayable the displayable
     * @param c The color
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void setColor(Displayable displayable, Color c)
            throws VisADException, RemoteException {
        //noop
    }


    /**
     * Handle event
     *
     * @param event The event
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMousePressed(DisplayEvent event)
            throws VisADException, RemoteException {
        return null;
    }


    /**
     * Handle event
     *
     * @param event The event
     *
     * @return This or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public DrawingGlyph handleMouseMoved(DisplayEvent event)
            throws VisADException, RemoteException {
        points = Misc.newList(getPoint(event));
        updateLocation();
        return this;
    }





    /**
     * Handle the property apply.
     *
     * @param compMap Holds property widgets
     *
     *
     * @return success
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected boolean applyProperties(Hashtable compMap)
            throws VisADException, RemoteException {
        if ( !super.applyProperties(compMap)) {
            return false;
        }
        String txt = pressureFld.getText().trim();
        if (txt.length() == 0) {
            pressure = Double.NaN;
        } else {
            pressure = Misc.parseNumber(txt);
        }
        
        if (englishBtn != null) {
            useEnglish = englishBtn.isSelected();
        }
        return true;
    }

    /**
     * get the prop gui comps
     *
     * @param comps List of comps
     * @param compMap comp map
     */
    protected void getPropertiesComponents(List comps, Hashtable compMap) {
        //Call parent with dummy list
        super.getPropertiesComponents(new ArrayList(), compMap);
        getTimePropertiesComponents(comps, compMap);
        String label = ((pressure == pressure)
                        ? Misc.format(pressure)
                        : "");
        pressureFld = new JTextField(label, 8);
        
        englishBtn = new JRadioButton("English (High, Low)", useEnglish);
        spanishBtn = new JRadioButton("Spanish (Alto, Bajo)", !useEnglish);
        
        languageGroup = new ButtonGroup();
        languageGroup.add(englishBtn);
        languageGroup.add(spanishBtn);
        
        comps.add(GuiUtils.rLabel("Label Convention:"));
        comps.add(GuiUtils.left(GuiUtils.topBottom(englishBtn, spanishBtn)));
        comps.add(GuiUtils.rLabel("Pressure:"));
        comps.add(GuiUtils.left(pressureFld));
    }
    
    /**
     * Get the name of this glyph type
     *
     * @return  The name
     */
    public String getTypeName() {
        return (high
                ? getHighText()
                : getLowText());
    }
    
    /**
     * Handle glyph moved
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void updateLocation() throws VisADException, RemoteException {
        super.updateLocation();
        if ((points.size() == 0) || (textDisplayable == null)) {
            return;
        }
        Text t1       = new Text(textType1, (high
                                             ? getHighLabel()
                                             : getLowLabel()));
        Data theData1 = null;
        Text t2       = new Text(textType2, ((pressure == pressure)
                                             ? Misc.format(pressure)
                                             : " "));
        Data theData2 = null;

        if (isInLatLonSpace()) {
            EarthLocationTuple el  = (EarthLocationTuple) points.get(0);
            Real               alt = el.getAltitude();
            el = new EarthLocationTuple(el.getLatLonPoint(),
                                        new Real((RealType) alt.getType(),
                                            getFixedAltitude()));
            theData1 = new Tuple(new Data[] { el, t1 });
            theData2 = new Tuple(new Data[] { el, t2 });

        } else {
            theData1 = new Tuple(new Data[] {
                new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                              (double[]) points.get(0)),
                t1 });
            theData2 = new Tuple(new Data[] {
                new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                              (double[]) points.get(0)),
                t2 });
        }


        textDisplayable.setData(getTimeField(theData1));
        pressureDisplayable.setData(getTimeField(theData2));
        /*
        textDisplayable.setConstantPosition(
            control.getVerticalValue(getZPosition()),
            control.getNavigatedDisplay().getDisplayAltitudeType());
        pressureDisplayable.setConstantPosition(
            control.getVerticalValue(getZPosition()),
            control.getNavigatedDisplay().getDisplayAltitudeType());
        */

    }


    /**
     *  Set the Pressure property.
     *
     *  @param value The new value for Pressure
     */
    public void setPressure(double value) {
        pressure = value;
    }

    /**
     *  Get the Pressure property.
     *
     *  @return The Pressure
     */
    public double getPressure() {
        return pressure;
    }

    /**
     *  Set the High property.
     *
     *  @param value The new value for High
     */
    public void setHigh(boolean value) {
        high = value;
    }

    /**
     *  Get the High property.
     *
     *  @return The High
     */
    public boolean getHigh() {
        return high;
    }
    
    /**
     * Change language convention.
     *
     * @param value {@code true} for English, {@code false} for Spanish.
     */
    public void setEnglish(boolean value) {
        useEnglish = value;
    }
    
    /**
     * Determine language convention.
     *
     * @return {@code true} if English, {@code false} for Spanish.
     */
    public boolean getEnglish() {
        return useEnglish;
    }
    
    /**
     * Get the label to use in the GUI for {@literal "high"}.
     *
     * English: {literal "High"}. Spanish: {@literal "Alto"}.
     *
     * @return Label to used in GUI widgets.
     */
    private String getHighText() {
        return useEnglish ? "High" : "Alto";
    }
    
    /**
     * Get the label to use in the GUI for {@literal "low"}.
     *
     * English: {literal "Low"}. Spanish: {@literal "Bajo"}.
     *
     * @return Label to used in GUI widgets.
     */
    private String getLowText() {
        return useEnglish ? "Low" : "Bajo";
    }
    
    /**
     * Get the label used to display {@literal "high"}.
     *
     * English: {@code A}. Spanish: {@code H}.
     *
     * @return Label to be used in main display.
     */
    private String getHighLabel() {
        return useEnglish ? "H" : "A";
    }
    
    /**
     * Get the label used to display {@literal "low"}.
     *
     * English: {@code L}. Spanish: {@code B}.
     *
     * @return Label to be used in main display.
     */
    private String getLowLabel() {
        return useEnglish ? "L" : "B";
    }
}

