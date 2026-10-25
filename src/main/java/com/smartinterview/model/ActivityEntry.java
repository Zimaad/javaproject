package com.smartinterview.model;

/** One line of the admin activity feed. {@code kind} is "interview" or "signup". */
public record ActivityEntry(String kind, String text, String detail, long at) {
}
