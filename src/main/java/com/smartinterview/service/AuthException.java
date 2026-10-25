package com.smartinterview.service;

/** A user-facing problem with sign-in, registration or account changes. */
public class AuthException extends Exception {
    public AuthException(String message) {
        super(message);
    }
}
