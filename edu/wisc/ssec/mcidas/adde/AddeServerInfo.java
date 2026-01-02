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

package edu.wisc.ssec.mcidas.adde;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;

import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import edu.wisc.ssec.mcidas.AreaDirectoryList;
import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.McIDASException;
import edu.wisc.ssec.mcidasv.servermanager.RemoteAddeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * All things related to ADDE servers, groups and datasets
 * Certain requests are dependent on appropriate prior requests.
 * E.g., must set server, then group, before you can request
 * File Format.
 *
 * @author  tomw
 * @version 0.2
 */

public class AddeServerInfo extends Object {
  
  private String[] serverList;
  private String selectedServer = null;
  private boolean hasServers = false;
  private String dataType = null;

  private String[] bandNames;
  private boolean hasBandNames = false;

  private String selectedGroup = null;
  private boolean hasGroup = false;

  private String selectedDataset = null;
  private boolean hasDataset = false;
  private AreaDirectory [][]dirs;

  private Vector table, groups;
  private String status="OK";

  private String userproj = null;
  private String DATEFORMAT = "yyyy-MM-dd / HH:mm:ss";
  
  private boolean debug = false;

  private boolean isArchive = false;
  private String archiveDate = null;

  private static final Logger logger = LoggerFactory.getLogger(AddeServerInfo.class);


  /** 
   * Creates new AddeServerInfo. This collects information about
   * ADDE server(s) -- their groups, datasets, and date-times (right
   * now only image date-time implemented).  This is a helper class
   * for anything else that needs to get these information.
   * 
   * The candidate list of ADDE servers should be obtained from a
   * yet-to-be-identified "well-known list", and made available
   * in this class.
   */
  
  public AddeServerInfo() {
    // go find the list of well-known servers ?
    this(null);
  }
    
  /** 
   * The non-default parameter is a list of of ADDE servers
   *
   * @param l a list of ADDE servers that can be used in lieu
   * of automatically obtaining it.
   *
   */
  
  public AddeServerInfo(String[] l) {
    // well known list of ADDE servers
	// XXX TJJ - I don't like this and would prefer to force a valid
	// server name into the constructor, and fail all requests otherwise
    String[] list = l;
    if (list == null) {
      list = new String[] {
                "adde.ucar.edu",
                "atm.ucar.edu"
    		 };
    }
    serverList = new String[list.length];
    for (int i=0; i<list.length; i++) {
      serverList[i] = list[i];
    }
    hasServers = true;
  }
    
  /** 
   * get a list of servers
   *
   * @return names of server hosts
   */
  
  public String[] getServerList() {
    if (hasServers) {
      return serverList;
    } else {
      return null;
    }
  }

  /** 
   * get name of the server that has been selected and for
   *  which all group and dataset info is currently valid for
   *
   * @return name of server host
   */
  
  public String getSelectedServer() {
    return selectedServer;
  }

  /**
   * define which server to select, and the type of ADDE data group
   * (image, point, grid, text) to get group and descriptor info for
   * <p>
   * This is the workhorse of the code.
   *
   * @param s     the name of the ADDE server to be selected
   * @param type  the type of data to select.
   * @return status code: 0=ok, -1=invalid accounting, -2=didn't get metadata
   */
  
