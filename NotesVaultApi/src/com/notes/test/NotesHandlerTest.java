package com.notes.test;

import com.notes.Note;
import com.notes.NotesHandler;
import com.notes.PostgresSQLJDBC;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Business logic test for the notes handler
 */
public class NotesHandlerTest {

    /**
     * Since we wont have a database instance running we need to mock it
     */
    private static class MockPostgresSQLJDBC extends PostgresSQLJDBC {
        @Override
        public List<Note> getAllNotes() {
            return List.of(TEST_NOTE);
        }

        @Override
        public Note getNoteById(UUID id) {
            return TEST_NOTE;
        }

        @Override
        public void persistNote(Note note) {
            System.out.println("Mock persist: " + note);
        }

        @Override
        public void deleteNote(UUID id) {
            System.out.println("Mock delete: " + id);
        }

        @Override
        public void updateNote(UUID id, String newContent) {
            System.out.println("Mock update: " + id + " with content: " + newContent);
        }

        @Override
        public List<Note> searchNotes(String keyword) {
            // Return test note if keyword matches, empty list otherwise
            if (TEST_NOTE.getContent().contains(keyword)) {
                return List.of(TEST_NOTE);
            }
            return List.of();
        }

        @Override
        public List<Note> getNotesByDateRange(Instant from, Instant to) {
            Instant createdAt = TEST_NOTE.getCreatedAt();
            if (!createdAt.isBefore(from) && !createdAt.isAfter(to)) {
                return List.of(TEST_NOTE);
            }
            return List.of();
        }

        @Override
        public void setupDb() {
            System.out.println("Mock setupDb");
        }
    }

    // Note to test with
    private final static Note TEST_NOTE = new Note(UUID.randomUUID(), "Test Note 1", Instant.now());

    public static void main(String[] args) throws Exception {
        // Setup mock and the handler as the uut
        MockPostgresSQLJDBC mockDB = new MockPostgresSQLJDBC();
        NotesHandler notesHandler = new NotesHandler(mockDB);

        // Start the server
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/notes", notesHandler);
        server.setExecutor(null);
        server.start();

        // Run all tests
        testGetAllNotes();
        testGetNoteById();
        testPostValidNote();
        testErrorResponsesForPost();
        testErrorMessageForGetNoteById();
        testErrorResponsesForDeleteNote();
        testUpdateNote();
        testUpdateNoteInvalidUUID();
        testUpdateNoteMissingContent();
        testUpdateNoteTooLong();
        testSearchNotes();
        testSearchNotesNoMatch();
        testFilterNotesByDateInRange();
        testFilterNotesByDateOutOfRange();
        testFilterNotesInvalidTimestamp();
        testPostNoteTooLong();

        // Make sure to stop the server and indicate to user that everything looked good
        server.stop(0);
        System.out.println("All tests passed.");
    }

    /**
     * Test the get all notes call
     * @throws Exception
     */
    private static void testGetAllNotes() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        String returnedString = getStringFromInputStream(connection);

