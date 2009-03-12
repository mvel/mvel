package org.mvel2.marshalling;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;
import org.mvel2.util.StringAppender;

import com.thoughtworks.xstream.XStream;

/**
 * Generates templates to marshaller classes.
 * TODO
 * -Currently uses BeanInfo, needs to handle all MVEL getter/setter types
 * -Use objenesis or equivalent to always be able to handle no-arg constructor
 * -replace @code(....) with built in orb, for slightly better performance
 * -handle special immutable classes like BigInteger, BigDecimal
 * -As well as allowing users to register custom templates, maybe also custom built in marshallers (i.e. how map, collection, array currently works)
 * -Allow custom format for dates
 * -Support optional generated imports, to reduce verbosity
 * -some issue related to values allowed in a Map
 *
 */
public class MarshallingTest extends TestCase {

    public static enum Type {
        PRIMITIVE, STRING, DATE, ARRAY, MAP, COLLECTION, OBJECT;
    }

    public static class ObjectConverter {
        private Class                  type;
        private ObjectConverterEntry[] fields;

        public ObjectConverter(Class type,
                               ObjectConverterEntry[] fields) {
            this.type = type;
            this.fields = fields;
        }

        public Class getType() {
            return this.type;
        }

        public ObjectConverterEntry[] getFields() {
            return fields;
        }
    }

    public static class ObjectConverterEntry {
        private String name;
        private Type   type;
        private Method method;

        public ObjectConverterEntry(String name,
                                    Method method,
                                    Type type) {
            this.name = name;
            this.type = type;
            this.method = method;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public Method getMethod() {
            return this.method;
        }

    }

    public static class MarshallerContext {
        private Marshaller                 marshaller;
        private StringAppender             appender = new StringAppender();
        private MapVariableResolverFactory factory;

        public MarshallerContext(Marshaller marshaller) {
            this.marshaller = marshaller;
            this.appender = new StringAppender();
            this.factory = new MapVariableResolverFactory( new HashMap() );
            this.factory.createVariable( "marshaller",
                                         this );
        }

        public void marshall(Object object) {
            marshaller.marshall( object,
                                 this );
        }

        public StringAppender getAppender() {
            return appender;
        }

        public MapVariableResolverFactory getFactory() {
            return factory;
        }

    }

    public static class Marshaller {
        private Map<Class, ObjectConverter> converters;

        public Marshaller() {
            this.converters = new HashMap<Class, ObjectConverter>();
        }

        public void marshall(Object object,
                             MarshallerContext ctx) {
            marshall( object,
                      getType( object.getClass() ),
                      ctx );
        }

        public void marshall(Object object,
                             Type type,
                             MarshallerContext ctx) {
            if ( object == null ) {
                ctx.getAppender().append( "null" );
                return;
            }

            if ( type != Type.OBJECT ) {
                marshallValue( object,
                               type,
                               ctx );
            } else {
                Class cls = object.getClass();
                ObjectConverter converter = this.converters.get( cls );
                if ( converter == null ) {
                    converter = generateConverter( cls );
                    this.converters.put( cls,
                                         converter );
                }

                try {
                    int i = 0;
                    ctx.getAppender().append( "new " + cls.getName() + "().{ " );
                    for ( ObjectConverterEntry entry : converter.getFields() ) {
                        if ( i++ != 0 ) {
                            ctx.getAppender().append( ", " );
                        }
                        ctx.getAppender().append( entry.getName() );
                        ctx.getAppender().append( " = " );

                        marshallValue( entry.getMethod().invoke( object,
                                                                 null ),
                                       entry.getType(),
                                       ctx );
                    }
                } catch ( Exception e ) {
                    throw new IllegalStateException( "Unable to marshall object " + object,
                                                     e );
                }
                ctx.getAppender().append( " }" );
            }
        }

        private void marshallValue(Object object,
                                   Type type,
                                   MarshallerContext ctx) {
            if ( object == null ) {
                ctx.getAppender().append( "null" );
                return;
            }

            switch ( type ) {
                case PRIMITIVE : {
                    ctx.getAppender().append( object );
                    break;
                }
                case STRING : {
                    ctx.getAppender().append( "'" );
                    ctx.getAppender().append( object );
                    ctx.getAppender().append( "'" );
                    break;
                }
                case DATE : {
                    ctx.getAppender().append( "new java.util.Date(" + ((Date) object).getTime() + ")" );
                    break;
                }
                case ARRAY : {
                    marshallArray( object,
                                   ctx );
                    break;
                }
                case MAP : {
                    marshallMap( (Map) object,
                                 ctx );
                    break;
                }
                case COLLECTION : {
                    marshallCollection( (Collection) object,
                                        ctx );
                    break;
                }
                case OBJECT : {
                    marshall( object,
                              type,
                              ctx );
                    break;
                }
            }
        }

