//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

package com.notes;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) {
        //Connect to the DB to be used to persist/retrieve notes
        PostgresSQLJDBC postgresSQLJDBC = new PostgresSQLJDBC();
        postgresSQLJDBC.setupDb();

        try {
            // Create server to listen for requests
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/notes", new NotesHandler(postgresSQLJDBC));
            server.setExecutor(null);
            server.start();

            System.out.println("Server is running on port 8000");
        } catch (IOException e) {
            System.out.println("There was an error starting the server");
            System.err.println(e);
            System.exit(1);
        }
    }
}
