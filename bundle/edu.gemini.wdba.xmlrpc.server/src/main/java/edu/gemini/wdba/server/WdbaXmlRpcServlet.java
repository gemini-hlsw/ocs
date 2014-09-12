package edu.gemini.wdba.server;

import edu.gemini.wdba.exec.ExecXmlRpcHandler;
import edu.gemini.wdba.xmlrpc.IExecXmlRpc;
import edu.gemini.wdba.xmlrpc.ITccXmlRpc;
import edu.gemini.wdba.xmlrpc.ISessionXmlRpc;
import edu.gemini.wdba.tcc.TccXmlRpcHandler;
import edu.gemini.wdba.session.SessionXmlRpcHandler;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcHandlerMapping;
import org.apache.xmlrpc.server.XmlRpcServerConfigImpl;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

import javax.servlet.ServletConfig;

/**
 * Gemini Observatory/AURA
 * $Id: WdbaXmlRpcServlet.java 874 2007-06-04 17:06:28Z gillies $
 */
public final class WdbaXmlRpcServlet extends XmlRpcServlet {
    /**
     * Creates a new handler mapping, setting the handler to the Horizons XML-RPC service
     *
     * @return a new <code>XmlRpcHandlerMapping</code> configured with one handler for
     *         the wdba service
     * @throws XmlRpcException if there are problems adding the handler
     */
    protected XmlRpcHandlerMapping newXmlRpcHandlerMapping() throws XmlRpcException {
        PropertyHandlerMapping mapping = new PropertyHandlerMapping();
        System.out.println("Adding mapping: " + IExecXmlRpc.NAME);
        mapping.addHandler(IExecXmlRpc.NAME, ExecXmlRpcHandler.class);
        System.out.println("Adding mapping: " + ITccXmlRpc.NAME);
        mapping.addHandler(ITccXmlRpc.NAME, TccXmlRpcHandler.class);
        System.out.println("Adding mapping: " + ISessionXmlRpc.NAME);
        mapping.addHandler(ISessionXmlRpc.NAME,  SessionXmlRpcHandler.class);
        return mapping;
    }

    /**
     * Creates a new instance of {@link org.apache.xmlrpc.server.XmlRpcServer}, which
     * is being used to process the requests. Uses the base implementation to get
     * the server, and then configures it a bit to use extensions (to allow the service
     * to pass null parameters)
     *
     * @param servletConfig Servlet configuration. Not used
     * @return the <code>XmlRpcServer</code> used to process the request. In this case it's a
     *         <code>XmlRpcServletServer</code>
     * @throws XmlRpcException if there is a problem getting the server
     */
    protected XmlRpcServletServer newXmlRpcServer(ServletConfig servletConfig) throws XmlRpcException {
        XmlRpcServletServer server = super.newXmlRpcServer(servletConfig);
        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) server.getConfig();
        serverConfig.setEnabledForExtensions(true);
        serverConfig.setContentLengthOptional(false);
        return server;
    }
}
