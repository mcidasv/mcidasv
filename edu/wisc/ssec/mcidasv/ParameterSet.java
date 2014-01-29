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

package edu.wisc.ssec.mcidasv;


import org.w3c.dom.*;

import java.util.List;

import ucar.unidata.util.StringUtil;


/**
 * An object to handle a saved parameter set.
 */
public class ParameterSet {

    /** Xml attribute name for the name */
    public static final String ATTR_NAME = "name";

    /** Xml attribute name for the category */
    public static final String ATTR_CATEGORY = "category";

    /** The name of the parameter set */
    private String name;

    /** The category of the parameter set */
    private List<String> categories;

    /** The type */
    private String type;
    
    /** The XML element */
    private Element element;

    /** prefix_ */
    private String uniquePrefix;

    public ParameterSet(String name, String category, String type) {
    	this(name, category, type, null);
    }

    public ParameterSet(String name, List<String> categories, String type) {
    	this(name, categories, type, null);
    }
    
    public ParameterSet(String name, String category, String type, Element element) {
    	List<String> categories = PersistenceManager.stringToCategories(category);
    	this.name = name;
    	this.categories = categories;
    	this.type = type;
    	this.element = element;
    }

    public ParameterSet(String name, List<String>categories, String type, Element element) {
    	this.name = name;
    	this.categories = categories;
    	this.type = type;
    	this.element = element;
    }
    
    /**
     * set the unique prefix
     *
     * @param p prefix
     */
    protected void setUniquePrefix(String p) {
        uniquePrefix = p;
    }

    /**
     * Get the name to use with the categories as a prefix
     *
     * @return categorized name
     */
    public String getCategorizedName() {
        String catString = StringUtil.join("_", categories);
        if (uniquePrefix != null) {
            catString = uniquePrefix + catString;
        }
        return catString + name;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategories(List value) {
        categories = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public List getCategories() {
        return categories;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Set the Element property.
     *
     * @param value The new value for Element
     */
    public void setElement(Element value) {
        element = value;
    }

    /**
     * Get the Element property.
     *
     * @return The Element
     */
    public Element getElement() {
        return element;
    }

    /**
     * Full label
     *
     * @return The name.
     */
    public String getLabel() {
        return PersistenceManager.categoriesToString(categories)
               + PersistenceManager.CATEGORY_SEPARATOR + name;
    }

    /**
     * Override toString.
     *
     * @return The name.
     */
    public String toString() {
//    	String description = name + " { ";
//    	NamedNodeMap attributes = element.getAttributes();
//    	for (int i=0; i<attributes.getLength(); i++) {
//    		Node attribute = attributes.item(i);
//    		description += attribute.getNodeName() + "=" + attribute.getNodeValue();
//    		if (i < attributes.getLength() - 1) description += ", ";
//    	}
//    	description += "}";
//        return description;
    	return name;
    }

}

