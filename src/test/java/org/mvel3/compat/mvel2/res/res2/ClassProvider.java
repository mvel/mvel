package org.mvel3.compat.mvel2.res.res2;

public class ClassProvider {
  public PublicClass getPrivate() {
    return new PrivateClass();
  }

  public PublicClass getPublic() {
    return new PublicClass();
  }
}
