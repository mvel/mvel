/**
 * 
 */
package org.mvel;

import java.util.Iterator;

public class ForeachContext {
    private String seperator; 
    private Iterator[] iter;
    private int count;
    
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
    
}