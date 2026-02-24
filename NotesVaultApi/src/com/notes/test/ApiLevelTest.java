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
    private final static String API_KEY = "super-secret-key";

    public static void main(String[] args) {
        try {
            testPostNote();
            testGetAllNotes();
            testGetOneNote();
            testSearchNotes();
            testFilterNotes();
            testUnauthorizedRequest();

            // Since we do some compare we need this to run later
            testUpdateNote();

            // Delete needs to be run last
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
        connection.setRequestProperty("X-API-Key", API_KEY);
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
        connection.setRequestProperty("X-API-Key", API_KEY);

        int responseCode = connection.getResponseCode();
        System.out.println("GET Response Code: " + responseCode);
        if (responseCode == 200) {
            String response = getStringFromInputStream(connection);
            //assert
            assert testedNote.toString().contentEquals(response);
            System.out.println("Notes: " + content);
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
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.setRequestMethod("DELETE");

        assert connection.getResponseCode() == 204 : "Expected 204 No Content";
        System.out.println("DELETE note test passed.");
    }

    /**
     * Test to update the note via the api
     * @throws Exception
     */
    private static void testUpdateNote() throws Exception {
        String newContent = "content=Yippie Im new content";
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL + "/" + id).toURL().openConnection();
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = connection.getOutputStream()) {
            os.write(newContent.getBytes());
        }

        assert connection.getResponseCode() == 200 : "Expected 200 OK for update";
        System.out.println("PUT update note test passed.");
    }

    /**
     * Test to search the notes
     * @throws Exception
     */
    private static void testSearchNotes() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL + "?search=Test").toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for search";
        String response = getStringFromInputStream(connection);
        System.out.println(response);
        assert testedNote.toString().contentEquals(response);
        System.out.println("GET search notes test passed.");
    }

    /**
     * Test to filter out notes based on time
     * @throws Exception
     */
    private static void testFilterNotes() throws Exception {
        //Want to cover everything to make sure we get the note
        String from = "2026-01-01T00:00:00Z";
        String to = "2026-12-31T00:00:00Z";

        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL + "?from=" + from + "&to=" + to).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-Key", API_KEY);
        connection.connect();

        assert connection.getResponseCode() == 200 : "Expected 200 OK for date filter";
        String response = getStringFromInputStream(connection);
        assert response.contains("Test Note Content") : "Expected filtered results to contain test note";
        System.out.println("GET filter notes by date test passed.");
    }

    /**
     * Test to have a 401 happen in api key is not set
     * @throws Exception
     */
    private static void testUnauthorizedRequest() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI(BASE_URL).toURL().openConnection();
        connection.setRequestMethod("GET");
        // Deliberately omit the API key header
        assert connection.getResponseCode() == 401 : "Expected 401 Unauthorized";
        System.out.println("Unauthorized request test passed.");
    }

    /**
     * Helper method to get string from response
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