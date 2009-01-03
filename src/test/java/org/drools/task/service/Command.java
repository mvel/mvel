package org.drools.task.service;

import java.io.Serializable;
import java.util.List;

public class Command implements Serializable {
    
    private int id;
    
    private CommandName name;    
    
    private List<Object> arguments;
    
    public Command(int id, CommandName name, List<Object> arguments) {
        super();
        this.id = id;
        this.arguments = arguments;
        this.name = name;
    }
    
    
    
    public int getId() {
        return id;
    }



    public void setId(int id) {
        this.id = id;
    }



    public CommandName getName() {
        return name;
    }
    public void setName(CommandName name) {
        this.name = name;
    }
    public List<Object> getArguments() {
        return arguments;
    }
    public void setArguments(List<Object> arguments) {
        this.arguments = arguments;
    }
            
}
