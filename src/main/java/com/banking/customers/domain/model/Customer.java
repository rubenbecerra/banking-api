package com.banking.customers.domain.model;


import java.util.Objects;

public class Customer {

    private Integer id;
    private String name;
    private String email;
    private Integer age;
    private String gender;
    private String password;
    private Role role;

    public Customer() {}

    public Customer(Integer id, String name, String email, Integer age, String gender, String password, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.password = password;
        this.role = role;
    }

    public Customer(String name, String email, Integer age, String gender, String password, Role role) {
        this.id = null;
        this.name = name;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.password = password;
        this.role = role;
    }

    public Customer(String name, String email, Integer age, String gender, String password) {
        this.id = null;
        this.name = name;
        this.email = email;
        this.age = age;
        this.gender = gender;
        this.password = password;
        this.role = Role.ROLE_USER;
    }

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) && Objects.equals(email, customer.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
}