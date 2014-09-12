//
// $Id: TriggerReg.java 46832 2012-07-19 00:28:38Z rnorris $
//
package edu.gemini.pot.spdb;

public final class TriggerReg {
    private IDBTriggerCondition _condition;
    private IDBTriggerAction _action;

    public TriggerReg(IDBTriggerCondition cond, IDBTriggerAction action) {
        _condition = cond;
        _action    = action;
    }

    public IDBTriggerCondition getTriggerCondition() {
        return _condition;
    }

    public IDBTriggerAction getTriggerAction() {
        return _action;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TriggerReg that = (TriggerReg) o;

        if (_action != null ? !_action.equals(that._action) : that._action != null) return false;
        if (_condition != null ? !_condition.equals(that._condition) : that._condition != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _condition != null ? _condition.hashCode() : 0;
        result = 31 * result + (_action != null ? _action.hashCode() : 0);
        return result;
    }
}
