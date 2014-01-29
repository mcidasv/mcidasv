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

package edu.wisc.ssec.mcidasv.chooser;


import edu.wisc.ssec.mcidasv.ResourceManager;

import java.awt.BorderLayout;
import java.awt.Dimension;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.imagery.AddeImageDescriptor;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.ui.imagery.ImageSelector;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.NamedThing;
import ucar.unidata.util.PreferenceList;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


public class ImageParameters extends NamedThing {

    private static final String TAG_FOLDER = "folder";
    private static final String TAG_SAVESET = "set";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_URL = "url";

    private static final String[] ATTRS = { "user", "proj", "pos",
        "satband", "band", "id", "key", "latlon", "linele", "loc",
        "mag", "num", "place", "size" , "spac", "unit", "nav",
        "center", "uleft", "lleft", "uright", "lright", "descriptor",
        "group"
    };

    private String server;
    private List properties;
    private List values;

    private String user;
    private String proj;
    private String pos;
    private String satband;
    private String band;
    private String id;
    private String key;
    private String latlon;
    private String linele;
    private String loc;
    private String mag;
    private String num;
    private String place;
    private String size;
    private String spac;
    private String unit;
    private String nav;
    private String center;
    private String uleft;
    private String lleft;
    private String uright;
    private String lright;
    private String descriptor;
    private String group;


    public ImageParameters(String url) {
        List props = new ArrayList();
        List vals = new ArrayList();
        parametersBreakdown(url, props, vals);
        this.properties = props;
        this.values = vals;
        setValues(props, vals);
    }

    public ImageParameters(List props, List vals) {
        this.properties = props;
        this.values = vals;
        setValues(props, vals);
    }

    public List getProperties() {
        return this.properties;
    }

    public List getValues() {
        return this.values;
    }

    public String getServer() {
        return this.server;
    }

    private void setValues(List props, List vals) {
        int len = props.size();
        if (len < 1) return;
        for (int i=0; i<len; i++) {
            String prop = (String)props.get(i);
            if (!isKeyword(prop)) break;
            String val = (String)vals.get(i);
            if (prop.equals("user")) {
                user = val;
                break;
            }
            if (prop.equals("proj")) {
                proj = val;
                break;
            }
            if (prop.equals("pos")) {
                pos = val;
                break;
            }
            if (prop.equals("satband")) {
                satband = val;
                break;
            }
            if (prop.equals("band")) {
                band = val;
                break;
            }
            if (prop.equals("id")) {
                id = val;
                break;
            }
            if (prop.equals("key")) {
                key = val;
                break;
            }
            if (prop.equals("latlon")) {
                latlon = val;
                break;
            }
            if (prop.equals("linele")) {
                linele = val;
                break;
            }
            if (prop.equals("loc")) {
                loc = val;
                break;
            }
            if (prop.equals("mag")) {
                mag = val;
                break;
            }
            if (prop.equals("num")) {
                num = val;
                break;
            }
            if (prop.equals("place")) {
                place = val;
                break;
            }
            if (prop.equals("size")) {
                size = val;
                break;
            }
            if (prop.equals("spac")) {
                spac = val;
                break;
            }
            if (prop.equals("unit")) {
                unit = val;
                break;
            }
            if (prop.equals("nav")) {
                nav = val;
                break;
            }
            if (prop.equals("center")) {
                center = val;
                break;
            }
            if (prop.equals("uleft")) {
                uleft = val;
                break;
            }
            if (prop.equals("lleft")) {
                lleft = val;
                break;
            }
            if (prop.equals("uright")) {
                uright = val;
                break;
            }
            if (prop.equals("lright")) {
                lright = val;
                break;
            }
            if (prop.equals("descriptor")) {
                descriptor = val;
                break;
            }
            if (prop.equals("group")) {
                group = val;
                break;
            }
        }
    }

    private boolean isKeyword(String prop) {
        for (int i=0; i<ATTRS.length; i++) {
            if (prop.equals(ATTRS[i])) return true;
        }
        return false;
   }

    public String getUser() {
        return user;
    }

    public String getProj() {
        return proj;
    }

    public String getPos() {
        return pos;
    }

    public String getSatband() {
        return satband;
    }

    public String getBand() {
        return band;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getLatlon() {
        return latlon;
    }

    public String getLinele() {
        return linele;
    }

    public String getLoc() {
        return loc;
    }

    public String getMag() {
        return mag;
    }

    public String getNum() {
        return num;
    }

    public String getPlace() {
        return place;
    }

    public String getSize() {
        return size;
    }

    public String getSpac() {
        return spac;
    }

    public String getUnit() {
        return unit;
    }

    public String getNav() {
        return nav;
    }

    public String getCenter() {
        return center;
    }

    public String getUleft() {
        return uleft;
    }

    public String getLleft() {
        return lleft;
    }

    public String getUright() {
        return uright;
    }

    public String getLright() {
        return lright;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getGroup() {
        return group;
    }

    private void parametersBreakdown(String url, List props, List vals) {
        //System.out.println("url=" + url);
        String prop;
        String val;
        //StringTokenizer tok = new StringTokenizer(url, "&");
        StringTokenizer tok = new StringTokenizer(url, "/");
        tok.nextToken();
        this.server = tok.nextToken();
        //System.out.println("server=" + server);
        tok = new StringTokenizer(url, "&");
        String remnant = tok.nextToken();
        while (tok.hasMoreElements()) {
            remnant = tok.nextToken();
            StringTokenizer tok2 = new StringTokenizer(remnant, "=");
            if (tok2.countTokens() >= 2) {
                props.add(tok2.nextToken());
                vals.add(tok2.nextToken());
            }
        }
/*
        for (int i=0; i<props.size(); i++) {
            System.out.println(props.get(i) + "=" + vals.get(i));
        }
*/
    }
}
