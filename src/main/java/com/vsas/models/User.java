package com.vsas.models;

public class User {
    private String idKey; // optional
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash; // stored as salted hash
    private String type; // ADMIN/GENERAL/GUEST

    public String getIdKey() { return idKey; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPasswordHash() { return passwordHash; }
    public String getType() { return type; }
}
