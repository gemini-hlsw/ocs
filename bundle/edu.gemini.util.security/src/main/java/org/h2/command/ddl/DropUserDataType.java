/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.command.CommandInterface;
import org.h2.constant.ErrorCode;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.UserDataType;
import org.h2.message.DbException;

/**
 * This class represents the statement
 * DROP DOMAIN
 */
public class DropUserDataType extends DefineCommand {

    private String typeName;
    private boolean ifExists;

    public DropUserDataType(Session session) {
        super(session);
    }

    public void setIfExists(boolean ifExists) {
        this.ifExists = ifExists;
    }

    public int update() {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        UserDataType type = db.findUserDataType(typeName);
        if (type == null) {
            if (!ifExists) {
                throw DbException.get(ErrorCode.USER_DATA_TYPE_NOT_FOUND_1, typeName);
            }
        } else {
            db.removeDatabaseObject(session, type);
        }
        return 0;
    }

    public void setTypeName(String name) {
        this.typeName = name;
    }

    public int getType() {
        return CommandInterface.DROP_DOMAIN;
    }

}
