package org.example.sharqi;

// 16. pont: Saját kivétel (exception) létrehozása
public class InvalidFragranceException extends Exception {
    public InvalidFragranceException(String message) {
        super(message);
    }
}