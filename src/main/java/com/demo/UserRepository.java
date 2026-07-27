package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {

    private final Map<Integer, UserController.User> users = new ConcurrentHashMap<>();

    public void save(UserController.User user) {
        users.put(user.getId(), user);
    }

    public UserController.User findById(int id) {
        return users.get(id);
    }

    public List<UserController.User> findAll() {
        return new ArrayList<>(users.values());
    }

    public void delete(int id) {
        users.remove(id);
    }

    public int count() {
        return users.size();
    }
}
