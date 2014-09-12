package edu.gemini.spdb.reports.impl.www;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.servlet.VelocityServlet;

import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spdb.reports.IQuery;
import edu.gemini.spdb.reports.ISort;
import edu.gemini.spdb.reports.ITable;
import edu.gemini.spdb.reports.ISort.NullPolicy;
import edu.gemini.spdb.reports.ISort.Order;
import edu.gemini.spdb.reports.impl.QueryManager;
import edu.gemini.spdb.reports.impl.ReportManager;
import edu.gemini.spdb.reports.impl.TableManager;
import edu.gemini.spdb.reports.osgi.Activator;
import edu.gemini.spdb.reports.util.HtmlEscaper;
import edu.gemini.spdb.reports.util.QueryWrapper;
import edu.gemini.spdb.reports.util.SimpleSort;

/**
 * This is a very simple web application that uses HTTP sessions to manage
 * state. Here is the basic idea:
 * <li>
 * <ul>The session maintains a working IQuery and a selected IDBDatabaseService.
 * <ul>The web app enables or disables navigation based on the presence of the
 * session values. If there is no query, you can't go to the query config page,
 * for example.
 * <ul>Actions simply modify the session state and do not perform navigation.
 * When an action is performed, the user is redirected to the requested page
 * without the action parameters on the URL. This fixes the refresh and
 * back-button problem.
 * <ul>All session references are added to the context as well, so the .vm
 * pages don't need to mess with the session.
 * <ul>The code is optimistic with respect to error-handling. That is, it is
 * possible to generate an error by sending bogus requests. Who cares.
 * </li>
 * @author rnorris
 */
@SuppressWarnings("serial")
public final class DispatchServlet extends VelocityServlet {

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(DispatchServlet.class.getName());

	// Session Keys
	private static String SKEY_QUERY = "query";

	// Request Parameters
	private static enum Param { ACTION, SUB_ACTION, UUID, SID }

	// Action and Sub-Action Tokens
	private static enum Action { SELECT_TABLE, SELECT_DATABASE,	EDIT_OUTPUT_COLUMN, EDIT_SORT_COLUMN, EDIT_GROUP }
	private static enum SubAction { MOVE_UP, MOVE_DOWN, DELETE, ADD, FLIP_ORDER, FLIP_POLICY }

    private final IDBDatabaseService db;
    private final Set<Principal> user;

	public DispatchServlet(IDBDatabaseService db, Set<Principal> user) {
        this.db = db;
        this.user = user;
	}

