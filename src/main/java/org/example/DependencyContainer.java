package org.example;

import org.example.annotation.Autowired;
import org.example.annotation.Component;
import org.example.annotation.PostConstructor;
import org.example.annotation.Qualifier;
import org.example.interfaces.ComponentListener;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DependencyContainer {
    private final String packagePrefix = this.getClass().getPackage().getName();
    private final Map<Class<?>, Object> instances = new HashMap<>();
    private final Set<Class<?>> circularDependencyCheckSet = new HashSet<>();
    private final List<ComponentListener> preAddListeners = new ArrayList<>();
    private final List<ComponentListener> postAddListeners = new ArrayList<>();
    private boolean autoRegistrationEnabled = false;


    public void addPreAddListener(ComponentListener listener) {
        preAddListeners.add(listener);
        autoRegistrationEnabled = true;
    }

    public void addPostAddListener(ComponentListener listener) {
        postAddListeners.add(listener);
        autoRegistrationEnabled = true;
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
        if (autoRegistrationEnabled) {
            try {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                autoRegisterComponents(packagePrefix, classLoader);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Failed to auto-register components.", e);
            }
            autoRegistrationEnabled = false;
        }
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

    public void autoRegisterComponents(String rootPackage, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        String packagePath = rootPackage.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(packagePath);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                String directoryPath = URLDecoder.decode(resource.getPath(), "UTF-8");
                registerComponentsInDirectory(rootPackage, directoryPath);
            } else if (resource.getProtocol().equals("jar")) {
                registerComponentsInJar(rootPackage, resource);
            }
        }
    }

    private void registerComponentsInDirectory(String packageName, String directoryPath) throws ClassNotFoundException {
        File directory = new File(directoryPath);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String subPackageName = packageName + "." + file.getName();
                    String subPackagePath = directoryPath + "/" + file.getName();
                    registerComponentsInDirectory(subPackageName, subPackagePath);
                } else if (file.isFile() && file.getName().endsWith(".class")) {
                    String className = file.getName().substring(0, file.getName().length() - 6);
                    String fullClassName = packageName + "." + className;
                    Class<?> clazz = Class.forName(fullClassName);
                    registerComponent(clazz);
                }
            }
        }
    }

    private void registerComponentsInJar(String packageName, URL jarUrl) throws IOException, ClassNotFoundException {
        JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
        JarFile jarFile = jarConnection.getJarFile();
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.startsWith(packageName) && entryName.endsWith(".class")) {
                String className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                Class<?> clazz = Class.forName(className);
                registerComponent(clazz);
            }
        }
    }
}
