package org.example;

import java.io.IOException;

public class IoC {
    private static final DependencyContainer container = new DependencyContainer();

    public static void registerBean(Object bean) {
        container.registerComponent(bean.getClass());
    }

    public static <T> T getBean(Class<T> clazz) {
        return container.getInstance(clazz);
    }

    public static void init(String rootPackage, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        container.autoRegisterComponents(rootPackage, classLoader);
    }
}