	// Set up Velocity
	@Override
	protected Properties loadConfiguration(ServletConfig config) throws IOException, FileNotFoundException {
		Properties p = new Properties();
		p.put(RuntimeConstants.RESOURCE_LOADER, "class");
		p.put("class.resource.loader.class", ClasspathResourceLoader.class.getName());
		p.put(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, DelegatingLogSystem.class.getName());
		return p;
	}

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // Don't accept secure requests. By limiting access to straight HTTP (which is only available
        // internally) we effectively hide this service from external users.
        if (req.isSecure()) {
            res.sendError(404);
        } else {
            super.doGet(req, res);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        // Don't accept secure requests. By limiting access to straight HTTP (which is only available
        // internally) we effectively hide this service from external users.
        if (req.isSecure()) {
            res.sendError(404);
        } else {
            super.doPost(req, res);    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

    // Handle requests.
	@Override
	@SuppressWarnings("unchecked")
	protected Template handleRequest(HttpServletRequest req, HttpServletResponse res, Context context) throws Exception {

		// If the user just goes to /reports or /reports/, redirect to the index
		// page and ignore anything else on the request. Normally the servlet
		// container would handle welcome pages like this.
		String pathInfo = req.getPathInfo();
		if (pathInfo == null || pathInfo.equals("/")) {
			res.sendRedirect(getMountPoint(req) + "/index.vm");
			return null;
		}

		// Handle the action, if any. Simple dispatch here, but notice that we
		// return null and redirect back to the requested page in order to remove
		// parameters from the URL and prevent the user from re-executing
		// actions by hitting refresh or back.
		Action action = getAction(req);
		if (action != null) {

			// Dispatch the action.
			switch (action) {

			case SELECT_TABLE:
				selectTable(req);
				break;

			case EDIT_OUTPUT_COLUMN:
				editOutputColumn(req);
				break;

			case EDIT_SORT_COLUMN:
				editSortColumn(req);
				break;

			case EDIT_GROUP:
				editGroup(req);
				break;

			}

			// And reditect to the requested page.
			res.sendRedirect(req.getRequestURI());
			return null;

		}

		// Context variables that are always available.
		context.put("escaper", new HtmlEscaper());
		context.put("tableManager", TableManager.getInstance());
		context.put("reportManager", ReportManager.getInstance());
		context.put("queryManager", new QueryManager(user));
		context.put("interactive", true);
        context.put("database", db);

		// Copy everything from the session into the context.
		HttpSession session = req.getSession(true);
		Enumeration<String> e = session.getAttributeNames();
		while (e.hasMoreElements()) {
			String key = e.nextElement();
			context.put(key, session.getAttribute(key));
		}

		// TODO: timout session

		// Try to keep the browser from caching the content we are about to
		// send. Otherwise the user can hit the back button and see previous
		// versions of a query definition, which is kind of confusing.
		res.setHeader("Cache-Control", "no-cache");
		res.setHeader("Pragma", "no-cache");
		res.setDateHeader("Expires", 0L);

		return getTemplate("/edu/gemini/spdb/reports/impl/www/vm" + pathInfo);

	}

	private Action getAction(HttpServletRequest req) {
		// May be null.
		String sv = req.getParameter(Param.ACTION.toString());
		return sv == null ? null : Action.valueOf(sv);
	}

	private SubAction getSubAction(HttpServletRequest req) {
		// May not be null.
		return SubAction.valueOf(req.getParameter(Param.SUB_ACTION.toString()));
	}

	private String getMountPoint(HttpServletRequest req) {
		String pathInfo = req.getPathInfo();
		String uri = req.getRequestURI();
		return (pathInfo == null) ? uri : uri.substring(0, uri.length() - pathInfo.length());
	}

	private UUID getUUID(HttpServletRequest req) {
		return UUID.fromString(req.getParameter(Param.UUID.toString()));
	}

	private String getSID(HttpServletRequest req) {
		return req.getParameter(Param.SID.toString());
	}

	private void selectTable(HttpServletRequest req) {
		ITable table = TableManager.getInstance().get(getSID(req));
		IQuery query = getQuery(req);
		if (query == null || query.getTable() != table) {
			setQuery(req, new QueryWrapper(new QueryManager(user).createQuery(table)));
		}
	}

	private QueryWrapper getQuery(HttpServletRequest req) {
		return (QueryWrapper) req.getSession(true).getAttribute(SKEY_QUERY);
	}

	private void setQuery(HttpServletRequest req, QueryWrapper query) {
		req.getSession(true).setAttribute(SKEY_QUERY, query);
	}

	@SuppressWarnings("unchecked")
	private void editOutputColumn(HttpServletRequest req) {

		QueryWrapper query = getQuery(req);
		String sid = getSID(req);

		switch (getSubAction(req)) {

		case MOVE_DOWN:
			query.swapOutputColumn(sid, 1);
			break;

		case MOVE_UP:
			query.swapOutputColumn(sid, -1);
			break;

		case DELETE:
			query.deleteOutputColumn(sid);
			break;

		case ADD:
			query.addOutputColumn(sid);
			break;

		default:
			throw new UnsupportedOperationException();

		}

	}

	@SuppressWarnings("unchecked")
	private void editSortColumn(HttpServletRequest req) {

		QueryWrapper query = getQuery(req);
		String sid = getSID(req);

		switch (getSubAction(req)) {

		case MOVE_DOWN:
			query.swapSort(sid, 1);
			break;

		case MOVE_UP:
			query.swapSort(sid, -1);
			break;

		case DELETE:
			query.deleteSort(sid);
			break;

		case ADD:
			query.addSort(new SimpleSort(query.getColumn(sid), Order.ASC, NullPolicy.NULL_LAST));
			break;

		case FLIP_ORDER: {
			ISort sort = query.getSort(sid);
			sort.setOrder(sort.getOrder() == Order.ASC ? Order.DESC : Order.ASC);
			break;
		}

		case FLIP_POLICY: {
			ISort sort = query.getSort(sid);
			sort.setNullPolicy(sort.getNullPolicy() == NullPolicy.NULL_FIRST ? NullPolicy.NULL_LAST : NullPolicy.NULL_FIRST);
			break;
		}

		}

	}


	@SuppressWarnings("unchecked")
	private void editGroup(HttpServletRequest req) {

		QueryWrapper query = getQuery(req);
		String sid = getSID(req);

		switch (getSubAction(req)) {

		case MOVE_DOWN:
			query.swapGroup(sid, 1);
			break;

		case MOVE_UP:
			query.swapGroup(sid, -1);
			break;

		case DELETE:
			query.deleteGroup(sid);
			break;

		case ADD:
			query.addGroup(new SimpleSort(query.getColumn(sid), Order.ASC, NullPolicy.NULL_LAST));
			break;

		case FLIP_ORDER: {
			ISort sort = query.getGroup(sid);
			sort.setOrder(sort.getOrder() == Order.ASC ? Order.DESC : Order.ASC);
			break;
		}

		case FLIP_POLICY: {
			ISort sort = query.getGroup(sid);
			sort.setNullPolicy(sort.getNullPolicy() == NullPolicy.NULL_FIRST ? NullPolicy.NULL_LAST : NullPolicy.NULL_FIRST);
			break;
		}

		}

	}



}

