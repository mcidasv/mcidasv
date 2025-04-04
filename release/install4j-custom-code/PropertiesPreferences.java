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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Preferences implementation that uses a properties file for storage. Not
 * intended for production!
 * 
 * @author David Dossot
 */
public class PropertiesPreferences extends AbstractPreferences {
  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private final boolean system;
  private final Properties backingStore;
  private final File backingStoreFile;
  private final Map children;
  
  static String getTargetFolder(boolean isSystem) {
    String result = isSystem ? System.getProperty("java.util.prefs.systemRoot") : System
        .getProperty("java.util.prefs.userRoot");
    
    if ((result == null) || ("".equals(result))) throw new IllegalArgumentException("The root folder system property is not correctly set");
    
    return result;
  }

  private PropertiesPreferences(AbstractPreferences parent, String name, boolean isSystem) {
    super(parent, name);
    this.system = isSystem;

    this.backingStore = new Properties();
    this.children = new HashMap();
    
    // we build the file full name by concatenating all names up to the parent
    // and prefixing with the right folder
    StringBuffer fullNameBuffer = new StringBuffer(name);
    Preferences parentPrefs = parent;
    while (parentPrefs != null) {
      fullNameBuffer.insert(0, '_').insert(0, parentPrefs.name());
      parentPrefs = parentPrefs.parent();
    }
    fullNameBuffer.insert(0, File.separatorChar).insert(0, getTargetFolder(isSystem));
    fullNameBuffer.append("_prefs.properties");

    backingStoreFile = new File(fullNameBuffer.toString());
  }

  public PropertiesPreferences(String name, boolean isSystem) {
    this(null, name, isSystem);
  }

  protected void putSpi(String key, String value) {
    backingStore.put(key, value);
    
    forceFlush();
  }

  protected String getSpi(String key) {
    forceSync();
    
    return (String) backingStore.get(key);
  }

  protected void removeSpi(String key) {
    backingStore.remove(key);
    forceFlush();
  }

  protected void removeNodeSpi() throws BackingStoreException {
    backingStore.clear();
    forceFlush();
  }

  protected String[] keysSpi() throws BackingStoreException {
    forceSync();
    return (String[]) backingStore.keySet().toArray(EMPTY_STRING_ARRAY);
  }

  protected String[] childrenNamesSpi() throws BackingStoreException {
    return (String[]) children.keySet().toArray(EMPTY_STRING_ARRAY);
  }

  protected AbstractPreferences childSpi(String name) {
    AbstractPreferences result = (AbstractPreferences) children.get(name);

    if (result == null) {
      result = new PropertiesPreferences(this, name, system);
      children.put(name, result);
    }
    return result;
  }

  protected void syncSpi() throws BackingStoreException {
    synchronized (backingStore) {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(backingStoreFile);
        backingStore.load(fis);
      } catch (FileNotFoundException e) {
        throw new BackingStoreException(e);
      } catch (IOException e) {
        throw new BackingStoreException(e);
      } finally {
        try {
          if (fis != null) fis.close();
        } catch (IOException e) {
          // fail quietly (is this dumb?)
        }
      }
    }
  }

  protected void flushSpi() throws BackingStoreException {
    synchronized (backingStore) {
      FileOutputStream fos = null;
      try {
        fos = new FileOutputStream(backingStoreFile, false);
        backingStore.store(fos, "Do not edit! Generated by " + this.getClass().getName());
      } catch (FileNotFoundException e) {
        throw new BackingStoreException(e);
      } catch (IOException e) {
        throw new BackingStoreException(e);
      } finally {
        try {
          if (fos != null) fos.close();
        } catch (IOException e) {
          // fail quietly (is this dumb?)
        }
      }
    }
  }

  private void forceSync() {
    try {
      // we force syncing
      syncSpi();
    } catch (BackingStoreException e) {
      // fail quietly
    }
  }

  private void forceFlush() {
    try {
      // we force flushing
      flushSpi();
    } catch (BackingStoreException e) {
      // fail quietly
    }
  }
}
