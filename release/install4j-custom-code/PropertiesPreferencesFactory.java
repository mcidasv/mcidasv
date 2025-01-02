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
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Factory for Preferences implementation that uses a properties file for storage. Not intended for production!
 * 
 * @author David Dossot
 */
public class PropertiesPreferencesFactory implements PreferencesFactory {
  static {
    // we create the target folder if needed
    new File(PropertiesPreferences.getTargetFolder(true)).mkdirs();
    new File(PropertiesPreferences.getTargetFolder(false)).mkdirs();
  }
  
  public Preferences userRoot() {
    return new PropertiesPreferences("", false);
  }

  public Preferences systemRoot() {
    return new PropertiesPreferences("", true);
  }
}
