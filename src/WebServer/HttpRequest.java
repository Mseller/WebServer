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

        if(DEBUG){
            System.out.println(requestLine);

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
           send400BadRequestHeader(os);
            os.write(("<HTML><HEAD><TITLE>Bad request</TITLE></HEAD><BODY>400 Bad Request</BODY></HTML>" + CRLF).getBytes());
            os.write(CRLF.getBytes());
        }else {

            // Open the requested file.
            FileInputStream fis = null;
            boolean fileExists = true;
            try {
                fis = new FileInputStream(fileName);
            } catch (FileNotFoundException e) {
                fileExists = false;
            }

            if (fileExists) {
                send200OKHeader(os, fileName);
                sendBytes(fis, os);
                os.write(CRLF.getBytes());
            } else {
                send404NotFoundHeader(os);
                os.write(("<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>404 Not Found</BODY></HTML>" + CRLF).getBytes());
                os.write(CRLF.getBytes());
            }
        }

        os.close();
        br.close();
        CLIENT.close();
        WebServer.minusThreadCount();
    }

    /**
     *
     * @param os
     */
    private void send400BadRequestHeader(OutputStream os){
        try{
            os.write(("HTTP/1.1 400 BAD REQUEST" + CRLF).getBytes()); // Status line
            os.write(("Content-type: text/html"+ CRLF).getBytes()); // Content type line
            os.write(("Date: "+ DATE + CRLF).getBytes()); // Date line
            os.write(CRLF.getBytes()); // Header have to end with CRLF
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param os
     * @param fileName
     */
    private void send200OKHeader(OutputStream os, String fileName){
        try{
            os.write(("HTTP/1.1 200 OK" + CRLF).getBytes()); // Status line
            os.write(("Content-type: " + contentType( fileName ) + CRLF).getBytes()); // Content type line
            os.write(("Date: "+ DATE + CRLF).getBytes()); // Date line
            os.write(CRLF.getBytes()); // Header have to end with CRLF
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param os
     */
    private void send404NotFoundHeader(OutputStream os){
        try{
            os.write(("HTTP/1.1 404 NOT FOUND" + CRLF).getBytes()); // Status line
            os.write(("Content-type: text/html"+ CRLF).getBytes()); // Content type line
            os.write(("Date: "+ DATE + CRLF).getBytes()); // Date line
            os.write(CRLF.getBytes()); // Header have to end with CRLF
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
        if(fileName.endsWith(".jpeg")) {
            return "image/jpeg";
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
