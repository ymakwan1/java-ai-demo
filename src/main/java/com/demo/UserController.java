package com.demo;

// Mocking Spring Boot architecture annotations
public class UserController {

    // @GetMapping("/api/users")
    public String getUsers() {
        return "['Alice', 'Bob', 'Charlie']";
    }

    // @PostMapping("/api/users")
    public void createUser(String name) {
        System.out.println("Saved user: " + name);
    }
}
