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

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.LogUtil;

import java.util.Hashtable;


import java.net.InetAddress;
import java.net.Socket;



/**
 * This provides http based access to a stack trace and enables the user to shut down McIDAS-V.
 * This only is responsive to incoming requests from localhost
 * the urls this provides are:
 * http://localhost:<port>/stack.html
 * http://localhost:<port>/info.html
 * http://localhost:<port>/shutdown.html
 *
 * @author IDV development team
 */
public class McIDASVMonitor extends HttpServer {

    private IntegratedDataViewer idv;

    /** The localhost */
    private InetAddress localHost;

    public McIDASVMonitor(IntegratedDataViewer idv, int port) {
        super(port);
        this.idv = idv;
    }

    /**
     * Make the handler for this request. Check if the client is coming from localhost
     * if not then return null.
     *
     * @param socket incoming socket
     * @return handler or null
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        if (localHost == null) {
            localHost = InetAddress.getLocalHost();
        }
        InetAddress inet = socket.getInetAddress();
        if (! (inet.getHostAddress().equals("127.0.0.1") || inet.getHostName().equals("localhost"))) {
            return null;
        }
        return new MonitorRequestHandler(idv, this, socket);
    }

    /**
     * Class OneInstanceRequestHandler the handler
     *
     *
     * @author IDV Development Team
     * @version $Revision$
     */
    public class MonitorRequestHandler extends HttpServer.RequestHandler {

        /** The idv */
        IntegratedDataViewer idv;
        
        /** The socket */
        Socket mysocket;

        /**
         * ctor
         *
         * @param idv the idv
         * @param server the server
         * @param socket the socket we handle the connection of
         *
         * @throws Exception On badness
         */
        public MonitorRequestHandler(IntegratedDataViewer idv,
                                         HttpServer server, Socket socket)
                throws Exception {
            super(server, socket);
            this.idv = idv;
            this.mysocket = socket;
        }

        /**
         * Try to trap the case where the socket doesn't contain any bytes
         * This can happen when mcservl connects to ping
         * Prevents an infinite loop in HttpServer
         */
        public void run() {
            try {
            	int availableBytes = mysocket.getInputStream().available();
            	if (availableBytes != 0) {
            		super.run();
            	}
            	mysocket.close();
            } catch (Exception e) {
            	System.err.println("HTTP server error");
            }
        }
        
        private void decorateHtml(StringBuffer sb) throws Exception {
            String header = "<h1>McIDAS-V HTTP monitor</h1><hr>" +
            	"<a href=stack.html>Stack Trace</a>&nbsp;|&nbsp;" +
                "<a href=info.html>System Information</a>&nbsp;|&nbsp;" +
                "<a href=shutdown.html>Shut Down</a><hr>";
            writeResult(true,  header+sb.toString(),"text/html");
        }

        /**
         *
         * @param path url path. ignored.
         * @param formArgs form args
         * @param httpArgs http args
         * @param content content. unused.
         *
         * @throws Exception On badness
         */
        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
                throws Exception {
        	if (path.equals("/stack.html")) {
        		StringBuffer stack = LogUtil.getStackDump(true);
        		decorateHtml(stack);
        	}
        	else if (path.equals("/info.html")) {
        		StringBuffer extra   = idv.getIdvUIManager().getSystemInfo();
        		extra.append("<H3>Data Sources</H3>");
        		extra.append("<div style=\"margin-left:20px;\">");
        		extra.append(idv.getDataManager().getDataSourceHtml());
        		extra.append("</div>");
        		extra.append(idv.getPluginManager().getPluginHtml());
        		extra.append(idv.getResourceManager().getHtmlView());
        		decorateHtml(extra);
        	}
        	else if (path.equals("/shutdown.html")) {
        		decorateHtml(new StringBuffer("<a href=\"reallyshutdown.html\">Shut down McIDAS-V</a>"));
        	}
        	else if (path.equals("/reallyshutdown.html")) {
        		writeResult(true, "McIDAS-V is shutting down","text/html");
        		System.exit(0);
        	}
        	else if (path.equals("/") || path.equals("/index.html")) {
        		decorateHtml(new StringBuffer(""));
        	}
        	else {
        		decorateHtml(new StringBuffer("Unknown url:" + path));
        	}
        }
    }

}
