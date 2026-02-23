package com.notes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Class to handle all incoming requests for the notes app
 */
public class NotesHandler implements HttpHandler {

    private static final int HTTP_OK=200;
    private static final int HTTP_CREATED=201;
    private static final int HTTP_NO_CONTENT=204;
    private static final int HTTP_BAD_REQUEST=400;
    private static final int HTTP_INTERNAL_ERROR=500;

    private PostgresSQLJDBC postgresSQLJDBC;

    public NotesHandler(PostgresSQLJDBC postgresSQLJDBC){
        this.postgresSQLJDBC = postgresSQLJDBC;
    }

    /**
     * @param exchange the exchange containing the request from the
     *                 client and used to send the response
     *
     * Handle different types of request and send out the proper response codes for different situations
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        // Check which type of call it is. If none throw a 400
        try{
            if (exchange.getRequestMethod().equals("GET")) {
                handleGet(exchange);
            } else if (exchange.getRequestMethod().equals("POST")) {
                handlePost(exchange);
            } else if (exchange.getRequestMethod().equals("DELETE")) {
                handleDelete(exchange);
            } else {
                sendResponse(exchange, "Method Not Allowed", HTTP_BAD_REQUEST);
            }
            // Just in case have a catch all to throw a 500 error in case service crashes
        } catch (Exception e) {
            sendResponse(exchange, "Internal Server Error: " + e.getMessage(), HTTP_INTERNAL_ERROR);
        }
    }

    /**
     * Get the note or notes from the database
     * @param exchange Request that came in
     * @throws IOException
     */
    private void handleGet(HttpExchange exchange) throws IOException {
        String response = "";
        String path = exchange.getRequestURI().getPath();
        int returnCode = HTTP_OK;

        // Verify if request is asking for one note or many notes
        if (path.contains("/notes/")) {
            // Get note of id
            UUID id = parseUUIDFromPath(path);
            if (id != null) {
                Note note = postgresSQLJDBC.getNoteById(id);
                response = (note != null) ? note.toString() : "Note not found";
            } else {
                // In case a UUID was not sent correctly
                response = "ERROR 400: Expected UUID";
                returnCode = HTTP_BAD_REQUEST;
            }
        } else {
            // Get all notes
            List<Note> notes = postgresSQLJDBC.getAllNotes();
            response = makeNotes(notes);
        }
        sendResponse(exchange, response, returnCode);
    }

    /**
     * Persist a new note in the database
     * @param exchange Request
     * @throws IOException
     */
    private void handlePost(HttpExchange exchange) throws IOException {
        // Get all parts of the body
        String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

        UUID id = null;
        String noteContent = null;
        Instant timestamp = null;

        // Split up the three different elements of the note
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            try {
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        // Find and store each part of the note
                        case "id":
                            id = parseUUID(keyValue[1]);
                            break;
                        case "content":
                            noteContent = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "timestamp":
                            timestamp = Instant.parse(URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                            break;
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, "ERROR 400: Unexpected error. Null value", HTTP_BAD_REQUEST);
            }
        }

        // If all were recieved persist and send back 201
        if (id != null && noteContent != null && timestamp != null) {
            Note note = new Note(id, noteContent, timestamp);
            postgresSQLJDBC.persistNote(note);
            String response = "Successfully persisted note";
            sendResponse(exchange, response, HTTP_CREATED);
        } else {
            // Otherwise throw an error
            sendResponse(exchange, "ERROR 400: Unexpected error. Null value", HTTP_BAD_REQUEST);
        }
    }

    /**
     * Delete a specified note from the database
     * @param exchange Request
     * @throws IOException
     */
    private void handleDelete(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        UUID id = parseUUIDFromPath(path);

        // So long as the ID is not null delete note
        if (id != null) {
            postgresSQLJDBC.deleteNote(id);
            String response = "Successfully deleted note";
            sendResponse(exchange, response, HTTP_NO_CONTENT);
            return;
        }

        // Otherwise throw a 400
        String response = "ERROR 400: Expected UUID";
        sendResponse(exchange, response, HTTP_BAD_REQUEST);
    }

    /**
     * Send response back to caller
     * @param exchange
     * @param response
     * @param code
     * @throws IOException
     */
    private void sendResponse(HttpExchange exchange,String response, int code) throws IOException {
        System.out.println(exchange.getResponseBody());
        exchange.sendResponseHeaders(code, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    /**
     * Make a string of all notes to send back to caller
     * @param notes Notes from database
     * @return String representing all notes
     */
    private String makeNotes(List<Note> notes){
        StringBuilder sb = new StringBuilder();
        for (Note note : notes){
            sb.append(note.toString());
        }
        return sb.toString();
    }

    /**
     * Get the UUID when a path is given with it
     * @param path full path
     * @return request UUID
     */
    private UUID parseUUIDFromPath(String path) {
        try {
            return UUID.fromString(path.split("/notes/")[1]);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse a UUID from a string representation
     * @param uuid UUID of type UUID
     * @return uuid of type String
     */
    private UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(URLDecoder.decode(uuid, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
