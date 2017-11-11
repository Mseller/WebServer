import WebServer.WebServer;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class Main {

    public static void main(String args[]){
       //System.out.println(System.getProperty("user.dir"));
       // System.getProperties().list(System.out);
       //WebServer.StartServer();
        String zone = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("z")).toString();
        DateTimeFormatter HTTP_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy H:mm:ss");
        String DATE = LocalDateTime.now().format(HTTP_DATE_FORMATTER).toString() + " " + zone;
        System.out.println(DATE);
    }
}
