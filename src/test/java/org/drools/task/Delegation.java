package org.drools.task;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;

import org.drools.task.utils.CollectionUtils;

@Embeddable
public class Delegation  implements Externalizable {
    @Enumerated(EnumType.STRING)      
    private AllowedToDelegate                    allowedToDelegate;
    
    @ManyToMany
    @JoinTable(name = "Delegation_delegates", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "entity_id"))    
    private List<OrganizationalEntity> delegates = Collections.emptyList();
    
    public void writeExternal(ObjectOutput out) throws IOException {
        if ( allowedToDelegate != null ) {
            out.writeBoolean( true );
            out.writeUTF( allowedToDelegate.toString() );
        } else {
            out.writeBoolean( false );
        }
        CollectionUtils.writeOrganizationalEntityList( delegates, out );       
    } 
    
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        if ( in.readBoolean() ) {
            allowedToDelegate = AllowedToDelegate.valueOf( in.readUTF() );
        }
        delegates = CollectionUtils.readOrganizationalEntityList( in );
    }       

    public AllowedToDelegate getAllowed() {
        return allowedToDelegate;
    }    
    
    public void setAllowed(AllowedToDelegate allowedToDelegate) {
        this.allowedToDelegate = allowedToDelegate;
    }

    public List<OrganizationalEntity> getDelegates() {
        return delegates;
    }
    

    public void setDelegates(List<OrganizationalEntity> delegates) {
        this.delegates = delegates;
    }    
    
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((allowedToDelegate == null) ? 0 : allowedToDelegate.hashCode());
        result = prime * result + CollectionUtils.hashCode( delegates );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Delegation) ) return false;
        Delegation other = (Delegation) obj;
        if ( allowedToDelegate == null ) {
            if ( other.allowedToDelegate != null ) return false;
        } else if ( !allowedToDelegate.equals( other.allowedToDelegate ) ) return false;
        
        return CollectionUtils.equals( delegates, other.delegates );
    }
}
