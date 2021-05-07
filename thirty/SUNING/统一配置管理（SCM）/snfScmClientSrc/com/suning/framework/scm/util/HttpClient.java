package com.suning.framework.scm.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public abstract class HttpClient {
    protected static final Logger LOG = Logger.getLogger(HttpClient.class.getName());
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HTTP_HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HTTP_HEADER_CONTENT_ENCODING = "Content-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    private static final boolean FOLLOW_REDIRECTS = true;

    public static String sendByPost(String url, String json, int timeout, int retry, int interval) throws Exception {
        Exception exception = null;
        for (int i = 0; i < retry; i++) {
            try {
                URLConnection connection = new URL(url).openConnection(Proxy.NO_PROXY);
                if (!(connection instanceof HttpURLConnection)) {
                    exception = new Exception("Service URL [" + url + "] is not an HTTP URL");
                    break;
                }
                HttpURLConnection con = (HttpURLConnection) connection;
                prepareConnection(con, json.length(), timeout);
                writeRequestBody(con, json);
                validateResponse(con);
                InputStream responseBody = readResponseBody(con);
                return readResult(responseBody);
            } catch (Exception ex) {
                exception = ex;
                if ((ex instanceof IOException)) {
                    break;
                }
            }
            if (interval > 0) {
                Thread.sleep(interval);
            }
            break;
        }
        throw exception;
    }

    private static void prepareConnection(HttpURLConnection connection, int contentLength, int timeout)
            throws IOException {
        if (timeout >= 0) {
            connection.setConnectTimeout(timeout);
        }
        if (timeout >= 0) {
            connection.setReadTimeout(timeout);
        }
        connection.setInstanceFollowRedirects(true);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(contentLength));
    }

    private static void writeRequestBody(HttpURLConnection con, String data) throws IOException {
        DataOutputStream printout = new DataOutputStream(con.getOutputStream());
        printout.write(data.getBytes("UTF-8"));
        printout.flush();
        printout.close();
    }

    private static void validateResponse(HttpURLConnection con) throws Exception {
        if (con.getResponseCode() == 503) {
            String errorMsg = con.getHeaderField("errorCode");
            if ((errorMsg != null) && (errorMsg.length() > 0)) {
                LOG.log(Level.WARNING, "Server handle meet error! Error Message is:" + errorMsg);
                throw new RuntimeException(errorMsg);
            }
            LOG.log(Level.INFO, "Server handle meet error!");
            throw new RuntimeException("Server handle meet error!");
        }
        if (con.getResponseCode() >= 300) {
            LOG.log(Level.WARNING, "Did not receive successful HTTP response: status code = " + con.getResponseCode() + ", status message = [" + con.getResponseMessage() + "]");
            throw new IOException("Did not receive successful HTTP response: status code = " + con.getResponseCode() + ", status message = [" + con.getResponseMessage() + "]");
        }
    }

    private static InputStream readResponseBody(HttpURLConnection con) throws IOException {
        if (isGzipResponse(con)) {
            return new GZIPInputStream(con.getInputStream());
        }
        return con.getInputStream();
    }

    private static boolean isGzipResponse(HttpURLConnection con) {
        String encodingHeader = con.getHeaderField("Content-Encoding");
        return (encodingHeader != null) && (encodingHeader.toLowerCase().contains("gzip"));
    }

    private static String readResult(InputStream is) throws IOException, ClassNotFoundException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        try {
            StringBuilder temp = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                temp.append(line);
                line = reader.readLine();
            }
            return temp.toString();
        } finally {
            reader.close();
        }
    }
}
