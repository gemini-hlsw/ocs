package jsky.app.ot.editor.template;

import edu.gemini.spModel.gemini.security.UserRole;
import edu.gemini.spModel.gemini.security.UserRolePrivileges;
import jsky.app.ot.OTOptions;

import static edu.gemini.spModel.gemini.security.UserRolePrivileges.*;

public class ReapplicationHelpers {

    public static UserRolePrivileges currentUserRolePrivileges() {
        final UserRole role = OTOptions.getUserRole();
        return (role != null) ? role.getUserRolePrivileges() : (OTOptions.isStaffGlobally() ? STAFF : PI);
    }

}
