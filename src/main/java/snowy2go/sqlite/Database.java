package snowy2go.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.logging.Level;

import snowy2go.zipcord.RandomCode;
import snowy2go.zipcord.Zipcord; // Import main class!


public abstract class Database {
    Zipcord plugin;
    Connection connection;
    // The name of the table we created back in SQLite class.
    public String table = "bins";
    public Database(Zipcord instance){
        plugin = instance;
    }

    public abstract Connection getSQLConnection();

    public abstract void load();

    public void initialize(){
        connection = getSQLConnection();
        try{
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM bins WHERE player = ?");
            ResultSet rs = ps.executeQuery();
            close(ps,rs);

            PreparedStatement ps2 = connection.prepareStatement("SELECT * FROM codes WHERE code = ?");
            ResultSet rs2 = ps2.executeQuery();
            close(ps2,rs2);

        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "Unable to retrieve connection", ex);
        }
    }

    // These are the methods you can use to get things out of your database. You of course can make new ones to return different things in the database.
    // This returns the number of people the player killed.
    public String getTags(String plr) {
        ResultSet rs;
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM bins WHERE player = '" + plr + "';")) {
            try {

                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getString("player").equalsIgnoreCase(plr.toLowerCase())) { // Tell database to search for the player you sent into the method. e.g getTokens(sam) It will look for sam.
                        return rs.getString("tags");
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
        return null;
    }

    public String[] getTagArray(String plr) {
        String tags = getTags(plr);
        if (tags!=null && !tags.isEmpty()) {
            return tags.split(",");
        } else {
            return new String[0];
        }
    }

    // Now we need methods to save things to the database
    public void setTags(String plr, String fullTags) {
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO bins (player,tags) VALUES(?,?)")) {
            try {
                // IMPORTANT. In SQLite class, We made 3 colums. player, Kills, Total.
                ps.setString(1, plr.toLowerCase());

                ps.setString(2, fullTags);
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
    }

    public void addTags(String plr, String tags) {
        String[] tagArray = getTagArray(plr);
        String[] newTagArray = tags.split(",");
        for (String value : newTagArray) {
            boolean found = Arrays.asList(tagArray).contains(value);
            if (!found) {
                tagArray = Arrays.copyOf(tagArray, tagArray.length + 1);
                tagArray[tagArray.length - 1] = value; // Add new element
            }
        }
        String finalTags = String.join(",", tagArray);
        if (!finalTags.isEmpty()) {
            try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO bins (player,tags) VALUES(?,?)")) {
                try {
                    ps.setString(1, plr.toLowerCase());
                    ps.setString(2, finalTags);
                    ps.executeUpdate();
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
            }
        }
    }

    public void removeTags(String plr, String tags) {
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("REPLACE INTO bins (player,tags) VALUES(?,?)")) {
            try {
                ps.setString(1, plr.toLowerCase());

                if (!tags.isEmpty()) {
                    String[] tagArray = getTagArray(plr);
                    String[] newTagArray = tags.split(",");
                    for (String value : newTagArray) {
                        boolean found = Arrays.asList(tagArray).contains(value);
                        if (!found) {
                            int index = Arrays.asList(tagArray).indexOf(value);
                            tagArray = removeElement(tagArray, index);
                        }
                    }

                    String finalTags = String.join(",", tagArray);

                    ps.setString(2, finalTags);
                    ps.executeUpdate();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
    }

    public boolean isSelfDestructive(String code) {
        ResultSet rs;
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("SELECT EXISTS(SELECT 1 FROM codes WHERE code = '"+code+"' AND selfdestruct = 1 );")) {
            try {
                rs = ps.executeQuery();
                return rs.next();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
        return true;
    }

    public String newCode(String code, String tags, long expiration, boolean selfDestruct) {
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO codes (code,tags,expiration,selfdestruct) VALUES(?,?,?,?)")) {
            try {
                if (code.isEmpty()) {
                    code = RandomCode.generateRandomString(4) + "-" + RandomCode.generateRandomString(4);
                }
                ps.setString(1, code);
                ps.setString(2, tags);
                ps.setLong(3, expiration);
                ps.setBoolean(4,selfDestruct);

                ps.executeUpdate();
                return code;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
        return null;
    }

    public String getTagsFromCode(String code) {
        ResultSet rs;
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM codes WHERE code = '" + code + "';")) {
            try {
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getString("code").equals(code)) {
                        return rs.getString("tags");
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
        return null;
    }


    public void clearDeadCodes() {
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM codes WHERE expiration <> 0 AND expiration < strftime('%s', 'now');")) {
            try {
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
    }

    public void destroyCode(String code) {
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM codes WHERE code = '" + code + "';")) {
            try {
                ps.executeUpdate();
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
    }

    public String[] findPlayersWithTag(String tag) {
        ResultSet rs;
        try (Connection conn = getSQLConnection(); PreparedStatement ps = conn.prepareStatement("SELECT * FROM bins WHERE tags LIKE '%" + tag + "%';")) {
            try {
                rs = ps.executeQuery();
                String[] players = new String[0];
                while (rs.next()) {
                    players = Arrays.copyOf(players, players.length + 1);
                    players[players.length - 1] = rs.getString(1); // Add new element
                }
                return players;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionExecute(), ex);
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, Errors.sqlConnectionClose(), ex);
        }
        return null;
    }


    private static String[] removeElement(String[] arr, int index) {
        String[] result = new String[arr.length - 1];
        // Copy elements before the index
        System.arraycopy(arr, 0, result, 0, index);
        // Copy elements after the index
        System.arraycopy(arr, index + 1, result, index, arr.length - index - 1);
        return result;
    }

    public void close(PreparedStatement ps,ResultSet rs){
        try {
            if (ps != null)
                ps.close();
            if (rs != null)
                rs.close();
        } catch (SQLException ex) {
            Error.close(plugin, ex);
        }
    }
}