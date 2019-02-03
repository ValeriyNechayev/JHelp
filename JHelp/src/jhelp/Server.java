/*
 * Server.java
 *
 */
package jhelp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLServerSocket;
import static jhelp.JHelp.DEFAULT_SERVER_PORT;

/**
 * This class sets a network connection between end client's objects of
 * {@link jhelp.Client} type and single {@link jhelp.ServerDb} object.
 *
 * @author <strong >Y.D.Zakovryashin, 2009</strong>
 * @version 1.0
 * @see jhelp.Client
 * @see jhelp.ClientThread
 * @see jhelp.ServerDb
 */
public class Server implements JHelp {

    /**
     *
     */
    private ServerSocket serverSocket;
    /**
     *
     */
    private Socket clientSocket;
    /**
     *
     */
    private ObjectInputStream input;
    /**
     *
     */
    private ObjectOutputStream output;

    private Socket dbSocket;
    private Properties prop;
    private int port;
    private int dbPort;
    private String dbHost;

    /**
     * Creates a new instance of Server
     */
    public Server() {
        this(DEFAULT_SERVER_PORT, DEFAULT_DATABASE_PORT);
        System.out.println("SERVER: Default Server Constructed");
    }

    /**
     *
     * @param port
     * @param dbPort
     */
    public Server(int port, int dbPort) {
        System.out.println("SERVER: Server Constructed");
        this.port = port;
        this.dbPort = dbPort;
    }

    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("SERVER: main");
        Server server = new Server();
        if (server.connect(args) == JHelp.OK) {
            server.run();
            server.disconnect();
        }
    }

    /**
     *
     */
    private void run() {
        ArrayList<ClientThread> clients = new ArrayList();
        try {
            while (true) {
                System.out.println("SERVER: run");

                if (dbSocket.isClosed() | serverSocket.isClosed()) {
                    break;
                }
                clientSocket = serverSocket.accept();
                ClientThread ct = new ClientThread(this, clientSocket);
                clients.add(ct);
                Thread t = new Thread(ct);
                t.start();
            }
        } catch (IOException ex) {
            showMessage("run(): " + ex.getMessage());
            
            for(ClientThread client:clients){
                client.disconnect();
            }
            
        }
    }

    /**
     * The method sets connection to database ({@link jhelp.ServerDb} object)
     * and create {@link java.net.ServerSocket} object for waiting of client's
     * connection requests. This method uses default parameters for connection.
     *
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * successfully opened, otherwise the method returns {@link JHelp#ERROR}.
     */
    public int connect() {
        System.out.println("SERVER: connect");

        return OK;
    }

    /**
     * The method sets connection to database ({@link jhelp.ServerDb} object)
     * and create {@link java.net.ServerSocket} object for waiting of client's
     * connection requests.
     *
     * @param args specifies properties of connection.
     * @return error code. The method returns {@link JHelp#OK} if connection are
     * openeds uccessfully, otherwise the method returns {@link JHelp#ERROR}.
     */
    public int connect(String[] args) {
        System.out.println("SERVER: connect");
        getConfig(args);
        int flag = OK;
        try {
            serverSocket = new ServerSocket(port);
            dbSocket = new Socket(dbHost, dbPort);
            output = new ObjectOutputStream(dbSocket.getOutputStream());
            input = new ObjectInputStream(dbSocket.getInputStream());
        } catch (IOException ex) {
            flag = ERROR;
            showMessage("coonnect(args): " + ex.getMessage());
        }
        return flag;
    }

    /**
     * Transports initial {@link Data} object from {@link ClientThread} object
     * to {@link ServerDb} object and returns modified {@link Data} object to
     * {@link ClientThread} object.
     *
     * @param data Initial {@link Data} object which was obtained from client
     * application.
     * @return modified {@link Data} object
     */
    public synchronized Data getData(Data data) {
        System.out.println("SERVER:getData()");
        try {
            output.writeObject(data);
            data = (Data) input.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            serverSocket.close();
            data.setOperation(DISCONNECT);
            showMessage("getData(data): " + ex.getMessage());
            serverSocket.close();
        } finally {
            return data;
        }
    }

    /**
     * The method closes connection with database.
     *
     * @return error code. The method returns {@link JHelp#OK} if a connection
     * with database ({@link ServerDb} object) closed successfully, otherwise
     * the method returns {@link JHelp#ERROR} or any error code.
     */
    public int disconnect() {
        System.out.println("SERVER: disconnect");
        Data disconnect = new Data();
        disconnect.setOperation(DISCONNECT);

        int flag = OK;
        try {
            if (!dbSocket.isClosed()) {
                output.writeObject(disconnect);
                serverSocket.close();
            }
            dbSocket.close();
        } catch (IOException ex) {
            showMessage("disconnect(): " + ex.getMessage());
            flag = ERROR;
        } finally {
            return flag;
        }
    }

    private Properties getConfig(String[] args) {
        String url = args.length > 0 ? args[0] : "ConfigServer.cfg";
        prop = new Properties();
        File file = new File(url);

        try (FileReader fr = new FileReader(file)) {
            prop.load(fr);
        } catch (FileNotFoundException ex) {
            showMessage("getConfig() :" + ex.getMessage());
        } catch (IOException ex) {
            showMessage("getConfig() :" + ex.getMessage());
        }

        dbHost = prop.getProperty("dbHost");
        String sp = prop.getProperty("port");
        if (sp != null && sp.matches("\\d++")) {
            port = Integer.valueOf(sp);
        }
        sp = prop.getProperty("dbPort");
        if (sp != null && sp.matches("\\d++")) {
            dbPort = Integer.valueOf(sp);
        }
        return prop;
    }

    private void showMessage(String msg) {
        System.out.println("ServerError: " + msg);
    }
}
