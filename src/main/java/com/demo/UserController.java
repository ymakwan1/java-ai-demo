package com.demo;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserController {

    private final Map<Integer, User> userStore = new ConcurrentHashMap<>();
    private final AuditService auditService = new AuditService();

    public UserController() {
        userStore.put(1, new User(1, "Alice", "alice@example.com", true));
        userStore.put(2, new User(2, "Bob", "bob@example.com", true));
    }

    public List<User> getUsers() {

        auditService.log("Fetching all users");

        return userStore.values()
                .stream()
                .filter(User::isActive)
                .sorted(Comparator.comparing(User::getName))
                .toList();
    }

    public User getUserById(int id) {

        auditService.log("Fetching user " + id);

        if (!userStore.containsKey(id)) {
            throw new IllegalArgumentException("User not found");
        }

        return userStore.get(id);
    }

    public User createUser(String name, String email) {

        validateEmail(email);

        int id = userStore.size() + 1;

        User user = new User(id, name, email, true);

        userStore.put(id, user);

        auditService.log("Created user " + name);

        return user;
    }

    public User updateUser(int id, String name, String email) {

        User existing = getUserById(id);

        existing.setName(name);
        existing.setEmail(email);

        auditService.log("Updated user " + id);

        return existing;
    }

    public void deactivateUser(int id) {

        User user = getUserById(id);

        user.setActive(false);

        auditService.log("Soft deleted user " + id);
    }

    private void validateEmail(String email) {

        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    // Inner class for auditing
    private static class AuditService {
        
        public void log(String message) {
            System.out.println(LocalDateTime.now() + " - " + message);
        }
    }

    // Inner class for User
    public static class User {
        private final int id;
        private String name;
        private String email;       
        private boolean active;

        public User(int id, String name, String email, boolean active) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.active = active;
        }
    }
}