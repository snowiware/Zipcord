package snowy2go.sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import snowy2go.zipcord.Zipcord; // import your main class

public class SQLite extends Database{
    String dbname;
    public SQLite(Zipcord instance){
        super(instance);
        dbname = plugin.getConfig().getString("link.database-name", "bins"); // Set the table name here e.g. player_kills
    }

    public String SQLiteCreateTokensTable = "CREATE TABLE IF NOT EXISTS bins (" + // make sure to put your table name in here too.
            "`player` varchar NOT NULL," + // This creates the different columns you will save data too. varchar(32) Is a string, int = integer
            "`tags` varchar," +
            "PRIMARY KEY (`player`)" +
            ");";

    public String SQLiteCreateCodesTable = "CREATE TABLE IF NOT EXISTS codes (" + // make sure to put your table name in here too.
            "`code` varchar(32) NOT NULL," + // This creates the different columns you will save data too. varchar(32) Is a string, int = integer
            "`tags` varchar," +
            "`expiration` int," +
            "`selfdestruct` int," +
            "PRIMARY KEY (`code`)" +
            ");";


    // SQL creation stuff, You can leave the blow stuff untouched.
    public Connection getSQLConnection() {
        File dataFolder = new File(plugin.getDataFolder(), dbname+".db");
        if (!dataFolder.exists()){
            try {
                dataFolder.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "File write error: "+dbname+".db");
            }
        }
        try {
            if(connection!=null&&!connection.isClosed()){
                return connection;
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            return connection;
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE,"SQLite exception on initialize", ex);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "You need the SQLite JBDC library. Google it. Put it in /lib folder.");
        }
        return null;
    }

    public void load() {
        connection = getSQLConnection();
        try {
            Statement s = connection.createStatement();
            s.executeUpdate(SQLiteCreateTokensTable);
            s.executeUpdate(SQLiteCreateCodesTable);
            s.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        initialize();
    }
}