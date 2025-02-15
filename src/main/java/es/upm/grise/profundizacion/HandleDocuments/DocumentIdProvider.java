package es.upm.grise.profundizacion.HandleDocuments;

import static es.upm.grise.profundizacion.HandleDocuments.Error.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DocumentIdProvider {

    // Environment variable
    protected static String ENVIRON = "APP_HOME";

    // ID for the newly created documents
    protected int documentId;

    // Connection to database (open during program execution)
    Connection connection = null;

    // Singleton access
    protected static DocumentIdProvider instance;

    public static DocumentIdProvider getInstance() throws NonRecoverableError {
        if (instance != null) {
            return instance;
        } else {

            instance = new DocumentIdProvider();
            return instance;

        }
    }

    protected DocumentIdProvider(){
    }

    // Create the connection to the database
    public void initDocumentIdProvider() throws NonRecoverableError {
        // If ENVIRON does not exist, null is returned
        String path = getPath();

        Properties propertiesInFile = loadProperties(path);

        loadBDDriver(propertiesInFile);

        connectDB(propertiesInFile);

        // Read from the COUNTERS table
        String query = "SELECT documentId FROM Counters";
        ResultSet resultSet = readTable(query);

        getObjectId(resultSet);

        closeDBConnections(resultSet);
    }

    // Return the next valid objectID
    public int getDocumentId() throws NonRecoverableError {

        documentId++;

        // Access the COUNTERS table
        String query = "UPDATE Counters SET documentId = ?";
        int numUpdatedRows = updateIdCounter(query);

        // Check that the update has been effectively completed
        if (numUpdatedRows != 1) {

            System.out.println(CORRUPTED_COUNTER.getMessage());
            throw new NonRecoverableError();

        }

        return documentId;
    }

    protected Properties loadProperties(String path) throws NonRecoverableError {
        Properties propertiesInFile = new Properties();
        InputStream inputFile = null;

        // Load the property file
        try {
            inputFile = new FileInputStream(path + "config.properties");
            propertiesInFile.load(inputFile);

        } catch (FileNotFoundException e) {

            System.out.println(NON_EXISTING_FILE.getMessage());
            throw new NonRecoverableError();

        } catch (IOException e) {

            System.out.println(CANNOT_READ_FILE.getMessage());
            throw new NonRecoverableError();
        }
        return propertiesInFile;
    }

    protected void loadBDDriver(Properties propertiesInFile) throws NonRecoverableError {
        // Load DB driver
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch (InstantiationException e) {
            System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
            throw new NonRecoverableError();
        } catch (IllegalAccessException e) {
            System.out.println(CANNOT_INSTANTIATE_DRIVER.getMessage());
            throw new NonRecoverableError();
        } catch (ClassNotFoundException e) {
            System.out.println(CANNOT_FIND_DRIVER.getMessage());
            throw new NonRecoverableError();
        }
    }

    protected void getObjectId(ResultSet resultSet) throws NonRecoverableError {
        // Get the last objectID
        int numberOfValues = 0;
        try {

            while (resultSet.next()) {

                documentId = resultSet.getInt("documentId");
                numberOfValues++;

            }

        } catch (SQLException e) {

            System.out.println(INCORRECT_COUNTER.getMessage());
            throw new NonRecoverableError();

        }

        // Only one objectID can be retrieved
        if (numberOfValues != 1) {

            System.out.println(CORRUPTED_COUNTER.getMessage());
            throw new NonRecoverableError();

        }
    }

    protected ResultSet readTable(String query) throws NonRecoverableError {
        Statement statement = null;
        ResultSet resultSet = null;
        try {

            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

        } catch (SQLException e) {

            System.out.println(CANNOT_RUN_QUERY.getMessage());
            throw new NonRecoverableError();

        } finally {
            try {
                statement.close();
            } catch (SQLException ex) {
                System.out.println(CONNECTION_LOST.getMessage());
                throw new NonRecoverableError();
            }
        }
        return resultSet;
    }

    protected void closeDBConnections(ResultSet resultSet) throws NonRecoverableError {
        // Close all DB connections
        try {
            resultSet.close();
        } catch (SQLException e) {
            System.out.println(CONNECTION_LOST.getMessage());
            throw new NonRecoverableError();
        }
    }

    protected String getPath() throws NonRecoverableError {
        String path = System.getenv(ENVIRON);

        if (path == null) {

            System.out.println(UNDEFINED_ENVIRON.getMessage());
            throw new NonRecoverableError();

        }
        return path;
    }

    protected void connectDB(Properties propertiesInFile) throws NonRecoverableError {
        // Get the DB username and password
        String url = propertiesInFile.getProperty("url");
        String username = propertiesInFile.getProperty("username");
        String password = propertiesInFile.getProperty("password");
        // Create DB connection
        try {

            connection = DriverManager.getConnection(url, username, password);

        } catch (SQLException e) {

            System.out.println(CANNOT_CONNECT_DATABASE.getMessage());
            throw new NonRecoverableError();
        }
    }

    protected int updateIdCounter(String query) throws NonRecoverableError {
        // Update the documentID counter
        int numUpdatedRows = -1;
        try {

            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, documentId);
            numUpdatedRows = preparedStatement.executeUpdate();

        } catch (SQLException e) {

            System.out.println(e.toString());
            System.out.println(CANNOT_UPDATE_COUNTER.getMessage());
            throw new NonRecoverableError();

        }
        return numUpdatedRows;
    }
}
