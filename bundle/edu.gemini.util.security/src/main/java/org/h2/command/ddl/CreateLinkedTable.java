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
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.TableLink;

/**
 * This class represents the statement
 * CREATE LINKED TABLE
 */
public class CreateLinkedTable extends SchemaCommand {

    private String tableName;
    private String driver, url, user, password, originalSchema, originalTable;
    private boolean ifNotExists;
    private String comment;
    private boolean emitUpdates;
    private boolean force;
    private boolean temporary;
    private boolean globalTemporary;
    private boolean readOnly;

    public CreateLinkedTable(Session session, Schema schema) {
        super(session, schema);
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public void setOriginalTable(String originalTable) {
        this.originalTable = originalTable;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public int update() {
        session.commit(true);
        Database db = session.getDatabase();
        session.getUser().checkAdmin();
        if (getSchema().findTableOrView(session, tableName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.TABLE_OR_VIEW_ALREADY_EXISTS_1,
                    tableName);
        }
        int id = getObjectId();
        TableLink table = getSchema().createTableLink(id, tableName, driver, url,
                user, password, originalSchema, originalTable, emitUpdates, force);
        table.setTemporary(temporary);
        table.setGlobalTemporary(globalTemporary);
        table.setComment(comment);
        table.setReadOnly(readOnly);
        if (temporary && !globalTemporary) {
            session.addLocalTempTable(table);
        } else {
            db.addSchemaObject(session, table);
        }
        return 0;
    }

    public void setEmitUpdates(boolean emitUpdates) {
        this.emitUpdates = emitUpdates;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setTemporary(boolean temp) {
        this.temporary = temp;
    }

    public void setGlobalTemporary(boolean globalTemp) {
        this.globalTemporary = globalTemp;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public void setOriginalSchema(String originalSchema) {
        this.originalSchema = originalSchema;
    }

    public int getType() {
        return CommandInterface.CREATE_LINKED_TABLE;
    }

}