  public int setSelectedServer(String s, String type) {
    selectedServer = s.trim();
    int istatus = 0;
    dataType = type.trim();
    groups = new Vector();
    status = "Failed to get PUBLIC.SRV file from from server "+s+".";

    boolean pubServPresent = true;
    try {
       // go get the PUBLIC.SRV file from the server...
       String req = "adde://"+selectedServer+"/text?file=PUBLIC.SRV";
      if (userproj != null) {
        String req2 = req+"&"+userproj+"&version=1";
        req = req2;
      }
      ReadTextFile rtf = new ReadTextFile(req);

    } catch (Exception e) {
      pubServPresent = false;
    }

    try {

      // go get the PUBLIC.SRV file from the server...
      String req = "adde://"+selectedServer+"/text?file=PUBLIC.SRV";

      if (! pubServPresent) {
        req = "adde://" + selectedServer + "/datasetinfo?" + "&type=" + type;
      }

      if (userproj != null) {
          String req2 = req + "&" + userproj + "&version=1";
          req = req2;
      }

      if (selectedGroup != null) {
          String req2 = req + "&GROUP=" + selectedGroup;
          req = req2;
      }

      ReadTextFile rtf = new ReadTextFile(req);

      // see if accounting data is required but not provided...
      if (rtf.getStatusCode() == -3) {
          return -1;
      }

      /* do we really want to do this????? */

      // if that fails, try to get RESOLV.SRV, but keep it hidden...
      if (!rtf.getStatus().equals("OK")) { // try again
        req = "adde://"+selectedServer+"/text?file=RESOLV.SRV";

        if (userproj != null) {
          String req2 = req+"&"+userproj+"&version=1";
          req = req2;
        }

        if (debug) System.out.println("2ndReq:"+req);
        rtf = new ReadTextFile(req);
        if (debug) System.out.println("Status from 2ndRTF="+rtf.getStatus());

      }
      
      table = rtf.getText();

      status = "Failed to locate required information on server "+s+".";
      
      // look at each table member and pull out info only
      // when the TYPE='dataType'
      for (int i=0; i<table.size(); i++) {
        String a = (String) table.elementAt(i);
        if (debug) System.out.println("Table: "+a);
        StringTokenizer st = new StringTokenizer(a,",");
        int num = st.countTokens();
        String[] tok = new String[num];
        String[] val = new String[num];
        int indexType = -1;
        int indexN1 = -1;
        int indexN2 = -1;
        int indexK = -1;
        int indexC = -1;

        // number of items in the record
        for (int j=0; j<num; j++) {
          String p = st.nextToken();
          // simple token=value within each group
          int x = p.indexOf("=");
          if (x < 0) continue;
          tok[j] = p.substring(0,x);
          val[j] = p.substring(x+1).trim();
          // flags to indicate found needed keys
          if (tok[j].equalsIgnoreCase("type")) indexType = j;
          if (tok[j].equalsIgnoreCase("N1")) indexN1 = j;
          if (tok[j].equalsIgnoreCase("N2")) indexN2 = j;
          if (tok[j].equalsIgnoreCase("K")) indexK = j;
          if (tok[j].equalsIgnoreCase("C")) indexC = j;
        }

        // skip if: a) no =, b> not TYPE type, c) element missing
        if (debug) {
          System.out.println("indexType = "+indexType+
            " dataType="+dataType+" indexN1,N2,C="+indexN1+" "+
            indexN2+" "+indexC);
        }
        
        if (indexType < 0) continue;
        if (!val[indexType].equalsIgnoreCase(dataType)) continue;
        if (indexN1 < 0 || indexN2 < 0) continue;
        // If no Comment field, just use N2 (kludge)
        if (indexC < 0) indexC = indexN2;

        boolean hit = false;

        // search to see if this group has been encountered before.
        // if so, just append descr; otherwise make a new entry
        for (int j=0; j<groups.size(); j++) {
          Vector vg = (Vector)groups.elementAt(j);

          // element: 0=groupname, 1=Vector of datasets, 2=Vector of descrs
          String g = (String)vg.elementAt(0);
          if (g.equalsIgnoreCase(val[indexN1])) {
            hit = true;
            Vector v = (Vector)vg.elementAt(1);
            v.addElement(val[indexN2]);
            v = (Vector)vg.elementAt(2);
            v.addElement(val[indexC]);
            if (vg.size() >= 4) {
            	v = (Vector)vg.elementAt(3);
            	v.addElement(val[indexK]);
            }
          }
        }

        // if needed, make a new entry
        if (!hit) {
          Vector v = new Vector();
          v.addElement(val[indexN1]);
          Vector v2 = new Vector();
          v2.addElement(val[indexN2]);
          Vector v3 = new Vector();
          v3.addElement(val[indexC]);
          Vector v4 = null;
          if (indexK > 0) {
        	  v4 = new Vector();
        	  v4.addElement(val[indexK]); 
          }
          v.addElement(v2);
          v.addElement(v3);
          if (v4 != null) v.addElement(v4);
          groups.addElement(v);
        }
      }

      String reqBand = "adde://"+selectedServer+"/text?file=SATBAND";

      if (userproj != null) {
        String reqBand2 = reqBand+"&"+userproj+"&version=1";
        reqBand = reqBand2;
      }

      if (debug) System.out.println("ReqBand:"+reqBand);

      ReadTextFile rtfBand = new ReadTextFile(reqBand);
      if (debug) System.out.println("Status from RTFBand="+rtfBand.getStatus());

      
      Vector vBand = null;

      if (rtfBand.getStatus().equals("OK")) {
        vBand = rtfBand.getText();
        int num = vBand.size();
        bandNames = new String[num];
        for (int i=0; i<num; i++) {
          bandNames[i] = (String) vBand.elementAt(i);
          if (debug) System.out.println("band = "+bandNames[i]);
        }
        hasBandNames = true;
        
      }

      status = "ADDE group & dataset information retrieved from server "+s+".";
      istatus = 0;

    } catch (Exception e) {e.printStackTrace(); return -2;}
    
    return istatus;
  }
  
