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

package edu.wisc.ssec.mcidasv.util;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A collection of utilities for XML..
 *
 */
public abstract class XmlUtil extends ucar.unidata.xml.XmlUtil {

    /**
     * Print all the attributes of the given node
     *
     * @param parent
     */
    public static void printNode(Node parent) {
        if (parent==null) {
            System.out.println("null node!");
            return;
        }
        System.out.println(parent.getNodeName() + " node:");
        NamedNodeMap attrs = parent.getAttributes();
        for(int i = 0 ; i<attrs.getLength() ; i++) {
            Attr attribute = (Attr)attrs.item(i);
            System.out.println("  " + attribute.getName()+" = "+attribute.getValue());
        }
    }

    /**
     *  Find all of the  descendant elements of the given parent Node
     *  whose tag name.equals the given tag.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tag The tag name to match.
     *  @return The list of descendants that match the given tag.
     */
    public static List<String> findDescendantNamesWithSeparator(Node parent, String tag, String separator) {
        List<String> found = new ArrayList<String>();
        findDescendantNamesWithSeparator(parent, tag, "", separator, found);
        return found;
    }
    
    /**
     *  Find all of the  descendant elements of the given parent Node
     *  whose tag name equals the given tag.
     *
     *  @param parent The root of the xml dom tree to search.
     *  @param tag The tag name to match.
     *  @param found The list of descendants that match the given tag.
     */
    private static void findDescendantNamesWithSeparator(Node parent, String tag, String descendants, String separator, List<String> found) {
            if (parent instanceof Element) {
                String elementName = ((Element)parent).getAttribute("name");
                if (!elementName.isEmpty()) {
                    descendants += ((Element)parent).getAttribute("name");
                }
                if (parent.getNodeName().equals(tag)) {
                    found.add(descendants);
                }
                if (!elementName.isEmpty()) {
                    descendants += separator;
                }
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
        	findDescendantNamesWithSeparator(child, tag, descendants, separator, found);
        }
    }
    
    /**
     * Find the element described by nameList (path).
     * 
     * @param parent
     * @param nameList
     *
     * @return {@code Element} described by the given path, or {@code null} if
     * there was a problem.
     */
    public static Element getElementAtNamedPath(Node parent, List<String> nameList) {
        return getMakeElementAtNamedPath(parent, nameList, "", false);
    }
    
    /**
     * Make the element described by nameList (path).
     * 
     * @param parent
     * @param nameList
     *
     * @return {@code Element} described by the given path, or {@code null} if
     * there was a problem.
     */
    public static Element makeElementAtNamedPath(Node parent, List<String> nameList, String tagname) {
        return getMakeElementAtNamedPath(parent, nameList, tagname, true);
    }
    
    /**
     * Find the element described by nameList (path).
     * 
     * @param parent
     * @param nameList
     *
     * @return {@code Element} described by the given path, or {@code null} if
     * there was a problem.
     */
    public static Element getMakeElementAtNamedPath(Node parent, List<String> nameList, String tagName, boolean makeNew) {
        Element thisElement = null;
        if (parent instanceof Element && !nameList.isEmpty()) {
            for (int i=0; i < nameList.size(); i++) {
                String thisName = nameList.get(i);
                NodeList children = parent.getChildNodes();
                boolean foundChild = false;
                for (int j=0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (!(child instanceof Element)) continue;
                    if (XmlUtil.getAttribute(child, "name").equals(thisName)) {
                        if (i == nameList.size()-1) thisElement = (Element)child;
                        parent = child;
                        foundChild = true;
                        break;
                    }
                }

                // Didn't find it where we expected to.  Create a new one.
                if (makeNew && !foundChild && parent instanceof Element) {
                    try {
                        Element newElement = XmlUtil.create(tagName, (Element)parent);
                        newElement.setAttribute("name", thisName);
                        parent.appendChild(newElement);
                        parent = newElement;
                        thisElement = newElement;
                    } catch (Exception ex) {
                        System.err.println("Error making new " + tagName + " node named " + thisName);
                        break;
                    }
                }
            }
        }
        return thisElement;
    }
    
    /**
     * Added by TJJ Feb 2014
     * 
     * This method ensures that the output String has only
     * valid XML unicode characters as specified by the
     * XML 1.0 standard. For reference, please see
     * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
     * standard</a>. This method will return an empty
     * String if the input is null or empty.
     *
     * @param in The String whose non-valid characters we want to remove.
     * @return The in String, stripped of non-valid characters.
     */
    
    public static String stripNonValidXMLCharacters(String in) {
        StringBuffer out = new StringBuffer(); // Used to hold the output.
        char current; // Used to reference the current character.

        if (in == null || ("".equals(in))) return ""; // vacancy test.
        for (int i = 0; i < in.length(); i++) {
            current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here; it should not happen.
            if ((current == 0x9) ||
                (current == 0xA) ||
                (current == 0xD) ||
                ((current >= 0x20) && (current <= 0xD7FF)) ||
                ((current >= 0xE000) && (current <= 0xFFFD)) ||
                ((current >= 0x10000) && (current <= 0x10FFFF)))
                out.append(current);
        }
        return out.toString();
    }
    
}
