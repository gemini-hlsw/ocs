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
import org.h2.schema.TriggerObject;
import org.h2.table.Table;

/**
 * This class represents the statement
 * CREATE TRIGGER
 */
public class CreateTrigger extends SchemaCommand {

    private String triggerName;
    private boolean ifNotExists;

    private boolean insteadOf;
    private boolean before;
    private int typeMask;
    private boolean rowBased;
    private int queueSize = TriggerObject.DEFAULT_QUEUE_SIZE;
    private boolean noWait;
    private String tableName;
    private String triggerClassName;
    private boolean force;
    private boolean onRollback;

    public CreateTrigger(Session session, Schema schema) {
        super(session, schema);
    }

    public void setInsteadOf(boolean insteadOf) {
        this.insteadOf = insteadOf;
    }

    public void setBefore(boolean before) {
        this.before = before;
    }

    public void setTriggerClassName(String triggerClassName) {
        this.triggerClassName = triggerClassName;
    }

    public void setTypeMask(int typeMask) {
        this.typeMask = typeMask;
    }

    public void setRowBased(boolean rowBased) {
        this.rowBased = rowBased;
    }

    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setTriggerName(String name) {
        this.triggerName = name;
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public int update() {
        session.commit(true);
        Database db = session.getDatabase();
        if (getSchema().findTrigger(triggerName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.TRIGGER_ALREADY_EXISTS_1, triggerName);
        }
        int id = getObjectId();
        Table table = getSchema().getTableOrView(session, tableName);
        TriggerObject trigger = new TriggerObject(getSchema(), id, triggerName, table);
        trigger.setInsteadOf(insteadOf);
        trigger.setBefore(before);
        trigger.setNoWait(noWait);
        trigger.setQueueSize(queueSize);
        trigger.setRowBased(rowBased);
        trigger.setTypeMask(typeMask);
        trigger.setOnRollback(onRollback);
        trigger.setTriggerClassName(triggerClassName, force);
        db.addSchemaObject(session, trigger);
        table.addTrigger(trigger);
        return 0;
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    public void setOnRollback(boolean onRollback) {
        this.onRollback = onRollback;
    }

    public int getType() {
        return CommandInterface.CREATE_TRIGGER;
    }

}
