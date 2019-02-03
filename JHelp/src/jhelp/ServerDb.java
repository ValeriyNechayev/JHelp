/*
 * ServerDb.java
 *
 */
package jhelp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class presents server directly working with database. The complete
 * connection string should take the form of:<br>
 * <code><pre>
 *     jdbc:subprotocol://servername:port/datasource:user=username:password=password
 * </pre></code> Sample for using MS Access data source:<br>
 * <code><pre>
 *  private static final String accessDBURLPrefix
 *      = "jdbc:odbc:Driver={Microsoft Access Driver (*.mdb)};DBQ=";
 *  private static final String accessDBURLSuffix
 *      = ";DriverID=22;READONLY=false}";
 *  // Initialize the JdbcOdbc Bridge Driver
 *  try {
 *         Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
 *      } catch(ClassNotFoundException e) {
 *         System.err.println("JdbcOdbc Bridge Driver not found!");
 *      }
 *
 *  // Example: method for connection to a Access Database
 *  public Connection getAccessDBConnection(String filename)
 *                           throws SQLException {
 *       String databaseURL = accessDBURLPrefix + filename + accessDBURLSuffix;
 *       return DriverManager.getConnection(databaseURL, "", "");
 *   }
 * </pre></code>
 *
 * @author <strong >Y.D.Zakovryashin, 2009</strong>
 */
public class ServerDb implements JHelp {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private InputStream is;
    private OutputStream os;

    private Data data;
    private Properties prop;
    private int serverPort;
    private int port;
    private String user;
    private String password;
    private String dbHost;
    private String dbName;
    private String url;
    private Connection con;
    private Statement st;
    private ResultSet rs;

    /**
     * Creates a new instance of <code>ServerDb</code> with default parameters.
     * Default parameters are:<br>
     * <ol>
     * <li><code>ServerDb</code> host is &laquo;localhost&raquo;;</li>
     * <li>{@link java.net.ServerSocket} is opened on
     * {@link jhelp.JHelp#DEFAULT_DATABASE_PORT};</li>
     * </ol>
     */
    public ServerDb() {
        this(DEFAULT_DATABASE_PORT);
        System.out.println("SERVERDb: default constructor");
    }

    /**
     * Constructor creates new instance of <code>ServerDb</code>.
     *
     * @param port defines port for {@link java.net.ServerSocket} object.
     */
    public ServerDb(int port) {
        System.out.println("SERVERDb: constructor");
    }

    private Properties getConfig(String[] args) {                                                      
        String configPath = args.length > 0 ? args[0] : "ConfigServerDb.cfg";
        prop = new Properties();
        File file = new File(configPath);
        try (FileReader fr = new FileReader(file)) {
            prop.load(fr);
        } catch (FileNotFoundException ex) {
            showMessage("getConfig(args): " + ex.getMessage());
        } catch (IOException ex) {
            showMessage("getConfig(args): " + ex.getMessage());
        }
        user = prop.getProperty("user");
        password = prop.getProperty("password");
        dbName = prop.getProperty("dbName");
        dbHost = prop.getProperty("dbHost");
        url = dbHost + ":" + port + "/" + dbName;
        String sp = prop.getProperty("port");
        if (sp != null && sp.matches("\\d++")) {
            port = Integer.valueOf(sp);
        }
        sp = prop.getProperty("serverPort");
        if (sp != null && sp.matches("\\d++")) {
            serverPort = Integer.valueOf(sp);
        }
        return prop;
//        
    }

    /**
     * Constructor creates new instance of <code>ServerDb</code>.
     *
     * @param args array of {@link java.lang.String} type contains connection
     * parameters.
     */
    public ServerDb(String[] args) {                                                            
        System.out.println("SERVERDb: constructor");

    }

    /**
     * Start method for <code>ServerDb</code> application.
     *
     * @param args array of {@link java.lang.String} type contains connection
     * parameters.
     */
    public static void main(String[] args) {
        System.out.println("SERVERDb: main");
        ServerDb server = new ServerDb(args);
        if (server.connect(args) == JHelp.READY) {
            server.run();
            server.disconnect();
        }
    }

