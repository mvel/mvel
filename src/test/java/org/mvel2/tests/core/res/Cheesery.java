package org.mvel2.tests.core.res;

public class Cheesery {
  private String name;
  private Cheese cheese;

  public Cheesery(String name) {
    this(name, null);
  }

  public Cheesery(String name,
                  Cheese cheese) {
    super();
    this.name = name;
    this.cheese = cheese;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Cheese getCheese() {
    return cheese;
  }

  public void setCheese(Cheese cheese) {
    this.cheese = cheese;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cheese == null) ? 0 : cheese.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final Cheesery other = (Cheesery) obj;
    if (cheese == null) {
      if (other.cheese != null) return false;
    }
    else if (!cheese.equals(other.cheese)) return false;
    if (name == null) {
      if (other.name != null) return false;
    }
    else if (!name.equals(other.name)) return false;
    return true;
  }


}  
