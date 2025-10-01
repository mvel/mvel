package org.mvel3;

import org.junit.Test;
import org.mvel3.MVELBuilder.TypesBuilder;
import org.mvel3.MVELBuilder.TypesBuilderCollector;

import java.util.Map;

public class DSLTest {

    @Test
    public void test1() {
        MVEL.map(TypesBuilder.add("name", String.class)
                             .add("age", int.class))
            .with(Person.class)
            .out(Integer.class)
            .expression("expr");


        MVEL.map(TypesBuilder.add("name", String.class)
                             .add("age", int.class))
            .out(Integer.class)
            .expression("expr");
        //MVELBuilder.MVELBuilderContent<Map, Void, Object> builder = MVEL.build(Map.class, Void.class, Object.class);
        //builder.

    }
}
