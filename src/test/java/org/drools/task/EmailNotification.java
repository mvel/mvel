package org.drools.task;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;


@Entity
public class EmailNotification extends Notification {
    @OneToMany(cascade = CascadeType.ALL)
    private Map<String, EmailNotificationHeader> emailHeaders;
    
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal( out );
        if ( emailHeaders != null ) {
            out.writeInt( emailHeaders.size() );
            for ( EmailNotificationHeader header : emailHeaders.values() ) {
                header.writeExternal( out );
            }
        } else {
            out.writeInt( 0 );
        }
    }
    
    @Override
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        super.readExternal( in );
        int size = in.readInt();
        if ( size > 0 ) {
            emailHeaders = new HashMap<String, EmailNotificationHeader>(size);
            for ( int i = 0; i < size; i++ ) {
                EmailNotificationHeader header = new EmailNotificationHeader();
                header.readExternal( in );
                emailHeaders.put( header.getLanguage(), header);
            }
        }
    }
    
    public NotificationType getNotificationType() {
        return NotificationType.Email;
    }        

    public Map<String, EmailNotificationHeader> getEmailHeaders() {
        return emailHeaders;
    }

    public void setEmailHeaders(Map<String, EmailNotificationHeader> emailHeaders) {
        this.emailHeaders = emailHeaders;
    }    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((emailHeaders == null) ? 0 : emailHeaders.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( !(obj instanceof EmailNotification) ) return false;
        EmailNotification other = (EmailNotification) obj;
        if ( emailHeaders == null ) {
            if ( other.emailHeaders != null ) return false;
        } else if ( !emailHeaders.equals( other.emailHeaders ) ) return false;
        return true;
    }    
    
    
    
}
