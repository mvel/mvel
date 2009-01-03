package org.drools.task;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.drools.task.utils.CollectionUtils;

@Embeddable
public class TaskData
    implements
    Externalizable {
    @Enumerated(EnumType.STRING)
    private Status           status      = Status.Created;         // initial default state
    
    private Status           previousStatus = null;

    @ManyToOne()
    private User             actualOwner;

    @ManyToOne()
    private User             createdBy;

    private Date             createdOn;

    private Date             activationTime;

    private Date             expirationTime;

    private boolean          skipable;
    
    private long             workItemId = -1;
    
    private AccessType       documentAccessType;

    private String           documentType;

    private long             documentContentId = -1;

    private AccessType       outputAccessType;

    private String           outputType;

    private long             outputContentId = -1;
    
    private String 	         faultName;

    private AccessType       faultAccessType;

    private String           faultType;

    private long             faultContentId = -1;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "TaskData_Comments_Id", nullable = true)
    private List<Comment>    comments    = Collections.emptyList();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "TaskData_Attachments_Id", nullable = true)
    private List<Attachment> attachments = Collections.emptyList();

    public void writeExternal(ObjectOutput out) throws IOException {
        if ( status != null ) {
            out.writeBoolean( true );
            out.writeUTF( status.toString() );
        } else {
            out.writeBoolean( false );
        }
        
        if ( previousStatus != null ) {
            out.writeBoolean( true );
            out.writeUTF( previousStatus.toString() );
        } else {
            out.writeBoolean( false );
        }        

        if ( actualOwner != null ) {
            out.writeBoolean( true );
            actualOwner.writeExternal( out );
        } else {
            out.writeBoolean( false );
        }

        if ( createdBy != null ) {
            out.writeBoolean( true );
            createdBy.writeExternal( out );
        } else {
            out.writeBoolean( false );
        }

        if ( createdOn != null ) {
            out.writeBoolean( true );
            out.writeLong( createdOn.getTime() );
        } else {
            out.writeBoolean( false );
        }

        if ( activationTime != null ) {
            out.writeBoolean( true );
            out.writeLong( activationTime.getTime() );
        } else {
            out.writeBoolean( false );
        }

        if ( expirationTime != null ) {
            out.writeBoolean( true );
            out.writeLong( expirationTime.getTime() );
        } else {
            out.writeBoolean( false );
        }
        
        out.writeBoolean( skipable );
        
        if ( workItemId != -1 ) {
            out.writeBoolean( true );
            out.writeLong( workItemId );
        } else {
            out.writeBoolean( false );
        }
        
        if ( documentAccessType != null ) {
            out.writeBoolean( true );
            out.writeObject( documentAccessType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( documentType != null ) {
            out.writeBoolean( true );
            out.writeUTF( documentType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( documentContentId != -1 ) {
            out.writeBoolean( true );
            out.writeLong( documentContentId );
        } else {
            out.writeBoolean( false );
        }
        
        if ( outputAccessType != null ) {
            out.writeBoolean( true );
            out.writeObject( outputAccessType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( outputType != null ) {
            out.writeBoolean( true );
            out.writeUTF( outputType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( outputContentId != -1 ) {
            out.writeBoolean( true );
            out.writeLong( outputContentId );
        } else {
            out.writeBoolean( false );
        }
        
        if ( faultName != null ) {
            out.writeBoolean( true );
            out.writeUTF( faultName );
        } else {
            out.writeBoolean( false );
        }
        
        if ( faultAccessType != null ) {
            out.writeBoolean( true );
            out.writeObject( faultAccessType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( faultType != null ) {
            out.writeBoolean( true );
            out.writeUTF( faultType );
        } else {
            out.writeBoolean( false );
        }
        
        if ( faultContentId != -1 ) {
            out.writeBoolean( true );
            out.writeLong( faultContentId );
        } else {
            out.writeBoolean( false );
        }
        
        CollectionUtils.writeCommentList( comments,
                                          out );
        CollectionUtils.writeAttachmentList( attachments,
                                             out );
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        if ( in.readBoolean() ) {
            status = Status.valueOf( in.readUTF() );
        }
        
        if ( in.readBoolean() ) {
            previousStatus = Status.valueOf( in.readUTF() );
        }

        if ( in.readBoolean() ) {
            actualOwner = new User();
            actualOwner.readExternal( in );
        }

        if ( in.readBoolean() ) {
            createdBy = new User();
            createdBy.readExternal( in );
        }

        if ( in.readBoolean() ) {
            createdOn = new Date( in.readLong() );
        }

        if ( in.readBoolean() ) {
            activationTime = new Date( in.readLong() );
        }

        if ( in.readBoolean() ) {
            expirationTime = new Date( in.readLong() );
        }

        skipable = in.readBoolean();

        if ( in.readBoolean() ) {
            workItemId = in.readLong();
        }
        
        if ( in.readBoolean() ) {
            documentAccessType = (AccessType) in.readObject();
        }

        if ( in.readBoolean() ) {
            documentType = in.readUTF();
        }
        
        if ( in.readBoolean() ) {
            documentContentId = in.readLong();
        }
        
        if ( in.readBoolean() ) {
            outputAccessType = (AccessType) in.readObject();
        }

        if ( in.readBoolean() ) {
            outputType = in.readUTF();
        }
        
        if ( in.readBoolean() ) {
            outputContentId = in.readLong();
        }
        
        if ( in.readBoolean() ) {
            faultName = in.readUTF();
        }
        
        if ( in.readBoolean() ) {
            faultAccessType = (AccessType) in.readObject();
        }

        if ( in.readBoolean() ) {
            faultType = in.readUTF();
        }
        
        if ( in.readBoolean() ) {
            faultContentId = in.readLong();
        }
        
        comments = CollectionUtils.readCommentList( in );
        attachments = CollectionUtils.readAttachmentList( in );

    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        previousStatus = this.status;
        this.status = status;
    }        

    public Status getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(Status previousStatus) {
        this.previousStatus = previousStatus;
    }

    public User getActualOwner() {
        return actualOwner;
    }

    public void setActualOwner(User actualOwner) {
        this.actualOwner = actualOwner;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(Date activationTime) {
        this.activationTime = activationTime;
    }

    public Date getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    public boolean isSkipable() {
        return skipable;
    }

    public void setSkipable(boolean isSkipable) {
        this.skipable = isSkipable;
    }
    
    public void setWorkItemId(long workItemId) {
    	this.workItemId = workItemId;
    }
    
    public long getWorkItemId() {
    	return workItemId;
    }
             
    public AccessType getDocumentAccessType() {
        return documentAccessType;
    }

    public void setDocumentAccessType(AccessType accessType) {
        this.documentAccessType = accessType;
    }

    public String getDocumentType() {
        return documentType;
    }        

    public long getDocumentContentId() {
        return documentContentId;
    }

    public void setDocumentContentId(long documentContentId) {
        this.documentContentId = documentContentId;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public AccessType getOutputAccessType() {
		return outputAccessType;
	}

	public void setOutputAccessType(AccessType outputAccessType) {
		this.outputAccessType = outputAccessType;
	}

	public String getOutputType() {
		return outputType;
	}

	public void setOutputType(String outputType) {
		this.outputType = outputType;
	}

	public long getOutputContentId() {
		return outputContentId;
	}

	public void setOutputContentId(long outputContentId) {
		this.outputContentId = outputContentId;
	}

	public String getFaultName() {
		return faultName;
	}

	public void setFaultName(String faultName) {
		this.faultName = faultName;
	}

	public AccessType getFaultAccessType() {
		return faultAccessType;
	}

	public void setFaultAccessType(AccessType faultAccessType) {
		this.faultAccessType = faultAccessType;
	}

	public String getFaultType() {
		return faultType;
	}

	public void setFaultType(String faultType) {
		this.faultType = faultType;
	}

	public long getFaultContentId() {
		return faultContentId;
	}

	public void setFaultContentId(long faultContentId) {
		this.faultContentId = faultContentId;
	}

	public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activationTime == null) ? 0 : activationTime.hashCode());
        result = prime * result + CollectionUtils.hashCode( attachments );
        result = prime * result + CollectionUtils.hashCode( comments );
        result = prime * result + ((createdOn == null) ? 0 : createdOn.hashCode());
        result = prime * result + ((expirationTime == null) ? 0 : expirationTime.hashCode());
        result = prime * result + (skipable ? 1231 : 1237);
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((previousStatus == null) ? 0 : previousStatus.hashCode());
        result = prime * result + ((workItemId == -1) ? 0 : (int) workItemId);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !(obj instanceof TaskData) ) return false;
        TaskData other = (TaskData) obj;

        if ( actualOwner == null ) {
            if ( other.actualOwner != null ) return false;
        } else if ( !actualOwner.equals( other.actualOwner ) ) {
            return false;
        }

        if ( createdBy == null ) {
            if ( other.createdBy != null ) return false;
        } else if ( !createdBy.equals( other.createdBy ) ) {
            return false;
        }

        if ( createdOn == null ) {
            if ( other.createdOn != null ) return false;
        } else if ( createdOn.getTime() != other.createdOn.getTime() ) return false;
        if ( expirationTime == null ) {
            if ( other.expirationTime != null ) return false;
        } else if ( expirationTime.getTime() != other.expirationTime.getTime() ) return false;
        if ( skipable != other.skipable ) return false;
        if ( workItemId != other.workItemId) return false;
        if ( status == null ) {
            if ( other.status != null ) return false;
        } else if ( !status.equals( other.status ) ) return false;
        if ( previousStatus == null ) {
            if ( other.previousStatus != null ) return false;
        } else if ( !previousStatus.equals( other.previousStatus ) ) return false;        
        if ( activationTime == null ) {
            if ( other.activationTime != null ) return false;
        } else if ( activationTime.getTime() != other.activationTime.getTime() ) return false;
                
        if ( workItemId != other.workItemId ) return false;
        
        if ( documentAccessType == null ) {
            if ( other.documentAccessType != null ) return false;
        } else if ( !documentAccessType.equals( other.documentAccessType ) ) return false;
        
        if ( documentContentId != other.documentContentId ) return false;
        if ( documentType == null ) {
            if ( other.documentType != null ) return false;
        } else if ( !documentType.equals( other.documentType ) ) return false;                
        
        return CollectionUtils.equals( attachments,
                                       other.attachments ) && CollectionUtils.equals( comments,
                                                                                      other.comments );
    }

}
