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

        if(DEBUG){
            System.out.println("There are currently '" + WebServer.getThreadCount() +"' thread(s) active");
        }
        // Get the clients input stream
        InputStream is = CLIENT.getInputStream();

        // Get the clients output stream
        OutputStream os = CLIENT.getOutputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String requestLine = br.readLine();

        if(DEBUG && requestLine != null){
            System.out.println("Request line: " + requestLine);

            String headerLine;

            if(br != null) {
                while ((headerLine = br.readLine()).length() != 0) {
                    System.out.println(headerLine);
                }
            }
        }

        StringTokenizer st = new StringTokenizer(requestLine);
        st.nextToken();


        String fileName = st.nextToken();

        fileName = "." + fileName;

        String httpVersion = st.nextToken();
        if(httpVersion.contains("1.0")){
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
        }else {

            // Open the requested file.
            FileInputStream fis = null;
            long fileSize;
            boolean fileExists = true;
            try {
                File f = new File(fileName);
                fileSize = f.length();
                fis = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                fileExists = false;
                fileSize = -1;
            }

            if (fileExists) {
                sendHeader(os, OK, contentType(fileName), fileSize);
                sendBytes(fis, os);
                os.write(CRLF.getBytes());
            } else {
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
            }
        }

        os.close();
        br.close();
        CLIENT.close();
        WebServer.minusThreadCount();
    }

    /**
     * Method for sending header to client
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
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fis
     * @param os
     */
    private static void sendBytes(FileInputStream fis, OutputStream os){
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;
        // Copy requested file into the socket's output stream.
        try{
            while ((bytes = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytes);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fileName
     * @return
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
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
