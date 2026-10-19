package org.mvel3.compat.mvel2;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class I18nTest extends AbstractTest {

  @Test
  public void testI18nProperties() {
    I18nPerson p = new I18nPerson();
    p.set名称("杜甫");
    p.setフラグ(true);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("人", p);

    assertTrue((Boolean) Mvel3TestSupport.eval("人.名称 == \"杜甫\"", variables));

    // MVEL-300
    assertTrue((Boolean) Mvel3TestSupport.eval("人.フラグ == true", variables));
  }

  public static class I18nPerson {

    private String 名称; // "name" in Chinese
    private boolean フラグ; // "flag" in Japanese

    public String get名称() {
      return 名称;
    }

    public void set名称(String 名称) {
      this.名称 = 名称;
    }

    public boolean isフラグ() {
      return フラグ;
    }

    public void setフラグ(boolean フラグ) {
      this.フラグ = フラグ;
    }
  }
}
