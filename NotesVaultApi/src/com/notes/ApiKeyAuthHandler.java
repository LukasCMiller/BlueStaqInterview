package com.notes;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class ApiKeyAuthHandler implements HttpHandler {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final Set<String> validApiKeys;
    private final HttpHandler next;

    public ApiKeyAuthHandler(HttpHandler next, Set<String> validApiKeys) {
        this.next = next;
        this.validApiKeys = validApiKeys;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String apiKey = exchange.getRequestHeaders().getFirst(API_KEY_HEADER);

        if (apiKey == null || !validApiKeys.contains(apiKey)) {
            String response = "Unauthorized";
            exchange.sendResponseHeaders(401, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            return;
        }

        // Key is valid — pass request along to the real handler
        next.handle(exchange);
    }
}