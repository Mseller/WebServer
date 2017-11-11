package WebServer;
// gist.github.com/kunst1080/7ac5595e8a845aacbe4d (SimpleWebServer Example)
// https://stackoverflow.com/questions/3828352/what-is-a-mime-type (MIMETYPE)
// https://www.codeproject.com/Tips/1040097/Create-a-Simple-Web-Server-in-Java-HTTP-Server (HTTPServer example)
// https://examples.javacodegeeks.com/core-java/sun/net-sun/httpserver-net-sun/httpserver-net-sun-httpserver-net-sun/com-sun-net-httpserver-httpserver-example/ (Good httpserver example)


import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

public class WebServer {

    public static void main(String args[]){
        WebServer.StartServer();
    }


    private final static boolean DEBUG = Boolean.parseBoolean(System.getenv("JAVA_DEBUG"));
    private final static int PORT = 5000;
    public static int threadCount = 0;

    public static void StartServer(){
        try (ServerSocket server = new ServerSocket(PORT)) {

            while(true){
                Socket client = server.accept();
                if(DEBUG){
                    System.out.println("Connected to client: " + client.getInetAddress() + " " +client.getPort());
                }
                Thread thread = new Thread(new HttpRequest(client, DEBUG));
                threadCount++;
                thread.run();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static synchronized int getThreadCount() {
        return threadCount;
    }
    public static synchronized void minusThreadCount(){threadCount--;}
}
