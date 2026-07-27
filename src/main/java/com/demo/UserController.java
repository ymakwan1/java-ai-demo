package com.demo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class UserController {

    private final Map<Integer, User> userStore = new ConcurrentHashMap<>();
    private final AuditService auditService = new AuditService();

    public UserController() {
        userStore.put(1, new User(1, "Alice", "alice@example.com", true));
        userStore.put(2, new User(2, "Bob", "bob@example.com", true));
        userStore.put(3, new User(3, "Charlie", "charlie@example.com", false));
        auditService.log("Initialized user store with seed data");
    }

    public List<User> getUsers() {
        auditService.log("Fetching all users");
        return userStore.values()
                .stream()
                .filter(User::isActive)
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> getUsersIncludingInactive() {
        auditService.log("Fetching all users including inactive");
        return userStore.values()
                .stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> getUsersByStatus(boolean active) {
        auditService.log("Filtering users by active=" + active);
        return userStore.values()
                .stream()
                .filter(user -> user.isActive() == active)
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> searchUsers(String query) {
        if (isBlank(query)) {
            return getUsersIncludingInactive();
        }

        String normalized = query.trim().toLowerCase();
        auditService.log("Searching users for query: " + normalized);

        return userStore.values()
                .stream()
                .filter(user -> user.getName().toLowerCase().contains(normalized)
                        || user.getEmail().toLowerCase().contains(normalized))
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public List<User> getUsersPage(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("Page must be 1 or greater");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        List<User> users = getUsersIncludingInactive();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, users.size());

        if (fromIndex >= users.size()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(users.subList(fromIndex, toIndex));
    }

    public List<User> getRecentlyActiveUsers(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must be 1 or greater");
        }

        return userStore.values()
                .stream()
                .filter(User::isActive)
                .limit(limit)
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public User toggleUserStatus(int id) {
        User user = getUserById(id);
        user.setActive(!user.isActive());
        auditService.log("Toggled user status for user " + id);
        return user;
    }

    public boolean hasEmailConflict(String email, Integer excludeId) {
        String normalized = normalizeEmail(email);
        return userStore.values().stream()
                .anyMatch(user -> user.getId() != excludeId && normalizeEmail(user.getEmail()).equals(normalized));
    }

    public User getUserById(int id) {
        auditService.log("Fetching user " + id);

        if (!userStore.containsKey(id)) {
            throw new IllegalArgumentException("User not found");
        }

        return userStore.get(id);
    }

    public boolean exists(int id) {
        return userStore.containsKey(id);
    }

    public int getUserCount() {
        return userStore.size();
    }

    public int getActiveUserCount() {
        return (int) userStore.values().stream().filter(User::isActive).count();
    }

    public int getInactiveUserCount() {
        return (int) userStore.values().stream().filter(user -> !user.isActive()).count();
    }

    public Map<String, Long> getUserStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", (long) getUserCount());
        stats.put("active", (long) getActiveUserCount());
        stats.put("inactive", (long) getInactiveUserCount());
        return stats;
    }

    public User createUser(String name, String email) {
        return createUser(name, email, true);
    }

    public User createUser(String name, String email, boolean active) {
        validateUserInput(name, email);

        if (hasEmailConflict(email, null)) {
            throw new IllegalArgumentException("Email already exists");
        }

        int id = nextId();
        User user = new User(id, name.trim(), normalizeEmail(email), active);
        userStore.put(id, user);

        auditService.log("Created user " + user.getName() + " with id " + id);
        return user;
    }

    public List<User> createUsers(List<String> names, List<String> emails) {
        if (names == null || emails == null || names.size() != emails.size()) {
            throw new IllegalArgumentException("Names and emails must be provided in matching pairs");
        }

        List<User> created = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            created.add(createUser(names.get(i), emails.get(i), true));
        }

        auditService.log("Bulk created " + created.size() + " users");
        return created;
    }

    public User updateUser(int id, String name, String email) {
        return updateUser(id, name, email, null);
    }

    public User updateUser(int id, String name, String email, Boolean active) {
        User existing = getUserById(id);
        validateUserInput(name, email);

        if (hasEmailConflict(email, id)) {
            throw new IllegalArgumentException("Email already exists");
        }

        existing.setName(name.trim());
        existing.setEmail(normalizeEmail(email));
        if (active != null) {
            existing.setActive(active);
        }

        auditService.log("Updated user " + id);
        return existing;
    }

    public void deactivateUser(int id) {
        User user = getUserById(id);
        user.setActive(false);
        auditService.log("Soft deleted user " + id);
    }

    public void deactivateUsers(List<Integer> ids) {
        if (ids == null) {
            throw new IllegalArgumentException("Ids cannot be null");
        }

        for (int id : ids) {
            deactivateUser(id);
        }
        auditService.log("Soft deleted " + ids.size() + " users");
    }

    public void reactivateUser(int id) {
        User user = getUserById(id);
        user.setActive(true);
        auditService.log("Reactivated user " + id);
    }

    public String getUserReport() {
        return "Users: " + getUserCount() + ", active: " + getActiveUserCount() + ", inactive: " + getInactiveUserCount();
    }

    public List<String> getAuditTrail() {
        return auditService.getMessages();
    }

    private int nextId() {
        return userStore.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }

    private void validateUserInput(String name, String email) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("Name is required");
        }
        validateEmail(email);
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class AuditService {
        private final List<String> messages = new ArrayList<>();

        public void log(String message) {
            String entry = LocalDateTime.now() + " - " + message;
            messages.add(entry);
            System.out.println(entry);
        }

        public List<String> getMessages() {
            return new ArrayList<>(messages);
        }
    }

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

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof User user)) {
                return false;
            }
            return id == user.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    ", active=" + active +
                    '}';
        }
    }
}