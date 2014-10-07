package edu.gemini.spModel.gemini.security;

import edu.gemini.spModel.core.Affiliate;
import static edu.gemini.spModel.core.Affiliate.*;

import java.io.Serializable;
import java.security.Principal;

/**
 * AURA/Gemini
 * User: gillies
 * $Id: UserRole.java 6951 2006-04-11 15:42:49Z anunez $
 * Date: Jun 3, 2003
 */
public final class UserRole implements Principal, Serializable {
    public static final UserRole PI = new UserRole("PI", "Principal Investigator", null, UserRolePrivileges.PI);

    public static final UserRole NGO_US = new UserRole("NGO/US", "NGO-United States",  UNITED_STATES, UserRolePrivileges.NGO);

    public static final UserRole NGO_UK = new UserRole("NGO/UK", "NGO-United Kingdom", UNITED_KINGDOM, UserRolePrivileges.NGO);

    public static final UserRole NGO_CA = new UserRole("NGO/CA", "NGO-Canada", CANADA, UserRolePrivileges.NGO);

    public static final UserRole NGO_CL = new UserRole("NGO/CL", "NGO-Chile", CHILE, UserRolePrivileges.NGO);

    public static final UserRole NGO_KR = new UserRole("NGO/KR", "NGO-Korea", KOREA, UserRolePrivileges.NGO);

    public static final UserRole NGO_AU = new UserRole("NGO/AU", "NGO-Australia", AUSTRALIA, UserRolePrivileges.NGO);

    public static final UserRole NGO_AR = new UserRole("NGO/AR", "NGO-Argentina", ARGENTINA, UserRolePrivileges.NGO);

    public static final UserRole NGO_BR = new UserRole("NGO/BR", "NGO-Brazil", BRAZIL, UserRolePrivileges.NGO);

    public static final UserRole NGO_UH = new UserRole("NGO/UH", "NGO-University of Hawaii", UNIVERSITY_OF_HAWAII, UserRolePrivileges.NGO);

    public static final UserRole SUBARU = new UserRole("EXC/SU", "EXC-Subaru", null, UserRolePrivileges.EXC);

    // This is the normal Gemini Staff person who looks at everything
    public static final UserRole STAFF = new UserRole("GeminiStaff", "Gemini Staff", null, UserRolePrivileges.STAFF);

    // Gemini Staff Only is the case of an NGO-like person who only wants Gemini Staff Programs
    public static final UserRole STAFF_ONLY = new UserRole("GeminiStaffOnly", "Gemini Staff Only", GEMINI_STAFF, UserRolePrivileges.NGO);

    public static final UserRole ACL = new UserRole("ACL", "Access List Controller", null, UserRolePrivileges.ACL);

    public static final UserRole INVALID_USER = new UserRole("un-authenticated user", "un-authenticated user", null, UserRolePrivileges.NOUSER);

    private static int _nextOrdinal = 0;
    private final int _ordinal = _nextOrdinal++;
    private String _name;

    private UserRolePrivileges _rolePrivileges;

    private final Affiliate _spProgramAffiliate;

    // A String for display only
    private final String _displayName;

    // Note that INVALID_USER is not included in this array so that one can't login as "unauthenticated user"
    public static final UserRole[] ALL_ROLES = new UserRole[]{PI, NGO_US, NGO_UK, NGO_CA, NGO_CL, NGO_KR, NGO_AU, NGO_AR, NGO_BR, NGO_UH, SUBARU, STAFF, STAFF_ONLY, ACL};

    // This is the list of ROLES that should be generally visible in the correct order (PI-alphabetical?)
    public static final UserRole[] ROLES = new UserRole[]{PI, NGO_AR, NGO_AU, NGO_BR, NGO_CA, NGO_CL, NGO_KR, NGO_UH, NGO_UK, NGO_US, SUBARU, STAFF};

    /**
     * Create a UserRole that is also a Principal.
     *
     * @param name the role name for this group.
     *
     * @exception NullPointerException if the <code>name</code> is <code>null</code>.
     */
    private UserRole(String name, String displayName, Affiliate affiliate, UserRolePrivileges priv) {
        if (name == null) throw new NullPointerException("UserRole must have a non-null name");
        _name = name;
        _displayName = displayName;
        _spProgramAffiliate = affiliate;
        _rolePrivileges = priv;
    }

    /**
     * Allows for lookup of a specific enumerated principal by role name.
     * @param name name or role
     * @return the user role or invalid user if it's a bogus role name
     */
    static public UserRole getUserRoleByName(String name) {
        for (UserRole role : ALL_ROLES) {
            if (name.equals(role.getName())) return role;
        }

        // This will keep the old code compatible with the change to remove the
        // space in "Gemini Staff".
        if ("Gemini Staff".equals(name)) return STAFF;
        return INVALID_USER;
    }

    /**
     * Allows for lookup of a specific enumerated principal by display name.
     * @param displayName display name of role
     * @return the user role or invalid user if it's a bogus display name
     */
    static public UserRole getUserRoleByDisplayName(String displayName) {
        for (UserRole role : ALL_ROLES) {
            if (displayName.equals(role.getDisplayName())) return role;
        }
        return INVALID_USER;
    }

    /**
     * Compares this principal to the specified object.  Returns true
     * if the object passed in matches the principal represented by
     * the implementation of this interface.
     *
     * @param another principal to compare with.
     *
     * @return true if the principal passed in is the same as that
     * encapsulated by this principal, and false otherwise.

     */
    public boolean equals(Object another) {
        return (this == another);
    }

    /**
     * Returns a string representation of this principal.
     *
     * @return a string representation of this principal.
     */
    public String toString() {
        return "RoleName:DisplayName:Affiliate " + getName() + ":" + getDisplayName() + ":" + getAffiliate().displayValue;
    }

    /**
     * Returns a hashcode for this principal.
     *
     * @return a hashcode for this principal.
     */
    public int hashCode() {
        return _ordinal;
    }

    /**
     * Returns the name of this principal.
     *
     * @return the name of this principal.
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns the name of this principal that should be displayed in GUIs.
     *
     * @return the display name of this principal.
     */
    public String getDisplayName() {
        return _displayName;
    }

    /**
     * @return affiliate associated with the UserRole, if any; <code>null</code>
     * if none
     */
    public Affiliate getAffiliate() {
        return _spProgramAffiliate;
    }

    Object readResolve() {
        if ((_ordinal < 0) || (_ordinal >= INVALID_USER._ordinal)) {
            return INVALID_USER;
        }
        return ALL_ROLES[_ordinal];
    }

    public int compareTo(Object obj) {
        UserRole other = (UserRole) obj;
        return getName().compareTo(other.getName());
    }

    public UserRolePrivileges getUserRolePrivileges () {
        return _rolePrivileges;
    }
}
