/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mvel2.util;

import static java.lang.System.arraycopy;

/**
 * Utilities for working with reflection.
 */
public class ReflectionUtil {
  private static final int CASE_OFFSET = ('z' - 'Z');

  /**
   * This new method 'slightly' outperforms the old method, it was
   * essentially a perfect example of me wasting my time and a
   * premature optimization.  But what the hell...
   *
   * @param s -
   * @return String
   */
  public static String getSetter(String s) {
    char[] chars = new char[s.length() + 3];

    chars[0] = 's';
    chars[1] = 'e';
    chars[2] = 't';

    if (s.charAt(0) > 'Z') {
      chars[3] = (char) (s.charAt(0) - CASE_OFFSET);
    }
    else {
      chars[3] = s.charAt(0);
    }

    for (int i = s.length() - 1; i != 0; i--) {
      chars[i + 3] = s.charAt(i);
    }

    return new String(chars);
  }


  public static String getGetter(String s) {
    char[] c = s.toCharArray();
    char[] chars = new char[c.length + 3];

    chars[0] = 'g';
    chars[1] = 'e';
    chars[2] = 't';

    if (c[0] > 'Z') {
      chars[3] = (char) (c[0] - CASE_OFFSET);
    }
    else {
      chars[3] = (c[0]);
    }

    arraycopy(c, 1, chars, 4, c.length - 1);

    return new String(chars);
  }


  public static String getIsGetter(String s) {
    char[] c = s.toCharArray();
    char[] chars = new char[c.length + 2];

    chars[0] = 'i';
    chars[1] = 's';

    if (c[0] > 'Z') {
      chars[2] = (char) (c[0] - CASE_OFFSET);
    }
    else {
      chars[2] = c[0];
    }

    arraycopy(c, 1, chars, 3, c.length - 1);

    return new String(chars);
  }

  public static String getPropertyFromAccessor(String s) {
    char[] c = s.toCharArray();
    char[] chars;

    if (c.length > 3 && c[1] == 'e' && c[2] == 't') {
      chars = new char[c.length - 3];

      if (c[0] == 'g' || c[0] == 's') {
        if (c[3] < 'a') {
          chars[0] = (char) (c[3] + CASE_OFFSET);
        }
        else {
          chars[0] = c[3];
        }

        for (int i = 1; i < chars.length; i++) {
          chars[i] = c[i + 3];
        }

        return new String(chars);
      }
      else {
        return s;
      }
    }
    else if (c.length > 2 && c[0] == 'i' && c[1] == 's') {
      chars = new char[c.length - 2];

      if (c[2] < 'a') {
        chars[0] = (char) (c[2] + CASE_OFFSET);
      }
      else {
        chars[0] = c[2];
      }

      for (int i = 1; i < chars.length; i++) {
        chars[i] = c[i + 2];
      }

      return new String(chars);
    }
    return s;
  }

  public static Class<?> toNonPrimitiveType(Class<?> c) {
    if (!c.isPrimitive()) return c;
    if (c == int.class) return Integer.class;
    if (c == long.class) return Long.class;
    if (c == double.class) return Double.class;
    if (c == float.class) return Float.class;
    if (c == short.class) return Short.class;
    if (c == byte.class) return Byte.class;
    if (c == char.class) return Character.class;
    return Boolean.class;
  }
}
