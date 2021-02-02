package com.example.beans.impl;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An utility class to help us to do some reflection tasks
 */
public class ReflectionUtils {

    public static Map<String, Method> getMethodsWithAnnotation(String packageName, Class annotation) throws IOException, ClassNotFoundException {
        Class[] clzs = getClasses(packageName);
        Map<String, Method> name2Methods = new HashMap<>();
        for (Class c : clzs) {
            Method [] methods  = c.getDeclaredMethods();
            String clzName = c.getSimpleName();
            for (Method m : methods) {
                if ( m.getAnnotation(annotation) != null) {
                    name2Methods.put(clzName + "@" + m.getName(), m);
                }
            }
        }
        return name2Methods;
    }


    /**
     * The spring way to find classes in a JAR or folder
     *
     * @param packageName
     * @return
     * @throws IOException
     */
    public static Class[] getClasses(String packageName) throws IOException, ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(true);

        scanner.addIncludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                ClassMetadata classMetadata = metadataReader.getClassMetadata();
                if (classMetadata.getClassName().contains(packageName)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        });

        Set<BeanDefinition> beans = scanner.findCandidateComponents(packageName);

        ArrayList<Class> classes = new ArrayList<>();
        for (BeanDefinition bean : beans) {
            classes.add(Class.forName(bean.getBeanClassName()));
        }

        return classes.toArray(new Class[0]);
    }

    public static List<Field> getFieldsByAnnotation(Class clz, Class annotationClz) {
        List<Field> allFields = new ArrayList<>();
        while (true) {
            allFields.addAll(Arrays.asList(clz.getDeclaredFields()));
            clz = clz.getSuperclass();
            if (clz == Object.class) {
                break;
            }
        }

        Predicate<Field> isAnnotated =
                field ->
                        Arrays.asList(field.getAnnotations()).stream().filter(a -> a.annotationType() == annotationClz).findAny().isPresent();
        List<Field> annotatedFields = allFields.stream()
                .filter(isAnnotated)
                .collect(Collectors.toList());
        return annotatedFields;
    }


}
