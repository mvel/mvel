package org.mvel2.tests.core.res.res2;

public class ClassProvider {
  public PublicClass getPrivate() {
    return new PrivateClass();
  }

  public PublicClass getPublic() {
    return new PublicClass();
  }
}
