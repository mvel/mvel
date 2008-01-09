/**
 *
 */
package org.mvel;

import java.util.Iterator;

public class ForeachContext {
    private String seperator;

    private String[] names;
    private String[] aliases;

    private Iterator[] iter;
    private int count;


    public ForeachContext() {
    }

    public ForeachContext(String seperator, int count, String[] names, String[] aliases) {
        this.seperator = seperator;
        this.count = count;
        this.names = names;
        this.aliases = aliases;
    }

    public ForeachContext(String seperator, int count) {
        this.seperator = seperator;
        this.count = count;
    }

    public ForeachContext(String seperator) {
        this.seperator = seperator;
    }

    public String getSeperator() {
        return this.seperator;
    }

    public void setIterators(Iterator[] iter) {
        this.iter = iter;
    }

    public Iterator[] getItererators() {
        return this.iter;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public ForeachContext clone() {
        return new ForeachContext(seperator, count, names, aliases);
    }

    public String[] getNames() {
        return names;
    }

    public void setNames(String[] names) {
        this.names = names;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public Iterator[] getIter() {
        return iter;
    }

    public void setIter(Iterator[] iter) {
        this.iter = iter;
    }


    public void setSeperator(String seperator) {
        this.seperator = seperator;
    }
}