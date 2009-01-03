package org.drools.task;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.drools.task.utils.CollectionUtils;

@Entity
public class Reassignment implements Externalizable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)    
    private long                       id;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "Reassignment_Documentation_Id", nullable = true)     
    private List<I18NText>             documentation = Collections.emptyList();; 
    
    @ManyToMany
    @JoinTable(name = "Reassignment_potentialOwners", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "entity_id"))    
    private List<OrganizationalEntity> potentialOwners = Collections.emptyList();;

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong( id );
        CollectionUtils.writeI18NTextList( documentation, out );
        CollectionUtils.writeOrganizationalEntityList( potentialOwners, out );
    }
    
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        id = in.readLong();
        documentation = CollectionUtils.readI18NTextList( in );
        potentialOwners = CollectionUtils.readOrganizationalEntityList( in );        
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<I18NText> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(List<I18NText> documentation) {
        this.documentation = documentation;
    }

    public List<OrganizationalEntity> getPotentialOwners() {
        return potentialOwners;
    }

    public void setPotentialOwners(List<OrganizationalEntity> potentialOwners) {
        this.potentialOwners = potentialOwners;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + CollectionUtils.hashCode( documentation );
        result = prime * result + CollectionUtils.hashCode( potentialOwners );
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof Reassignment) ) return false;
        Reassignment other = (Reassignment) obj;
        return CollectionUtils.equals( documentation, other.documentation ) && CollectionUtils.equals( potentialOwners, other.potentialOwners );
    }
    
    

}
