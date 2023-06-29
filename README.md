# Dependency Injection Container

This is a simple dependency injection container implementation that provides a way to register components and perform dependency injection using annotations, similar to the Spring framework.

## Annotations

**@Component**: This annotation is applied to a class and indicates that the class is a component and should be registered in the dependency container.

**@Autowired**: This annotation is applied to a constructor and indicates that the constructor should be used for dependency injection into the component.

**@Qualifier**: This annotation is applied to constructor parameters and identifies the parameter by a string. It allows specifying which specific component implementation to use for dependency injection.

**@PostConstructor**: This annotation is applied to a method that should be called immediately after the component instance is created.

## Class DependencyContainer

This class represents the dependency container. It contains the main logic for registering components and injecting dependencies.

- **instances**: A map that stores instances of registered components.
- **circularDependencyCheckSet**: A set used for checking circular dependencies between components.
- **preAddListeners and postAddListeners**: Lists of listeners that are called before and after adding components to the container.
- **addPreAddListener and addPostAddListener**: Methods for adding listeners.
- **registerComponent**: A method that registers a component in the container.
- **getAutowiredConstructor**: A method that returns a constructor with the `@Autowired` annotation for a given class.
- **getQualifierAnnotation**: A method that returns the `@Qualifier` annotation for a given array of constructor parameter annotations.
- **getInstance**: A method that returns an instance of a component for a given class. If the instance is not found, it is created and registered.
- **invokePostConstructMethods**: A method that invokes methods with the `@PostConstructor` annotation for a given component instance.
- **getInstance**: A method that returns an instance of a component for a given class from the container.
- **getComponentName**: A method that returns the component name based on the `@Component` annotation.

## Interface ComponentListener

This interface defines the `onComponentAdded` method, which is called when a component is added to the container.

## Component classes

The following component classes are provided as examples:

- **ComponentA**: Represents a component that provides a `doSomething()` method.
- **ComponentB**: Depends on `ComponentA` and is injected through the constructor.
- **ComponentC**: Depends on `ComponentB` and is injected through the constructor. It also has a `@PostConstructor` method.
- **ComponentD**: Depends on `ComponentB` and is injected through the constructor.

## Execution result example

1. Creat components:

```
@Component
public class ComponentA {
public void doSomething() {
System.out.println("ComponentA: doSomething()");
}
}
```
```
@Component
public class ComponentB {
    @Autowired
    public ComponentB(ComponentA componentA) {
    }
}
```
```
@Component
public class ComponentC {
    @Autowired
    public ComponentC(ComponentB componentB) {
    }
    @PostConstructor
    public void init() {
        System.out.println("ComponentC: Initialized");
    }
}
```
```
@Component
public class ComponentD {
    @Autowired
    public ComponentD(ComponentB componentB) {
    }
}
```

2.Main

```
public class Main {
    public static void main(String[] args) {
        DependencyContainer container = DependencyContainer.getContext();
        ComponentA componentA = container.getInstance(ComponentA.class);
        componentA.doSomething();
    }
}
``` 

**Result**:

Adding component: ComponentA

Component added: ComponentA

Adding component: ComponentB

Component added: ComponentB

Adding component: ComponentC

ComponentC: Initialized

Component added: ComponentC

Adding component: ComponentD

Component added: ComponentD

ComponentA: doSomething()
