//
// $
//

package edu.gemini.p2checker.api;



/**
 * A rule that combines multiple rules into one.
 */
public class CompositeRule implements IRule {

    /**
     * Determines the logic applied when one of the rules in this composite
     * returns problems.  Either all the rules are applied regardless, or the
     * first rule that returns problems ends the search.
     */
    public enum Type {
        any,
        all,
    }

    private final IRule[] rules;
    private final Type type;

    /**
     * Constructs with the rules to apply.
     */
    public CompositeRule(IRule[] rules) {
        this(rules, Type.all);
    }

    /**
     * Constructs with the rules to apply and what to do when problems are
     * found.
     *
     * @param rules rules to apply
     * @param type if <code>any</code>, then the problems returned by the first
     * rule
     */
    public CompositeRule(IRule[] rules, Type type) {
        this.rules = new IRule[rules.length];
        System.arraycopy(rules, 0, this.rules, 0, rules.length);
        this.type  = type;
    }

    public IP2Problems check(ObservationElements elements)  {
        IP2Problems problems = new P2Problems();

        for (IRule rule : rules) {
            IP2Problems tmp = rule.check(elements);
            if ((tmp != null) && (tmp.getProblemCount() > 0)) {
                problems.append(tmp);
                if (type == Type.any) break;
            }
        }

        return problems;
    }

}
