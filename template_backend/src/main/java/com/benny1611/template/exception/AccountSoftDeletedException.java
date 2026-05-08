package com.benny1611.template.exception;

public class AccountSoftDeletedException extends RuntimeException {
    private final String email;
    public AccountSoftDeletedException(String email) {
        super("ACCOUNT_SOFT_DELETED");
        this.email = email;
    }
    public String getEmail() { return email; }
}
