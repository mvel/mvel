package org.mvel2.tests.core.res;

import java.util.List;

public class Task {
  private int priority;
  private List<String> users;
  private List<String> names;

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users;
  }

  public List<String> getNames() {
    return names;
  }

  public void setNames(List<String> names) {
    this.names = names;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((names == null) ? 0 : names.hashCode());
    result = prime * result + priority;
    result = prime * result + ((users == null) ? 0 : users.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof Task)) return false;
    Task other = (Task) obj;
    if (names == null) {
      if (other.names != null) return false;
    }
    else if (!names.equals(other.names)) return false;
    if (priority != other.priority) return false;
    if (users == null) {
      if (other.users != null) return false;
    }
    else if (!users.equals(other.users)) return false;
    return true;
  }


}
