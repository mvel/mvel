package org.mvel2.templates;

import org.mvel2.templates.res.Node;

public class TemplateDebug {

    public static void decompile(CompiledTemplate t, char[] template) {
        int i = 1;
        for (Node n = t.getRoot(); n != null; n = n.getNext()) {
            System.out.println((i++) + "> " + n.toString() + "['" + new String(template, n.getBegin(), n.getEnd() - n.getBegin()) + "']");
        }
    }

}
