package com.demo;

public class UserApplication {

    public static void main(String[] args) {
        UserController controller = new UserController();
        UserService service = new UserService(controller);
        UserRepository repository = new UserRepository();

        controller.getUsers().forEach(repository::save);

        System.out.println(service.buildSummary());
    }
}
