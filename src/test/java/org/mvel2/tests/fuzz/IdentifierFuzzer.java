package org.mvel2.tests.fuzz;

import org.mvel2.MVEL;
import org.mvel2.util.ParseTools;

import static java.lang.Character.isJavaIdentifierStart;

public class IdentifierFuzzer {

  public static void main(String[] args) {
    try {
      for (int i = 0; i < 1000000; i++) {
        MVEL.compileExpression(getIndentifierSample());
      }
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    System.out.println("**Done**");
  }

  private static char[] getIndentifierSample() {
    int idLength = (int) (10 * Math.random()) + 2;
    char[] sample = new char[idLength + 5];

    boolean ok = false;
    char candidate;
    while (!ok) {
      if (isJavaIdentifierStart(candidate = (char) (128 * Math.random()))) {
        sample[0] = candidate;
        ok = true;
      }
    }
    for (int i = 1; i < idLength; i++) {
      ok = false;
      while (!ok) {
        candidate = (char) (128 * Math.random());
        if (Character.isJavaIdentifierPart(candidate)) {
          sample[i] = candidate;
          ok = true;
        }
      }

    }

    if (ParseTools.isReservedWord(new String(sample, 0, idLength).trim())) {
      return getIndentifierSample();
    }

    sample[idLength] = ' ';
    sample[idLength + 1] = '=';
    sample[idLength + 2] = ' ';
    sample[idLength + 3] = '1';
    sample[idLength + 4] = ';';
    return sample;
  }
}
