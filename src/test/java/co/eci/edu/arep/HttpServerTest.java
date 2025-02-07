package co.eci.edu.arep;

import org.junit.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.URISyntaxException;

public class HttpServerTest {

    @Test
    void initializeServerSocket_shouldCreateBoundSocket() throws IOException {
        ServerSocket serverSocket = HttpServer.initializeServerSocket(35000);
        assertNotNull(serverSocket);
        assertTrue(serverSocket.isBound());
        serverSocket.close();
    }

    @Test
    void generateResponse_shouldReturn200OKForValidRequest() throws IOException, URISyntaxException {
        String request = "GET /data?name=John&age=25 HTTP/1.1";
        BufferedReader reader = new BufferedReader(new StringReader(request));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServer.generateResponse("/data", reader, out);
        String response = out.toString();
        assertTrue(response.contains("200 OK"));
        assertTrue(response.contains("application/json"));
    }

    @Test
    void generateFileResponse_shouldReturn200OKForExistingFile() throws IOException {
        String filePath = "/index.html";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServer.generateFileResponse(filePath, out);
        String response = out.toString();
        assertTrue(response.contains("200 OK"));
        assertTrue(response.contains("Content-Type: text/html"));
    }

    @Test
    void sendErrorResponse_shouldReturn404NotFound() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServer.sendErrorResponse(out, "404 Not Found");
        String response = out.toString();
        assertTrue(response.contains("404 Not Found"));
    }

    @Test
    void generateFileResponse_shouldReturn404ForNonExistingFile() throws IOException {
        String filePath = "/nonexistent.html";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HttpServer.generateFileResponse(filePath, out);
        String response = out.toString();
        assertTrue(response.contains("404 Not Found"));
    }
}