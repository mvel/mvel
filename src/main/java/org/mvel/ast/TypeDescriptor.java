package org.mvel.ast;

import org.mvel.CompileException;
import static org.mvel.ast.ASTNode.COMPILE_IMMEDIATE;
import org.mvel.compiler.ExecutableStatement;
import static org.mvel.util.ArrayTools.findFirst;
import static org.mvel.util.ParseTools.*;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;

public class TypeDescriptor implements Serializable {
    private String className;
    private ArraySize[] arraySize;
    private ExecutableStatement[] compiledArraySize;

    public TypeDescriptor() {
    }

    public TypeDescriptor(char[] name, int fields) {
        updateClassName(name, fields);
    }

    public void updateClassName(char[] name, int fields) {
        int endRange = findFirst('(', name);
        if (endRange == -1) {
            if ((endRange = findFirst('[', name)) != -1) {
                className = new String(name, 0, endRange);
                int to;

                LinkedList<char[]> sizes = new LinkedList<char[]>();

                while (endRange < name.length) {
                    while (endRange < name.length && isWhitespace(name[endRange])) endRange++;

                    if (endRange == name.length) break;

                    if (name[endRange] != '[')
                        throw new CompileException("unexpected token in contstructor", name, endRange);

                    to = balancedCapture(name, endRange, '[');

                    sizes.add(subset(name, ++endRange, to - endRange));
                    endRange = to + 1;
                }

                Iterator<char[]> iter = sizes.iterator();
                arraySize = new ArraySize[sizes.size()];

                for (int i = 0; i < arraySize.length; i++)
                    arraySize[i] = new ArraySize(iter.next());

                if ((fields & COMPILE_IMMEDIATE) != 0) {
                    compiledArraySize = new ExecutableStatement[arraySize.length];
                    for (int i = 0; i < compiledArraySize.length; i++)
                        compiledArraySize[i] = (ExecutableStatement) subCompileExpression(arraySize[i].value);
                }

                return;
            }

            className = new String(name);
        }
        else {
            className = new String(name, 0, endRange);
        }
    }

    public boolean isArray() {
        return arraySize != null;
    }

    public int getArrayLength() {
        return arraySize.length;
    }

    public ArraySize[] getArraySize() {
        return arraySize;
    }

    public ExecutableStatement[] getCompiledArraySize() {
        return compiledArraySize;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
