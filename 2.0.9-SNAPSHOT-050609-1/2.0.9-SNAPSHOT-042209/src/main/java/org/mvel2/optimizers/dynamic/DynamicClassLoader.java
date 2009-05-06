/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
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
 */

package org.mvel2.optimizers.dynamic;

import org.mvel2.util.MVELClassLoader;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.LinkedList;


public class DynamicClassLoader extends ClassLoader implements MVELClassLoader {
    private int totalClasses;
    private int tenureLimit;
    //    private final ConcurrentLinkedQueue<DynamicAccessor> allAccessors = new ConcurrentLinkedQueue<DynamicAccessor>();
    private final LinkedList<DynamicAccessor> allAccessors = new LinkedList<DynamicAccessor>();

    private static boolean sunJVM;
    private static Object sunUnsafe;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sunUnsafe = f.get(null);
            sunJVM = true;
        }
        catch (Throwable t) {
            t.printStackTrace();
            sunJVM = false;
        }
    }


    public DynamicClassLoader(ClassLoader classLoader, int tenureLimit) {
        super(classLoader);
        this.tenureLimit = tenureLimit;
    }

    public Class defineClassX(String className, byte[] b, int start, int end) {
        totalClasses++;
        if (sunJVM) {
            return ((Unsafe) sunUnsafe).defineClass(className, b, start, end);
        }
        else {
            return super.defineClass(className, b, start, end);
        }
    }

    public int getTotalClasses() {
        return totalClasses;
    }

    public DynamicAccessor registerDynamicAccessor(DynamicAccessor accessor) {
        allAccessors.add(accessor);
        assert accessor != null;
        return accessor;
    }

    public void deoptimizeAll() {
        synchronized (allAccessors) {
            //        try {
            for (DynamicAccessor a : allAccessors) {
                if (a != null) a.deoptimize();
            }
//            }
//            catch (ConcurrentModificationException e) {
//                // just back out.
//            }
        }
    }

    public boolean isOverloaded() {
        return tenureLimit < totalClasses;
    }
}
