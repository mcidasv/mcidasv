/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2026
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

package edu.wisc.ssec.mcidasv.chooser;

import static edu.wisc.ssec.mcidasv.util.McVGuiUtils.*;
import static javax.swing.BorderFactory.*;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.LayoutStyle.ComponentPlacement.RELATED;
import static javax.swing.LayoutStyle.ComponentPlacement.UNRELATED;
import static ucar.unidata.util.IOUtil.hasSuffix;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.text.JTextComponent;

import edu.wisc.ssec.mcidasv.util.CollectionHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LayoutUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;
import visad.util.ImageHelper;
import edu.wisc.ssec.mcidasv.Constants;
import edu.wisc.ssec.mcidasv.data.AxformInfo;
import edu.wisc.ssec.mcidasv.data.EnviInfo;
import edu.wisc.ssec.mcidasv.data.HeaderInfo;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Position;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Prefer;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.TextColor;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.Width;
import edu.wisc.ssec.mcidasv.util.McVGuiUtils.IconPanel;

public class FlatFileChooser extends IdvChooser implements Constants {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(FlatFileChooser.class);

    /** Set default stride to keep dimensions within this */
    private final int maxDefDim = 1000;
    
    // Properties associated with the button selector
    private File dataFile;
    private final JTextField dataFileText = new JTextField();
    private JButton dataFileButton = new JButton();
    private final JLabel dataFileDescription = new JLabel();
    private final JLabel textDescription = new JLabel();
    
    // Dimensions
    // elements, lines, bands
    private final JTextComponent textElements = new JTextField();
    private final JTextComponent textLines = new JTextField();
    private final JTextComponent textBands = new JTextField();
    private final JTextComponent textUnit = new JTextField();
    private final JTextComponent textStride = new JTextField();
    private final AbstractButton checkTranspose = new JCheckBox("Transpose elements/lines");
    private final List<String> bandNames = new ArrayList<>(20);
    private final List<String> bandFiles = new ArrayList<>(20);

    // Navigation
    // lat/lon files or bounds
    private final JToggleButton radioLatLonFiles = new JRadioButton("Files", true);
    private final JToggleButton radioLatLonBounds = new JRadioButton("Bounds", false);
    private File latFile;
    private File lonFile;
    private final JLabel textLatFile = new JLabel();
    private JButton buttonLatFile = new JButton();
    private final JLabel textLonFile = new JLabel();
    private JButton buttonLonFile = new JButton();
    private JPanel panelLatLonFiles = new JPanel();
    private final JTextComponent textLatUL = new JTextField();
    private final JTextComponent textLonUL = new JTextField();
    private final JTextComponent textLatLR = new JTextField();
    private final JTextComponent textLonLR = new JTextField();
    private JPanel panelLatLonBounds = new JPanel();
    private final JTextComponent textLatLonScale = new JTextField();
    private final AbstractButton checkEastPositive = new JCheckBox("East positive");


    // Properties associated with the data file
    // bytes/pixel, ASCII delimiter, endianness, interleave, offset, missing
    private final JToggleButton radioBinary = new JRadioButton("Binary", true);
    private final JToggleButton radioASCII = new JRadioButton("ASCII", false);
    private final JToggleButton radioImage = new JRadioButton("Image", false);
    private final JToggleButton radioEndianLittle = new JRadioButton("Little", true);
    private final JToggleButton radioEndianBig = new JRadioButton("Big", false);
    private final JComboBox<TwoFacedObject> comboByteFormat = new JComboBox<>();
    private final JComboBox<TwoFacedObject> comboInterleave = new JComboBox<>();
    private final JTextComponent textOffset = new JTextField("0");
    private JPanel panelBinary = new JPanel();
    private final JTextComponent textDelimiter = new JTextField();
    private JPanel panelASCII = new JPanel();
    private JPanel panelImage = new JPanel();
    private final JTextComponent textMissing = new JTextField();

    private static File lastDir = null;
    
    private final List<TwoFacedObject> listByteFormat = CollectionHelpers.list(
        new TwoFacedObject("1-byte unsigned integer", HeaderInfo.kFormat1ByteUInt),
        new TwoFacedObject("2-byte signed integer", HeaderInfo.kFormat2ByteSInt),
        new TwoFacedObject("4-byte signed integer", HeaderInfo.kFormat4ByteSInt),
        new TwoFacedObject("4-byte float", HeaderInfo.kFormat4ByteFloat),
        new TwoFacedObject("8-byte double", HeaderInfo.kFormat8ByteDouble),
        new TwoFacedObject("2x8-byte complex number", HeaderInfo.kFormat2x8Byte),
        new TwoFacedObject("2-byte unsigned integer", HeaderInfo.kFormat2ByteUInt)
    );

