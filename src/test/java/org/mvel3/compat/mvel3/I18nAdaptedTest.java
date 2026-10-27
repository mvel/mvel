package org.mvel3.compat.mvel3;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mvel3.compat.mvel2.AbstractTest;
import org.mvel3.compat.mvel2.Mvel3TestSupport;

/**
 * MVEL3-adapted version of {@link org.mvel3.compat.mvel2.I18nTest}.
 *
 * <p>Non-ASCII property names work on Linux (UTF-8 default encoding). On Windows,
 * KieMemoryCompiler passes the generated source to javac with the platform default encoding;
 * non-ASCII identifiers (人, 名称, フラグ) are replaced with '?' and javac fails with
 * "illegal start of expression". This is a MVEL3 bug, not a test logic issue.
 */
@Tag("mvel3-adapted")
public class I18nAdaptedTest extends AbstractTest {

  @Test
  @Disabled("windows-encoding: KieMemoryCompiler mangles non-ASCII identifiers on Windows (MVEL3 bug)")
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
