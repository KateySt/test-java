package org.example.components;

import org.example.annotation.*;

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