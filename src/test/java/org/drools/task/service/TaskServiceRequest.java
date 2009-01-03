package org.drools.task.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.drools.task.User;

public class TaskServiceRequest {
	
	private String type;
	private User user;
	private Map<String, Object> parameters;
	private boolean allowed = true;
	private List<String> reasons;
	
	public TaskServiceRequest(String type, User user, Map<String, Object> parameters) {
		this.type = type;
		this.user = user;
		this.parameters = parameters;
	}
	
	public String getType() {
		return type;
	}
	
	public User getUser() {
		return user;
	}
	
	public Map<String, Object> getParameters() {
		return parameters;
	}
	
	public Object getParameter(String name) {
		if (parameters == null) {
			return null;
		}
		return parameters.get(name);
	}
	
	public boolean isAllowed() {
		return allowed;
	}
	
	public void setAllowed(boolean allowed) {
		this.allowed = allowed;
	}
	
	public List<String> getReasons() {
		return reasons;
	}
	
	public void addReason(String reason) {
		if (reasons == null) {
			reasons = new ArrayList<String>();
		}
		reasons.add(reason);
	}

}
