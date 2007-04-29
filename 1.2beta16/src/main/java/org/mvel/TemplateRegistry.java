package org.mvel;

import java.io.Reader;

/**
 * Interface to allow MVEL Templates to be registerd so they can be used inside other Templates.
 * Templates are included using the following:
 * <pre>
 * @includeByRef{templateName( var1 = value1 ) }
 * </pre>
 * @author mproctor
 *
 */
public interface TemplateRegistry {
    
    void registerTemplate(String name, String template);
    
    void registerTemplate(Reader reader);    
    
    String getTemplate(String name);

}
