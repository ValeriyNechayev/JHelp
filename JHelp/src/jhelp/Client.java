/*
 * Client.java
 *
 */
package jhelp;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;


/**
 * Client class provides users's interface of the application.
 *
 * @author <strong >Y.D.Zakovryashin, 2009</strong>
 * @version 1.0
 */
public class Client extends JFrame implements JHelp {

    private JButton butFind, butAdd, butEdit, butDelete, butNext, butPrevious, butExit;
    private JTextField term;
    private JTextArea definition;
    private JLabel labelDef, labelTerm;
    private JScrollPane scroll;

    ClientListener cl;
    private int port;
    private String host;
    private int index;

    /**
     * Static constant for serialization
     */
    public static final long serialVersionUID = 1234;
    /**
     * Programm properties
     */
    private Properties prop;
    /**
     * Private Data object presents informational data.
     */
    private Data data = new Data();

    /**
     * Constructor with parameters.
     *
     * @param args Array of {@link String} objects. Each item of this array can
     * define any client's property.
     */
    public Client(String[] args) {

        System.out.println("Client: constructor");
        cl = new ClientListener(this);
        addWindowListener(cl);
        getGUI();
    }

    /**
     * Method for application start
     *
     * @param args agrgument of command string
     */
    static public void main(String[] args) {
        Client client = new Client(args);
        if (client.connect(args) == JHelp.OK) {

            client.run();
//            client.disconnect();
        }
    }

    /**
     * Method define main job cycle
     */
    public void run() {
        System.out.println("Client: run");
//        while (true) {           
//            if(!this.isVisible())break;
//        }
    }

    /**
     * Method set connection to default server with default parameters
     *
     * @return error code
     */
    @Override
    public int connect() {
        return JHelp.ERROR;
    }

    /**
     * Method set connection to server with parameters defines by argument
     * <code>args</code>
     *
     * @return error code
     */
    public int connect(String[] args) {
        getConfig(args);
        int stateOfConnect = cl.connect(host, port);
        return stateOfConnect;
    }

    /**
     * Method gets data from data source
     *
     * @param data initial object (template)
     * @return new object
     */
    /**
     * Method disconnects client and server
     *
     * @return error code
     */
    public int disconnect() {
        System.out.println("Client: disconnect");
        return cl.disconnect();
    }

    private void getGUI() {
        setTitle("JHelp");
        setBounds(100, 100, 640, 480);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        Container cp = getContentPane();
        term = new JTextField();
        term.setBounds(80, 50, 400, 20);
        term.addKeyListener(new ClientListener(this));

        definition = new JTextArea();
        definition.setLineWrap(true); // перевод строки 
        definition.setWrapStyleWord(true);//перевод строки: деление по словам

        scroll = new JScrollPane(definition);
        scroll.setBounds(80, 100, 400, 300);

        labelTerm = new JLabel("Term:");
        labelTerm.setBounds(20, 50, 40, 20);
        labelDef = new JLabel("Defintion:");
        labelDef.setBounds(20, 100, 60, 20);

        setButtons();

        cp.add(term);
        cp.add(scroll);
        cp.add(butFind);
        cp.add(butAdd);
        cp.add(butEdit);
        cp.add(butDelete);
        cp.add(butNext);
        cp.add(butPrevious);
        cp.add(butExit);
        cp.add(labelTerm);
        cp.add(labelDef);

    }

    private void setButtons() {
        butFind = new JButton("Find");
        butFind.setBounds(500, 50, 100, 20);
        butAdd = new JButton("Add");
        butAdd.setBounds(500, 100, 100, 20);
        butEdit = new JButton("Edit");
        butEdit.setBounds(500, 130, 100, 20);
        butDelete = new JButton("Delete");
        butDelete.setBounds(500, 160, 100, 20);
        butNext = new JButton("Next");
        butNext.setBounds(500, 210, 100, 20);
        butPrevious = new JButton("Previous");
        butPrevious.setBounds(500, 240, 100, 20);
        butExit = new JButton("Exit");
        butExit.setBounds(500, 380, 100, 20);

        JButton[] butArr = {butFind, butAdd, butEdit, butDelete,
            butNext, butPrevious, butExit};
        for (int i = 0; i < butArr.length; ++i) {
            butArr[i].setActionCommand(butArr[i].getText());
            butArr[i].addActionListener(cl);
            if (!butArr[i].getActionCommand().equals("Exit")) {
                butArr[i].setEnabled(false);
            }
        }

    }

