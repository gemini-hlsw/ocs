/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.schema;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;
import org.h2.command.Parser;
import org.h2.constant.ErrorCode;
import org.h2.engine.DbObject;
import org.h2.engine.Session;
import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.result.Row;
import org.h2.table.Table;
import org.h2.util.Utils;
import org.h2.util.StatementBuilder;
import org.h2.value.DataType;
import org.h2.value.Value;

/**
 *A trigger is created using the statement
 * CREATE TRIGGER
 */
public class TriggerObject extends SchemaObjectBase {

    /**
     * The default queue size.
     */
    public static final int DEFAULT_QUEUE_SIZE = 1024;

    private boolean insteadOf;
    private boolean before;
    private int typeMask;
    private boolean rowBased;
    private boolean onRollback;
    // TODO trigger: support queue and noWait = false as well
    private int queueSize = DEFAULT_QUEUE_SIZE;
    private boolean noWait;
    private Table table;
    private String triggerClassName;
    private Trigger triggerCallback;

    public TriggerObject(Schema schema, int id, String name, Table table) {
        initSchemaObjectBase(schema, id, name, Trace.TRIGGER);
        this.table = table;
        setTemporary(table.isTemporary());
    }

    public void setBefore(boolean before) {
        this.before = before;
    }

    public void setInsteadOf(boolean insteadOf) {
        this.insteadOf = insteadOf;
    }

    private synchronized void load() {
        if (triggerCallback != null) {
            return;
        }
        try {
            Session session = database.getSystemSession();
            Connection c2 = session.createConnection(false);
            Object obj = Utils.loadUserClass(triggerClassName).newInstance();
            triggerCallback = (Trigger) obj;
            triggerCallback.init(c2, getSchema().getName(), getName(), table.getName(), before, typeMask);
        } catch (Throwable e) {
            // try again later
            triggerCallback = null;
            throw DbException.get(ErrorCode.ERROR_CREATING_TRIGGER_OBJECT_3, e, getName(),
                            triggerClassName, e.toString());
        }
    }

    /**
     * Set the trigger class name and load the class if possible.
     *
     * @param triggerClassName the name of the trigger class
     * @param force whether exceptions (due to missing class or access rights)
     *            should be ignored
     */
    public void setTriggerClassName(String triggerClassName, boolean force) {
        this.triggerClassName = triggerClassName;
        try {
            load();
        } catch (DbException e) {
            if (!force) {
                throw e;
            }
        }
    }

    /**
     * Call the trigger class if required. This method does nothing if the
     * trigger is not defined for the given action. This method is called before
     * or after any rows have been processed, once for each statement.
     *
     * @param session the session
     * @param type the trigger type
     * @param beforeAction if this method is called before applying the changes
     */
    public void fire(Session session, int type, boolean beforeAction) {
        if (rowBased || before != beforeAction || (typeMask & type) == 0) {
            return;
        }
        load();
        Connection c2 = session.createConnection(false);
        boolean old = false;
        if (type != Trigger.SELECT) {
            old = session.setCommitOrRollbackDisabled(true);
        }
        Value identity = session.getLastScopeIdentity();
        try {
            triggerCallback.fire(c2, null, null);
        } catch (Throwable e) {
            throw DbException.get(ErrorCode.ERROR_EXECUTING_TRIGGER_3, e, getName(),
                            triggerClassName, e.toString());
        } finally {
            session.setLastScopeIdentity(identity);
            if (type != Trigger.SELECT) {
                session.setCommitOrRollbackDisabled(old);
            }
        }
    }

    private static Object[] convertToObjectList(Row row) {
        if (row == null) {
            return null;
        }
        int len = row.getColumnCount();
        Object[] list = new Object[len];
        for (int i = 0; i < len; i++) {
            list[i] = row.getValue(i).getObject();
        }
        return list;
    }

