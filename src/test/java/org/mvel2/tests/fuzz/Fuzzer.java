package org.mvel2.tests.fuzz;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.UnresolveablePropertyException;
import org.mvel2.util.StringAppender;

public class Fuzzer {
    private static final int MAX = 100000000;

    private static final char[] CHAR_TABLE = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '-', '_', '+', '=', '{', '[', '}', ']', ':', ';', '"', '\'', '<', ',', '>',
            '.', '?', '/', '`', ' ', '\t', '\n', '\r'
    };


    public static void main(String[] args) {


        StringAppender append = new StringAppender();
        int len;

        for (int run = 0; run < MAX; run++) {
            len = (int) (Math.random() * 500);
            append.reset();
            for (int i = 0; i < len; i++) {
                append.append(CHAR_TABLE[(int) ((Math.random() * 1000) % CHAR_TABLE.length)]);
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
            catch (Exception e) {
                System.out.println("untrapped error!\n---\n" + append.toString() + "\n---\n");
                System.out.flush();
                e.printStackTrace();
                System.err.flush();
            }

            if (run % 10000 == 0) System.out.println("Run:" + run);
        }
    }


}
