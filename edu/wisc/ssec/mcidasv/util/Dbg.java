package edu.wisc.ssec.mcidasv.util;

import java.util.ArrayList;
import java.util.List;

import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.idv.ui.IdvComponentGroup;
import ucar.unidata.idv.ui.IdvComponentHolder;
import ucar.unidata.idv.ui.WindowInfo;

public class Dbg {

	public static void compGroup(final IdvComponentGroup g) {
		compGroup(g, 0);
	}
	
	public static void compGroup(final IdvComponentGroup g, final int level) {
		p("Comp Group", level);
		p("  name=" + g.getName(), level);
		p("  id=" + g.getUniqueId(), level);
		p("  layout=" + g.getLayout(), level);
		p("  comp count=" + g.getDisplayComponents().size() + ": ", level);
		List<IdvComponentGroup> gs = new ArrayList<IdvComponentGroup>();
		List<IdvComponentHolder> hs = new ArrayList<IdvComponentHolder>();
		for (Object comp : g.getDisplayComponents()) {
			if (comp instanceof IdvComponentHolder)
				compHolder((IdvComponentHolder)comp, level+1);
			else if (comp instanceof IdvComponentGroup)
				compGroup((IdvComponentGroup)comp, level+1);
			else
				p("    umm=" + comp.getClass().getName(), level);
		}
	}
	
	public static void compHolder(final IdvComponentHolder h, final int level) {
		p("Comp Holder", level);
		p("  cat=" + h.getCategory(), level);
		p("  name=" + h.getName(), level);
		p("  id=" + h.getUniqueId(), level);
		if (h.getViewManagers() == null) {
			System.err.println("  null vms!");
			return;
		}
		p("  vm count=" + h.getViewManagers().size() + ": ", level);
		for (ViewManager vm : (List<ViewManager>)h.getViewManagers()) {
			p("    " + vmType(vm) + "=" + vm.getViewDescriptor().getName(), level);
		}
	}

	public static List<ViewManager> findvms(final List<WindowInfo> windows) {
		List<ViewManager> vms = new ArrayList<ViewManager>();
		
		for (WindowInfo window : windows) {
			for (IdvComponentHolder h : CompGroups.getComponentHolders(window)) {
//				for (ViewManager vm : (List<ViewManager>)h.getViewManagers()) {
//					
//				}
				if (h.getViewManagers() != null)
					vms.addAll((List<ViewManager>)h.getViewManagers());
				else
					System.err.println(h.getUniqueId() + " has no vms!");
			}
		}
		
		for (ViewManager vm : vms) 
			System.err.println("vm=" + vm.getViewDescriptor().getName());
		
		return vms;
	}
	
	private static String vmType(final ViewManager vm) {
		if (vm instanceof MapViewManager)
			if (((MapViewManager)vm).getUseGlobeDisplay())
				return "Globe";
			else
				return "Map";
		return "Other";
	}
	
	private static String pad(final String str, final int pad) {
		char[] padding = new char[pad*2];
		for (int i = 0; i < pad*2; i++)
			padding[i] = ' ';
		return new String(padding).concat(str);
	}
	
	private static void p(final String str, final int padding) {
		System.err.println(pad(str, padding));
	}
	
}
