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
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/notes", notesHandler);
        server.setExecutor(null);
        server.start();

        // Run tests
        testGetAllNotes();
        testGetNoteById();
        testPostValidNote();
        testDeleteNote();
        testErrorResponsesForPost();
        testErrorMessageForGetNoteById();
        testErrorResponsesForDeleteNote();

        // Stop the server
        server.stop(0);
    }

    /**
     * Test the get all notes call
     * @throws Exception
     */
    private static void testGetAllNotes() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes").toURL().openConnection();
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
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes/" + id).toURL().openConnection();
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

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes").toURL().openConnection();
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
     * Test deleting a specific note
     * @throws Exception
     */
    private static void testDeleteNote() throws Exception {
        UUID id = UUID.randomUUID();
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes/" + id).toURL().openConnection();
        connection.setRequestMethod("DELETE");
        connection.connect();

        assert connection.getResponseCode() == 204 : "Expected 204 No Content";
        System.out.println("DELETE note test passed.");
    }

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

        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes").toURL().openConnection();
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
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes/IAMNOTAUUID").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        assert connection.getResponseCode() == 400 : "Expected 400 OK for bad note retrieval";
        System.out.println("Dont GET note by ID test passed.");
        connection.disconnect();
    }

    /**
     * Test to verify we get a 400 with a non UUID
     * @throws Exception
     */
    private static void testErrorResponsesForDeleteNote() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://localhost:8080/notes/IAMNOTAUUID").toURL().openConnection();
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