        private ObjectConverter generateConverter(Class cls) {
            BeanInfo beanInfo = null;

            try {
                beanInfo = Introspector.getBeanInfo( cls );
            } catch ( IntrospectionException e ) {
                throw new RuntimeException( e );
            }

            PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
            List<ObjectConverterEntry> list = new ArrayList<ObjectConverterEntry>();

            for ( int i = 0, length = props.length; i < length; i++ ) {
                PropertyDescriptor prop = props[i];
                if ( "class".equals( prop.getName() ) ) {
                    continue;
                }

                list.add( new ObjectConverterEntry( prop.getName(),
                                                    prop.getReadMethod(),
                                                    getType( prop.getPropertyType() ) ) );
            }

            return new ObjectConverter( cls,
                                        list.toArray( new ObjectConverterEntry[list.size()] ) );
        }

        private Type getType(Class cls) {
            Type type = null;
            if ( cls.isPrimitive() || Number.class.isAssignableFrom( cls ) ) {
                type = Type.PRIMITIVE;
            } else if ( String.class.isAssignableFrom( cls ) ) {
                type = Type.STRING;
            } else if ( Date.class.isAssignableFrom( cls ) ) {
                type = Type.DATE;
            } else if ( cls.isArray() ) {
                type = Type.ARRAY;
            } else if ( Map.class.isAssignableFrom( cls ) ) {
                type = Type.MAP;
            } else if ( Collection.class.isAssignableFrom( cls ) ) {
                type = Type.COLLECTION;
            } else {
                type = Type.OBJECT;
            }
            return type;
        }

        private void marshallMap(Map map,
                                 MarshallerContext ctx) {
            ctx.getAppender().append( " [ " );
            int i = 0;
            for ( Iterator<Entry> it = map.entrySet().iterator(); it.hasNext(); i++ ) {
                if ( i != 0 ) {
                    ctx.getAppender().append( ", " );
                }
                Entry entry = it.next();
                marshall( entry.getKey(),
                          ctx );
                ctx.getAppender().append( ':' );
                marshall( entry.getValue(),
                          ctx );

            }
            ctx.getAppender().append( " ] " );
        }

        private void marshallCollection(Collection collection,
                                        MarshallerContext ctx) {
            ctx.getAppender().append( " [ " );
            int i = 0;
            for ( Iterator it = collection.iterator(); it.hasNext(); i++ ) {
                if ( i != 0 ) {
                    ctx.getAppender().append( ", " );
                }
                marshall( it.next(),
                          ctx );
            }
            ctx.getAppender().append( " ] " );
        }

        private void marshallArray(Object array,
                                   MarshallerContext ctx) {
            ctx.getAppender().append( " { " );

            for ( int i = 0, length = Array.getLength( array ); i < length; i++ ) {
                if ( i != 0 ) {
                    ctx.getAppender().append( ", " );
                }
                marshall( Array.get( array,
                                     i ),
                          ctx );
            }
            ctx.getAppender().append( " } " );
        }

        public String marshallToString(Object object) {
            MarshallerContext ctx = new MarshallerContext( this );
            marshall( object,
                      ctx );
            return ctx.getAppender().toString();
        }

    }

    private Object getData() {
        Pet pet = new Pet();
        pet.setName( "rover" );
        pet.setAge( 7 );
        List list = new ArrayList();
        list.add( "a" );
        list.add( 12 );
        list.add( new Date() );
        list.add( new Cheese( "cheddar",
                              6 ) );

        pet.setList( list );
        pet.setArray( new int[]{1, 2, 3} );

        Map map = new HashMap();
        //map.put( new Date(), new Cheese( "stilton", 11) ); // TODO why doesn't this work
        map.put( "key1",
                 13 );
        map.put( "key3",
                 "value3" );
        map.put( "key2",
                 15 );
        map.put( "key4",
                 new Cheese( "stilton",
                             11 ) );
        //map.put( "key4", new String[] { "a", "b" } ); // TODO why doesn't this work

        Person person = new Person();
        person.setName( "mark" );
        person.setAge( 33 );
        person.setPet( pet );
        person.setSomeDate( new Date() );
        person.setMap( map );

        return person;
    }

    private static final int COUNT = 10000;

