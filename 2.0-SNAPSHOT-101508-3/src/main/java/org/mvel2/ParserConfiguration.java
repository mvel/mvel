package org.mvel2;

import org.mvel2.compiler.AbstractParser;
import org.mvel2.integration.Interceptor;
import org.mvel2.util.MethodStub;
import static org.mvel2.util.ParseTools.getSimpleClassName;
import org.mvel2.util.PropertyTools;

import java.io.Serializable;
import static java.lang.Thread.currentThread;
import java.lang.reflect.Method;
import java.util.*;

public class ParserConfiguration implements Serializable {
    protected Map<String, Object> imports;
    protected Set<String> packageImports;
    protected Map<String, Interceptor> interceptors;
    protected transient ClassLoader classLoader = currentThread().getContextClassLoader();

    public ParserConfiguration() {
    }

    public ParserConfiguration(Map<String, Object> imports, Map<String, Interceptor> interceptors) {
        if (imports != null) {
            this.imports = new HashMap<String, Object>();
            Object o;

            for (Map.Entry<String, Object> entry : imports.entrySet()) {
                if ((o = entry.getValue()) instanceof Method) {
                    this.imports.put(entry.getKey(), new MethodStub((Method) o));
                }
                else {
                    this.imports.put(entry.getKey(), o);
                }
            }
        }

        this.interceptors = interceptors;
    }

    public Set<String> getPackageImports() {
        return packageImports;
    }

    public void setPackageImports(Set<String> packageImports) {
        this.packageImports = packageImports;
    }

    public Class getImport(String name) {
        return (imports != null && imports.containsKey(name) ? (Class) imports.get(name) : (Class) AbstractParser.LITERALS.get(name));
    }

    public MethodStub getStaticImport(String name) {
        return imports != null ? (MethodStub) imports.get(name) : null;
    }

    public Object getStaticOrClassImport(String name) {
        return (imports != null && imports.containsKey(name) ? imports.get(name) : AbstractParser.LITERALS.get(name));
    }

    public void addPackageImport(String packageName) {
        if (packageImports == null) packageImports = new HashSet<String>();
        packageImports.add(packageName);
    }

    private boolean checkForDynamicImport(String className) {
        if (packageImports == null) return false;

        int found = 0;
        Class cls = null;
        for (String pkg : packageImports) {
            try {
                cls = classLoader.loadClass(pkg + "." + className);
                found++;
            }
            catch (ClassNotFoundException e) {
                // do nothing.
            }
            catch (NoClassDefFoundError e) {
                if (PropertyTools.contains(e.getMessage(), "wrong name")) {
                    // do nothing.  this is a weirdness in the jvm.
                    // see MVEL-43
                }
                else {
                    throw e;
                }
            }
        }

        if (found > 1) {
            throw new CompileException("ambiguous class name: " + className);
        }
        else if (found == 1) {
            addImport(className, cls);
            return true;
        }
        else {
            return false;
        }
    }

    public boolean hasImport(String name) {
        return (imports != null && imports.containsKey(name)) ||
                (!"this".equals(name) && !"self".equals(name) && !"empty".equals(name) && !"null".equals(name) &&
                        !"nil".equals(name) && !"true".equals(name) && !"false".equals(name)
                        && AbstractParser.LITERALS.containsKey(name))
                || checkForDynamicImport(name);
    }

    public void addImport(Class cls) {
        addImport(getSimpleClassName(cls), cls);
    }

    public void addImport(String name, Class cls) {
        if (this.imports == null) this.imports = new LinkedHashMap<String, Object>();
        this.imports.put(name, cls);
    }

    public void addImport(String name, Method method) {
        addImport(name, new MethodStub(method));
    }

    public void addImport(String name, MethodStub method) {
        if (this.imports == null) this.imports = new LinkedHashMap<String, Object>();
        this.imports.put(name, method);
    }

    public Map<String, Interceptor> getInterceptors() {
        return interceptors;
    }

    public void setInterceptors(Map<String, Interceptor> interceptors) {
        this.interceptors = interceptors;
    }

    public Map<String, Object> getImports() {
        return imports;
    }

    public void setImports(Map<String, Object> imports) {
        if (imports == null) return;

        Object val;

        for (Map.Entry<String, Object> entry : imports.entrySet()) {
            if ((val = entry.getValue()) instanceof Class) {
                addImport(entry.getKey(), (Class) val);
            }
            else if (val instanceof Method) {
                addImport(entry.getKey(), (Method) val);
            }
            else if (val instanceof MethodStub) {
                addImport(entry.getKey(), (MethodStub) val);
            }
            else {
                throw new RuntimeException("invalid element in imports map: " + entry.getKey() + " (" + val + ")");
            }
        }
    }

    public boolean hasImports() {
        return (imports != null && imports.size() != 0) || (packageImports != null && packageImports.size() != 0);
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
