/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2017
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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.python.core.Py;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoudPyStringMap extends PyStringMap {
    private static final Logger logger = LoggerFactory.getLogger(LoudPyStringMap.class);
    
    private final String mapName;
    
    private Map<Object, PyObject> initMap;
    
    public LoudPyStringMap(String name) {
        mapName = name;
    }
    
    public void bake() {
        if (initMap == null) {
            initMap = new ConcurrentHashMap<>(this.getMap());
            logger.trace("sealing up {}; no overwrites or deletions of existing vars?", mapName);
            logger.trace("contents: {}", initMap);
        } else {
//            logger.trace("already baked {}!", mapName);
        }
    }
    
    private boolean isAlwaysAllowed(String key) {
        // __package__
        // __doc__
        // __name__
        // __builtins__
        return Objects.equals(key, "__builtins__")
               || Objects.equals(key, "__doc__")
               || Objects.equals(key, "__name__")
               || Objects.equals(key, "__package__");
    }
    
    private boolean isAlwaysAllowed(PyObject key) {
        String str = key.toString();
        logger.trace("key: '{}' str value: '{}'", key, str);
        return isAlwaysAllowed(str);
    }
    
    private boolean checkForDone(Map<Object, PyObject> table) {
        boolean result = false;
        if (table.containsKey("___init_finished")) {
            result = Py.py2boolean(table.get("___init_finished"));
        }
//        logger.trace("mapname: '{}' namespace contains init_finished: {}", mapName, result);
        return result;
    }
    
    @Override public PyObject __getitem__(String key) {
        if (isAlwaysAllowed(key)) {
            return super.__getitem__(key);
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
//        if (initMap != null) {
//            return initMap.get(key);
//        } else {
//            return super.__getitem__(key);
//        }
        return super.__getitem__(key);
    }
    
    @Override public PyObject __getitem__(PyObject key) {
        if (isAlwaysAllowed(key)) {
            return super.__getitem__(key);
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }

//        if (initMap != null) {
//            return initMap.get(pyToKey(key));
//        } else {
//            return super.__getitem__(key);
//        }
        return super.__getitem__(key);
    }
    
    @Override public PyObject __iter__() {
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
        return super.__iter__();
    }
    
    @Override public void __setitem__(String key, PyObject value) {
        if (isAlwaysAllowed(key)) {
            super.__setitem__(key, value);
            return;
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
        boolean fromInit = false;
        if (initMap != null) {
            fromInit = initMap.containsKey(key);
        } else {
//            logger.trace("initMap doesn't exist; bake() was never called for '{}'! (key='{}')", mapName, key);
        }
        
        if (!fromInit) {
            if (value == null) {
                logger.trace("mapName: {} fromInit={} actually removing key: {} value: {}", mapName, fromInit, key, value);
            } else if (table.containsKey(key)) {
                logger.trace("mapName: {} fromInit={} changing key: {} oldval: {} newval: {}", mapName, fromInit, key, table.get(key), value);
            }
//            super.__setitem__(key, value);
        } else {
            logger.trace("!!! *** mapName: {} fromInit={} changing key: {} oldval: {} newval: {}", mapName, fromInit, key, table.get(key), value);
        }
    
        super.__setitem__(key, value);
    }

    @Override public void __setitem__(PyObject key, PyObject value) {
//        logger.trace("mapName: {} key='{}' value: {}", mapName, key, value);
        if (isAlwaysAllowed(key)) {
            super.__setitem__(key, value);
            return;
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
        Object convert = pyToKey(key);
        boolean fromInit = false;
        if (initMap != null) {
            fromInit = initMap.containsKey(convert);
        } else {
//            logger.trace("initMap doesn't exist; bake() was never called for '{}'! (key='{}')", mapName, key);
        }
        
        if (!fromInit) {
            if (value == null) {
                logger.trace("mapName: {} fromInit={} actually removing key: {} value: {}", mapName, key, value);
            } else if (table.containsKey(convert)) {
                logger.trace("mapName: {} fromInit={} changing key: {} oldval: {} newval: {}", mapName, key, table.get(key), value);
            }
//            super.__setitem__(key, value);
        } else {
            logger.trace("!!! *** mapName: {} fromInit={} changing key: {} oldval: {} newval: {}", mapName, fromInit, key, table.get(key), value);
        }
        super.__setitem__(key, value);
    }
    
    @Override public void __delitem__(String key) {
        if (isAlwaysAllowed(key)) {
            super.__delitem__(key);
            return;
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
        boolean fromInit = false;
        if (initMap != null) {
            fromInit = initMap.containsKey(key);
        } else {
//            logger.trace("initMap doesn't exist; bake() was never called for '{}'! (key='{}')", mapName, key);
        }
        
        if (!fromInit) {
            logger.trace("mapName: {} fromInit={} trying to remove '{}'", mapName, fromInit, key);
//            super.__delitem__(key);
        } else {
            logger.trace("!!! *** mapName: {} fromInit={} changing key: {} oldval: {}", mapName, fromInit, key, table.get(key));
        }
        super.__delitem__(key);
    }
    
    @Override public void __delitem__(PyObject key) {
        if (isAlwaysAllowed(key)) {
            super.__delitem__(key);
            return;
        }
        Map<Object, PyObject> table = getMap();
        if ((initMap == null) && checkForDone(table)) {
            bake();
        }
        Object convert = pyToKey(key);
        boolean fromInit = false;
        if (initMap != null) {
            fromInit = initMap.containsKey(convert);
        } else {
//            logger.trace("initMap doesn't exist; bake() was never called for '{}'! (key='{}')", mapName, key);
        }
        
        if (!fromInit) {
            logger.trace("mapName: {} fromInit={} trying to remove '{}'", mapName, fromInit, key);
//            super.__delitem__(key);
        } else {
            logger.trace("!!! *** mapName: {} fromInit={} changing key: {} oldval: {}", mapName, fromInit, key, table.get(key));
        }
        super.__delitem__(key);
    }
    
    private static PyObject keyToPy(Object objKey){
        if (objKey instanceof String) {
            return PyString.fromInterned((String)objKey);
        } else {
            return (PyObject)objKey;
        }
    }
    
    private static Object pyToKey(PyObject pyKey) {
        if (pyKey instanceof PyString) {
            return ((PyString)pyKey).internedString();
        } else {
            return pyKey;
        }
    }
}
