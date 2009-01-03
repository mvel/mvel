package org.drools.task;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.persistence.Entity;

@Entity
public class User extends OrganizationalEntity implements Externalizable{
    
    public User() {
        super();
    }
    
    public User(String id) {
        super(id);
    }
    
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
    } 
    
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );       
    }          


    
        
}
