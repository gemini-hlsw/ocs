/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.dml;

import java.sql.ResultSet;
import org.h2.command.CommandInterface;
import org.h2.command.Prepared;
import org.h2.engine.Session;
import org.h2.expression.Expression;
import org.h2.expression.ExpressionVisitor;
import org.h2.result.LocalResult;
import org.h2.result.ResultInterface;
import org.h2.value.Value;

/**
 * This class represents the statement
 * CALL.
 */
public class Call extends Prepared {

    private boolean isResultSet;
    private Expression expression;
    private Expression[] expressions;

    public Call(Session session) {
        super(session);
    }

    public ResultInterface queryMeta() {
        LocalResult result;
        if (isResultSet) {
            Expression[] expr = expression.getExpressionColumns(session);
            result = new LocalResult(session, expr, expr.length);
        } else {
            result = new LocalResult(session, expressions, 1);
        }
        result.done();
        return result;
    }

    public int update() {
        Value v = expression.getValue(session);
        int type = v.getType();
        switch(type) {
        case Value.RESULT_SET:
            // this will throw an exception
            // methods returning a result set may not be called like this.
            return super.update();
        case Value.UNKNOWN:
        case Value.NULL:
            return 0;
        default:
            return v.getInt();
        }
    }

    public ResultInterface query(int maxrows) {
        setCurrentRowNumber(1);
        Value v = expression.getValue(session);
        if (isResultSet) {
            v = v.convertTo(Value.RESULT_SET);
            ResultSet rs = v.getResultSet();
            return LocalResult.read(session, rs, maxrows);
        }
        LocalResult result = new LocalResult(session, expressions, 1);
        Value[] row = { v };
        result.addRow(row);
        result.done();
        return result;
    }

    public void prepare() {
        expression = expression.optimize(session);
        expressions = new Expression[] { expression };
        isResultSet = expression.getType() == Value.RESULT_SET;
        if (isResultSet) {
            prepareAlways = true;
        }
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public boolean isQuery() {
        return true;
    }

    public boolean isTransactional() {
        return true;
    }

    public boolean isReadOnly() {
        return expression.isEverything(ExpressionVisitor.READONLY_VISITOR);

    }

    public int getType() {
        return CommandInterface.CALL;
    }

    public boolean isCacheable() {
        return !isResultSet;
    }

}
