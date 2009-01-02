/*
 * $Id$
 *
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2009
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
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

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;

import org.w3c.dom.Element;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.PluginManager;

public class McIDASVPluginManager extends PluginManager {

	/** */
	private static final String[] FILE_INSTALL = 
		{"Install Plugin From File", "installPluginFromFile"};

	/** */
	private static final String[] URL_INSTALL = 
		{"Install Plugin From URL", "installPluginFromUrl"};

	/** */
	private static final String[] CLOSE = 
		{"Close", "closePluginDialog"};

	/** */
	private static final String[] PLUGIN_MANAGER = 
		{"Plugin Manager", "showManagerHelp"};

	/** */
	private static final String OPEN_ICON = 
		"/auxdata/ui/icons/DocumentOpen16.png";

	/** */
	private static final String FIND_ICON = "/auxdata/ui/icons/FindAgain16.gif";		

    /** Tracks what categories to show */
    private Hashtable<String, Boolean> categoryToggle = 
    	new Hashtable<String, Boolean>();	

	/** List of loaded plugin files */
	private List myPlugins = new ArrayList();

	/** List of plugins that are not local */
	private List otherPlugins = new ArrayList();	     
    
    /** Where are the local plugins kept */
    private File localPluginDir;	
	
	/** Keep the IDV reference around to avoid getIdv() calls. */
	private IntegratedDataViewer idv;
	
	/** */
	private JFrame pluginWindow;
	
	/** */
	private JEditorPane availablePluginEditor;
	
	/** */
	private JEditorPane loadedPluginEditor;
	
	/** */
	private JScrollPane availablePluginScroller;
	
	public McIDASVPluginManager(IntegratedDataViewer idv) {
		super(idv);

		this.idv = idv;
	}

	private JMenuItem makeMenuItem(String[] itemData) {
		return GuiUtils.makeMenuItem(itemData[0], this, itemData[1]);
	}

	// taken entirely from the IDV--soon to be entirely replaced
	private void makePluginDialog() {
		Component[] available = GuiUtils.getHtmlComponent("", idv, 700, 300);
		
		availablePluginEditor = (JEditorPane)available[0];
		availablePluginScroller = (JScrollPane)available[1];
	    
		Component[] loaded = GuiUtils.getHtmlComponent("", idv, 700, 200);

	    loadedPluginEditor = (JEditorPane)loaded[0];

	    JComponent contents = GuiUtils.vsplit((JComponent)loaded[1],
            (JComponent)available[1], 150);

	    contents.setSize(new Dimension(700, 500));
	    
	    JButton closeBtn = 
	    	GuiUtils.makeButton("Close", this, "closePluginDialog");

	    JMenuBar menuBar  = new JMenuBar();
	    JMenu fileMenu = new JMenu("File");
	    fileMenu.add(makeMenuItem(FILE_INSTALL));
	    fileMenu.add(makeMenuItem(URL_INSTALL));
	    fileMenu.addSeparator();
	    fileMenu.add(makeMenuItem(CLOSE));
	    
	    JMenu helpMenu = new JMenu("Help");
	    helpMenu.add(makeMenuItem(PLUGIN_MANAGER));

	    menuBar.add(fileMenu);
	    menuBar.add(helpMenu);

	    JLabel lbl1 = new JLabel(GuiUtils.getImageIcon(OPEN_ICON, getClass()));

	    JLabel lbl2 = new JLabel(GuiUtils.getImageIcon(FIND_ICON, getClass()));

	    JComponent bottom = GuiUtils.hbox(new Component[] {
          new JLabel("Key:  "), lbl1,
          new JLabel(" Send to Plugin Creator      "), lbl2,
          new JLabel("View Contents") });

	    bottom = GuiUtils.inset(bottom, 4);
	    contents = GuiUtils.topCenterBottom(menuBar, contents, bottom);

	    contents = GuiUtils.centerBottom(
	    		contents,
	    		GuiUtils.center(GuiUtils.inset(GuiUtils.wrap(closeBtn), 5)));

	    pluginWindow = GuiUtils.createFrame("Plugin Manager");

	    pluginWindow.getContentPane().add(contents);
	    pluginWindow.pack();
	    ucar.unidata.util.Msg.translateTree(pluginWindow);
	    pluginWindow.setLocation(100, 100);
	}
	
	/**
	 * close dialog
	 */
	public void closePluginDialog() {
	    if (pluginWindow != null) {
	        pluginWindow.setVisible(false);
	    }
	}
	
	public void installPlugin(final String plugin) {
		System.err.println("installPlugin");
		Misc.run(new Runnable() {
			public void run() {
				installPluginInThread(plugin);
			}
		});
	}
	
    /**
     * Load in any plugins.
     *
     * @throws Exception On badness
     */
    protected void loadPlugins() throws Exception {
        ResourceCollection rc = getResourceManager().getResources(
                                    getResourceManager().RSC_PLUGINS);

        List<String> plugins = new ArrayList<String>(getArgsManager().plugins);
        
        for (int resourceIdx = 0; resourceIdx < rc.size(); resourceIdx++) {
            String path = rc.get(resourceIdx).toString();
            if ((localPluginDir == null) && rc.isWritable(resourceIdx)) {
                localPluginDir = new File(path);
            }
            plugins.add(path);
        }
       
        for (String path : plugins)
        	handlePlugin(path);
    }
	
    /**
     * 
     */
    @Override
	public void installPluginInThread(String plugin) {
	     try {
	         Plugin p = (Plugin) Plugin.pathToPlugin.get(plugin);
	         File   f = installPlugin(plugin, false);
	         if (f == null) {
	             return;
	         }
	         if (p != null) {
	             p.install();
	             p.file = f;
	         }
	         updatePlugins();
	         LogUtil.userMessage(
	             "Please restart McIDAS-V for this plugin to take effect.");
	     } catch (Throwable exc) {
	         logException("Installing plugin: " + plugin, exc);
	     }
	}
	
    /**
	 * Install the plugin. May be a filename or url.
	 * Copy the bytes into the plugin directory.
	 *
	 * @param plugin filename or url
	 * @param andLoad should we also load the plugin
	 *
	 * @return The new file path
	 * @throws Exception On badness
	 */
	private File installPlugin(String plugin, boolean andLoad)
	         throws Exception {

		String filename = encode(plugin);
	    String extDir =
	        IOUtil.joinDir(getStore().getUserDirectory().toString(),
	                        "plugins");
	     
	    File newFile = new File(IOUtil.joinDir(extDir, filename));

	    Object loadId =
	        JobManager.getManager().startLoad("Installing plugin", true);
	     	     
	    try {
	        URL url = IOUtil.getURL(plugin, getClass());
	        if (IOUtil.writeTo(url, newFile, loadId) <= 0) {
	            return null;
	        }
	    } finally {
	        JobManager.getManager().stopLoad(loadId);
	    }

	    if (andLoad) {
	        new Plugin(newFile);
	        handlePlugin(newFile.toString());
	    }

	    return newFile;
	}
	 
	/**
	 * remove plugin
	 *
	 * @param file file
	 */
	@Override
	public void removePlugin(File file) {
		myPlugins.remove(file.toString());
		try {
			String deleteThisFile = file + ".deletethis";
			IOUtil.writeFile(deleteThisFile, "");
		} catch (Exception exc) {}
		 
		Plugin p = (Plugin) Plugin.pathToPlugin.get(decode(file));
		if (p != null) {
			p.delete();
		}
		updatePlugins();
	}
	 
	/**
	 * Create the list of plugins
	 */
	private void createPluginList() {
	    XmlResourceCollection xrc = getResourceManager().getXmlResources(
	                                    getResourceManager().RSC_PLUGININDEX);

	    double version = getStateManager().getNumberVersion();

	    for (int i = 0; i < xrc.size(); i++) {
	        String path = xrc.get(i).toString();
	        
	        path = IOUtil.getFileRoot(path);
	        
	        Element root = xrc.getRoot(i);
	        
	        if (root == null)
	            continue;
	        
	        List<Element> children = XmlUtil.findChildren(root, TAG_PLUGIN);
	        for (Element pluginNode : children) {
	            String  name = XmlUtil.getAttribute(pluginNode, ATTR_NAME);
	            
	            String desc = XmlUtil.getAttribute(pluginNode, ATTR_DESC,
	                              name);
	            
	            String size = XmlUtil.getAttribute(pluginNode, ATTR_SIZE,
	                              (String) null);
	            
	            String url = XmlUtil.getAttribute(pluginNode, ATTR_URL);
	            if (IOUtil.isRelativePath(url))
	                url = path + "/" + url;
	            
	            String category = XmlUtil.getAttribute(pluginNode,
	                                  ATTR_CATEGORY, "Miscellaneous");
	            
	            Plugin plugin = new Plugin(name, desc, url, category);
	            
	            plugin.size = size;
	            
	            if (XmlUtil.hasAttribute(pluginNode, ATTR_VERSION)) {
	                plugin.version = XmlUtil.getAttribute(pluginNode,
	                        ATTR_VERSION, version);
	            
	                plugin.versionOk = plugin.version <= version;
	            }
	        }
	    }

	    if (localPluginDir != null) {
	        scourPlugins(localPluginDir);
	        
	        File[] files = localPluginDir.listFiles();
	        
	        for (int fileIdx = 0; fileIdx < files.length; fileIdx++) {
	            final File file = files[fileIdx];
	            
	            if (pluginFileOK(file))
	                new Plugin(file);
	        }
	    }
	}

    /**
     * Is the given file ok to load in as a plugin. This checks if there is a .deletethi file
     * or if this file is a .deletethis file
     *
     * @param file file
     *
     * @return ok to load as plugin
     */
    private boolean pluginFileOK(File file) {
        if (file.toString().endsWith(".deletethis"))
            return false;
        
        File deleteFile = new File(file.toString() + ".deletethis");
        if (deleteFile.exists())
            return false;
            
        return true;
    }
	
    /**
     * Remove all .deletethis files from the given directory
     *
     * @param dir directory to scour
     */
    private void scourPlugins(File dir) {
        File[] files = dir.listFiles();
        for (int jarFileIdx = 0; jarFileIdx < files.length; jarFileIdx++) {
            String filename = files[jarFileIdx].toString();
            
            if (filename.endsWith(".deletethis")) {
                File deleteFile = new File(filename);
                
                deleteFile.delete();
                
                deleteFile = new File(IOUtil.stripExtension(filename));
                
                if (deleteFile.exists())
                    deleteFile.delete();
            }
        }
    }

    /**
     * Show or hide the category
     *
     * @param category category to show or hide
     */
    public void toggleCategory(String category) {
        Boolean show = (Boolean) categoryToggle.get(category);
        if (show == null) {
            show = Boolean.FALSE;
        } else {
            show = new Boolean( !show.booleanValue());
        }
        categoryToggle.put(category, show);
        updatePlugins();
    }    
    
	/**
	 * Show a dialog that lists the loaded plugins
	 */
    public void updatePlugins() {

    	List<String> cats = new ArrayList<String>();
    	
    	Hashtable<String, StringBuffer> catBuffs = 
    		new Hashtable<String, StringBuffer>();
    	
    	boolean firstTime = false;
    	if (pluginWindow == null) {
    		makePluginDialog();
    		createPluginList();
    		firstTime = true;
    	}

    	StringBuffer loadedBuff =
	         new StringBuffer(
	             "<b>Installed Plugins</b><br><table width=\"100%\" border=\"0\">");
    	
    	for (Plugin plugin : Plugin.plugins) {
    		StringBuffer catBuff = catBuffs.get(plugin.category);
    		
    		if (catBuff == null) {
    			catBuff = new StringBuffer();
    			cats.add(plugin.category);
    			catBuffs.put(plugin.category, catBuff);
    		}
    		
    		String prefix = "";
    		String encodedPath;
    	
    		if (plugin.file != null) {
    			encodedPath = encode(plugin.file.toString());
    			prefix =
	                 "&nbsp;<a href=\"jython:idv.getPluginManager().listPlugin('"
	                 + encodedPath
	                 + "');\"><img src=\"idvresource:/auxdata/ui/icons/FindAgain16.gif\" border=\"0\"></a>";
    		} else {
    			encodedPath = encode(plugin.url.toString());
    			prefix      = "&nbsp;";
    		}

    		prefix =
	             prefix
	             + "<a href=\"jython:idv.getPluginManager().importPlugin('"
	             + encodedPath
	             + "');\"><img alt='Import Plugin into Plugin Creator' src=\"idvresource:/auxdata/ui/icons/DocumentOpen16.png\" border=\"0\"></a>";
    		String sizeString = "";
    		if (plugin.size != null) {
    			int s = new Integer(plugin.size).intValue();
    			sizeString = " <b>" + (s / 1000) + "KB</b>";
    		}

    		catBuff.append(
	             "<tr valign=\"top\"><td align=\"right\" width=\"60\">"
	             + prefix + "</td><td width=\"30%\">" + plugin.name
	             + "</td><td align=\"right\">" + sizeString + "</td>");

    		String installHtml =
	             "<a href=\"jython:idv.getPluginManager().installPlugin('"
	             + plugin.url + "')\">";
    		catBuff.append("<td width=\"30%\">");
    		if ( !plugin.versionOk) {
    			catBuff.append("<b>requires IDV version: " + plugin.version
	                            + "</b>");
    		} else if (plugin.deleted) {
	             catBuff.append("<b>removed</b>");
    		} else if (plugin.installed) {
    			String extra = "";
    			if (plugin.hasOriginal) {
    				extra = installHtml + "Reinstall</a>";
    			}
    			catBuff.append(
	                 "<a href=\"jython:idv.getPluginManager().removePlugin('"
	                 + plugin.file + "')\">" + "Uninstall"
	                 + "</a>&nbsp;&nbsp;" + extra);
    			loadedBuff.append("<tr><td width=\"60\">" + prefix
	                               + "</td><td>" + plugin.category + "&gt;"
	                               + plugin.name + "</td><td>");

    			loadedBuff.append(
	                 "<a href=\"jython:idv.getPluginManager().removePlugin('"
	                 + plugin.file + "')\">" + "Uninstall"
	                 + "</a>&nbsp;&nbsp;" + extra);
    			loadedBuff.append("</td></tr>");
    		} else {
    			catBuff.append(installHtml + "Install</a>\n");
    		}
    		catBuff.append("</td><td width=\"30%\">" + plugin.desc
	                        + "</td></tr>");
    	}
    	
    	StringBuffer sb =
	         new StringBuffer(
	             "<b>Available Plugins</b><br><table border=\"0\" width=\"100%\">\n");
    	
    	for (String category : cats) {
    	
    		StringBuffer catBuff = catBuffs.get(category);
    		
    		Boolean show = categoryToggle.get(category);
    		
    		if (show == null) {
    			show = new Boolean(false);
    			categoryToggle.put(category, show);
    		}
    		
    		String toggleHref =
	             "<a href=\"jython:idv.getPluginManager().toggleCategory('"
	             + category + "')\">";
    		
    		String catToShow = StringUtil.replace(category, " ", "&nbsp;");
    		
    		if ((show == null) || show.booleanValue()) {
    			sb.append(
	                 "<tr><td colspan=\"3\">" + toggleHref
	                 + "<img src=\"idvresource:/auxdata/ui/icons/CategoryOpen.gif\"  border=\"0\"></a>&nbsp; <b><span style=\"xxxxfont-size:18\">"
	                 + catToShow + "</span></b></td></tr>");
    			sb.append(catBuff.toString());
    		} else {
    			sb.append(
	                 "<tr><td colspan=\"3\">" + toggleHref
	                 + "<img src=\"idvresource:/auxdata/ui/icons/CategoryClosed.gif\" border=\"0\"></a>&nbsp; <b><span style=\"xxxxfont-size:18\">"
	                 + catToShow + "</span></b></td></tr>");
    		}
    	}
    	sb.append("</table>");

    	loadedBuff.append("</table>");

    	loadedPluginEditor.setText(loadedBuff.toString());
    	loadedPluginEditor.invalidate();
    	loadedPluginEditor.repaint();

    	availablePluginEditor.setText(sb.toString());
    	availablePluginEditor.invalidate();
    	availablePluginEditor.repaint();
    	if (firstTime) {
    		GuiUtils.showDialogNearSrc(null, pluginWindow);
    	} else {
    		pluginWindow.setVisible(true);
    	}
    }
    
    /**
     * Prompt for a plugin url and install it.
     */
    @Override public void installPluginFromUrl() {
        String filename = "";
        while (true) {
            filename = GuiUtils.getInput(
                "Please enter the URL to an McIDAS-V plugin JAR file", "URL: ",
                filename);
            if ((filename == null) || (filename.trim().length() == 0)) {
                return;
            }
            try {
                installPlugin(filename, true);
                updatePlugins();
                LogUtil.userMessage(
                    "You will need to restart McIDAS-V for this plugin to take effect");
                return;
            } catch (Throwable exc) {
                logException("Installing plugin", exc);
            }
        }
    }
    
    /**
     * Class Plugin holds info about all of the loaded and available plugins
     *
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    private static class Plugin {

    	/** mapping */
    	static Hashtable<String, Plugin> pathToPlugin = 
    		new Hashtable<String, Plugin>();

    	/** list */
    	static List<Plugin> plugins = new ArrayList<Plugin>();

    	/** file for installed plugins */
    	File file;

    	/** state */
    	boolean deleted = false;

    	/** state */
    	boolean installed = false;

    	/** name */
    	String name;

    	/** desc */
    	String desc;

    	/** url or original file name */
    	String url;

    	/** category */
    	String category;

    	/** Is version ok */
    	boolean versionOk = true;

    	/** The version */
    	double version = Double.MAX_VALUE;

    	/** DO we have a version of this plugin on the web site */
    	boolean hasOriginal = false;

    	/** The size string from the xml */
    	String size;

    	/**
    	 * ctor
    	 *
    	 * @param name name
    	 * @param desc desc
    	 * @param url url
    	 * @param category cat
    	 */
    	public Plugin(String name, String desc, String url, String category) {
    		this.name        = name;
    		this.desc        = desc;
    		this.url         = url;
    		this.hasOriginal = true;
    		this.category    = category;
    		pathToPlugin.put(url, this);
    		plugins.add(this);
    	}

    	/**
    	 * ctor for installed plugins
    	 *
    	 * @param f file
    	 */
    	public Plugin(File f) {
    		this.file = f;
    		this.url = decode(f);
    		Plugin p = pathToPlugin.get(url);
    		installed = true;
    		if (p != null) {
    			this.size        = p.size;
    			this.name        = p.name;
    			this.desc        = p.desc;
    			this.category    = p.category;
    			this.hasOriginal = p.hasOriginal;
    			int index = plugins.indexOf(p);
    			plugins.remove(p);
    			plugins.add(index, this);
    		} else {
    			this.category = "Miscellaneous";
    			this.name     = IOUtil.getFileTail(url);
    			this.desc     = "";
    			plugins.add(this);
    		}
    		pathToPlugin.put(url, this);
    	}

    	/**
    	 * set state
    	 */
    	public void install() {
    		deleted   = false;
    		installed = true;
    	}

    	/**
    	 * set state
    	 */
    	public void delete() {
    		installed = false;
    		deleted   = true;
    	}

    	/**
    	 * tostring
    	 *
    	 * @return string
    	 */
    	public String toString() {
    		return name + " installed:" + installed + " deleted:" + deleted;
    	}
    }
}
