package org.drools.task;

import java.util.Iterator;

public interface UserInfo {
    String getDisplayName(OrganizationalEntity entity);
    
    Iterator<OrganizationalEntity> getMembersForGroup(Group group);
    
    boolean hasEmail(Group group);
    
    String getEmailForEntity(OrganizationalEntity entity);
    
    String getLanguageForEntity(OrganizationalEntity entity);
}
