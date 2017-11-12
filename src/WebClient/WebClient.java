package WebClient;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class WebClient {
    private final static boolean DEBUG = Boolean.parseBoolean(System.getenv("JAVA_DEBUG"));

    public static void main(String args[]){
        String urlString = args[0];
        WebClient w = new WebClient();
        try {
            String r = w.getWebContent("GET", urlString + "1");
            System.out.println(r);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Method that can be used for GET request when the default charset (UTF-8) and timeout (1 sec)
     * is to be used.
     * @param requestMethod The request method
     * @param urlString Where to send the request
     * @return A string with the server response, or null of something went wrong.
     * @throws IOException
     */
    public String getWebContent(String requestMethod, String urlString) throws IOException{
        return getWebContent(requestMethod, urlString, null, "UTF-8",1000);
    }

    /**
     * Method that can be used for POST request when the default charset (UTF-8) and timeout (1 sec)
     * is to be used.
     * @param requestMethod The request method
     * @param url Where to send the request
     * @param data The data to send
     * @return A string with the server response, or null of something went wrong.
     * @throws IOException
     */
    public String getWebContent(String requestMethod, String url, String data) throws IOException{
        return getWebContent(requestMethod, url, data, "UTF-8", 1000);
    }

    /**
     * Method for getting web content.
     *
     * @param requestMethod The method to send
     * @param urlString Where to send the request
     * @param data If the request is POST, this should be the data to send
     *             to the server.
     * @param charset What charset is used on the data.
     * @param timeoutMilli How long to wait for a respons from the server (in milli sec)
     * @return A string with the server response, or null of something went wrong.
     * @throws IOException
     */
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
                break;
            case "POST":
                setPostSettings(conn, timeoutMilli);
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
    private String getResponse(HttpURLConnection conn, String charset){
        InputStream is;
        try{
            // Will try to get the requested file/files
            // if it fails, probebly the wrong URL was given
            is = conn.getInputStream();
        }catch(IOException e){
            // If the wrong URL was requested, get the
            // error stream instead.
            is = conn.getErrorStream();
        }

        BufferedReader br = null;

        try{
            br = new BufferedReader(new InputStreamReader(is, charset));
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }

        String line;
        StringBuffer sb = new StringBuffer();

        if(DEBUG){
            System.out.println("Status: " + conn.getHeaderField(0)); // Status line
            System.out.println("Content-type: " + conn.getHeaderField(1)); // Content type
            System.out.println("Date: " + conn.getHeaderField(2)); // Date
            System.out.println("Content-Length: " + conn.getHeaderField(3)); // Content length
            System.out.print("\n\n\n");
        }

        try{
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            if(br != null) {
                br.close();
            }

        }catch(IOException e){
            e.printStackTrace();
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
