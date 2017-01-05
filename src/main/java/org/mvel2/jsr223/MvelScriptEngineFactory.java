package org.mvel2.jsr223;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.mvel2.MVEL;

/**
 * Created by rahult on 4/7/16.
 */
public class MvelScriptEngineFactory implements ScriptEngineFactory {
    private static final String           ENGINE_NAME        = MVEL.NAME;
    private static final String           ENGINE_VERSION     = MVEL.VERSION;
    private static final String           LANGUAGE_NAME      = "mvel";
    private static final String           LANGUAGE_VERSION   = MVEL.VERSION;

    private static final List<String>     NAMES              = new ArrayList<String>();
    private static final List<String>     EXTENSIONS         = new ArrayList<String>();
    private static final List<String>     MIME_TYPES         = new ArrayList<String>();

    private static final MvelScriptEngine MVEL_SCRIPT_ENGINE = new MvelScriptEngine();

    public MvelScriptEngineFactory() {
        NAMES.add(LANGUAGE_NAME);
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }

    @Override
    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public List<String> getNames() {
        return NAMES;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        if (key.equals(ScriptEngine.NAME)) {
            return getLanguageName();
        } else if (key.equals(ScriptEngine.ENGINE)) {
            return getEngineName();
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return getEngineVersion();
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return getLanguageName();
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return getLanguageVersion();
        } else if (key.equals("THREADING")) {
            return "THREAD-ISOLATED";
        } else {
            return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        return null;
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return null;
    }

    @Override
    public String getProgram(String... statements) {
        return null;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return MVEL_SCRIPT_ENGINE;
    }
}