    public void testXStream() {
        XStream xstream = new XStream();

        // run once to allow for caching
        Object data1 = getData();
        String str = xstream.toXML( data1 );
        System.out.println( str );
        Object data2 = xstream.fromXML( str );
        assertNotSame( data1,
                       data2 );
        assertEquals( data1,
                      data2 );

        long start = System.currentTimeMillis();
        for ( int i = 0; i < COUNT; i++ ) {
            data1 = getData();
            str = xstream.toXML( data1 );
            data2 = xstream.fromXML( str );
            assertNotSame( data1,
                           data2 );
            assertEquals( data1,
                          data2 );
        }
        long end = System.currentTimeMillis();

        System.out.println( "xstream : " + (end - start) );
    }

    public void testMVEL() throws Exception {
        Marshaller marshaller = new Marshaller();

        // run once to generate templates
        Object data1 = getData();
        String str = marshaller.marshallToString( data1 );
        System.out.println( str );
        Object data2 = MVEL.eval( str );
        assertNotSame( data1,
                       data2 );
        assertEquals( data1,
                      data2 );

        long start = System.currentTimeMillis();
        for ( int i = 0; i < COUNT; i++ ) {
            data1 = getData();
            str = marshaller.marshallToString( data1 );
            data2 = MVEL.eval( str );
            assertNotSame( data1,
                           data2 );
            assertEquals( data1,
                          data2 );
        }
        long end = System.currentTimeMillis();

        System.out.println( "mvel : " + (end - start) );
    }

    public static class Person {
        private String name;
        private int    age;
        private Date   someDate;

        private Pet    pet;

        private Object nullTest;

        private Map    map;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Pet getPet() {
            return this.pet;
        }

        public void setPet(Pet pet) {
            this.pet = pet;
        }

        public Date getSomeDate() {
            return someDate;
        }

        public void setSomeDate(Date someDate) {
            this.someDate = someDate;
        }

        public Object getNullTest() {
            return nullTest;
        }

        public void setNullTest(Object nullTest) {
            this.nullTest = nullTest;
        }

        public Map getMap() {
            return map;
        }

        public void setMap(Map map) {
            this.map = map;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + age;
            result = prime * result + ((map == null) ? 0 : map.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((nullTest == null) ? 0 : nullTest.hashCode());
            result = prime * result + ((pet == null) ? 0 : pet.hashCode());
            result = prime * result + ((someDate == null) ? 0 : someDate.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Person other = (Person) obj;
            if ( age != other.age ) return false;
            if ( map == null ) {
                if ( other.map != null ) return false;
            } else if ( !map.equals( other.map ) ) return false;
            if ( name == null ) {
                if ( other.name != null ) return false;
            } else if ( !name.equals( other.name ) ) return false;
            if ( nullTest == null ) {
                if ( other.nullTest != null ) return false;
            } else if ( !nullTest.equals( other.nullTest ) ) return false;
            if ( pet == null ) {
                if ( other.pet != null ) return false;
            } else if ( !pet.equals( other.pet ) ) return false;
            if ( someDate == null ) {
                if ( other.someDate != null ) return false;
            } else if ( !someDate.equals( other.someDate ) ) return false;
            return true;
        }

    }

    public static class Pet {
        private String  name;
        private Integer age;

        private List    list;
        private int[]   array;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer agr) {
            this.age = agr;
        }

        public List getList() {
            return list;
        }

        public void setList(List list) {
            this.list = list;
        }

        public int[] getArray() {
            return array;
        }

        public void setArray(int[] array) {
            this.array = array;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((age == null) ? 0 : age.hashCode());
            result = prime * result + Arrays.hashCode( array );
            result = prime * result + ((list == null) ? 0 : list.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Pet other = (Pet) obj;
            if ( age == null ) {
                if ( other.age != null ) return false;
            } else if ( !age.equals( other.age ) ) return false;
            if ( !Arrays.equals( array,
                                 other.array ) ) return false;
            if ( list == null ) {
                if ( other.list != null ) return false;
            } else if ( !list.equals( other.list ) ) return false;
            if ( name == null ) {
                if ( other.name != null ) return false;
            } else if ( !name.equals( other.name ) ) return false;
            return true;
        }

    }

    public static class Cheese {
        private String  type;
        private int     age;
        private boolean edible;

        public Cheese() {

        }

        public Cheese(String type,
                      int age) {
            this.type = type;
            this.age = age;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public boolean isEdible() {
            return edible;
        }

        public void setEdible(boolean edible) {
            this.edible = edible;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + age;
            result = prime * result + (edible ? 1231 : 1237);
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if ( this == obj ) return true;
            if ( obj == null ) return false;
            if ( getClass() != obj.getClass() ) return false;
            Cheese other = (Cheese) obj;
            if ( age != other.age ) return false;
            if ( edible != other.edible ) return false;
            if ( type == null ) {
                if ( other.type != null ) return false;
            } else if ( !type.equals( other.type ) ) return false;
            return true;
        }

    }
}
