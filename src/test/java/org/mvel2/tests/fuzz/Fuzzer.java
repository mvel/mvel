package org.mvel2.tests.fuzz;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ScriptRuntimeException;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.util.StringAppender;

import java.io.IOException;

import static java.lang.Math.random;
import static java.lang.System.currentTimeMillis;

import java.text.DecimalFormat;
import java.util.Random;

public class Fuzzer {
  private static final int MAX = 100000000;

  private static final int[] SALTS = {83, 301, 320, 102, 105, 993, 203, 102, 4904, 1202,
      102, 303, 83, 1, 5, 85, 19, 20, 193, 669, 344, 901,
      930, 12, 1, 1, 89, 61, 8392, 2, 2038, 9, 7, 10, 484, 2301};

  private static final char[] CHAR_TABLE = {
      'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
      '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '{', '[', '}', ']', ':', ';', '"', '\'', '<', ',', '>',
      '.', '?', '/', '`', ' ', '\t', '\n', '\r'
  };

  public static void main(String[] args) throws IOException {
    DecimalFormat df = new DecimalFormat("###,###.##");
    StringAppender append = new StringAppender();
    int len;
    long start = currentTimeMillis();
    long time;
    double rate;

    int seed;

    boolean flip = false;

    Random rand = new Random(System.currentTimeMillis());
    Random rand1 = new Random(System.currentTimeMillis() + 1);
    Random rand2 = new Random(rand1.nextInt());
    Random rand3 = new Random(rand.nextInt(SALTS.length - 1));
    Random rand4 = new Random(rand3.nextInt());

    for (int run = 0; run < MAX; run++) {
      len = (int) (random() * 500) + 10;
      append.reset();

      for (int i = 0; i < len; i++) {
        append.append(CHAR_TABLE[((SALTS[((rand.nextInt(1000)) + 1) % SALTS.length]) * ((flip = !flip) ? rand1.nextInt(1000) : rand2.nextInt(1000)) + 1) % CHAR_TABLE.length]);
        SALTS[rand3.nextInt(SALTS.length - 1)] ^= rand4.nextInt(1000) + 1;
      }


      try {
        MVEL.eval(append.toString());
      }
      catch (UnresolveablePropertyException e) {
        //ignore
      }
      catch (CompileException e) {
        //ignore
      }
      catch (ArithmeticException e) {
        //ignore
      }
      catch (ScriptRuntimeException e) {
        //ignore
      }
      catch (Exception e) {
        System.out.println("untrapped error!\n---\n" + append.toString() + "\n---\n");
        System.out.flush();
        e.printStackTrace();
        System.err.flush();
      }

      if (run % 25000 == 0 && run != 0) {
        long l = time = (currentTimeMillis() - start) / 1000;
        if (l == 0) {
          l = 1;
        }
        rate = run / l;
        System.out.println("Run: " + df.format(run) + " times; "
            + df.format(time) + "secs; " + df.format(rate) + " avg. per second.");
      }

    }
  }


}
