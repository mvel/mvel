package org.mvel2.tests.fuzz;

import org.mvel2.MVEL;

public class IdentifierFuzzer {

    public static void main(String[] args) {
        try {
            for (int i = 0; i < 10000; i++) {
                char[] identifier = getIndentifierSample();
                System.out.println(identifier);
                MVEL.compileExpression(identifier);
            }
        } catch (Throwable t) {
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
            candidate = (char) (128 * Math.random());
            if (Character.isJavaIdentifierStart(candidate)) {
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
        sample[idLength] = ' ';
        sample[idLength + 1] = '=';
        sample[idLength + 2] = ' ';
        sample[idLength + 3] = '1';
        sample[idLength + 4] = ';';
        return sample;
    }
}
