package org.mvel2.tests.core.res;

/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static org.mvel2.MVEL.parseMacros;

import org.mvel2.Macro;

import java.util.HashMap;
import java.util.Map;

public class KnowledgeHelperFixer {

  public String fix(final String raw) {
    if (raw == null) {
      return raw;
    }

    Map macros = new HashMap(5);

    macros.put("insert",
        new Macro() {
          public String doMacro() {
            return "drools.insert";
          }
        });

    macros.put("insertLogical",
        new Macro() {
          public String doMacro() {
            return "drools.insertLogical";
          }
        });

    macros.put("modifyRetract",
        new Macro() {
          public String doMacro() {
            return "drools.modifyRetract";
          }
        });

    macros.put("modifyInsert",
        new Macro() {
          public String doMacro() {
            return "drools.modifyInsert";
          }
        });

    macros.put("update",
        new Macro() {
          public String doMacro() {
            return "drools.update";
          }
        });

    macros.put("retract",
        new Macro() {
          public String doMacro() {
            return "drools.retract";
          }
        });


    return parseMacros(raw, macros);
  }
}