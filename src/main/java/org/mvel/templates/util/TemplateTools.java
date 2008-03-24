package org.mvel.templates.util;

import org.mvel.templates.res.Node;
import org.mvel.templates.res.TerminalNode;
import static org.mvel.util.ParseTools.balancedCapture;

public class TemplateTools {
    public static Node getLastNode(Node node) {
        Node n = node;
        while (true) {
            if (n.getNext() instanceof TerminalNode) return n;
            n = n.getNext();
        }
    }


    public static int captureToEOS(char[] expression, int cursor) {
        int length = expression.length;
        while (cursor != length) {
            switch (expression[cursor]) {
                case '(':
                case '[':
                case '{':
                    cursor = balancedCapture(expression, cursor, expression[cursor]);
                    break;

                case ';':
                case '}':
                    return cursor;

            }
            cursor++;
        }

        return cursor;
    }
}
