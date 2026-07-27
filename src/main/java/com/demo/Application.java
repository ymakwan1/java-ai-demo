package com.demo;

public class Application {
    public static void main(String[] args) {
        System.out.println("Starting Java Application...");
        System.out.println(greet("Developer"));
    }

    public static String greet(String name) {
        return "Hello, " + name + "!";
    }
}
