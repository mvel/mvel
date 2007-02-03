/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package org.mvel;

import org.mvel.util.StringAppender;

import java.io.Serializable;
import java.util.ArrayList;

public class FastTokenIterator implements TokenIterator, Serializable {
    private final Token[] token;
    private int length = 0;
    private int cursor = 0;


    public FastTokenIterator(Token[] token) {
        this.length = (this.token = token).length;
    }

    public FastTokenIterator(final FastTokenIterator fi) {
        token = fi.token;
        length = fi.length;
    }

    public FastTokenIterator(TokenIterator map) {
        if (map instanceof FastTokenIterator) {
            this.length = (this.token = ((FastTokenIterator) map).token).length;
        }
        else {
            ArrayList<Token> tokens = new ArrayList<Token>();
            map.reset();
            while (map.hasMoreTokens()) {
                tokens.add(map.nextToken());
            }

            token = tokens.toArray(new Token[length = tokens.size()]);
        }
    }

    public void reset() {
        cursor = 0;
    }


    public Token firstToken() {
        return token[0];
    }

    public Token nextToken() {
        if (cursor < length)
            return token[cursor++];
        else
            return null;
    }


    public void skipToken() {
        cursor++;
    }


    public Token peekNext() {
        if (cursor < length)
            return token[cursor + 1];
        else
            return null;
    }

    public Token peekToken() {
        if (cursor < length)
            return token[cursor];
        else
            return null;
    }


//    public boolean peekNextTokenFlags(int flags) {
//        return cursor < length && (token[cursor].getFlags() & flags) != 0;
//    }


    public Token peekLast() {
        if (cursor > 0) {
            return token[cursor - 1];
        }
        else {
            return null;
        }
    }


    public Token tokensBack(int offset) {
        if (cursor - offset >= 0) {
            return token[cursor - offset];
        }
        else {
            return null;
        }
    }

    public void back() {
        cursor--;
    }

    public boolean hasMoreTokens() {
        return cursor < length;
    }


    public String showTokenChain() {
        StringAppender sb = new StringAppender();
        for (int i = 0; i < length; i++) {
            sb.append("(" + i + "): <<" + token[i].getName() + ">> = <<" + token[i].getLiteralValue() + ">> [" + (token[i].getLiteralValue() != null ? token[i].getLiteralValue().getClass() : "null") + "]").append("\n");
        }

        return sb.toString();
    }


    public int size() {
        return length;
    }


    public int index() {
        return cursor;
    }
}
