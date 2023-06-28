package org.example;

import org.example.components.ComponentA;
import org.example.components.ComponentB;
import org.example.components.ComponentC;
import org.example.components.ComponentD;
import org.example.container.DependencyContainer;

public class Main {
    public static void main(String[] args) {
        DependencyContainer container = new DependencyContainer();

        container.addPreAddListener(clazz -> System.out.println("Adding component: " + clazz.getSimpleName()));
        container.addPostAddListener(clazz -> System.out.println("Component added: " + clazz.getSimpleName()));

        container.registerComponent(ComponentA.class);
        container.registerComponent(ComponentB.class);
        container.registerComponent(ComponentC.class);
        container.registerComponent(ComponentD.class);

        ComponentA componentA = container.getInstance(ComponentA.class);
        componentA.doSomething();
    }
}

