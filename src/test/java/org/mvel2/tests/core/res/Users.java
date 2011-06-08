package org.mvel2.tests.core.res;

import java.util.List;

public class Users {
  private List<User> users;

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((users == null) ? 0 : users.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof Users)) return false;
    Users other = (Users) obj;
    if (users == null) {
      if (other.users != null) return false;
    }
    else if (!users.equals(other.users)) return false;
    return true;
  }


}
