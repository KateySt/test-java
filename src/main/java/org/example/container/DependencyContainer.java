package org.example.container;

import org.example.annotation.*;
import org.example.interfaces.ComponentListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

public class DependencyContainer {
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Set<Class<?>> circularDependencyCheckSet = new HashSet<>();
    private final List<ComponentListener> preAddListeners = new ArrayList<>();
    private final List<ComponentListener> postAddListeners = new ArrayList<>();

    public void addPreAddListener(ComponentListener listener) {
        preAddListeners.add(listener);
    }

    public void addPostAddListener(ComponentListener listener) {
        postAddListeners.add(listener);
    }

    public void registerComponent(Class<?> clazz) {
        if (instances.containsKey(clazz)) {
            return;
        }

        if (!clazz.isAnnotationPresent(Component.class)) {
            return;
        }

        if (circularDependencyCheckSet.contains(clazz)) {
            throw new RuntimeException("Circular dependency detected: " + clazz.getSimpleName());
        }

        circularDependencyCheckSet.add(clazz);

        preAddListeners.forEach(listener -> listener.onComponentAdded(clazz));

        try {
            Constructor<?>[] constructors = clazz.getConstructors();
            Constructor<?> constructor = getAutowiredConstructor(constructors);
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                Qualifier qualifier = getQualifierAnnotation(constructor.getParameterAnnotations()[i]);
                parameters[i] = getInstance(parameterType, qualifier);
            }

            Object instance = clazz.getClassLoader()
                    .loadClass(clazz.getName())
                    .getDeclaredConstructor(parameterTypes)
                    .newInstance(parameters);
            instances.put(clazz, instance);

            invokePostConstructMethods(instance);

            postAddListeners.forEach(listener -> listener.onComponentAdded(clazz));
        } catch (Exception e) {
            throw new RuntimeException("Failed to register component: " + clazz.getSimpleName(), e);
        }

        circularDependencyCheckSet.remove(clazz);
    }

    private Constructor<?> getAutowiredConstructor(Constructor<?>[] constructors) {
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(Autowired.class)) {
                return constructor;
            }
        }
        return constructors[0];
    }

    private Qualifier getQualifierAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().getName().equals(Qualifier.class.getName())) {
                try {
                    Class<?> qualifierClass = annotation.getClass()
                            .getClassLoader()
                            .loadClass(annotation.annotationType().getName());
                    return (Qualifier) qualifierClass.cast(annotation);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private Object getInstance(Class<?> clazz, Qualifier qualifier) {
        if (qualifier != null) {
            return instances.values().stream()
                    .filter(instance -> clazz.isAssignableFrom(instance.getClass()))
                    .filter(instance -> qualifier.value().equals(getComponentName(instance.getClass())))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Dependency not found: " + clazz.getName()));
        }

        return instances.computeIfAbsent(clazz, key -> {
            registerComponent(key);
            return instances.get(key);
        });
    }

    private void invokePostConstructMethods(Object instance) {
        Class<?> clazz = instance.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(PostConstructor.class)) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke post-constructor method: " + method.getName(), e);
                }
            }
        }
    }

    public <T> T getInstance(Class<T> clazz) {
        Object instance = instances.get(clazz);
        if (instance == null) {
            throw new RuntimeException("Instance not found for class: " + clazz.getName());
        }
        return clazz.cast(instance);
    }

    private String getComponentName(Class<?> clazz) {
        Component componentAnnotation = clazz.getAnnotation(Component.class);
        return componentAnnotation != null ? clazz.getSimpleName() : null;
    }
}
