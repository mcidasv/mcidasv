/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2024
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

package edu.wisc.ssec.mcidasv;

import java.net.InetAddress;
import java.net.Socket;

import java.util.Hashtable;

import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.LogUtil;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.slf4j.LoggerFactory;

import edu.wisc.ssec.mcidasv.jython.Console;

/**
 * This provides http based access to a stack trace and enables the user to
 * shut down McIDAS-V.
 *
 * This only is responsive to incoming requests from localhost.
 * The only URLs available are the following:
 *
 * http://localhost:&lt;port&gt;/stack.html
 * http://localhost:&lt;port&gt;/info.html
 * http://localhost:&lt;port&gt;/shutdown.html
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
     * Make the handler for this request. Check if the client is coming from
     * localhost, if not then return null.
     *
     * @param socket Incoming socket.
     * @return handler or {@code null}.
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        if (localHost == null) {
            localHost = InetAddress.getLocalHost();
        }
        InetAddress inet = socket.getInetAddress();
        if (! (inet.getHostAddress().equals("127.0.0.1") ||
               inet.getHostName().equals("localhost")))
        {
            return null;
        }
        return new MonitorRequestHandler(idv, this, socket);
    }

    public class MonitorRequestHandler extends HttpServer.RequestHandler {

        IntegratedDataViewer idv;

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
                throws Exception
        {
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
                "<a href=shutdown.html>Shut Down</a>&nbsp;|&nbsp;" +
                "<a href=jython.html>Jython</a>&nbsp;|&nbsp;" +
                "<a href=trace.html>Enable TRACE logging</a><hr>";
            writeResult(true,  header+sb.toString(), "text/html");
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
                throws Exception
        {
            if (path.equals("/stack.html")) {
                StringBuffer stack = LogUtil.getStackDump(true);
                decorateHtml(stack);
            } else if (path.equals("/info.html")) {
                StringBuffer extra   = idv.getIdvUIManager().getSystemInfo();
                extra.append("<H3>Data Sources</H3>");
                extra.append("<div style=\"margin-left:20px;\">");
                extra.append(idv.getDataManager().getDataSourceHtml());
                extra.append("</div>");
                extra.append(idv.getPluginManager().getPluginHtml());
                extra.append(idv.getResourceManager().getHtmlView());
                decorateHtml(extra);
            } else if (path.equals("/shutdown.html")) {
                decorateHtml(new StringBuffer("<a href=\"reallyshutdown.html\">Shut down McIDAS-V</a>"));
            } else if (path.equals("/reallyshutdown.html")) {
                writeResult(true, "McIDAS-V is shutting down","text/html");
                System.exit(0);
            } else if (path.equals("/jython.html")) {
                StringBuffer extra = new StringBuffer("Try to show <a href=shell.html>Jython Shell</a> in your McV session.<br/><br/>");
                extra.append("Try to <a href=console.html>show alternative Jython console</a><br/>");
                extra.append("If the event dispatch queue is not functioning, the console will probably not appear.");
                decorateHtml(extra);
            } else if (path.equals("/shell.html")) {
                StringBuffer extra = new StringBuffer("Try to show Jython Shell in your McV session.<br/><br/>");
                extra.append("Try to <a href=console.html>show alternative Jython console</a><br/>");
                extra.append("If the event dispatch queue is not functioning, the console will probably not appear.");
                decorateHtml(extra);
                McIDASV.getStaticMcv().getJythonManager().createShell();
            } else if (path.equals("/trace.html")) {
                enableTraceLogging();
            } else if (path.equals("/console.html")) {
                StringBuffer extra = new StringBuffer("Try to show <a href=shell.html>Jython Shell</a> in your McV session.<br/><br/>");
                extra.append("Try to show alternative Jython console</a><br/>");
                extra.append("If the event dispatch queue is not functioning, the console will probably not appear.");
                decorateHtml(extra);
                Console.testConsole(false);
            } else if (path.equals("/") || path.equals("/index.html")) {
                decorateHtml(new StringBuffer(""));
            } else {
                decorateHtml(new StringBuffer("Unknown url:" + path));
            }
        }

        private void enableTraceLogging() throws Exception {
            Logger rootLogger = (Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            Level currentRootLevel = rootLogger.getLevel();
            Level currentEffectiveLevel = rootLogger.getEffectiveLevel();
            rootLogger.setLevel(Level.TRACE);
            StringBuffer extra = new StringBuffer(512);
            extra.append("Logging level set to TRACE<br/><br/>")
                 .append("Previous level: ").append(currentRootLevel).append("<br/>")
                 .append("Previous <i>effective</i> level: ").append(currentEffectiveLevel).append("<br/>");
            decorateHtml(extra);
        }
    }
}