    private final List<TwoFacedObject> listInterleave = CollectionHelpers.list(
        new TwoFacedObject("Sequential", HeaderInfo.kInterleaveSequential),
        new TwoFacedObject("By line", HeaderInfo.kInterleaveByLine),
        new TwoFacedObject("By pixel", HeaderInfo.kInterleaveByPixel)
    );

    private final JLabel statusLabel = new JLabel("Status");

    /** Handle to the IDV. */
    protected IntegratedDataViewer idv = getIdv();
    
    /**
     * Super setStatus() takes a second string to enable "simple" mode
     * which highlights the required component.  We don't really care
     * about that feature, and we don't want getStatusLabel() to
     * change the label background color.
     */
    @Override
    public void setStatus(String statusString, String foo) {
        if (statusString == null) {
            statusString = "";
        }
        statusLabel.setText(statusString);
    }

    /**
     * Create the FileChooser, passing in the manager and the xml element
     * from choosers.xml
     *
     * @param mgr The manager
     * @param root The xml root
     */
    public FlatFileChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        
        loadButton = makeImageTextButton(ICON_ACCEPT_SMALL, getLoadCommandName());
        loadButton.setActionCommand(getLoadCommandName());
        loadButton.addActionListener(this);

        dataFileButton = makeImageButton(ICON_OPEN, "Open file");
        dataFileButton.addActionListener(e -> {
            dataFile = getDataFile(dataFile);
            if (dataFile != null) {
                dataFileText.setText(dataFile.getAbsolutePath());
                inspectDataFile(dataFile);
            }
        });
        dataFileText.addActionListener(e -> {
            dataFile = new File(dataFileText.getText());
            inspectDataFile(dataFile);
        });
        dataFileText.addFocusListener(new FocusListener() {
            @Override public void focusGained(FocusEvent e) {}
            @Override public void focusLost(FocusEvent e) {
                dataFile = new File(dataFileText.getText());
                inspectDataFile(dataFile);
            }
        });
        
        radioLatLonFiles.addActionListener(e -> checkSetLatLon());
        radioLatLonBounds.addActionListener(e -> checkSetLatLon());

        buttonLatFile = makeImageButton(ICON_OPEN, "Select latitude file");
        buttonLatFile.addActionListener(e -> {
            latFile = getDataFile(latFile);
            if (latFile != null) {
                textLatFile.setText(latFile.getName());
            }
        });
        buttonLonFile = makeImageButton(ICON_OPEN, "Select longitude file");
        buttonLonFile.addActionListener(e -> {
            lonFile = getDataFile(lonFile);
            if (lonFile != null) {
                textLonFile.setText(lonFile.getName());
            }
        });
        GuiUtils.buttonGroup(radioLatLonFiles, radioLatLonBounds);
        
        radioBinary.addActionListener(e -> checkSetBinaryASCIIImage());
        radioASCII.addActionListener(e -> checkSetBinaryASCIIImage());
        radioImage.addActionListener(e -> checkSetBinaryASCIIImage());

        GuiUtils.buttonGroup(radioBinary, radioASCII, radioImage);
        GuiUtils.buttonGroup(radioEndianLittle, radioEndianBig);

        GuiUtils.setListData(comboByteFormat, listByteFormat);
        GuiUtils.setListData(comboInterleave, listInterleave);