        assert connection.getResponseCode() == 200 : "Expected 200 OK";
        assert returnedString.contains(TEST_NOTE.toString()): "Received note did not match test note";
        System.out.println("GET all notes test passed.");
    }

    /**
     * Testing getting the note by ID
     * @throws Exception
     */
    private static void testGetNoteById() throws Exception {
        UUID id = UUID.randomUUID();
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/" + id).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        String returnedString = getStringFromInputStream(connection);

        assert connection.getResponseCode() == 200 : "Expected 200 OK for note retrieval";
        assert returnedString.contains(TEST_NOTE.toString()): "Received note did not match test note";
        System.out.println("GET note by ID test passed.");
    }

    /**
     * Test the persitance of the note
     * @throws Exception
     */
    private static void testPostValidNote() throws Exception {
        UUID id = UUID.randomUUID();
        String content = "Test Note Content";
        Instant timestamp = Instant.now();

        Note note = new Note(id, content, timestamp);

        String requestBody = "id=" + id + "&content=" + content + "&timestamp=" + timestamp;

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes").toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 201 : "Expected 201 Created";
        System.out.println("POST valid note test passed.");
    }

    /**
     * Test the update call
     * @throws Exception
     */
    private static void testUpdateNote() throws Exception {
        String requestBody = "content=Yippie Im getting updated";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/" + UUID.randomUUID()).toURL().openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 200 : "Expected 200 OK for update";
        System.out.println("PUT update note test passed.");
    }

    /**
     * Test the search call by looking for test in the note
     * @throws Exception
     */
    private static void testSearchNotes() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?search=Test").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for search";
        String response = getStringFromInputStream(connection);
        assert response.contains(TEST_NOTE.toString()) : "Expected search to return matching note";
        System.out.println("GET search notes match test passed.");
    }

    /**
     * Test filtering on valid time
     * @throws Exception
     */
    private static void testFilterNotes() throws Exception {
        // Cover time for Instant.now to be covered
        String from = "2020-01-01T00:00:00Z";
        String to = "2099-01-01T00:00:00Z";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?from=" + from + "&to=" + to).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for date filter";
        String response = getStringFromInputStream(connection);
        assert response.contains(TEST_NOTE.toString()) : "Expected note to appear in date range";
        System.out.println("GET filter notes in range test passed.");
    }

    /**
     * Test searching but for a value that doesnt exist in the mock db
     * @throws Exception
     */
    private static void testSearchNotesNoMatch() throws Exception {
        // Make it so instant.now wont happen
        String from = "2000-01-01T00:00:00Z";
        String to = "2001-01-01T00:00:00Z";
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?search=NOMATCHWILLHAPPEN").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for search with no results";
        String response = getStringFromInputStream(connection);
        assert response.isEmpty() : "Expected empty response for no matching notes";
        System.out.println("GET search notes no match test passed.");
    }

    private static void testFilterNotesByDateInRange() throws Exception {
        // Use a wide range that will contain TEST_NOTE's Instant.now() timestamp
        String from = "2020-01-01T00:00:00Z";
        String to = "2099-01-01T00:00:00Z";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?from=" + from + "&to=" + to).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        System.out.println(connection.getResponseCode());
        assert connection.getResponseCode() == 200 : "Expected 200 OK for date filter";
        String response = getStringFromInputStream(connection);
        assert response.contains(TEST_NOTE.toString()) : "Expected note to appear in date range";
        System.out.println("GET filter notes in range test passed.");
    }

    /**
     * Test that even when note is out of range no error happens
     * @throws Exception
     */
    private static void testFilterNotesByDateOutOfRange() throws Exception {
        // Use a range in the past that wont contain TEST_NOTE's Instant.now() timestamp
        String from = "2000-01-01T00:00:00Z";
        String to = "2001-01-01T00:00:00Z";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?from=" + from + "&to=" + to).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for date filter";
        String response = getStringFromInputStream(connection);
        assert response.isEmpty() : "Expected no notes outside date range";
        System.out.println("GET filter notes out of range test passed.");
    }

    /**
     * Test deleting a specific note
     * @throws Exception
     */
    private static void testDeleteNote() throws Exception {
        UUID id = UUID.randomUUID();
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/" + id).toURL().openConnection();
        connection.setRequestMethod("DELETE");
        connection.connect();

        assert connection.getResponseCode() == 204 : "Expected 204 No Content";
        System.out.println("DELETE note test passed.");
    }


    /****************************
     * Error tests
     ****************************/

    /**
     * Test to make sure we send back a 400 on post when body is incorrect
     * @throws Exception
     */
    private static void testErrorResponsesForPost() throws Exception {
        UUID id = UUID.randomUUID();
        String content = "Test Note Content";
        Instant timestamp = Instant.now();

        Note note = new Note(id, content, timestamp);

        // Make the request body nonsense
        String requestBody = "id= I AM NOT A UUID" + "&content=" + 123 + "&timestamp=" + 5678.0;

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes").toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.connect();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 400 : "Expected 400 Bad Request";
        System.out.println("POST invalid note test passed.");
    }

    /**
     * Test to verify we get a 400 on get with a non UUID
     * @throws Exception
     */
    private static void testErrorMessageForGetNoteById() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/IAMNOTAUUID").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 400 : "Expected 400 OK for bad note retrieval";
        System.out.println("Dont GET note by ID test passed.");
        connection.disconnect();
    }

    /**
     * Verify that the length requierment works
     * @throws Exception
     */
    private static void testPostNoteTooLong() throws Exception {
        String longContent = "a".repeat(1001);
        String requestBody = "id=" + UUID.randomUUID() + "&content=" + longContent + "&timestamp=" + Instant.now();

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes").toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 400 : "Expected 400 for content that is too long";
        System.out.println("POST note too long test passed.");
    }

    /**
     * Try and update with an invalid UUID type
     * @throws Exception
     */
    private static void testUpdateNoteInvalidUUID() throws Exception {
        String requestBody = "content=Updated Content";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/IAMNOTAUUID").toURL().openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 400 : "Expected 400 for invalid UUID on update";
        System.out.println("PUT update invalid UUID test passed.");
    }

    /**
     * Try and update with content that is incorrect in the path
     * @throws Exception
     */
    private static void testUpdateNoteMissingContent() throws Exception {
        String requestBody = "somefield=somevalue";

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/" + UUID.randomUUID()).toURL().openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 400 : "Expected 400 for missing content on update";
        System.out.println("PUT update missing content test passed.");
    }

    /**
     * Test updating when the note is too long to verify length requierment works
     * @throws Exception
     */
    private static void testUpdateNoteTooLong() throws Exception {
        String longContent = "a".repeat(1001);
        String requestBody = "content=" + longContent;

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/" + UUID.randomUUID()).toURL().openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 400 : "Expected 400 for content too long on update";
        System.out.println("PUT update note too long test passed.");
    }

    /**
     * Test to verify that timestamp works
     * @throws Exception
     */
    private static void testFilterNotesInvalidTimestamp() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes?from=NOTADATE").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 400 : "Expected 400 for invalid timestamp";
        System.out.println("GET filter invalid timestamp test passed.");
        connection.disconnect();
    }

    /**
     * Test to verify we get a 400 with a non UUID
     * @throws Exception
     */
    private static void testErrorResponsesForDeleteNote() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8081/notes/IAMNOTAUUID").toURL().openConnection();
        connection.setRequestMethod("DELETE");
        connection.connect();

        assert connection.getResponseCode() == 400 : "Expected 400 OK for bad note delete";
        System.out.println("Dont DELETE note by ID test passed.");
        connection.disconnect();
    }

    /**
     * Helper method to get a string representation of the note to compare to the test note
     */
    private static String getStringFromInputStream(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine).append("\n");
        }

        return content.toString();
    }
}
