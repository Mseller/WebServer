package WebServer;

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
