package org.example.components;

import org.example.annotation.*;

@Component
public class ComponentD {

    @Autowired
    public ComponentD(ComponentB componentB) {
    }
}