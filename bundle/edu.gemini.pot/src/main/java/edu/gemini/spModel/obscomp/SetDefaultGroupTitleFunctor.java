package edu.gemini.spModel.obscomp;


import java.security.Principal;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.gemini.pot.sp.ISPGroup;
import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.sp.SPNodeNotLocalException;
import edu.gemini.pot.spdb.DBAbstractFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;

/**
 * Pass in a [new] group, and the functor will set its name to Group-N, where N is a reasonable number.
 * Note that the group must already have been added to a program, otherwise the functor does nothing.
 * @author rnorris
 * @version $Id$
 */
public class SetDefaultGroupTitleFunctor extends DBAbstractFunctor {

	private static final String PREFIX = "Group-";
	private static final Pattern PATTERN = Pattern.compile("^" + PREFIX + "(\\d+)$");
	
	public static void setDefaultGroupTitle(IDBDatabaseService db, ISPGroup group, Set<Principal> user) throws SPNodeNotLocalException {
		db.getQueryRunner(user).execute(new SetDefaultGroupTitleFunctor(), group);
	}
	
	public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> user) {
        ISPProgram program = getProgram(node);
        ISPGroup group = (ISPGroup) node;
        SPGroup spGroup = (SPGroup) group.getDataObject();
        if (program != null) {
            int max = 0;
            for (Iterator it = program.getGroups().iterator(); it.hasNext();) {
                ISPGroup g = (ISPGroup) it.next();
                String title = ((SPGroup) g.getDataObject()).getTitle();
                Matcher m = PATTERN.matcher(title);
                if (m.matches()) {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                }
            }
            spGroup.setTitle(PREFIX + (max + 1));
            node.setDataObject(spGroup);
        }
	}

	private ISPProgram getProgram(ISPNode node)  {
		while (node != null && !(node instanceof ISPProgram))
			node = node.getParent();
		return (ISPProgram) node;
	}
	
}


