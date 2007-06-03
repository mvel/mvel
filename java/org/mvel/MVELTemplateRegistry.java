package org.mvel;

import org.mvel.util.StringAppender;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import static java.util.Collections.synchronizedMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MVELTemplateRegistry implements TemplateRegistry {
    private Map<String, String> registry;

    public MVELTemplateRegistry() {
        //noinspection unchecked
        this.registry = Collections.EMPTY_MAP;
    }

    public String getTemplate(String name) {
        return this.registry.get(name);
    }

    public void registerTemplate(String name, String template) {
        if (this.registry == Collections.EMPTY_MAP) {
            if (MVEL.THREAD_SAFE) {
                this.registry = synchronizedMap(new HashMap<String, String>());
            }
            else {
                this.registry = new HashMap<String, String>();
            }

        }
        this.registry.put(name, template);
    }

    public void registerTemplate(Reader reader) {
        if (reader == null)
            throw new CompileException("Reader cannot be null");
        int nameStart = -1;
        int nameEnd = -1;
        int contentStart = -1;
        int contentEnd;
        StringAppender sb = new StringAppender();
        //  char ch;

        try {
            int c;
            while ((c = reader.read()) != -1) {
                //    ch = (char)c;
                if ('<' == (char) c && sb.charAt(sb.length() - 1) == '<' && sb.charAt(sb.length() - 2) == '='
                        && sb.charAt(sb.length() - 3) == ':' && sb.charAt(sb.length() - 4) == ':') {
                    // we have ::=<< so backtrack to get function name                
                    contentStart = sb.length() + 1;

                    // backtrack to ()
                    int pos = sb.length() - 4;
                    while (sb.charAt(pos) != ')' && sb.charAt(pos - 1) != '(') {
                        pos--;
                    }
                    //pos is now at the end of the template name
                    nameEnd = pos;

                    // backtrack to new line or 
                    while (pos != -1 && sb.charAt(pos) != '\n' && sb.charAt(pos) != '\r' && sb.charAt(pos) != ' ') {
                        pos--;
                    }
                    nameStart = pos + 1;
                }

                if (':' == (char) c && sb.charAt(sb.length() - 1) == ':' && sb.charAt(sb.length() - 2) == '='
                        && sb.charAt(sb.length() - 3) == '>' && sb.charAt(sb.length() - 4) == '>') {
                    // we have ::=>>
                    contentEnd = sb.length() - 4;
                    registerTemplate(new String(sb.getChars(nameStart, nameEnd - nameStart - 1)), new String(sb.getChars(contentStart, contentEnd - contentStart)));
                    nameStart = -1;
                    nameEnd = -1;
                    contentStart = -1;
                    //    contentEnd=-1;
                }
                sb.append((char) c);
            }
        }
        catch (IOException e) {
            //empty
        }
    }

    public boolean isEmpty() {
        return this.registry.isEmpty();
    }

    public Iterator iterateTemplates() {
        return this.registry.keySet().iterator();
    }
}