    void termAction(KeyEvent e) {
        if (cl.checkConnection()) {
            if (term.getText().isEmpty()) {
                butFind.setEnabled(false);
                butAdd.setEnabled(false);
            } else {
                butFind.setEnabled(true);
                butAdd.setEnabled(true);
            }
        }
    }

    private void getConfig(String[] args) {
        String url = args.length > 0 ? args[0] : "ConfigClient.cfg";
        prop = new Properties();
        File file = new File(url);

        try (FileReader fr = new FileReader(file)) {
            prop.load(fr);
        } catch (FileNotFoundException ex) {
            showMessage(ex);
        } catch (IOException ex) {
            showMessage(ex);
        }
        host = prop.getProperty("host");
        String sp = prop.getProperty("port");
        if (sp != null && sp.matches("\\d+")) {
            port = Integer.valueOf(sp);
        }        
    }

    void buttonAction(ActionEvent e) {
        String command = e.getActionCommand();
        Item newKey = new Item();
        Item[] newValues = new Item[1];
        Item newValue = new Item();
        newKey.setItem(term.getText());

        newValue.setItem(definition.getText());
        newKey.setId(data.getKey().getId());
        Data request = new Data(SELECT, newKey, newValues);
        switch (command) {
            case "Find":
                if (term.getText().isEmpty()) {
                    showMessage(new Exception("Term field can't be empty"));
                    return;
                }
                break;
            case "Add":
                if (term.getText().isEmpty() || definition.getText().isEmpty()) {
                    showMessage(new Exception("Term or Definition field can't be empty"));
                    return;
                }
                request.setOperation(JHelp.INSERT);
                break;
            case "Edit":
                if (term.getText().isEmpty() || definition.getText().isEmpty()) {
                    showMessage(new Exception("Term or Definition field can't be empty"));
                    return;
                }
                if (newKey.getItem() != data.getKey().getItem()) {
                    newKey.setState(UPDATE);
                }
                newValue.setId(data.getValue(index).getId());
                if (newValue.getItem() != data.getValue(index).getItem()) {
                    newValue.setState(UPDATE);
                }
                request.setOperation(UPDATE);
                break;
            case "Delete":
                if (term.getText().isEmpty()) {
                    showMessage(new Exception("Term field can't be empty"));
                    return;
                }
                newKey.setState(DELETE);
                newValue.setId(data.getValue(index).getId());
                newValue.setState(DELETE);
                request.setOperation(DELETE);
                break;
            case "Next":
                displayData(++index);
                return;
            case "Previous":
                displayData(--index);
                return;
            case "Exit":
                disconnect();
                this.dispose();
                return;

        }
        request.setKey(newKey);
        newValues[0] = newValue;
        request.setValues(newValues);
        System.out.println("Clieent reuaest value: " + request.getValue(0).getItem());
        data = cl.getData(request);
        displayData(index = 0);
    }

    private void displayData(int i) {
        if (data == null) {
            return;
        }

        int count = data.getValues().length;
        Item item = data.getValue(i);
        String text = item.getItem();
        definition.setText(text);

        butPrevious.setEnabled(i == 0 ? false : true);
        butNext.setEnabled(i >= (count - 1) ? false : true);

        butEdit.setEnabled(data.getKey().getId() == JHelp.ERROR ? false : true);
        butDelete.setEnabled(data.getKey().getId() == JHelp.ERROR ? false : true);
    }

    public void showMessage(Exception ex) {
        JOptionPane op = new JOptionPane();

        op.showMessageDialog(null, ex.getMessage(), ex.getClass().toString(),
                op.INFORMATION_MESSAGE);

    }

    @Override
    public Data getData(Data data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