        setHaveData(false);
    }
    
    /**
     * enable/disable widgets for navigation
     */
    private void checkSetLatLon() {
        boolean isFile = radioLatLonFiles.isSelected();
        GuiUtils.enableTree(panelLatLonFiles, isFile);
        GuiUtils.enableTree(panelLatLonBounds, !isFile);
    }

    /**
     * enable/disable widgets for binary/ASCII
     */
    private void checkSetBinaryASCIIImage() {
        GuiUtils.enableTree(panelBinary, radioBinary.isSelected());
        GuiUtils.enableTree(panelASCII, radioASCII.isSelected());
        GuiUtils.enableTree(panelImage, radioImage.isSelected());
    }

    /**
     * Set whether the user has made a selection that contains data.
     *
     * @param have   true to set the haveData property.  Enables the
     *               loading button
     */
    @Override public void setHaveData(boolean have) {
        super.setHaveData(have);
        updateStatus();
    }
    
    /**
     * Set the status message appropriately
     */
    @Override protected void updateStatus() {
        super.updateStatus();
        checkSetLatLon();
        checkSetBinaryASCIIImage();
        if (!getHaveData()) {
            setStatus("Select a file"); 
        }
    }

    private static boolean isImage(File f) {
        String s = f.getName();
        return hasSuffix(s, ".gif") || hasSuffix(s, ".jpg") || hasSuffix(s, ".png");
    }

    private static boolean isXml(File f) {
        String s = f.getName();
        return hasSuffix(s, ".xml") || hasSuffix(s, ".ximg");
    }

    private void inspectDataFile(File thisFile) {
        if ((thisFile == null) || thisFile.getName().isEmpty()) {
            dataFileDescription.setText("");
            setHaveData(false);
        } else if (!thisFile.exists()) {
            dataFileDescription.setText("File does not exist");
            setHaveData(false);
        } else {
            try (Reader fr = new FileReader(thisFile)) {
                char[] first80c = new char[80];
                fr.read(first80c, 0, 80);
                String first80 = new String(first80c);

                clearValues();

                boolean doStride = false;

                if (isImage(thisFile)) {
                    dataFileDescription.setText("Image file");
                    processImageFile(thisFile);
                } else if (isXml(thisFile)) {
                    dataFileDescription.setText("XML image header file");
                    processXmlHeaderFile(thisFile);
                } else if (first80.contains("                     Space Science & Engineering Center")) {
                    dataFileDescription.setText("McIDAS-X AXFORM header file");
                    processAxformHeaderFile(thisFile);
                    doStride = true;
                } else if (isENVI()) {
                    dataFileDescription.setText("ENVI Data File");
                    logger.trace("Found ENVI file, about to process header...");
                    processEnviHeaderFile(new File(dataFile.getAbsolutePath().replace(".img", ".hdr")));
                    doStride = true;
                } else {
                    dataFileDescription.setText("Binary, ASCII or Image data");
                    processGenericFile(thisFile);
                    doStride = true;
                }

                // Default the stride
                int newStride = 1;
                if (doStride) {
                    String textLinesText = textLines.getText();
                    String textElementsText = textElements.getText();
                    if (!(textLinesText.isEmpty() || textElementsText.isEmpty())) {
                        int myLines = Integer.parseInt(textLinesText);
                        int myElements = Integer.parseInt(textElementsText);
                        if ((myLines > maxDefDim) || (myElements > maxDefDim)) {
                            newStride = Math.max((int) Math.ceil((float) myLines / (float) maxDefDim), (int) Math.ceil((float) myElements / (float) maxDefDim));
                        }
                    }
                }
                textStride.setText(Integer.toString(newStride));
                setHaveData(true);
            } catch (Exception e) {
                logger.error("error inspecting file '" + thisFile + '\'', e);
            }
        }
    }
        
    private boolean isENVI() {
        // look for a corresponding header file
        // filename.replace(".hdr", ".img");
        boolean result = false;
        File f = new File(dataFile.getAbsolutePath().replace(".img", ".hdr"));
//        if (!f.exists()) {
//            return false;
//        }
        if (f.exists()) {
            try (Reader fr = new FileReader(f)) {
                char[] first80c = new char[80];
                fr.read(first80c, 0, 80);
                fr.close();
                String first80 = new String(first80c);
                if (first80.contains("ENVI")) {
                    result = true;
                }
            } catch (FileNotFoundException fnfe) {
                logger.error("could not locate file", fnfe);
            } catch (IOException ioe) {
                logger.error("could not read file", ioe);
            }
        }
        return result;
    }

	/**
     * Special processing for a known data type.
     *
     * <p>This deals specifically with AXFORM header files.</p>
     *
     * @param thisFile AXFORM header file.
     */
    private void processAxformHeaderFile(File thisFile) {
        bandNames.clear();
        bandFiles.clear();

        try {
            AxformInfo axformInfo = new AxformInfo(thisFile);
            
            // Set the properties in the GUI
            textDescription.setText(axformInfo.getParameter(HeaderInfo.DESCRIPTION, ""));
            textElements.setText(axformInfo.getParameter(HeaderInfo.ELEMENTS, 0).toString());
            textLines.setText(axformInfo.getParameter(HeaderInfo.LINES, 0).toString());
            textUnit.setText(axformInfo.getParameter(HeaderInfo.UNIT, ""));
            bandNames.addAll(axformInfo.getParameter(HeaderInfo.BANDNAMES, Collections.emptyList()));
            bandFiles.addAll(axformInfo.getParameter(HeaderInfo.BANDFILES, Collections.emptyList()));
            textBands.setText(Integer.toString(bandNames.size()));
            textOffset.setText(axformInfo.getParameter(HeaderInfo.OFFSET, 0).toString());
            textMissing.setText(axformInfo.getParameter(HeaderInfo.MISSINGVALUE, (float)0).toString());
            
            Integer dataType = axformInfo.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown);
            Boolean bigEndian = axformInfo.getParameter(HeaderInfo.BIGENDIAN, Boolean.FALSE);
            if (dataType == HeaderInfo.kFormatASCII) {
                radioASCII.setSelected(true);
            } else if (dataType == HeaderInfo.kFormatImage) {
                radioImage.setSelected(true);
            } else {
                radioBinary.setSelected(true);
                TwoFacedObject tfo = TwoFacedObject.findId(dataType.intValue(), listByteFormat);
                if (tfo != null) {
                    comboByteFormat.setSelectedItem(tfo);
                }
                tfo = TwoFacedObject.findId(HeaderInfo.kInterleaveSequential, listInterleave);
                if (tfo != null) {
                    comboInterleave.setSelectedItem(tfo);
                }
            }
            
            radioEndianLittle.setSelected(!bigEndian);
            radioEndianBig.setSelected(bigEndian);

            List<String> latlonFiles = axformInfo.getParameter(HeaderInfo.NAVFILES, new ArrayList<String>(4));
            if (latlonFiles.size() == 2) {
                latFile = new File(latlonFiles.get(0));
                lonFile = new File(latlonFiles.get(1));
            }

            if ((latFile == null) || (lonFile == null)) {
                radioLatLonBounds.setSelected(true);
            } else {
                textLatFile.setText(latFile.getName());
                textLonFile.setText(lonFile.getName());
                radioLatLonFiles.setSelected(true);
            }
            
            textLatLonScale.setText("100");
            checkEastPositive.setSelected(true);
        } catch (Exception e) {
            logger.error("error processing AXFORM header file", e);
        }
    }

    /**
     * Special processing for a known data type.
     *
     * <p>This deals specifically with ENVI header files.</p>
     *
     * @param thisFile ENVI header file.
     */
    private void processEnviHeaderFile(File thisFile) {
        try {
            EnviInfo enviInfo = new EnviInfo(thisFile);
            
            // Set the properties in the GUI
            textDescription.setText(enviInfo.getParameter(HeaderInfo.DESCRIPTION, ""));
            textElements.setText(enviInfo.getParameter(HeaderInfo.ELEMENTS, 0).toString());
            textLines.setText(enviInfo.getParameter(HeaderInfo.LINES, 0).toString());
            textUnit.setText(enviInfo.getParameter(HeaderInfo.UNIT, ""));
            bandNames.addAll(enviInfo.getParameter(HeaderInfo.BANDNAMES, Collections.emptyList()));
            bandFiles.addAll(enviInfo.getParameter(HeaderInfo.BANDFILES, Collections.emptyList()));
            textBands.setText(Integer.toString(bandNames.size()));
            textOffset.setText(enviInfo.getParameter(HeaderInfo.OFFSET, 0).toString());
            textMissing.setText(enviInfo.getParameter(HeaderInfo.MISSINGVALUE, (float)0).toString());

            Integer dataType = enviInfo.getParameter(HeaderInfo.DATATYPE, HeaderInfo.kFormatUnknown);
            String interleaveType = enviInfo.getParameter(HeaderInfo.INTERLEAVE, HeaderInfo.kInterleaveSequential);
            Boolean bigEndian = enviInfo.getParameter(HeaderInfo.BIGENDIAN, Boolean.FALSE);
            radioBinary.setSelected(true);
            TwoFacedObject tfo = TwoFacedObject.findId(dataType.intValue(), listByteFormat);
            if (tfo != null) {
                comboByteFormat.setSelectedItem(tfo);
            }
            tfo = TwoFacedObject.findId(interleaveType, listInterleave);
            if (tfo != null) {
                comboInterleave.setSelectedItem(tfo);
            }
            
            radioEndianLittle.setSelected(!bigEndian);
            radioEndianBig.setSelected(bigEndian);

            // Look for a geo.hdr file that contains Latitude and Longitude bands
            String parent = thisFile.getParent();
            if (parent == null) {
                parent = ".";
            }
            String navFile = thisFile.getName().replace(".hdr", "");
            int lastDot = navFile.lastIndexOf('.');
            if (lastDot >= 0) {
                navFile = navFile.substring(0, lastDot) + ".geo.hdr";
            }
            navFile = parent + '/' + navFile;
            EnviInfo navInfo = new EnviInfo(navFile);
            if (navInfo.isNavHeader()) {
                latFile = new File(navFile);
                lonFile = new File(navFile);
            }
            
            if ((latFile == null) || (lonFile == null)) {
                radioLatLonBounds.setSelected(true);
            }
            else {
                textLatFile.setText(latFile.getName());
                textLonFile.setText(lonFile.getName());
                radioLatLonFiles.setSelected(true);
            }
            
            // fill in Lat/Lon bounds if we can
            if (enviInfo.isHasBounds()) {
                textLatUL.setText(enviInfo.getParameter("BOUNDS.ULLAT", ""));
                textLonUL.setText(enviInfo.getParameter("BOUNDS.ULLON", ""));
                textLatLR.setText(enviInfo.getParameter("BOUNDS.LRLAT", ""));
                textLonLR.setText(enviInfo.getParameter("BOUNDS.LRLON", ""));
            }
            
            textLatLonScale.setText("1");
            checkEastPositive.setSelected(false);
        } catch (Exception e) {
            logger.error("error processing ENVI header file", e);
        }
    }

    /**
     * Special processing for a known data type.
     *
     * <p>This deals specifically with XML header files.</p>
     *
     * @param thisFile XML header file.
     */
    private void processXmlHeaderFile(File thisFile) {
        try {
            
            bandFiles.clear();
            bandNames.clear();

            Element root = XmlUtil.getRoot(thisFile.getAbsolutePath(), getClass());
            if (!"image".equals(root.getTagName())) {
                processGenericFile(thisFile);
                return;
            }
            
            String description = XmlUtil.getAttribute(root, "name", (String)null);
            String url = "";
            if (XmlUtil.hasAttribute(root, "url")) {
                url = XmlUtil.getAttribute(root, "url");
                if (description.isEmpty()) {
                    description = url;
                }
                String parent = thisFile.getParent();
                if (parent == null) {
                    parent = ".";
                }
                url = parent + '/' + url;
            }
            else {
                processGenericFile(thisFile);
                return;
            }
            if (XmlUtil.hasAttribute(root, "ullat")) {
                radioLatLonBounds.setSelected(true);
                textLatUL.setText(XmlUtil.getAttribute(root, "ullat"));
            }
            if (XmlUtil.hasAttribute(root, "ullon")) {
                radioLatLonBounds.setSelected(true);
                textLonUL.setText(XmlUtil.getAttribute(root, "ullon"));
            }
            if (XmlUtil.hasAttribute(root, "lrlat")) {
                radioLatLonBounds.setSelected(true);
                textLatLR.setText(XmlUtil.getAttribute(root, "lrlat"));
            }
            if (XmlUtil.hasAttribute(root, "lrlon")) {
                radioLatLonBounds.setSelected(true);
                textLonLR.setText(XmlUtil.getAttribute(root, "lrlon"));
            }

            // Try to read the referenced image to get lines and elements
            setStatus("Loading image");
            InputStream is   = null;
            is = IOUtil.getInputStream(url, getClass());
            byte[] imageContent = IOUtil.readBytes(is);
            Image image = Toolkit.getDefaultToolkit().createImage(imageContent);
            MediaTracker tracker = new MediaTracker(this); 
            tracker.addImage(image, 0);
            try { 
                tracker.waitForAll(); 
            } catch(InterruptedException e) {}
            ImageHelper ih = new ImageHelper();
            image.getWidth(ih);
            if (ih.badImage) {
                throw new IllegalStateException("Bad image: " + url);
            }
            int elements = image.getWidth(ih);
            int lines = image.getHeight(ih);
            
            // Bands
            bandFiles.add(url);
            bandNames.add("XML image file");
            
            // Set the properties in the GUI
            textDescription.setText(description);
            textElements.setText(Integer.toString(elements));
            textLines.setText(Integer.toString(lines));
            textBands.setText(Integer.toString(bandNames.size()));

            radioImage.setSelected(true);
            textMissing.setText(Float.toString(-1.0F));
            
            textLatLonScale.setText("1");
            checkEastPositive.setSelected(false);
        } catch (Exception e) {
            logger.error("error processing XML header file", e);
        }
    }

    /**
     * Special processing for a known data type.
     *
     * <p>This deals specifically with {@literal "image"} files.</p>
     *
     * @param thisFile Image file.
     */
    private void processImageFile(File thisFile) {
        try {

            bandFiles.clear();
            bandNames.clear();

            String description = thisFile.getName();
            String url = thisFile.getAbsolutePath();

            // Try to read the referenced image to get lines and elements
            setStatus("Loading image");
            InputStream is   = null;
            is = IOUtil.getInputStream(url, getClass());
            byte[] imageContent = IOUtil.readBytes(is);
            Image image = Toolkit.getDefaultToolkit().createImage(imageContent);
            MediaTracker tracker = new MediaTracker(this); 
            tracker.addImage(image, 0);
            try { 
                tracker.waitForAll(); 
            } catch (InterruptedException e) {}
            ImageHelper ih = new ImageHelper();
            image.getWidth(ih);
            if (ih.badImage) {
                throw new IllegalStateException("Bad image: " + url);
            }

            // Bands
            bandFiles.add(url);
            bandNames.add("Image file");

            // Set the properties in the GUI
            textDescription.setText(description);
            textElements.setText(Integer.toString(image.getWidth(ih)));
            textLines.setText(Integer.toString(image.getHeight(ih)));
            textBands.setText(Integer.toString(bandNames.size()));
            
            radioImage.setSelected(true);
            textMissing.setText(Float.toString(-1.0F));
            
            textLatLonScale.setText("1");
            checkEastPositive.setSelected(false);
        }
        catch (Exception e) {
            logger.error("error processing image file", e);
        }
    }

    /**
     * Special processing for an unknown data type.
     *
     * <p>Can we glean anything about the file by inspecting it more?</p>
     *
     * @param thisFile Unknown file type.
     */
    private void processGenericFile(File thisFile) {

        clearValues();

        // Set appropriate defaults
        // Bands
        bandFiles.clear();
        bandFiles.add(thisFile.getAbsolutePath());
        bandNames.clear();
        bandNames.add("Flat data");

        // Set the properties in the GUI
        textDescription.setText(thisFile.getName());
//      textElements.setText(Integer.toString(elements));
//      textLines.setText(Integer.toString(lines));
        textBands.setText("1");
        
        radioBinary.setSelected(true);
        
        textLatLonScale.setText("1");
        checkEastPositive.setSelected(false);

    }
    
    /**
     * Clear out any data values presented to the user
     */
    private void clearValues() {
        textDescription.setText("");
        textElements.setText("");
        textLines.setText("");
        textBands.setText("");
        textUnit.setText("");
        textStride.setText("");
        checkTranspose.setSelected(false);

        textLatFile.setText("");
        textLonFile.setText("");
        textLatUL.setText("");
        textLonUL.setText("");
        textLatLR.setText("");
        textLonLR.setText("");

        textLatLonScale.setText("");
        checkEastPositive.setSelected(false);

        textOffset.setText("0");
        textMissing.setText("");
    }
    
    /**
     * Ask the user for a data file.
     *
     * @param thisFile File or directory to use as initial location.
     *
     * @return Selected data file.
     */
    private static File getDataFile(File thisFile) {
	    Preferences prefs = Preferences.userNodeForPackage(FlatFileChooser.class);

        // If no file provided, try in-memory lastDir first
        if (thisFile == null && lastDir != null) {
            thisFile = lastDir;
        }

        // If still nothing, fall back to stored preference
        if (thisFile == null) {
            String storedDir = prefs.get("flatfile.lastDir", null);
            if (storedDir != null) {
                thisFile = new File(storedDir);
            }
        }

        JFileChooser fileChooser = new JFileChooser(thisFile);
        fileChooser.setMultiSelectionEnabled(false);
        int status = fileChooser.showOpenDialog(null);
        if (status == JFileChooser.APPROVE_OPTION) {
            thisFile = fileChooser.getSelectedFile();
            // Save directory in memory (for same session)
            lastDir = thisFile.getParentFile();
            // Save directory in preferences (for next session)
            prefs.put("flatfile.lastDir", lastDir.getAbsolutePath());
        }
        return thisFile;
    }
    
    /**
     * Get the name of the dataset.
     *
     * @return descriptive name of the dataset.
     */
    public static String getDatasetName() {
        return "Data Set Name";
    }
    
    /**
     * Get the properties from the datasource
     *
     * @param ht  a Hashtable of properties
     */
    @Override protected void getDataSourceProperties(Hashtable ht) {
        super.getDataSourceProperties(ht);
        ht.put("FLAT.NAME", textDescription.getText());
        ht.put("FLAT.ELEMENTS", textElements.getText());
        ht.put("FLAT.LINES", textLines.getText());
        ht.put("FLAT.BANDNAMES", bandNames);
        ht.put("FLAT.BANDFILES", bandFiles);
        ht.put("FLAT.UNIT", textUnit.getText());
        ht.put("FLAT.STRIDE", textStride.getText());
        ht.put("FLAT.TRANSPOSE", checkTranspose.isSelected());
        ht.put("FLAT.MISSING", textMissing.getText());
        
        // Navigation
        if (radioLatLonFiles.isSelected()) {
            ht.put("NAV.TYPE", "FILES");
            ht.put("FILE.LAT", latFile.getAbsolutePath());
            ht.put("FILE.LON", lonFile.getAbsolutePath());
        } else if (radioLatLonBounds.isSelected()) {
            ht.put("NAV.TYPE", "BOUNDS");
            ht.put("BOUNDS.ULLAT", textLatUL.getText());
            ht.put("BOUNDS.ULLON", textLonUL.getText());
            ht.put("BOUNDS.LRLAT", textLatLR.getText());
            ht.put("BOUNDS.LRLON", textLonLR.getText());
        } else {
            ht.put("NAV.TYPE", "UNKNOWN");
        }
        ht.put("NAV.SCALE", textLatLonScale.getText());
        ht.put("NAV.EASTPOS", checkEastPositive.isSelected());

        // Data type
        if (radioBinary.isSelected()) {
            TwoFacedObject format = (TwoFacedObject) comboByteFormat.getSelectedItem();
            TwoFacedObject interleave = (TwoFacedObject) comboInterleave.getSelectedItem();
            ht.put("FORMAT.TYPE", "BINARY");
            ht.put("BINARY.FORMAT", format.getId());
            ht.put("BINARY.INTERLEAVE", interleave.getId());
            ht.put("BINARY.BIGENDIAN", radioEndianBig.isSelected());
            ht.put("BINARY.OFFSET", textOffset.getText());
        } else if (radioASCII.isSelected()) {
            ht.put("FORMAT.TYPE", "ASCII");
            ht.put("ASCII.DELIMITER", textDelimiter.getText());
        } else if (radioImage.isSelected()) {
            ht.put("FORMAT.TYPE", "IMAGE");
        } else {
            ht.put("FORMAT.TYPE", "UNKNOWN");
        }

    }
        
    /**
     * User said go, so we go.
     *
     * <p>Simply get the list of images from the imageChooser and create the
     * {@code FILE.FLAT} {@code DataSource}.</p>
     */
    public void doLoadInThread() {
        String definingObject = dataFileText.getText();
        String dataType = "FILE.FLAT";
        
        Hashtable properties = new Hashtable();
        getDataSourceProperties(properties);
        
        makeDataSource(definingObject, dataType, properties);
    }
    
    /**
     * Creates the dimensions inner panel.
     *
     * @return The {@literal "dimensions"} panel.
     */
    protected JPanel makeDimensionsPanel() {
        JPanel myPanel = new JPanel();
        myPanel.setBorder(createTitledBorder("Dimensions"));
        
        JLabel elementsLabel = makeLabelRight("Elements:");
        setComponentWidth(textElements);
        
        JLabel linesLabel = makeLabelRight("Lines:");
        setComponentWidth(textLines);
        
        JLabel bandsLabel = makeLabelRight("Bands:");
        setComponentWidth(textBands);

        JLabel unitLabel = makeLabelRight("Units:");
        setComponentWidth(textUnit);

        JLabel strideLabel = makeLabelRight("Sampling:");
        setComponentWidth(textStride);

//      JLabel transposeLabel = McVGuiUtils.makeLabelRight("");
        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(elementsLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textElements))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(linesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textLines))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(bandsLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textBands))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(unitLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textUnit))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(strideLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textStride)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textElements)
                    .addComponent(elementsLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textLines)
                    .addComponent(linesLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textBands)
                    .addComponent(bandsLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textUnit)
                    .addComponent(unitLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textStride)
                    .addComponent(strideLabel))
                .addContainerGap())
        );
        
        return myPanel;
    }

    /**
     * Creates the navigation inner panel.
     *
     * @return The {@literal "navigation"} panel.
     */
    protected JPanel makeNavigationPanel() {
        JPanel myPanel = new JPanel();
        myPanel.setBorder(createTitledBorder("Navigation"));
        
        setComponentWidth(textLatFile, Width.DOUBLE);
        setComponentWidth(textLonFile, Width.DOUBLE);
        panelLatLonFiles = topBottom(
            LayoutUtil.leftRight(makeLabeledComponent("Latitude:", textLatFile), buttonLatFile),
            LayoutUtil.leftRight(makeLabeledComponent("Longitude:", textLonFile), buttonLonFile),
            Prefer.NEITHER);
        
        // Images to make the bounds more clear
        Component urPanel = new IconPanel("/edu/wisc/ssec/mcidasv/images/upper_right.gif");
        Component llPanel = new IconPanel("/edu/wisc/ssec/mcidasv/images/lower_left.gif");
        
        setComponentWidth(textLatUL);
        setComponentWidth(textLonUL);
        setComponentWidth(textLatLR);
        setComponentWidth(textLonLR);
        panelLatLonBounds = topBottom(
            makeLabeledComponent("UL Lat/Lon:", LayoutUtil.leftRight(GuiUtils.hbox(textLatUL, textLonUL), urPanel)),
            makeLabeledComponent("LR Lat/Lon:", LayoutUtil.leftRight(llPanel, GuiUtils.hbox(textLatLR, textLonLR))),
            Prefer.NEITHER);
        
        setComponentWidth(radioLatLonFiles);
        setComponentWidth(radioLatLonBounds);
        
        JLabel labelScale = makeLabelRight("Scale:");
        setComponentWidth(textLatLonScale);
        
        JPanel panelScaleEastPositive = LayoutUtil.hbox(textLatLonScale, checkEastPositive);
        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioLatLonFiles)
                        .addGap(GAP_RELATED)
                        .addComponent(panelLatLonFiles))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioLatLonBounds)
                        .addGap(GAP_RELATED)
                        .addComponent(panelLatLonBounds))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelScale)
                        .addGap(GAP_RELATED)
                        .addComponent(panelScaleEastPositive)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioLatLonFiles)
                    .addComponent(panelLatLonFiles))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioLatLonBounds)
                    .addComponent(panelLatLonBounds))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(labelScale)
                    .addComponent(panelScaleEastPositive))
                .addContainerGap())
        );
        return myPanel;
    }

    /**
     * Creates the format inner panel.
     *
     * @return The {@literal "format"} panel.
     */
    protected JPanel makeFormatPanel() {
        JPanel myPanel = new JPanel();
        myPanel.setBorder(createTitledBorder("Format"));

        setComponentWidth(radioBinary);
        setComponentWidth(radioASCII);
        setComponentWidth(radioImage);
        setComponentWidth(radioEndianLittle);
        setComponentWidth(radioEndianBig);

        setComponentWidth(comboByteFormat, Width.TRIPLE);
        setComponentWidth(comboInterleave, Width.DOUBLE);
        setComponentWidth(textOffset, Width.HALF);

        panelBinary = topBottom(
            topBottom(
                makeLabeledComponent("Byte format:", comboByteFormat),
                makeLabeledComponent("Interleave:", comboInterleave),
                Prefer.NEITHER),
            topBottom(
                makeLabeledComponent("Endian:", GuiUtils.hbox(radioEndianLittle, radioEndianBig, GAP_RELATED)),
                makeLabeledComponent("Offset:", makeComponentLabeled(textOffset, "bytes")),
                Prefer.NEITHER),
            Prefer.NEITHER);
        
        setComponentWidth(textDelimiter, Width.HALF);
        panelASCII = makeLabeledComponent("Delimiter:", textDelimiter);
        panelImage = new JPanel();
        
        JLabel missingLabel = makeLabelRight("Missing value:");
        setComponentWidth(textMissing);
        JPanel missingPanel = makeComponentLabeled(textMissing, "");

        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioBinary)
                        .addGap(GAP_RELATED)
                        .addComponent(panelBinary))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioASCII)
                        .addGap(GAP_RELATED)
                        .addComponent(panelASCII))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(radioImage)
                        .addGap(GAP_RELATED)
                        .addComponent(panelImage))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(missingLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(missingPanel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioBinary)
                    .addComponent(panelBinary))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioASCII)
                    .addComponent(panelASCII))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(radioImage)
                    .addComponent(panelImage))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(missingLabel)
                    .addComponent(missingPanel))
                .addContainerGap())
        );
        return myPanel;
    }

    /**
     * Creates the main panel properties panel.
     *
     * @return The {@literal "properties"} panel.
     */
    protected JPanel makePropertiesPanel() {
        JPanel topPanel = sideBySide(makeDimensionsPanel(), makeNavigationPanel());
        JPanel bottomPanel = makeFormatPanel();
        return topBottom(topPanel, bottomPanel, Prefer.NEITHER);
    }
    
    /**
     * Builds the GUI of the flat file chooser.
     *
     * @return GUI of this chooser.
     */
    protected JComponent doMakeContents() {

        Element chooserNode = getXmlNode();
        String path = (String)idv.getPreference(PREF_DEFAULTDIR + getId());
        if (path == null) {
            path = XmlUtil.getAttribute(chooserNode, "path", (String)null);
        }

        JPanel myPanel = new JPanel();
        
        // File
        JLabel fileLabel = makeLabelRight("File:");
        setComponentWidth(dataFileText, Width.DOUBLEDOUBLE);
        
        JLabel typeLabel = makeLabelRight("Type:");
        setLabelBold(dataFileDescription, true);

        JLabel descriptionLabel = makeLabelRight("Description:");
        setLabelBold(textDescription, true);

        JLabel propertiesLabel = makeLabelRight("Properties:");
        JPanel propertiesPanel = makePropertiesPanel();
        
        JLabel statusLabelLabel = makeLabelRight("");
        setLabelPosition(statusLabel, Position.RIGHT);
        setComponentColor(statusLabel, TextColor.STATUS);
                
        JButton helpButton = makeImageButton(ICON_HELP, "Show help");
        helpButton.setActionCommand(GuiUtils.CMD_HELP);
        helpButton.addActionListener(this);
        
        setComponentWidth(loadButton, Width.DOUBLE);
        
        GroupLayout layout = new GroupLayout(myPanel);
        myPanel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(fileLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(dataFileText)
                        .addGap(GAP_RELATED)
                        .addComponent(dataFileButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(typeLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(dataFileDescription))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(descriptionLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(textDescription))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(propertiesLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(propertiesPanel))
                    .addGroup(TRAILING, layout.createSequentialGroup()
                        .addComponent(helpButton)
                        .addPreferredGap(RELATED)
                        .addComponent(loadButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(statusLabelLabel)
                        .addGap(GAP_RELATED)
                        .addComponent(statusLabel, DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(LEADING)
            .addGroup(TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(dataFileButton)
                    .addComponent(dataFileText)
                    .addComponent(fileLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(dataFileDescription)
                    .addComponent(typeLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(textDescription)
                    .addComponent(descriptionLabel))
                .addPreferredGap(RELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(propertiesPanel)
                    .addComponent(propertiesLabel))
                .addPreferredGap(RELATED, DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(statusLabelLabel)
                    .addComponent(statusLabel))
                .addPreferredGap(UNRELATED)
                .addGroup(layout.createParallelGroup(BASELINE)
                    .addComponent(loadButton)
                    .addComponent(helpButton))
                .addContainerGap())
        );
        return myPanel;
    }
}