    /**
     * Method defines job cycle for client request processing.
     */
    private void run() {
        System.out.println("SERVERDb: run");

        try {
            while (true) {
                Object obj = input.readObject();
                if (obj instanceof Data) {
                    data = (Data) obj;
                } else {
                    System.out.println("Recived invalid Data");
                    continue;
                }
                if (data.getOperation() == DISCONNECT) {
                    clientSocket.close();
                    throw new IOException("Client disconnected");
                }
                data = getData(data);
                output.writeObject(data);
            }
        } catch (IOException | ClassNotFoundException ex) {
            showMessage("run(): " + ex.getMessage());
        }
    }

    /**
     *
     * @return error code. The method returns {@link JHelp#OK} if streams are
     * opened successfully, otherwise the method returns {@link JHelp#ERROR}.
     */
    public int connect() {
        System.out.println("SERVERDb: connect");
        return JHelp.READY;
    }

    /**
     * Method sets connection to database and create
     * {@link java.net.ServerSocket} object for waiting of client's connection
     * requests.
     *
     * @return error code. Method returns {@link jhelp.JHelp#READY} in success
     * case. Otherwise method return {@link jhelp.JHelp#ERROR} or error code.
     */
    public int connect(String[] args) {
        System.out.println("SERVERDb: connect");
        prop = getConfig(args);

        int flag = ERROR;
        Driver dr = null;
        try {
            dr = DriverManager.getDriver(url);
            if (dr != null) {
                con = DriverManager.getConnection(url, user, password);
                st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_UPDATABLE);

                serverSocket = new ServerSocket(serverPort);
                clientSocket = serverSocket.accept();
                input = new ObjectInputStream(clientSocket.getInputStream());
                output = new ObjectOutputStream(clientSocket.getOutputStream());
                flag = READY;
            }
        } catch (SQLException ex) {
            flag = ERROR;
            showMessage("connect(args): " + ex.getMessage());
        } finally {
            return flag;
        }

    }

    /**
     * Method returns result of client request to a database.
     *
     * @param data object of {@link jhelp.Data} type with request to database.
     * @return object of {@link jhelp.Data} type with results of request to a
     * database.
     * @see Data
     * @since 1.0
     */
    public Data getData(Data data) {
        System.out.println("SERVERDb: getData():");
        String tblTerms = "TBLTERMS";
        String tblDef = "TBLDEFINITIONS";
        String term = data.getKey().getItem();
        String definition = data.getValue(0).getItem();
        System.out.println("definition " + definition);
        String sql;
        int idTerm = ERROR;
        int idDef = ERROR;
        int fk = ERROR;
        Data dbData = null;
        try {
            switch (data.getOperation()) {
                case SELECT:
                    break;
                case INSERT:
                    dbData = setData(term);
                    /*if term is absent add new*/
                    if (dbData.getKey().getId() == ERROR) {
                        idTerm = getIdSQL(tblTerms);
                        sql = insertSQL(tblTerms, idTerm, term);
                        st.execute(sql);
                    } else {
                        idTerm = dbData.getKey().getId();
                    }
                    /*add new definition*/
                    fk = idTerm;
                    idDef = getIdSQL(tblDef);
                    sql = insertSQL(tblDef, idDef, definition, fk);
                    st.execute(sql);
                    break;
                case UPDATE:
                    if (data.getKey().getId() == ERROR) {
                        dbData = new Data();
                        dbData.setValue(0, new Item("**At first do a search**"));
                        break;
                    }
                    /*update term*/
                    if (data.getKey().getState() == UPDATE) {
                        idTerm = data.getKey().getId();
                        sql = updateSQL(tblTerms, "Term", idTerm, term);
                        st.execute(sql);
                    }
                    /*update definition*/
                    if (data.getValue(0).getState() == UPDATE) {
                        idDef = data.getValue(0).getId();
                        sql = updateSQL(tblDef, "Definition", idDef, definition);
                        st.execute(sql);
                    }
                    break;
                case DELETE:                    
                    if (data.getKey().getId() == ERROR) {
                        dbData = new Data();
                        dbData.setValue(0, new Item("**At first do a search**"));
                        break;
                    }
                    /*delete definition*/
                    if (data.getValue(0).getState() == DELETE) {
                        idDef = data.getValue(0).getId();
                        sql = deleteSQL(tblDef, idDef);
                        st.execute(sql);
                    }                    
                    dbData = setData(term);
                    /*if definirion is not found in TBLDEFINITIONS delete term*/
                    if (data.getKey().getState() == DELETE
                            && dbData.getKey().getId() == ERROR) {
                        idTerm = data.getKey().getId();
                        sql = deleteSQL(tblTerms, idTerm);
                        st.execute(sql);
                    }
                    break;
            }
            dbData = setData(term);
        } catch (SQLException ex) {
            showMessage("getData(data): " + ex.getMessage());
        }
        return dbData;
    }

    private Data setData(String term) throws SQLException {
        Item key = null;
        Item[] values;
        Data data;
        ArrayList<Item> list = new ArrayList();
        String sql;

        sql = selectSQL(term);
        rs = st.executeQuery(sql);

        boolean flag = true;
        while (rs.next()) {
            if (flag) {
                key = new Item(rs.getInt(1), rs.getString(2), ORIGIN);
                flag = false;
            }
            list.add(new Item(rs.getInt(3), rs.getString(4), ORIGIN));
        }

        values = new Item[list.size()];
        list.toArray(values);
        if (key == null) {
            key = new Item("Unknown term");
        }
        if (values.length == 0) {
            values = new Item[1];
            values[0] = new Item("Defintions not found");
        }
        data = new Data(ORIGIN, key, values);
        return data;
    }

    /**
     * Method disconnects <code>ServerDb</code> object from a database and
     * closes {@link java.net.ServerSocket} object.
     *
     * @return disconnect result. Method returns {@link #DISCONNECT} value, if
     * the process ends successfully. Othewise the method returns error code,
     * for example {@link #ERROR}.
     * @see jhelp.JHelp#DISCONNECT
     * @since 1.0
     */
    public int disconnect() {
        System.out.println("SERVERDb: disconnect");
        try {
            if (!clientSocket.isClosed()) {
                Data disconnect = new Data();
                disconnect.setOperation(DISCONNECT);
                output.writeObject(disconnect);
                clientSocket.close();
            }
            serverSocket.close();
            con.close();
        } catch (IOException ex) {
            showMessage("disconnect(): " + ex.getMessage());
        } catch (SQLException ex) {
            showMessage("disconnect(): " + ex.getMessage());
//        }catch(NullPointerException ex){
//            showMessage(ex);
        }

        return JHelp.DISCONNECT;
    }

