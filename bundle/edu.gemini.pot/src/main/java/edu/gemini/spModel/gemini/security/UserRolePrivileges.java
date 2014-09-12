package edu.gemini.spModel.gemini.security;

/**
 * Defines the different user role privileges that an UserRole can have.
 * $Id: UserRolePrivileges.java 6951 2006-04-11 15:42:49Z anunez $
 */

public enum UserRolePrivileges {

    NGO("NGO Privilege"),
    EXC("Exchange Partner Privilege"),
    STAFF("Super User Privilege"),
    ACL("no desc"),
    PI("PI Privilege"),
    NOUSER("Unauthorized Privilege");

    private String _desc;

    UserRolePrivileges(String description) {
        _desc = description;
    }

    public String getDescription() {
        return _desc;
    }
}
