package org.mvel2.tests.core;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mvel2.MVEL;

public class I18nTest extends TestCase {
    
  public void testI18nProperties() {
    I18nPerson p = new I18nPerson();
    p.set名称("杜甫");
    p.setフラグ(true);
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("人", p);
    
    assertTrue((Boolean)MVEL.eval("人.名称 == \"杜甫\"", variables));
    
    // MVEL-300
    assertTrue((Boolean)MVEL.eval("人.フラグ == true", variables));
  }
  
  public class I18nPerson {
      
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
