package com.smartinterview.ai;

/** Any failure talking to the AI provider, or reading what it sent back. */
public class AiException extends Exception {
    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }
}