  /** 
   * get a list of all groups valid for this server
   *
   * @return array of group ids
   */
  
  public String[] getGroupList() {

    int num = groups.size();
    String[] sg = new String[num];
    for (int i=0; i < num; i++) {
      Vector v = (Vector) groups.elementAt(i);
      sg[i] = (String) v.elementAt(0);
    }
    status = "List of groups on server " + selectedServer + " obtained.";
    return sg;
    
  }
    
  /** 
   * get a list of all datasets valid for this server and
   * the designated group. 
   *
   * @return array of dataset tags
   *
   */
  
  public String[] getDatasetList() {
    int num = groups.size();
    boolean autoUpcase = Boolean.getBoolean("adde.auto-upcase");
    boolean match;
    for (int i=0; i<num; i++) {
      Vector v = (Vector)groups.elementAt(i);
      String g = (String)v.elementAt(0);
      if (autoUpcase) {
        match = g.equalsIgnoreCase(selectedGroup);
      } else {
        match = g.equals(selectedGroup);
      }
      if (match) {
        Vector ds = (Vector)v.elementAt(1);
        int numds = ds.size();
        String[] sdl = new String[numds];
        for (int j=0; j<numds; j++) {
          sdl[j] = (String)ds.elementAt(j);
        }
        status = "Dataset list for server "+selectedServer+
                               " and group "+selectedGroup+" obtained.";
        return sdl;
      }
    }
    status = "Dataset list for server "+selectedServer+
                            " and group "+selectedGroup+" not found.";
    return null;
  }
  
  /** 
   * get a list of all dataset descriptions for this server and
   * the designated group 
   *
   * @return array of dataset descriptors
   *
   */
  
  public String[] getDatasetListDescriptions() {
    boolean autoUpcase = Boolean.getBoolean("adde.auto-upcase");
    boolean match;
    int num = groups.size();
    for (int i=0; i<num; i++) {
      Vector v = (Vector)groups.elementAt(i);
      String g = (String)v.elementAt(0);
      if (autoUpcase) {
          match = g.equalsIgnoreCase(selectedGroup);
        } else {
          match = g.equals(selectedGroup);
        }
      if (match) {
        Vector dsd = (Vector)v.elementAt(2);
        int numdsd = dsd.size();
        String[] sdld = new String[numdsd];
        for (int j=0; j<numdsd; j++) {
          sdld[j] = (String)dsd.elementAt(j);
        }
        status = "Dataset list for server "+selectedServer+
                              " and group "+selectedGroup+" obtained.";
        return sdld;
      }
    }
    status = "Dataset list for server "+selectedServer+
                               " and group "+selectedGroup+" not found.";
    return null;
  }
  
  /** 
   * get the valid date-time list for the selected server/group/dataset.
   * Note that you must 'setSelectedGroup()' and 'setSelectedDataset()'
   * prior to using this method.
   *
   * [Also, Don Murray at Unidata wrote the original - thanks!]
   *
   * @return list of date-times ("yy-MM-dd / hh:mm:ss" format)
   * or the string "No data available"
   *
   */
  
  public String[] getDateTimeList() {
    status = "Trying to get date-times for "+selectedGroup+
                                    " from server "+selectedServer;
    if (!hasGroup || !hasDataset) return null;
    StringBuffer addeCmdBuff = 
      new StringBuffer("adde://"+selectedServer+
                               "/imagedir?group=" + selectedGroup);
    addeCmdBuff.append("&descr=");
    addeCmdBuff.append(selectedDataset);
    // if user/proj supplied, then put them in here...
    if (userproj != null) addeCmdBuff.append("&"+userproj);
    addeCmdBuff.append("&pos=all&version=1");

    if (isArchive && archiveDate != null) {
      addeCmdBuff.append("&DAY="+archiveDate);
    }

    if (debug) System.out.println("cmd:"+addeCmdBuff.toString());

    String[] times = {"No data available"};
    
    try {
      AreaDirectoryList adir = 
        new AreaDirectoryList(addeCmdBuff.toString());
      dirs = adir.getSortedDirs();
      int numTimes = dirs.length;
      times = new String[numTimes];
      
      SimpleDateFormat sdf = new SimpleDateFormat();
      sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
      sdf.applyPattern(DATEFORMAT);
      for (int i=0; i < numTimes; i++)
      {
        times[i] = 
          sdf.format( 
              dirs[i][0].getNominalTime(), 
              new StringBuffer(), 
              new FieldPosition(0)).toString();
      }
    }
    catch (McIDASException e) {status = "Error getting times";}
    return times;
  }
  
