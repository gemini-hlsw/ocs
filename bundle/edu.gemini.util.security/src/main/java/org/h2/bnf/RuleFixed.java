/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.bnf;

import java.util.HashMap;

/**
 * Represents a hard coded terminal rule in a BNF object.
 */
public class RuleFixed implements Rule {

    public static final int YMD = 0, HMS = 1, NANOS = 2;
    public static final int ANY_EXCEPT_SINGLE_QUOTE = 3;
    public static final int ANY_EXCEPT_DOUBLE_QUOTE = 4;
    public static final int ANY_UNTIL_EOL = 5;
    public static final int ANY_UNTIL_END = 6;
    public static final int ANY_WORD = 7;
    public static final int ANY_EXCEPT_2_DOLLAR = 8;
    public static final int HEX_START = 10, CONCAT = 11, AZ_UNDERSCORE = 12, AF = 13, DIGIT = 14;
    public static final int OPEN_BRACKET = 15, CLOSE_BRACKET = 16;

    private final int type;

    RuleFixed(int type) {
        this.type = type;
    }

    public void accept(BnfVisitor visitor) {
        visitor.visitRuleFixed(type);
    }

    public void setLinks(HashMap<String, RuleHead> ruleMap) {
        // nothing to do
    }

    public boolean autoComplete(Sentence sentence) {
        sentence.stopIfRequired();
        String query = sentence.getQuery();
        String s = query;
        switch(type) {
        case YMD:
            while (s.length() > 0 && "0123456789-".indexOf(s.charAt(0)) >= 0) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("2006-01-01", "1", Sentence.KEYWORD);
            }
            break;
        case HMS:
            while (s.length() > 0 && "0123456789:".indexOf(s.charAt(0)) >= 0) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("12:00:00", "1", Sentence.KEYWORD);
            }
            break;
        case NANOS:
            while (s.length() > 0 && Character.isDigit(s.charAt(0))) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("nanoseconds", "0", Sentence.KEYWORD);
            }
            break;
        case ANY_EXCEPT_SINGLE_QUOTE:
            while (true) {
                while (s.length() > 0 && s.charAt(0) != '\'') {
                    s = s.substring(1);
                }
                if (s.startsWith("''")) {
                    s = s.substring(2);
                } else {
                    break;
                }
            }
            if (s.length() == 0) {
                sentence.add("anything", "Hello World", Sentence.KEYWORD);
                sentence.add("'", "'", Sentence.KEYWORD);
            }
            break;
        case ANY_EXCEPT_2_DOLLAR:
            while (s.length() > 0 && !s.startsWith("$$")) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("anything", "Hello World", Sentence.KEYWORD);
                sentence.add("$$", "$$", Sentence.KEYWORD);
            }
            break;
        case ANY_EXCEPT_DOUBLE_QUOTE:
            while (true) {
                while (s.length() > 0 && s.charAt(0) != '\"') {
                    s = s.substring(1);
                }
                if (s.startsWith("\"\"")) {
                    s = s.substring(2);
                } else {
                    break;
                }
            }
            if (s.length() == 0) {
                sentence.add("anything", "identifier", Sentence.KEYWORD);
                sentence.add("\"", "\"", Sentence.KEYWORD);
            }
            break;
        case ANY_WORD:
            while (s.length() > 0 && !Character.isSpaceChar(s.charAt(0))) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("anything", "anything", Sentence.KEYWORD);
            }
            break;
        case HEX_START:
            if (s.startsWith("0X") || s.startsWith("0x")) {
                s = s.substring(2);
            } else if ("0".equals(s)) {
                sentence.add("0x", "x", Sentence.KEYWORD);
            } else if (s.length() == 0) {
                sentence.add("0x", "0x", Sentence.KEYWORD);
            }
            break;
        case CONCAT:
            if (s.equals("|")) {
                sentence.add("||", "|", Sentence.KEYWORD);
            } else if (s.startsWith("||")) {
                s = s.substring(2);
            } else if (s.length() == 0) {
                sentence.add("||", "||", Sentence.KEYWORD);
            }
            break;
        case AZ_UNDERSCORE:
            if (s.length() > 0 && (Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("character", "A", Sentence.KEYWORD);
            }
            break;
        case AF:
            if (s.length() > 0) {
                char ch = Character.toUpperCase(s.charAt(0));
                if (ch >= 'A' && ch <= 'F') {
                    s = s.substring(1);
                }
            }
            if (s.length() == 0) {
                sentence.add("hex character", "0A", Sentence.KEYWORD);
            }
            break;
        case DIGIT:
            if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
                s = s.substring(1);
            }
            if (s.length() == 0) {
                sentence.add("digit", "1", Sentence.KEYWORD);
            }
            break;
        case OPEN_BRACKET:
            if (s.length() == 0) {
                sentence.add("[", "[", Sentence.KEYWORD);
            } else if (s.charAt(0) == '[') {
                s = s.substring(1);
            }
            break;
        case CLOSE_BRACKET:
            if (s.length() == 0) {
                sentence.add("]", "]", Sentence.KEYWORD);
            } else if (s.charAt(0) == ']') {
                s = s.substring(1);
            }
            break;
        // no autocomplete support for comments
        // (comments are not reachable in the bnf tree)
        case ANY_UNTIL_EOL:
        case ANY_UNTIL_END:
        default:
            throw new AssertionError("type="+type);
        }
        if (!s.equals(query)) {
            while (s.length() > 0 && Character.isSpaceChar(s.charAt(0))) {
                s = s.substring(1);
            }
            sentence.setQuery(s);
            return true;
        }
        return false;
    }

}
