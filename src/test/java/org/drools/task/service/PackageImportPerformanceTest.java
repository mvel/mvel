package org.drools.task.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import org.drools.task.Deadline;
import org.drools.task.Group;
import org.drools.task.Task;
import org.drools.task.User;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExpressionCompiler;

public class PackageImportPerformanceTest extends TestCase {
    protected Map<String, User>  users;
    protected Map<String, Group> groups;

    @Override
    protected void setUp() throws Exception {
        Map vars = new HashMap();

        Reader reader = new InputStreamReader( getClass().getResourceAsStream( "LoadUsers.mvel" ) );
        users = (Map<String, User>) eval( reader,
                                          vars );

        reader = new InputStreamReader( getClass().getResourceAsStream( "LoadGroups.mvel" ) );
        groups = (Map<String, Group>) eval( reader,
                                            vars );
    }

    protected void tearDown() throws Exception {
    }

    public void testPerformance() throws Exception {
        Map vars = new HashMap();
        vars.put( "users",
                  users );
        vars.put( "groups",
                  groups );

        //Reader reader;
        Reader reader = new InputStreamReader( getClass().getResourceAsStream( "QueryData_UnescalatedDeadlines.mvel" ) );
        long time1 = System.currentTimeMillis();
        
        
        ExpressionCompiler compiler = new ExpressionCompiler( toString( reader ) );
        long time2 = System.currentTimeMillis();
                
        
        ParserContext context = new ParserContext();
        context.addPackageImport( "org.drools.task" );
        context.addPackageImport( "org.drools.task.service" );
        context.addPackageImport( "org.drools.task.query" );
        context.addPackageImport( "java.util" );
        long time3 = System.currentTimeMillis();
        
        
        vars.put( "now",
                  new Date() );
        Serializable expr = compiler.compile( context );               
        long time4 = System.currentTimeMillis();
        
        
        List<Task> tasks = (List<Task>) MVEL.executeExpression( expr,
                                                vars );
        long time5 = System.currentTimeMillis();
        
        
        assertEquals( 2, tasks.size() );
        assertTrue( tasks.get(0) instanceof Task);
        assertTrue( tasks.get(1) instanceof Task);
        
        
        long compilerCreation = (time2 - time1);
        long contextCreation = (time3-time2);
        long compilerCompilation = (time4 - time3);
        long exprExecution = (time5 - time4);
        
        System.out.println( "Compiler Creation : " + compilerCreation );
        System.out.println( "Context Creation : " + contextCreation );
        System.out.println( "Compilation : " + compilerCompilation );
        System.out.println( "execution : " + exprExecution );
        
    }

    public Object eval(Reader reader,
                       Map vars) {
        try {
            return eval( toString( reader ),
                         vars );
        } catch ( IOException e ) {
            throw new RuntimeException( "Exception Thrown",
                                        e );
        }
    }

    public String toString(Reader reader) throws IOException {
        int charValue = 0;
        StringBuffer sb = new StringBuffer( 1024 );
        while ( (charValue = reader.read()) != -1 ) {
            //result = result + (char) charValue;
            sb.append( (char) charValue );
        }
        return sb.toString();
    }

    public Object eval(String str,
                       Map vars) {
        ExpressionCompiler compiler = new ExpressionCompiler( str.trim() );

        ParserContext context = new ParserContext();
        context.addPackageImport( "org.drools.task" );
        context.addPackageImport( "org.drools.task.service" );
        context.addPackageImport( "org.drools.task.query" );
        context.addPackageImport( "java.util" );

        vars.put( "now",
                  new Date() );
        return MVEL.executeExpression( compiler.compile( context ),
                                       vars );
    }

}
