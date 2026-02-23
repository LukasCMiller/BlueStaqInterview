package com.notes;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PostgresSQLJDBC {
    Connection c = null;

    /**
     * Setup connection to postgres server running on port 5432
     */
    public void setupDb() {
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection("jdbc:postgresql://db:5432/notesdb","notesuser", "notespass");
        } catch (Exception e) {
            System.err.println("Error connecting to database.");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Opened database successfully");
    }

    /**
     * Persists any notes passed in into database
     * @param note Note to be persisted
     */
    public void persistNote(Note note) {
        String insert = "INSERT INTO note (id, content, created_at) VALUES (?, ?, ?)";
        try {
            // Setup different elements on sql statement to insert into databasew
            PreparedStatement ps = c.prepareStatement(insert);
            ps.setObject(1, note.getId());
            ps.setString(2, note.getContent());
            ps.setObject(3, Timestamp.from(note.getCreatedAt()));
            int rowsAffected = ps.executeUpdate();

            // If any rows are effected that means that the note already exists
            if (rowsAffected > 0) {
                System.out.println("A new user was persisted successfully!");
            }
        } catch (SQLException e) {
            System.err.println("Error inserting note into database.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get all existing notes in the database
     * @return all notes
     */
    public List<Note> getAllNotes() {
        String select = "SELECT * FROM note";
        // List to hold all notes
        List<Note> notes = new ArrayList<>();
        try {
            PreparedStatement ps = c.prepareStatement(select);
            ResultSet rs = ps.executeQuery();

            // Walk through all the elements in the notes table
            while (rs.next()) {
                UUID id = rs.getObject("id", UUID.class);
                String content = rs.getString("content");
                Instant createdAt = rs.getObject("created_at", Timestamp.class).toInstant();
                Note note = new Note(id, content, createdAt);
                notes.add(note);
            }

            return notes;
        } catch (SQLException e) {
            System.err.println("Error retrieving notes from database.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a specific note from the database
     * @param id UUID of note
     * @return Note with matching ID. If no note found return null
     */
    public Note getNoteById(UUID id) {
        String select = "SELECT * FROM note WHERE id = ?";
        Note note = null;

        try {
            PreparedStatement ps = c.prepareStatement(select);
            ps.setObject(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                note = new Note(rs.getObject("id", UUID.class), rs.getString("content"), rs.getObject("created_at", Timestamp.class).toInstant());
            } else {
                // Not an error but should still print to console
                System.out.println("Note with id " + id + " not found!");
            }
            System .out.println("Note with id " + id + " found!");
            return note;
        } catch (SQLException e) {
            System.err.println("Error retrieving notes from database.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a note from the database with a specific UUID
     * @param id UUID of note to be deleted
     */
    public void deleteNote(UUID id) {
        String delete = "DELETE FROM note WHERE id = ?";
        try {
            PreparedStatement ps = c.prepareStatement(delete);
            ps.setObject(1, id);
            ps.executeUpdate();
            System.out.println("Note with id " + id + " deleted!");
        } catch (SQLException e) {
            System.err.println("Error retrieving notes from database.");
            throw new RuntimeException(e);
        }
    }
}