//    private Data selectFromDb();
    private int getIdSQL(String TblName) throws SQLException {
        String sql = "SELECT MAX(id) FROM " + TblName;
        ResultSet rs = st.executeQuery(sql);
        int id = 0;
        if (rs.next()) {
            id = rs.getInt(1);
        }
        return id + 1;
    }

    private String selectSQL(String val) {
        String sql = "SELECT TBLTERMS.id, term,TBLDEFINITIONS.ID,DEFINITION "
                + "FROM TBLTERMS JOIN TBLDEFINITIONS ON  "
                + "TBLTERMS.ID=TBLDEFINITIONS.TERM_ID and term = '"
                + val + "'";
        return sql;
    }

    private String insertSQL(String TblName, int id, String val) {
        String sql = "INSERT INTO " + TblName + " values (" + id + ",'" + val + "')";
        return sql;
    }

    private String insertSQL(String TblName, int id, String val, int fk) {
        String sql = "INSERT INTO " + TblName + " values (" + id + ",'" + val + "'," + fk + ")";
        return sql;
    }

    private String updateSQL(String TblName, String field, int id, String val) {
        String sql = "UPDATE " + TblName + " SET " + field + " = '"
                + val + "' WHERE ID = " + id;
        return sql;
    }

    private String deleteSQL(String tblName, int id) {
        String sql = "DELETE FROM " + tblName + " WHERE ID = " + id;
        return sql;
    }

    private void showMessage(String msg) {
        System.out.println("serverDBError: " + msg);
    }
}
