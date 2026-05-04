package snowy2go.sqlite;

import snowy2go.zipcord.Zipcord;

import java.util.logging.Level;

public class Error {
    public static void execute(Zipcord plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(Zipcord plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}