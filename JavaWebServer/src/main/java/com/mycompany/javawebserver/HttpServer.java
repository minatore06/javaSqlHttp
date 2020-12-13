package com.mycompany.javawebserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.StringTokenizer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// The tutorial can be found just here on the SSaurel's Blog : 
// https://www.ssaurel.com/blog/create-a-simple-http-web-server-in-java
// Each Client Connection will be managed in a dedicated Thread
public class HttpServer implements Runnable{ 
	
    static final File WEB_ROOT = new File("C:\\Users\\stei2\\Documents\\NetBeansProjects\\JavaApplication5\\files");
    static final String DEFAULT_FILE = "index.html";
    static final String FILE_NOT_FOUND = "404.html";
    static final String METHOD_NOT_SUPPORTED = "not_supported.html";
    static final String FILE_MOVED = "301.html";
    // port to listen connection
    static final int PORT = 8080;

    // verbose mode
    static final boolean verbose = true;

    // Client Connection via Socket Class
    private Socket connect;

    public HttpServer(Socket c) {
            connect = c;
    }

    public static void main(String[] args) {
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");
            
            Class.forName("com.mysql.cj.jdbc.Driver").newInstance();
            // we listen until user halts server execution
            while (true) {
                HttpServer myServer = new HttpServer(serverConnect.accept());

                if (verbose) {
                        System.out.println("Connecton opened. (" + new Date() + ")");
                }

                // create dedicated thread to manage the client connection
                Thread thread = new Thread(myServer);
                thread.start();
            }

        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        } catch (Exception ex) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        // we manage our particular client connection
        BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            // we read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            // we get character output stream to client (for headers)
            out = new PrintWriter(connect.getOutputStream());
            // get binary output stream to client (for requested data)
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // get first line of the request from the client
            String input = in.readLine();
            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();
            System.out.println(fileRequested);
            // we support only GET and HEAD methods, we check
            if (!method.equals("GET")  &&  !method.equals("HEAD")) {
                if (verbose) {
                    System.out.println("501 Not Implemented : " + method + " method.");
                }

                // we return the not supported file to the client
                File file = new File(WEB_ROOT, METHOD_NOT_SUPPORTED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                //read content to return to client
                byte[] fileData = readFileData(file, fileLength);

                // we send HTTP Headers with data to client
                out.println("HTTP/1.1 501 Not Implemented");
                out.println("Server: Java HTTP Server from SSaurel : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println(); // blank line between headers and content, very important !
                out.flush(); // flush character output stream buffer
                // file
                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();

            } else {
                // GET or HEAD method
                if (fileRequested.endsWith("/")) {
                    fileRequested += DEFAULT_FILE;
                }else if(fileRequested.equals("/puntivendita.xml")){
                    ObjectMapper objMap = new ObjectMapper();
                    PuntiVendita pv = objMap.readValue(new File(WEB_ROOT+"/puntiVendita.json"), PuntiVendita.class);
                    XmlMapper xmlMapper = new XmlMapper();
                    xmlMapper.writeValue(new File(WEB_ROOT+"/puntiVendita.xml"),pv);
                    File file = new File(WEB_ROOT+"/puntiVendita.xml");
                }else if(fileRequested.contains("/db")){
                    if(fileRequested.endsWith("/xml")){
                        dbToXml();
                        fileRequested = "/persone.xml";
                    }
                    else if(fileRequested.endsWith("/json")){
                        dbToJson();
                        fileRequested = "/persone.json";
                    }
                }

                File file = new File(WEB_ROOT, fileRequested);
                int fileLength = (int) file.length();
                String content = getContentType(fileRequested);

                if (method.equals("GET")) { // GET method so we return content
                    byte[] fileData = readFileData(file, fileLength);

                    // send HTTP Headers
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server from SSaurel : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println(); // blank line between headers and content, very important !
                    out.flush(); // flush character output stream buffer

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }

                if (verbose) {
                    System.out.println("File " + fileRequested + " of type " + content + " returned");
                }

            }

        } catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(out, dataOut, fileRequested);
            } catch (IOException ioe) {
                System.err.println("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            System.err.println("Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            } 

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }


    }
    
    public List<Persona> getDB(){
        Connection conn;
        Statement stmt = null;
        ResultSet rs = null;
        List<Persona> listPers = new LinkedList();
        
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost/java?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", "oshino");

            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM foobar");
            while(rs.next()){
                listPers.add(new Persona(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            
        } catch (SQLException ex) {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException sqlEx) { } // ignore

                rs = null;
            }

            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException sqlEx) { } // ignore

                stmt = null;
            }
        }
        return listPers;
    }
    
    public void dbToJson() throws IOException{
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(new File(WEB_ROOT+"\\persone.json"), getDB());
    }
    
    public void dbToXml() throws IOException{
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.writeValue(new File(WEB_ROOT+"\\persone.xml"), getDB());
        File file = new File(WEB_ROOT+"persone.xml");
    }

    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null) 
                fileIn.close();
        }

        return fileData;
    }

    // return supported MIME Types
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm")  ||  fileRequested.endsWith(".html"))
            return "text/html";
        else if (fileRequested.endsWith(".xml"))
            return "text/xml";
        else if (fileRequested.endsWith(".json"))
            return "text/json";//application/json
        else
            return "text/plain";
    }

    private void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested) throws IOException {
        if (!fileRequested.endsWith("/")&&!fileRequested.endsWith(".html")&&!fileRequested.endsWith(".xml")) {
            //File file = new File(WEB_ROOT, File_Moved);
            //Files.write(Paths.get(WEB_ROOT+FILE_MOVED), ("<br><p><a href='"+fileRequested+"/index.html'>"+fileRequested+"/index.html</a></p>").getBytes(), StandardOpenOption.APPEND);
            String append301 = "<br><p><a href='"+fileRequested+"/index.html'>"+fileRequested+"/index.html</a></p>";
            String input = null;
            
            FileWriter fw = new FileWriter((WEB_ROOT+"\\"+FILE_MOVED), true);
            fw.write(append301);
            fw.close();
            File file = new File(WEB_ROOT, FILE_MOVED);
            
            int fileLength = (int) file.length();
            byte[] fileData = readFileData(file, fileLength);
            String content = getContentType(FILE_MOVED);

            // send HTTP Headers
            out.println("HTTP/1.1 301 Moved Permanently");
            out.println("Server: Java HTTP Server from SSaurel : 1.0");
            out.println("Date: " + new Date());
            out.println("Location: "+fileRequested + "/");
            out.println("Content-type: " + content);
            out.println("Content-length: " + fileLength);
            out.println(); // blank line between headers and content, very important !
            out.flush(); // flush character output stream buffer

            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();
            
            Scanner sc = new Scanner(new File(WEB_ROOT, FILE_MOVED));
            StringBuffer sb = new StringBuffer();
            input = sc.nextLine();
            input = input.replaceAll(append301, "");
            PrintWriter writer = new PrintWriter(new File(WEB_ROOT, FILE_MOVED));
            writer.append(input);
            writer.flush();
            
        }else{
            File file = new File(WEB_ROOT, FILE_NOT_FOUND);
            int fileLength = (int) file.length();
            String content = "text/html";
            byte[] fileData = readFileData(file, fileLength);

            out.println("HTTP/1.1 404 File Not Found");
            out.println("Server: Java HTTP Server from SSaurel : 1.0");
            out.println("Date: " + new Date());
            out.println("Content-type: " + content);
            out.println("Content-length: " + fileLength);
            out.println(); // blank line between headers and content, very important !
            out.flush(); // flush character output stream buffer

            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();

        }
        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }
	
}