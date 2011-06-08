package org.mvel2.tests.core.res;

import java.util.Calendar;
import java.util.Date;

public class PDFFieldUtil {
  public int calculateAge(Date gebDat) {
    long milliAge = System.currentTimeMillis() - gebDat.getTime();
    Calendar geburtsTag = Calendar.getInstance();
    geburtsTag.setTimeInMillis(milliAge);
    return geburtsTag.get(Calendar.YEAR) - 1970;
  }
}
