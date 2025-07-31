package com.example.model;

import com.example.graphics.Coord;

public class VoidObj extends GameObject {

    public VoidObj() {
        super(new Coord(0, 0), GameObjType.VOID_OBJ, -1, 0);
        this.enabled = false;
    }

    @Override
    public void update(double deltaTime) {
        
    }
    
}
