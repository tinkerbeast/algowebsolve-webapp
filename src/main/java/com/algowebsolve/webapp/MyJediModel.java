package com.algowebsolve.webapp;

import lombok.Data;

@Data
public class MyJediModel {
    String name;

    MyJediModel(String name) {
        this.name = name;
    }
}
