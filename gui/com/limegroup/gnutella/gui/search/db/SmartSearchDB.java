package com.limegroup.gnutella.gui.search.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.limewire.util.FileUtils;

public class SmartSearchDB {

    public static final int OBJECT_NOT_SAVED_ID = -1;
    public static final int OBJECT_INVALID_ID = -2;
    
    public static final int ASYNC_OBJECT_NOT_PROCESSED = Integer.MIN_VALUE;
    
    public static final int SMART_SEARCH_DATABASE_VERSION = 5;

    private final File _databaseFile;
    private final String _name;

    private Connection _connection;

    private boolean _closed;

    static {
        try {
        	Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public SmartSearchDB(File databaseFile) {
        _databaseFile = databaseFile;

        File path = databaseFile;
        _name = databaseFile.getName();

        _connection = openOrCreateDatabase(path, _name);
    }

    public File getDatabaseFile() {
        return _databaseFile;
    }

    public String getName() {
        return _name;
    }

    public boolean isClosed() {
        return _closed;
    }
    
    public synchronized List<List<Object>> query(String statementSql, Object... arguments) {
        if (isClosed()) {
            return new ArrayList<List<Object>>();
        }

        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            statement = _connection.prepareStatement(statementSql);

            if (arguments != null) {
                for (int i = 0; i < arguments.length; i++) {
                    statement.setObject(i + 1, arguments[i]);
                }
            }

            resultSet = statement.executeQuery();

            return convertResultSetToList(resultSet);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }

        return new ArrayList<List<Object>>();
    }
    
    /**
     * This method is synchronized due to possible concurrent issues, specially
     * during recently generated id retrieval.
     * @param expression
     * @return
     */
    public synchronized int insert(String statementSql, Object... arguments) {
        if (isClosed()) {
            return OBJECT_INVALID_ID;
        }

        if (!statementSql.toUpperCase().startsWith("INSERT")) {
            return OBJECT_INVALID_ID;
        }

        if (update(statementSql, arguments) != -1) {
            return getIdentity();
        }

        return OBJECT_INVALID_ID;
    }
    
    /**
     * Some SQL operations depend on an a identifier that has been generated
     * automatically.
     * 
     * Given that the synchronized insertion is causing +100% CPU usage, we'll make
     * insertion asynchronous and when we have an ID we'll continue the rest of the logic
     * by implementing an IdentitySQLCallback.
     *
     * @see SmartSearchDB#insertAsync(String, IdentitySQLCallback, Object...)
     * 
     * @author gubatron
     *
     */
    public abstract class IdentitySQLCallback implements Runnable {
        protected final int newId;
        private int resultCode;
        
        public IdentitySQLCallback(int newId) {
            this.newId = newId;
            setResultCode(ASYNC_OBJECT_NOT_PROCESSED);
        }
        
        public void setResultCode(int resultCode) {
            this.resultCode = resultCode;
        }
        
        public int getResultCode() {
            return this.resultCode;
        }
        
        @Override
        abstract public void run();
    }
    
    public void insertAsync(String statementSql, IdentitySQLCallback callback, Object... arguments ) {
        if (isClosed()) {
            callback.setResultCode(OBJECT_INVALID_ID);
            return;
        }

        if (!statementSql.toUpperCase().startsWith("INSERT")) {
            callback.setResultCode(OBJECT_INVALID_ID);
            return;
        }

        updateAsync(statementSql, callback, arguments);
    }
        
    
    /**
     * This method is synchronized due to possible concurrent issues, specially
     * during recently generated id retrieval.
     * @param expression
     * @return
     */
    public synchronized int update(String statementSql, Object... arguments) {
        if (isClosed()) {
            return -1;
        }

        return update(_connection, statementSql, arguments);
    }

    public synchronized void close() {
    	//System.out.println("SmartSearchDB is shutting down.");

    	if (isClosed()) {
            return;
        }

        _closed = true;

        try {
            Statement statement = _connection.createStatement();
            statement.execute("SHUTDOWN");
            _connection.close();
            //System.out.println("SmartSearchDB has shut down.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public synchronized Connection reset() {
        try {
            close();
            FileUtils.deleteRecursive(_databaseFile);
            _connection = createDatabase(_databaseFile, _name);
            return _connection; 
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    protected Connection onUpdateDatabase() {
        return reset();
    }

    private Connection openConnection(File path, String name, boolean createIfNotExists) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("jdbc:h2:");
            sb.append(new File(path, name).getAbsolutePath());

            if (!createIfNotExists) {
                sb.append(";ifexists=true");
            }
            
            _closed = false;
            return DriverManager.getConnection(sb.toString(), "SA", "");
        } catch (Exception e) {
        	_closed = true;
            return null;
        }
    }

    private Connection createDatabase(File path, String name) {
        Connection connection = openConnection(path, name, true);

        update(connection, "SET IGNORECASE TRUE");
        
        //TORRENTS
        update(connection, "CREATE TABLE TORRENTS (torrentId INTEGER IDENTITY, infoHash VARCHAR(60), timestamp BIGINT, torrentName VARCHAR(10000), seeds INTEGER, json VARCHAR(131072))");
        update(connection, "CREATE INDEX idxTorrents ON TORRENTS (infoHash)");
        update(connection, "CREATE INDEX idxSeeds ON TORRENTS(seeds)");
        
        //FILES
        update(connection, "CREATE ALIAS IF NOT EXISTS FTL_INIT FOR \"org.h2.fulltext.FullTextLucene.init\"");
        update(connection, "CALL FTL_INIT()");
        
        update(connection, "CREATE TABLE FILES (fileId INTEGER IDENTITY, torrentId INTEGER, fileName VARCHAR(10000), json VARCHAR(131072), keywords VARCHAR(131072))");
        //update(connection, "CREATE INDEX idxFiles ON Files (fileName)");
        update(connection, "CALL FTL_CREATE_INDEX('PUBLIC', 'FILES', 'FILENAME, KEYWORDS')");
        update(connection, "CREATE INDEX idxTorrentId ON FILES (torrentId)");
        
        //SNAPSHOTS - (Created right before user imports a DB, this way the user can delete (rollback) all new insertions after the snapshot)
        update(connection, "CREATE TABLE Snapshots (snapshotId INTEGER IDENTITY, timestamp BIGINT)");

        /** This table keeps only a single row to identify what version of the database we have */
        update(connection, "CREATE TABLE SmartSearchMetaData (smartSearchId INTEGER IDENTITY, name VARCHAR(500), version INTEGER)");
        update(connection, "INSERT INTO SmartSearchMetaData (name , version) VALUES (?, ?)", name, SMART_SEARCH_DATABASE_VERSION);
        
        return connection;
    }

    private Connection openOrCreateDatabase(File path, String name) {
        Connection connection = openConnection(path, name, false);
        if (connection == null) {
            return createDatabase(path, name);
        } else {
            _connection = connection; // not the best solution
            int databaseVersion = getDatabaseVersion();
            if (databaseVersion < SMART_SEARCH_DATABASE_VERSION) {
                return onUpdateDatabase();
            } else {
                return connection;
            }
        }
    }

    private List<List<Object>> convertResultSetToList(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        int numColums = meta.getColumnCount();
        int i;

        List<List<Object>> result = new LinkedList<List<Object>>();

        while (resultSet.next()) {
            List<Object> row = new ArrayList<Object>(numColums);
            for (i = 1; i <= numColums; i++) {
                row.add(resultSet.getObject(i));
            }
            result.add(row);
        }
        return result;
    }

    private int getIdentity() {
        if (isClosed()) {
            return OBJECT_INVALID_ID;
        }

        Statement statment = null;
        ResultSet resultSet = null;

        try {
            statment = _connection.createStatement();
            resultSet = statment.executeQuery("CALL IDENTITY()");

            resultSet.next();

            return resultSet.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statment != null) {
                try {
                    statment.close();
                } catch (SQLException e) {
                }
            }
        }

        return OBJECT_INVALID_ID;
    }
    
    private int update(Connection connection, String statementSql, Object... arguments) {

        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement(statementSql);

            if (arguments != null) {
                for (int i = 0; i < arguments.length; i++) {
                    statement.setObject(i + 1, arguments[i]);
                }
            }

            return statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }
        }

        return -1;
    }
    
    private int getDatabaseVersion() {
        List<List<Object>> query = query("SELECT version FROM SmartSearchMetaData");
        return (Integer) query.get(0).get(0);
    }
}
