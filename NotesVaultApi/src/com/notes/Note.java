package com.notes;

import java.time.Instant;
import java.util.UUID;

/**
 * Representaion of Note for backend
 */
public class Note {

    private UUID id;
    private String content;
    private Instant createdAt;

    public Note(UUID id, String content, Instant createdAt) {
        this.id = id;
        this.content = content;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ID " + id + "\nContent " + content + "\nCreatedAt " + createdAt + "\n";
    }
}
