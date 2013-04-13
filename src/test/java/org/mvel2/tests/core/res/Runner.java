/**
 *
 */
package org.mvel2.tests.core.res;

public class Runner
{

    public String run(String... args)
    {
        StringBuilder result = new StringBuilder();
        if (args == null)
        {
            return "null";
        }
        for (String arg : args)
        {
            result.append(arg);
            result.append(",");
        }
        return result.toString();
    }

    public String run(String name, String... args)
    {
        StringBuilder result = new StringBuilder();
        if (args == null)
        {
            return name + "null";
        }
        for (String arg : args)
        {
            result.append(arg);
            result.append(",");
        }
        return name + result.toString();
    }
}
