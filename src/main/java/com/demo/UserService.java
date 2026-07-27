package com.demo;

import java.util.List;
import java.util.stream.Collectors;

public class UserService {

    private final UserController userController;

    public UserService(UserController userController) {
        this.userController = userController;
    }

    public List<UserController.User> getActiveUsers() {
        return userController.getUsers();
    }

    public List<UserController.User> findUsers(String query) {
        return userController.searchUsers(query);
    }

    public UserController.User createUser(String name, String email) {
        return userController.createUser(name, email);
    }

    public String buildSummary() {
        return "Service has " + userController.getUserCount() + " users and " + userController.getActiveUserCount() + " active users";
    }

    public List<String> getUserNames() {
        return userController.getUsers().stream()
                .map(UserController.User::getName)
                .collect(Collectors.toList());
    }
}
