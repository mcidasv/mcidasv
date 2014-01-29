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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.ResourceCollection;

import edu.wisc.ssec.mcidasv.servermanager.LocalEntryEditor;

/**
 * McIDAS-V extension to the IDV's {@link PluginManager}. The chief difference
 * is that all requests for plugins are redirected to SSEC rather than Unidata.
 */
public class McvPluginManager extends PluginManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalEntryEditor.class);

    public McvPluginManager(IntegratedDataViewer idv) {
        super(idv);
    }

    @Override protected void loadPlugins() throws Exception {
        ResourceCollection rc = getResourceManager().getResources(
            getResourceManager().RSC_PLUGINS);

        for (int i = 0; i < rc.size(); i++) {
            String path = rc.get(i).toString();
            if (!rc.isWritable(i))
                continue;

            logger.debug("loadPlugins: path={}", path);
            File pluginDir = new File(path);
            File[] plugins = pluginDir.listFiles();
            if (plugins == null)
                continue;

            for (File plugin : plugins) {
                String current = plugin.getName();
                if (current.startsWith(".tmp.") || current.endsWith(".deletethis"))
                    continue;

                if (current.startsWith("http%3A%2F%2Fwww.unidata")) {
                    String newName = "http%3A%2F%2Fwww.ssec.wisc.edu%2Fmcidas%2Fsoftware%2Fv%2Fresources%2Fplugins%2F"+IOUtil.getFileTail(decode(current));
                    logger.debug("  current={}", current);
                    logger.debug("  newName={}\n",newName);
                    File checkExisting = new File(plugin.getParent(), newName);
                    if (checkExisting.exists())
                        logger.debug("    newName already exists...");
                    else
                        logger.debug("    rename plugin...");
                }
            }
        }
        super.loadPlugins();
    }

    /**
     * McIDAS-V overrides {@link PluginManager#removePlugin(File)} so that the
     * user is given an update on their plugin situation.
     */
    @Override public void removePlugin(File file) { 
        super.removePlugin(file);
        LogUtil.userMessage("You must restart McIDAS-V to complete the removal of this plugin.");
    }
    
    /**
     * Do not give the option to restart.  Simply note that a restart is necessary at some point in the future.
     */
    protected void notifyUser() {
    	LogUtil.userMessage("You must restart McIDAS-V to complete the installation of this plugin.");
        return;
    }
}
