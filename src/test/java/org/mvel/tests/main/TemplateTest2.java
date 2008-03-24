package org.mvel.tests.main;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import junit.framework.TestCase;

public class TemplateTest2 extends TestCase {

    public static final String OK_TEMPLATE =
            "DELIVERY: 5 Sheath Street <br />\n" +
                    "EMAIL: nicola at realimage.co.nz</p>\n" +
                    "    <div id=\"contactCheckBoxes\">Where did you hear about Nicola?\n" +
                    "    <p class=\"contactInput\">@{someval} Bride and Groom Magazine</p>";

    public static final String BAD_TEMPLATE =
            "DELIVERY: 5 Sheath Street <br />\n" +
                    "EMAIL: nicola@@realimage.co.nz</p>\n" +
                    "    <div id=\"contactCheckBoxes\">Where did you hear about Nicola?\n" +
                    "    <p class=\"contactInput\">@{someval} Bride and Groom Magazine</p>";

      /**
     * This fails because there is an @ symbol in the template which is
     *  interpreted as denoting an expression, when in fact its just part
     *  of content. It is not connected to curly braces
     *
     * @throws java.io.IOException
     */
    public void test_fails() throws IOException {
        runTemplate(BAD_TEMPLATE);
    }

    /**
     * This template is the same as above except that the @ symbol has been replaced
     * with ' at '. It succeeds.
     *
     * @throws java.io.IOException
     */
    public void test_works() throws IOException {
        runTemplate(OK_TEMPLATE);
    }



    private void runTemplate(String template) throws IOException {
        Map map = new HashMap();
        map.put("someval", "some value goes here");
        String r = TemplateInterpreter.evalToString(template,map);
        System.out.println("r: " + r);
    }
}
