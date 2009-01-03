package org.drools.task;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.drools.task.utils.CollectionUtils;

@Embeddable
public class Deadlines implements Externalizable {    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "Deadlines_StartDeadLine_Id", nullable = true)    
    private List<Deadline> startDeadlines = Collections.emptyList();
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "Deadlines_EndDeadLine_Id", nullable = true)    
    private List<Deadline> endDeadlines  = Collections.emptyList();
    
    public void writeExternal(ObjectOutput out) throws IOException {
        CollectionUtils.writeDeadlineList( startDeadlines, out );
        CollectionUtils.writeDeadlineList( endDeadlines, out );       
    } 
    
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        startDeadlines = CollectionUtils.readDeadlinesList( in );
        endDeadlines = CollectionUtils.readDeadlinesList( in );        
    }        
      
    public List<Deadline> getStartDeadlines() {
        return startDeadlines;
    }
    
    public void setStartDeadlines(List<Deadline> startDeadlines) {
        this.startDeadlines = startDeadlines;
    }
    
    public List<Deadline> getEndDeadlines() {
        return endDeadlines;
    }
    
    public void setEndDeadlines(List<Deadline> endDeadlines) {
        this.endDeadlines = endDeadlines;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CollectionUtils.hashCode( endDeadlines );
        result = prime * result + CollectionUtils.hashCode( startDeadlines );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Deadlines) ) return false;
        Deadlines other = (Deadlines) obj;       
        
        return CollectionUtils.equals( endDeadlines, other.endDeadlines ) && CollectionUtils.equals( startDeadlines, other.startDeadlines );
    }
                   
    
}
