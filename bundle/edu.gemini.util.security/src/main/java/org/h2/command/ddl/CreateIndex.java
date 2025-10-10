/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.command.CommandInterface;
import org.h2.constant.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.engine.Right;
import org.h2.engine.Session;
import org.h2.index.IndexType;
import org.h2.message.DbException;
import org.h2.schema.Schema;
import org.h2.table.IndexColumn;
import org.h2.table.Table;

/**
 * This class represents the statement
 * CREATE INDEX
 */
public class CreateIndex extends SchemaCommand {

    private String tableName;
    private String indexName;
    private IndexColumn[] indexColumns;
    private boolean primaryKey, unique, hash;
    private boolean ifNotExists;
    private String comment;

    public CreateIndex(Session session, Schema schema) {
        super(session, schema);
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setIndexColumns(IndexColumn[] columns) {
        this.indexColumns = columns;
    }

    public int update() {
        if (!transactional) {
            session.commit(true);
        }
        Database db = session.getDatabase();
        boolean persistent = db.isPersistent();
        Table table = getSchema().getTableOrView(session, tableName);
        if (getSchema().findIndex(session, indexName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.INDEX_ALREADY_EXISTS_1, indexName);
        }
        session.getUser().checkRight(table, Right.ALL);
        table.lock(session, true, true);
        if (!table.isPersistIndexes()) {
            persistent = false;
        }
        int id = getObjectId();
        if (indexName == null) {
            if (primaryKey) {
                indexName = table.getSchema().getUniqueIndexName(session, table, Constants.PREFIX_PRIMARY_KEY);
            } else {
                indexName = table.getSchema().getUniqueIndexName(session, table, Constants.PREFIX_INDEX);
            }
        }
        IndexType indexType;
        if (primaryKey) {
            if (table.findPrimaryKey() != null) {
                throw DbException.get(ErrorCode.SECOND_PRIMARY_KEY);
            }
            indexType = IndexType.createPrimaryKey(persistent, hash);
        } else if (unique) {
            indexType = IndexType.createUnique(persistent, hash);
        } else {
            indexType = IndexType.createNonUnique(persistent, hash);
        }
        IndexColumn.mapColumns(indexColumns, table);
        table.addIndex(session, indexName, id, indexColumns, indexType, create, comment);
        return 0;
    }

    public void setPrimaryKey(boolean b) {
        this.primaryKey = b;
    }

    public void setUnique(boolean b) {
        this.unique = b;
    }

    public void setHash(boolean b) {
        this.hash = b;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public int getType() {
        return CommandInterface.CREATE_INDEX;
    }

}
