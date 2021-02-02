package com.example.beans.impl;

import com.example.beans.beans.BeanA;
import com.example.beans.beans.BeanB;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MySpringImpl {

    public static void main(String[] args) throws Exception{
        String testPackage = "com.example.beans.beans";
        Context c = new Context(testPackage);
        System.out.println("Below is A==============");
        BeanA beanA = (BeanA) c.getBean(testPackage + "." + "BeanA");
        beanA.print();
        System.out.println("Below is b==============");
        BeanB beanB = (BeanB) c.getBean(testPackage + "." + "BeanB");
        beanB.printMe();
    }

    public static  class Context {
        private Map<String, Object> earlyExposed = new HashMap<>();
        private Map<String, Object> created = new HashMap<>();
        private Map<String,  ObjectFactory> objectFactoryMap = new HashMap<>();
        private Set<String> creating = new HashSet<>();

        private Map<String, Class> defines = new HashMap<>();

        public Context(String pack) {
            // scans all packages under this classes....
            try {
                Class[] clzList = ReflectionUtils.getClasses(pack);
                // assume all has a @Service tag
                // init all objects
                for (Class c : clzList) {
                    defines.put(c.getName(), c);

                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        }

        public Object getBean(String beanName) throws Exception {
            Object o = getSingleton(beanName);
            if (o == null) {
               o = getSingleton(beanName, new ObjectFactory() {

                    @Override
                    public Object getObject(String c) {
                        try {
                            return creatBean(beanName);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            throw new IllegalArgumentException(e);
                        }
                    }
                });

            }
            return o;
        }

        private Object creatBean(String beanName) throws Exception {
            Object shared = Class.forName(beanName).newInstance();
            if (creating.contains(beanName)) {
                objectFactoryMap.put(beanName, new ObjectHolder(shared));
            }
            // get declared objects
            List<Field> fs = ReflectionUtils.getFieldsByAnnotation(Class.forName(beanName), Autowired.class);
            for (Field f : fs) {
                f.setAccessible(true);
                Object v = getBean(f.getType().getName());
                f.set(shared, v);
            }
            creating.remove(beanName);
            objectFactoryMap.remove(beanName);
            return shared;
        }

        private Object getSingleton(String beanName, ObjectFactory objectFactory) throws Exception {
            return objectFactory.getObject(beanName);
        }
        private Object getSingleton(String beanName) throws Exception {
            if (!defines.containsKey(beanName)) {
                throw new IllegalArgumentException("not register - " + beanName);
            }
            Object target = created.get(beanName);
            if (target != null) {
                return target;
            }

            if (creating.contains(beanName)) {
                target = earlyExposed.get(beanName);
                if (target == null) {
                    ObjectFactory objectFactory = objectFactoryMap.get(beanName);
                    if (objectFactory != null) {
                        target = objectFactory.getObject(beanName);
                        objectFactoryMap.remove(beanName);
                        earlyExposed.put(beanName, target);
                    }
                }
            } else {
                creating.add(beanName);
            }
            return target;
        }


    }

    public interface ObjectFactory {
        Object getObject(String c);
    }

    public static class ObjectHolder implements ObjectFactory {
        private Object o;
        public ObjectHolder(Object o) {
            this.o = o;
        }

        @Override
        public Object getObject(String c) {
            return o;
        }
    }



}
