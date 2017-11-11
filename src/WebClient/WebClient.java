package WebClient;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class WebClient {
    private final static boolean DEBUG = Boolean.parseBoolean(System.getenv("JAVA_DEBUG"));

    public static void main(String args[]){
        String urlString = args[0];
        WebClient w = new WebClient();
        try {
            String r = w.getWebContent("GET", urlString, "UTF-8", 1000);
            System.out.println(r);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String getWebContent(String requestMethod, String urlString, final String charset, int timeoutMilli) throws IOException {
        return getWebContent(requestMethod, urlString, null, charset, timeoutMilli);
    }

    public String getWebContent(String requestMethod, String urlString, String data, final String charset, int timeoutMilli) throws IOException {
        if (urlString == null || urlString.length() == 0) {
            return null;
        }

        // Creates the correct url string
        urlString = checkUrlPrefix(urlString);
        URL url = new URL(urlString);

        // Connecting to the server
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        switch(requestMethod){
            case "GET":
                setGetSettings(conn, timeoutMilli);
                if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
                    return null;
                }
                break;
            case "POST":
                setPostSettings(conn, timeoutMilli);
                if(conn.getResponseCode() != HttpURLConnection.HTTP_OK){
                    return null;
                }
                if(data != null){
                    postData(conn, data, charset);
                }
                break;
        }
        String response = getResponse(conn, charset);
        if(conn != null){
            conn.disconnect();
        }
        return response;
    }

    /**
     *  Posts data to the server
     * @param conn The connection to send data to
     * @param data The data to send
     * @param charset The charset that is used in the data
     * @throws IOException
     */
    private void postData(HttpURLConnection conn, String data, String charset) throws IOException{
        // Gets the output stream from the connection
        DataOutputStream out = new DataOutputStream(conn.getOutputStream());

        // Transforms the data to be sent into bytes
        byte[] content = data.getBytes(charset);

        // Writes the data to the output stream
        out.write(content);
        out.flush();

        // Closing the output stream
        out.close();
    }

    /**
     * This method sets the correct parameters on the connection for a GET request
     * @param conn The connection to configure
     * @param timeoutMilli Timeout time in milliseconds
     */
    private void setGetSettings(HttpURLConnection conn, int timeoutMilli){
        try {
            conn.setRequestMethod("GET");
        }catch(ProtocolException e){
            e.printStackTrace();
        }
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; "+System.getProperty("os.name")+")");
        conn.setRequestProperty("Accept","text/html");
        conn.setConnectTimeout(timeoutMilli);
    }

    /**
     * This method sets the correct parameters on the connection for a POST request
     * @param conn The connection to configure
     * @param timeoutMilli Timeout time in milliseconds
     */
    private void setPostSettings(HttpURLConnection conn, int timeoutMilli){
        try {
            conn.setRequestMethod("POST");
        }catch(ProtocolException e){
            e.printStackTrace();
        }
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Connect-Type", "text/xml;charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; "+System.getProperty("os.name")+")");
        conn.setRequestProperty("Accept", "text/xml");
        conn.setConnectTimeout(timeoutMilli);
    }

    /**
     * Retrieves the response from a server
     * @param conn The connection to retrieve the response from
     * @param charset The charset of the message
     * @return The response from the server
     * @throws IOException
     */
    private String getResponse(HttpURLConnection conn, String charset) throws IOException{
        InputStream is = conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, charset));
        String line = null;
        StringBuffer sb = new StringBuffer();

        if(DEBUG){
            System.out.println("Status: " + conn.getHeaderField(0)); // Status line
            System.out.println("Content-type: " + conn.getHeaderField(1)); // Content type
            System.out.println("Date: " + conn.getHeaderField(2)); // Date
            System.out.print("\n\n\n");
        }

        while ((line = br.readLine()) != null) {
            sb.append(line).append("\r\n");
        }
        if(br != null) {
            br.close();
        }
        return sb.toString();
    }

    /**
     *
     * @param urlString The url to request a connection to
     * @return The full url path
     */
    private String checkUrlPrefix(String urlString){
        return (urlString.startsWith("http://") || urlString.startsWith("https://"))
                ? urlString : ("http://" + urlString).intern();
    }
}
