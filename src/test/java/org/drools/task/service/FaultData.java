package org.drools.task.service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class FaultData extends ContentData {
	
	private String faultName;

	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF( faultName );
		super.writeExternal( out );
    }
    
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        faultName = in.readUTF();
        super.readExternal( in );
    }

}