  /** 
   * Get the File Format (AREA, TEXT, NEXR, etc.) for the
   * currently selected group.
   *
   * @return file format for currently selected group
   *
   */
  
   public String getFileFormat() {
     int num = groups.size();
     boolean autoUpcase = Boolean.getBoolean("adde.auto-upcase");
     boolean match;
     for (int i=0; i<num; i++) {
       Vector v = (Vector) groups.elementAt(i);
       String g = (String) v.elementAt(0);
       if (autoUpcase) {
         match = g.equalsIgnoreCase(selectedGroup);
       } else {
         match = g.equals(selectedGroup);
       }
       if (match) {
    	   if (debug) System.out.println("VECTOR SIZE: " + v.size());
    	   if (v.size() >= 4) {
    		   Vector ffv = (Vector) v.elementAt(3);
    		   String ff = (String) ffv.elementAt(0);
    		   if (ff != null) {
    			   status = "File Format for server " + selectedServer +
    			   " and group " + selectedGroup + " obtained.";
    			   return ff;
    		   }
    	   }
       }
     }
     status = "File Format for server "+selectedServer+
                             " and group "+selectedGroup+" not found.";
     return null;
   }
   
  /** 
   * get the sorted list of AreaDirectory objects
   * 
   * @return list of directories
   */
   
  public AreaDirectory[][] getAreaDirectories() {
    return dirs;
  }
  
  /** 
   * identify the selected group
   *
   * @param g the name of the group to select
   */
  
  public void setSelectedGroup(String g) {
    selectedGroup = g;
    hasGroup = true;
  }
  
  /** 
   * identify the selected dataset
   *
   * @param d the name of the dataset/descr to select
   */
  
  public void setSelectedDataset(String d) {
    selectedDataset = d;
    hasDataset = true;
  }

  public void setIsArchive(boolean flag) {
    isArchive = flag;
  }

  public boolean getIsArchive() {
    return isArchive;
  }

  public void setArchiveDate(String d) {
    archiveDate = d;
  }
 
  public String getArchiveDate() {
    return archiveDate;
  }
  
  /** 
   * return the bandNames text
   *
   * @return array of text of the SATBAND data fetched from selectedServer
   *
   */

  public String[] getBandNames() {
    if (hasBandNames) {
      return bandNames;
    } else {
      return null;
    }
  }

  /**
   * Not used??
   */
  
  public String getRequestString(int reqestType) {
    return null;
    // requestType  = static ints IMAGE, DIR, etc
  }
  
  /** 
   * set the userID and proj number if required for transactions,
   * as required by servers that use accounting.
   *
   * @param up the user/project number request string in 
   * the form "user=tom&proj=1234"
   *
   */
  
  public void setUserIDandProjString(String up) {
    userproj = up;
    return;
  }

  /** 
   * get the status of the last request
   *
   * @return words describing last transaction
   */
  
  public String getStatus() {
    return status;
  }

  /** 
   * For testing purposes. Edit server, and run the class as if it
   * were an application.
   */
  
  public static void main(String[] args) {
	  
	System.out.println("AddeServerInfo unit test begin...");
    AddeServerInfo asi = new AddeServerInfo();
    
    // NOTE!
    // if you are testing this class, edit below with your server info!
    asi.setUserIDandProjString("user=ZZZZ&proj=YYYY");
    int sstat = asi.setSelectedServer("some-server.ssec.wisc.edu", "image");

    System.out.println("Status = " + asi.getStatus() + " code=" + sstat);
    String[] a = asi.getGroupList();
    System.out.println("Status = " + asi.getStatus());
    for (int i=0; i < a.length; i++) {
      System.out.println("group = " + a[i]);
      // set the selected group
      asi.setSelectedGroup(a[i]);
      System.out.println(" File Format: " + asi.getFileFormat());
      String[] b = asi.getDatasetList();
      String[] c = asi.getDatasetListDescriptions();

      for (int k=0; k < b.length; k++) {
    	// datasets and their descriptions  
        System.out.println("    " + b[k] + " == " + c[k]);

        if (i==0 && k==0) {
          // set the selected Dataset, and fetch its date-time info
          asi.setSelectedDataset(b[k]);
          String[] dt = asi.getDateTimeList();
          for (int m=0; m < dt.length; m++) {
            System.out.println("DateTime = " + dt[m]);
          }
        }
      }
      
    }
   
    System.out.println("Status = " + asi.getStatus());
    
  }

}