    /**
     * Call the fire method of the user-defined trigger class if required. This
     * method does nothing if the trigger is not defined for the given action.
     * This method is called before or after a row is processed, possibly many
     * times for each statement.
     *
     * @param session the session
     * @param oldRow the old row
     * @param newRow the new row
     * @param beforeAction true if this method is called before the operation is
     *            applied
     * @param rollback when the operation occurred within a rollback
     * @return true if no further action is required (for 'instead of' triggers)
     */
    public boolean fireRow(Session session, Row oldRow, Row newRow, boolean beforeAction, boolean rollback) {
        if (!rowBased || before != beforeAction) {
            return false;
        }
        if (rollback && !onRollback) {
            return false;
        }
        load();
        Object[] oldList;
        Object[] newList;
        boolean fire = false;
        if ((typeMask & Trigger.INSERT) != 0) {
            if (oldRow == null && newRow != null) {
                fire = true;
            }
        }
        if ((typeMask & Trigger.UPDATE) != 0) {
            if (oldRow != null && newRow != null) {
                fire = true;
            }
        }
        if ((typeMask & Trigger.DELETE) != 0) {
            if (oldRow != null && newRow == null) {
                fire = true;
            }
        }
        if (!fire) {
            return false;
        }
        oldList = convertToObjectList(oldRow);
        newList = convertToObjectList(newRow);
        Object[] newListBackup;
        if (before && newList != null) {
            newListBackup = new Object[newList.length];
            System.arraycopy(newList, 0, newListBackup, 0, newList.length);
        } else {
            newListBackup = null;
        }
        Connection c2 = session.createConnection(false);
        boolean old = session.getAutoCommit();
        boolean oldDisabled = session.setCommitOrRollbackDisabled(true);
        Value identity = session.getLastScopeIdentity();
        try {
            session.setAutoCommit(false);
            triggerCallback.fire(c2, oldList, newList);
            if (newListBackup != null) {
                for (int i = 0; i < newList.length; i++) {
                    Object o = newList[i];
                    if (o != newListBackup[i]) {
                        Value v = DataType.convertToValue(session, o, Value.UNKNOWN);
                        newRow.setValue(i, v);
                    }
                }
            }
        } catch (Exception e) {
            if (onRollback) {
                // ignore
            } else {
                throw DbException.convert(e);
            }
        } finally {
            session.setLastScopeIdentity(identity);
            session.setCommitOrRollbackDisabled(oldDisabled);
            session.setAutoCommit(old);
        }
        return insteadOf;
    }

    /**
     * Set the trigger type.
     *
     * @param typeMask the type
     */
    public void setTypeMask(int typeMask) {
        this.typeMask = typeMask;
    }

    public void setRowBased(boolean rowBased) {
        this.rowBased = rowBased;
    }

    public void setQueueSize(int size) {
        this.queueSize = size;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setNoWait(boolean noWait) {
        this.noWait = noWait;
    }

    public boolean isNoWait() {
        return noWait;
    }

    public void setOnRollback(boolean onRollback) {
        this.onRollback = onRollback;
    }

    public String getDropSQL() {
        return null;
    }

    public String getCreateSQLForCopy(Table targetTable, String quotedName) {
        StringBuilder buff = new StringBuilder("CREATE FORCE TRIGGER ");
        buff.append(quotedName);
        if (insteadOf) {
            buff.append(" INSTEAD OF ");
        } else if (before) {
            buff.append(" BEFORE ");
        } else {
            buff.append(" AFTER ");
        }
        buff.append(getTypeNameList());
        buff.append(" ON ").append(targetTable.getSQL());
        if (rowBased) {
            buff.append(" FOR EACH ROW");
        }
        if (noWait) {
            buff.append(" NOWAIT");
        } else {
            buff.append(" QUEUE ").append(queueSize);
        }
        buff.append(" CALL ").append(Parser.quoteIdentifier(triggerClassName));
        return buff.toString();
    }

    public String getTypeNameList() {
        StatementBuilder buff = new StatementBuilder();
        if ((typeMask & Trigger.INSERT) != 0) {
            buff.appendExceptFirst(", ");
            buff.append("INSERT");
        }
        if ((typeMask & Trigger.UPDATE) != 0) {
            buff.appendExceptFirst(", ");
            buff.append("UPDATE");
        }
        if ((typeMask & Trigger.DELETE) != 0) {
            buff.appendExceptFirst(", ");
            buff.append("DELETE");
        }
        if ((typeMask & Trigger.SELECT) != 0) {
            buff.appendExceptFirst(", ");
            buff.append("SELECT");
        }
        if (onRollback) {
            buff.appendExceptFirst(", ");
            buff.append("ROLLBACK");
        }
        return buff.toString();
    }

    public String getCreateSQL() {
        return getCreateSQLForCopy(table, getSQL());
    }

    public int getType() {
        return DbObject.TRIGGER;
    }

    public void removeChildrenAndResources(Session session) {
        table.removeTrigger(this);
        database.removeMeta(session, getId());
        if (triggerCallback != null) {
            try {
                triggerCallback.remove();
            } catch (SQLException e) {
                throw DbException.convert(e);
            }
        }
        table = null;
        triggerClassName = null;
        triggerCallback = null;
        invalidate();
    }

    public void checkRename() {
        // nothing to do
    }

    /**
     * Get the table of this trigger.
     *
     * @return the table
     */
    public Table getTable() {
        return table;
    }

    /**
     * Check if this is a before trigger.
     *
     * @return true if it is
     */
    public boolean isBefore() {
        return before;
    }

    /**
     * Get the trigger class name.
     *
     * @return the class name
     */
    public String getTriggerClassName() {
        return triggerClassName;
    }

    /**
     * Close the trigger.
     */
    public void close() throws SQLException {
        if (triggerCallback != null) {
            triggerCallback.close();
        }
    }

    /**
     * Check whether this is a select trigger.
     *
     * @return true if it is
     */
    public boolean isSelectTrigger() {
        return (typeMask & Trigger.SELECT) != 0;
    }

}
