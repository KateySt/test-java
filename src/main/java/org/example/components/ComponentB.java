package org.example.components;

import org.example.annotation.*;

@Component
public class ComponentB {

    @Autowired
    public ComponentB(ComponentA componentA) {
    }
}