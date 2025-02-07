package co.eci.edu.arep;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class HttpServer {
    private static final int PORT = 8080;
    private static String WEB_ROOT = "src/main/resources";
    private static Map<String, BiFunction<HttpRequest, HttpResponse, String>> servicios = new HashMap<>();

    public static void start(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                }
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException, URISyntaxException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null) return;
        System.out.println("Request: " + requestLine);

        String[] tokens = requestLine.split(" ");
        if (tokens.length < 2) return;

        String method = tokens[0];
        String uri = tokens[1];

        // Parsear la URI
        URI resourceUri = new URI(uri);
        String path = resourceUri.getPath();
        String query = resourceUri.getQuery();

        System.out.println("Path: " + path);
        System.out.println("Query: " + query);


        if (servicios.containsKey(path)) {
            HttpRequest req = new HttpRequest(path, query);
            HttpResponse resp = new HttpResponse();
            String outputLine = processRequest(req, resp);
            out.write(outputLine.getBytes());
        } else {
            serveStaticFile(path, out);
        }
    }

    private static String processRequest(HttpRequest req, HttpResponse resp) {
        BiFunction<HttpRequest, HttpResponse, String> service = servicios.get(req.getPath());
        System.out.println("service: " + service);
        System.out.println("req: " + req.getPath());

        if (service != null) {
            String responseBody = service.apply(req, resp);
            return "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "\r\n"
                    + "{\"response\":\"" + responseBody + "\"}";
        } else {
            return "HTTP/1.1 404 Not Found\r\n"
                    + "Content-Type: text/plain\r\n"
                    + "\r\n"
                    + "404 Not Found";
        }
    }

    public static void get(String route, BiFunction<HttpRequest, HttpResponse, String> service) {
        System.out.println("route: " + route);
        servicios.put("/app" + route, service);
    }

    public static void staticfiles(String path) {
        WEB_ROOT = path;
        System.out.println("Static files directory set to: " + WEB_ROOT);
    }

    private static void serveStaticFile(String path, OutputStream out) throws IOException {

        if (path.equals("/")) path = "/index.html";
        System.out.println("Serving static file: " + path);
        File file = new File(WEB_ROOT, path);
        System.out.println(file.getAbsolutePath());
        File notFoundFile = new File(WEB_ROOT, "error.html");

        if (!file.exists() || file.isDirectory()) {

            if (notFoundFile.exists()) {

                BufferedReader reader = new BufferedReader(new FileReader(notFoundFile));
                StringBuilder responseContent = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    responseContent.append(line).append("\n");
                }

                reader.close();

                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + responseContent.length() + "\r\n" +
                        "\r\n" +
                        responseContent.toString();

                out.write(response.getBytes());
            } else {

                String response = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "\r\n" +
                        "404 Not Found";
                out.write(response.getBytes());
            }
        } else {
            String contentType = getContentType(file);
            byte[] fileData = Files.readAllBytes(file.toPath());

            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + contentType + "\r\n" +
                    "Content-Length: " + fileData.length + "\r\n" +
                    "\r\n";
            out.write(responseHeaders.getBytes());
            out.write(fileData);
        }
        out.flush();
    }

    private static String getContentType(File file) {
        Map<String, String> mimeTypes = new HashMap<>();
        mimeTypes.put("html", "text/html");
        mimeTypes.put("css", "text/css");
        mimeTypes.put("js", "application/javascript");
        mimeTypes.put("jpg", "image/jpeg");
        mimeTypes.put("png", "image/png");

        String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        return mimeTypes.getOrDefault(ext, "application/octet-stream");
    }
}
