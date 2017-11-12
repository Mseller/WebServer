package WebServer;

import java.io.*;

import java.net.Socket;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringTokenizer;

public class HttpRequest implements Runnable{
    private final String CRLF = "\r\n";
    private final boolean DEBUG;
    private final Socket CLIENT;
    private final String DATE;

    private final String BAD_REQUEST = "400 BAD REQUEST";
    private final String FILE_NOT_FOUND = "404 FILE NOT FOUND";
    private final String OK = "200 OK";
    private final String POST = "201 CREATED"; // A post request will always result in a created, since nothing is stored

    /**
     *
     * @param client
     * @param debug
     */
    HttpRequest(Socket client, boolean debug){
        this.CLIENT = client;
        DEBUG = debug;

        // Setting the current date
        Locale localeUS = new Locale("us","US");
        String zone = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("z")).toString();
        DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy H:mm:ss",localeUS);
        DATE = LocalDateTime.now().format(HTTP_DATE_FORMATTER).toString() + " " + zone;
    }

    /**
     *
     * @throws IOException
     */
    private void processRequest() throws IOException{
        // Get the clients input stream
        InputStream is = CLIENT.getInputStream();

        // Get the clients output stream
        OutputStream os = CLIENT.getOutputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String requestLine = br.readLine();

        // Sometimes when connecting to localhost, a empty request line
        // is sent after the first request, resulting in an invalid request.
        if(requestLine == null) {
            BADRequest(os);
        }else {
            if (DEBUG) {
                System.out.println("--------------------------------------------------------- DEBUG INFORMATION ---------------------------------------------------------");
                System.out.println("Request line: " + requestLine);

                String headerLine;
                if (br != null) {
                    while ((headerLine = br.readLine()).length() != 0) {
                        System.out.println(headerLine);
                    }
                }
                System.out.println("-------------------------------------------------------------------------------------------------------------------------------------");
            }


            StringTokenizer st = new StringTokenizer(requestLine);
            String requestMethod = st.nextToken();
            String fileName = st.nextToken();
            String httpVersion = st.nextToken();

            fileName = "." + fileName;

            if (httpVersion.equals("HTTP/1.0")) {
                BADRequest(os);
            } else {
                switch (requestMethod) {
                    case "GET":
                        GETRequest(os, fileName);
                        break;
                    case "HEAD":
                        HEADRequest(os, fileName);
                        break;
                    case "POST":
                        POSTRequest(os, br);
                        break;
                    default:
                        BADRequest(os);
                }
            }
        }
        os.close();
        br.close();
        if(DEBUG){
            System.out.println("Closing client: " +
                    CLIENT.getInetAddress() + " " + CLIENT.getPort() +
                    "\n\n");
        }
        CLIENT.close();
    }

    /**
     * Responds to a POST request
     * @param os The output stream to write to
     * @param br The buffer reader to read from
     * @throws IOException
     */
    private void POSTRequest(OutputStream os, BufferedReader br) throws IOException{
        br.readLine(); // Pretend to get the POST request and doing something with it.
        String response = "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <title>Post response</title>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "    <p>Successfully created your POST request</p>\n" +
                "  </body>\n" +
                "</html>\n";
        sendHeader(os, POST, "text/html", response.getBytes().length);
        os.write(response.getBytes());
        os.flush();
    }

    /**
     * Responds to a HEAD request
     * @param os The output stream to read from
     * @param fileName The filename that should have been sent in the GET request
     * @throws IOException
     */
    private void HEADRequest(OutputStream os, String fileName) throws IOException{
        File f = new File(fileName);
        if(!f.exists()){
            String response = "<!DOCTYPE html>\n" +
                    "<HTML>\n" +
                    "  <HEAD>\n" +
                    "    <TITLE>Not Found</TITLE>\n" +
                    "  </HEAD>\n" +
                    "  <BODY>\n" +
                    "    404 Not Found\n" +
                    "  </BODY>\n" +
                    "</HTML>";
            sendHeader(os, FILE_NOT_FOUND, "text/html", response.getBytes().length);
        }else{
            long fileSize = f.length();
            sendHeader(os, OK, contentType(fileName), fileSize);
        }
    }

    /**
     * Responds to a bad request
     * @param os The output stream to write to
     * @throws IOException
     */
    private void BADRequest(OutputStream os) throws IOException{
        String response = "<!DOCTYPE html>\n" +
                "<HTML>\n" +
                "  <HEAD>\n" +
                "    <TITLE>Bad request</TITLE>\n" +
                "  </HEAD>\n" +
                "  <BODY>\n" +
                "    400 Bad Request\n" +
                "  </BODY>\n" +
                "</HTML>\n" +
                "\n";
        sendHeader(os, BAD_REQUEST, "text/html", response.getBytes().length);
        os.write((response + CRLF).getBytes());
        os.write(CRLF.getBytes());
        os.flush();
    }

    /**
     * Responds to a get request. Will send a fileNotFound respnse if the
     * requested file could not be found.
     *
     * @param os The output stream to write to
     * @param fileName The file that was requested
     * @throws IOException
     */
    private void GETRequest(OutputStream os, String fileName) throws IOException{
        File f = new File(fileName);
        if(!f.exists()){
            fileNotFound(os);
        }else{
            FileInputStream fis = new FileInputStream(f);
            long fileSize = f.length();
            sendHeader(os, OK, contentType(fileName), fileSize);
            sendBytes(fis, os);
        }
    }

    /**
     * Response which should be sent when a requested file could not be found
     * @param os The output stream to write to
     * @throws IOException
     */
    private void fileNotFound(OutputStream os) throws IOException{
        String response = "<!DOCTYPE html>\n" +
                "<HTML>\n" +
                "  <HEAD>\n" +
                "    <TITLE>Not Found</TITLE>\n" +
                "  </HEAD>\n" +
                "  <BODY>\n" +
                "    404 Not Found\n" +
                "  </BODY>\n" +
                "</HTML>";
        sendHeader(os, FILE_NOT_FOUND, "text/html", response.getBytes().length);
        os.write((response + CRLF).getBytes());
        os.write(CRLF.getBytes());
        os.flush();
    }

    /**
     * Method for sending header to client.
     *
     * @param os The output stram of the connected client
     * @param status The status code and message of the request
     * @param contentType The content type of the data to be sent
     * @param fileSize The size of the data to be sent
     */
    private void sendHeader(OutputStream os, String status, String contentType, long fileSize){
        try {
            os.write(("HTTP/1.1 " + status + CRLF).getBytes());             // Status line
            os.write(("Content-type: " + contentType + CRLF).getBytes());   // Content type line
            os.write(("Date: " + DATE + CRLF).getBytes());                  // Date line
            os.write(("Content-Length: " + fileSize + CRLF).getBytes());    // FileSize line
            os.write(CRLF.getBytes());                                      // Header have to end with CRLF
            os.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Streams the input file to the output stream.
     *
     * @param fis The file input stream of the file to send
     * @param os The output stream to send the file to
     */
    private void sendBytes(FileInputStream fis, OutputStream os){
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;
        // Copy requested file into the socket's output stream.
        try{
            while ((bytes = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytes);
            }
            os.write(CRLF.getBytes());
            os.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Method for getting the content type of the requested file.
     *
     * @param fileName The name of the requested file
     * @return The content type of the requested file
     */
    private static String contentType(String fileName) {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if(fileName.endsWith(".jpg")) {
            return "image/jpg";
        }
        return "application/octet-stream"; }


    @Override
    public void run() {
        try{
            processRequest();
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
}
