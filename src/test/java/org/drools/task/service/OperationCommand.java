package org.drools.task.service;

import java.util.List;

import org.drools.task.Status;

public class OperationCommand {
    private List<Status>        status;
    private List<Status>        previousStatus;
    private List<Allowed> allowed;
    private Status        newStatus;
    private boolean       setNewOwnerToUser;
    private boolean       setNewOwnerToNull;
    private boolean       setToPreviousStatus;
    private boolean       userIsExplicitPotentialOwner;
    private boolean       addTargetUserToPotentialOwners;
    private boolean       removeUserFromPotentialOwners;
    private boolean       skippable;
    private Operation     exec;
    
    public List<Status> getStatus() {
        return status;
    }
    public void setStatus(List<Status> status) {
        this.status = status;
    }
    public List<Status> getPreviousStatus() {
        return previousStatus;
    }
    public void setPreviousStatus(List<Status> previousStatus) {
        this.previousStatus = previousStatus;
    }
    public List<Allowed> getAllowed() {
        return allowed;
    }
    public void setAllowed(List<Allowed> allowed) {
        this.allowed = allowed;
    }
    public Status getNewStatus() {
        return newStatus;
    }        
    
    public boolean isSetNewOwnerToNull() {
        return setNewOwnerToNull;
    }
    public void setSetNewOwnerToNull(boolean setNewOwnerToNull) {
        this.setNewOwnerToNull = setNewOwnerToNull;
    }
    public boolean isAddTargetUserToPotentialOwners() {
        return addTargetUserToPotentialOwners;
    }
    public void setNewStatus(Status newStatus) {
        this.newStatus = newStatus;
    }
    public boolean isSetNewOwnerToUser() {
        return setNewOwnerToUser;
    }
    public void setSetNewOwnerToUser(boolean setNewOwnerToTargetUser) {
        this.setNewOwnerToUser = setNewOwnerToTargetUser;
    }
    public boolean isSetToPreviousStatus() {
        return setToPreviousStatus;
    }
    public void setSetToPreviousStatus(boolean setToPreviousStatus) {
        this.setToPreviousStatus = setToPreviousStatus;
    }
    public boolean isUserIsExplicitPotentialOwner() {
        return userIsExplicitPotentialOwner;
    }
    public void setUserIsExplicitPotentialOwner(boolean userIsExplicitPotentialOwner) {
        this.userIsExplicitPotentialOwner = userIsExplicitPotentialOwner;
    }
    public boolean isAddTargetEntityToPotentialOwners() {
        return addTargetUserToPotentialOwners;
    }
    public void setAddTargetUserToPotentialOwners(boolean addTargetUserToPotentialOwners) {
        this.addTargetUserToPotentialOwners = addTargetUserToPotentialOwners;
    }
    public boolean isRemoveUserFromPotentialOwners() {
        return removeUserFromPotentialOwners;
    }
    public void setRemoveUserFromPotentialOwners(boolean removeUserFromPotentialOwners) {
        this.removeUserFromPotentialOwners = removeUserFromPotentialOwners;
    }
    public boolean isSkippable() {
		return skippable;
	}
	public void setSkippable(boolean skippable) {
		this.skippable = skippable;
	}
	public Operation getExec() {
        return exec;
    }
    public void setExec(Operation exec) {
        this.exec = exec;
    }
    
    
}
