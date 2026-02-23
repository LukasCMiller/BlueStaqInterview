package com.notes.test;

import com.notes.Note;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Test to run the api from end to end
 */
public class ApiLevelTest {

    private final static UUID id = UUID.randomUUID();
    private final static String content = "Test Note Content";
    private final  static Instant timestamp = Instant.parse("2026-02-23T01:25:19.013092Z");
    private final static Note testedNote = new Note(id, content, timestamp);
    private final static String BASE_URL = "http://localhost:8080/notes";

    public static void main(String[] args) {
        try {
            testPostNote();
            testGetAllNotes();
            testGetOneNote();
            testDeleteNote();
        } catch (Exception e) {
            System.err.println("Something went wrong in the test");
            System.err.println(e);
        }
    }

    /**
     * Test persisting the note
     * @throws Exception
     */
    private static void testPostNote() throws Exception {
        String requestBody = "id=" + id + "&content=" + content + "&timestamp=" + timestamp;

        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL).toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        assert connection.getResponseCode() == 201 : "Expected 201 Created";
    }

    /**
     * Test the get call for all notes
     * @throws Exception
     */
    private static void testGetAllNotes() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL).toURL().openConnection();
        setupGetRequest(connection);
    }

    /**
     * Test teh get call for one note
     * @throws Exception
     */
    private static void testGetOneNote() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL + "/" + id).toURL().openConnection();
        setupGetRequest(connection);
    }

    /**
     * Since both get tests had a lot of common code pull it into a method
     * @param connection
     * @throws IOException
     */
    private static void setupGetRequest(HttpURLConnection connection) throws IOException {
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        System.out.println("GET Response Code: " + responseCode);
        if (responseCode == 200) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
                //assert
                assert testedNote.toString().contentEquals(content);
                System.out.println("Notes: " + content);
            }
        } else {
            System.out.println("Failed to retrieve notes.");
        }
    }

    /**
     * Test delete a note
     * @throws Exception
     */
    private static void testDeleteNote() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL + "/" + id).toURL().openConnection();
        connection.setRequestMethod("DELETE");

        assert connection.getResponseCode() == 204 : "Expected 204 No Content";
        System.out.println("DELETE note test passed.");
    }
}