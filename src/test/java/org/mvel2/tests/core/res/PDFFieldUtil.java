package org.mvel2.tests.core.res;

import java.util.Calendar;
import java.util.Date;

public class PDFFieldUtil {
  public int calculateAge(Date gebDat) {
    Calendar c2 = Calendar.getInstance();
    c2.set(2023,
           11,
           25); // 2023 dec 25
    long milliAge = c2.getTimeInMillis() - gebDat.getTime();
    Calendar geburtsTag = Calendar.getInstance();
    geburtsTag.setTimeInMillis(milliAge);
    return geburtsTag.get(Calendar.YEAR) - 1970; // get age of a person who was born in gebDat on 2023 dec 25
  }
}
