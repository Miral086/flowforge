package com.flowforge.cli;

import com.flowforge.core.EngineInfo;

public class Main {

    public static void main(String[] args) {
        String info = EngineInfo.version();
        System.out.println("Starting " + info);
    }
}