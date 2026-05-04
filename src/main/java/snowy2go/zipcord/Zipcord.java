package snowy2go.zipcord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.NonNull;
import snowy2go.sqlite.Database;
import snowy2go.sqlite.SQLite;

import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

public final class Zipcord extends JavaPlugin {

    private Database db;

    boolean run = true;
    boolean pause = false;
    boolean verbose = false;

    boolean linkEn = getConfig().getBoolean("link.enabled",true);

    @Override
    public void onEnable() {
        // Initialize the plugin
        getLogger().info("Zipcord is initializing...");
        saveDefaultConfig(); // Set config to default if non-existent
        getConfig().options().copyDefaults(true); // Copy new config options
        if (linkEn) {
            db = new SQLite(this);
            db.load();

            Objects.requireNonNull(this.getCommand("link")).setExecutor(new Link(this));
        }

        // Get the config
        int port = getConfig().getInt("api-settings.port");
        String token = getConfig().getString("api-settings.token");
        boolean tokenEn = getConfig().getBoolean("api-settings.token-enabled");

        // Start the API
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                getLogger().info("Zipcord is running on port " + port + "!");
                while (run) {
                    if (!pause) {
                        Socket clientSocket = serverSocket.accept(); // Wait for client
                        String[] request = getString(clientSocket);

                        JsonObject json = new JsonObject();
                        if (request[1] != null && !request[1].isEmpty()) {
                            json = JsonParser.parseString(request[1]).getAsJsonObject();
                        }

                        String response = "";

                        // Begin processing

                        String[] args = request[0].split("\\s+");

                        if (verbose) { getLogger().info("Zipcord: Request Received! " + request[0] + request[1]); }

                        if (args.length > 2) {
                            if (Objects.equals(args[2], "HTTP/1.1")){ // Request is an HTTP request
                                String pathArg = args[1].substring(1);
                                String[] path = pathArg.split("/");

                                JsonElement tkEle = json.get("token");
                                String sentToken = "";
                                if (tkEle!=null) {
                                    sentToken = tkEle.getAsString();
                                }

                                if (!tokenEn || Objects.equals(token,sentToken)) {
                                    if (Objects.equals(args[0], "GET")) {
                                        // Sort GET requests

                                        switch (gson(path, 0)) {
                                            case "world": /// All world-related functions are handled here. Uses the data's "world" key to select a world.
                                                World world = getWorld(getKeyStringOrNull(json, "world"));
                                                switch (gson(path, 1)) {

                                                    case "time":
                                                        switch (gson(path, 2)) {
                                                            case "day-or-night": /// Returns day or night
                                                                response = getDayOrNight(world);
                                                                break;

                                                            case "ticks": /// Returns the amount of ticks of the day
                                                                response = String.valueOf(world.getTime());
                                                                break;

                                                            case null: /// Returns the specific time of day
                                                                response = getSpecificTime(world);
                                                                break;

                                                            default:
                                                                break;
                                                        }
                                                        break;

                                                    case "weather": /// Returns the weather
                                                        response = getWeather(world);
                                                        break;

                                                    case "forecast": /// Returns both the time and weather
                                                        String weatherText = getWeatherWithSun(world);
                                                        String timeText = getDayOrNight(world);

                                                        response = weatherText + " " + timeText;
                                                        break;

                                                    case null, default:
                                                        break;
                                                }
                                                break;

                                            case "players": /// Player-related functions
                                                switch (gson(path, 1)) {
                                                    case "list": /// List all player usernames on the server
                                                        response = getPlayerList();
                                                        break;

                                                    case "count": /// Returns the count of players online
                                                        response = String.valueOf(Bukkit.getOnlinePlayers().size());
                                                        break;

                                                    case null, default:
                                                        break;
                                                }
                                                break;

                                            case "link": /// Functions related to storing tags in a database. GET category only
                                                if (linkEn) {
                                                    String tags = getKeyStringOrNull(json, "tags");
                                                    String player = getKeyStringOrNull(json, "player");
                                                    switch (gson(path, 1)) {
                                                        case "find": /// Returns a List of players with VALUE
                                                            String[] result = db.findPlayersWithTag(tags);
                                                            response = String.join(", ", result);
                                                            break;

                                                        case "find-username": /// Returns a List of players with VALUE
                                                            String[] results = db.findPlayersWithTag(tags);
                                                            String[] userResults = uuidConvertToUsernames(results);
                                                            response = String.join(", ", userResults);
                                                            break;

                                                        case "get": /// Returns a List of tags under PLAYER
                                                            if (!player.isEmpty()) {

                                                                String allTags = db.getTags(player);
                                                                if (!allTags.isEmpty()) {
                                                                    response = allTags;
                                                                }
                                                            }
                                                            break;

                                                        case null, default:
                                                            break;
                                                    }
                                                }
                                                break;

                                            case null, default:
                                                break;
                                        }
                                    } else if (Objects.equals(args[0], "POST")) { // POST functions
                                        switch (gson(path, 0)) {
                                            case "link": /// Functions related to storing tags in a database. POST category only
                                                if (linkEn) {
                                                    String tags = getKeyStringOrNull(json, "tags");
                                                    String player = getKeyStringOrNull(json, "player");
                                                    String code = getKeyStringOrNull(json, "code");
                                                    JsonElement timeElement = json.get("time");
                                                    long time = 0;
                                                    if (timeElement != null) {
                                                        time = timeElement.getAsLong();
                                                    }
                                                    boolean selfDestruct = true;
                                                    JsonElement selfDestructElement = json.get("self-destruct");
                                                    if (selfDestructElement != null) {
                                                        selfDestruct = selfDestructElement.getAsBoolean();
                                                    }
                                                    switch (gson(path, 1)) {
                                                        case "add": /// Adds TAGS to PLAYER
                                                            addTagsToPlayer(player, tags);
                                                            break;
                                                        case "set": /// Sets the TAGS of PLAYER
                                                            setPlayerTags(player, tags);
                                                            break;
                                                        case "remove": /// Removes a TAGS from PLAYER
                                                            removeTagsFromPlayer(player, tags);
                                                            break;
                                                        case "create": /// Returns a Code that applies VALUE to PLAYER when typed into commands (optional setting with CODE, expiration with TIME in seconds)
                                                            response = createCode(code, tags, time, selfDestruct);
                                                            break;
                                                        case "destroy": /// Nullifies one or more codes. Can be sorted by CODE, PLAYER, and VALUE
                                                            destroyCode(code);
                                                            break;

                                                        case null, default:
                                                            break;
                                                    }
                                                }
                                                break;

                                            case null, default:
                                                break;
                                        }
                                    }
                                }
                            }
                        }

                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                        out.println("HTTP/1.0 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + response.length() + "\r\n\r\n"
                                + response);
                        out.flush();
                        //out.close();
                    }
                }
            } catch (IOException e) {
                getLogger().warning("Zipcord cannot run on port " + port + ". Error: " + e);
                getServer().getPluginManager().disablePlugin(this);
            }
        }).start();
    }

    public void removeTagsFromPlayer(String player, String tags) {
        if (!player.isEmpty() && !tags.isEmpty()) {
            db.removeTags(player, tags);
        }
    }

    public void setPlayerTags(String player, String tags) {
        if (!player.isEmpty() && !tags.isEmpty()) {
            db.setTags(player, tags);
        }
    }

    public boolean addTagsToPlayer(String player, String tags) {
        if (!player.isEmpty() && tags != null && !tags.isEmpty()) {
            db.addTags(player, tags);
            return true;
        }
        return false;
    }

    private String getPlayerList() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        if (!players.isEmpty()) {
            boolean first = true;
            StringBuilder temp = new StringBuilder();
            for (Player plr : players) {
                if (!first) {
                    temp.append(", ");
                } else {
                    first = false;
                }
                temp.append(plr.getName());
            }
            return temp.toString();
        }
        return "";
    }

    private static @NonNull String[] getString(Socket clientSocket) throws IOException {
        BufferedReader buffer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));

        String line;
        int contentLength = 0;

        String request = buffer.readLine();

        // 1. Read headers to find Content-Length
        while (!(line = buffer.readLine()).isEmpty()) {
            if (line.startsWith("Content-Length:")) {
                contentLength = Integer.parseInt(line.substring(15).trim());
            }
        }

        // 2. Read the body based on Content-Length
        char[] body = new char[contentLength];
        buffer.read(body, 0, contentLength);
        String[] returnInfo = new String[2];
        returnInfo[0] = request;
        returnInfo[1] = new String(body);
        return returnInfo;
    }

    private World getWorld(String worldName) {
        return Optional.ofNullable(Bukkit.getWorld(worldName))
                .orElse(Bukkit.getWorld(Objects.requireNonNull(getConfig().getString("default-world"))));
    }

    private String getKeyStringOrNull(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element==null) {
            return "";
        }
        return element.getAsString();
    }

    private String getWeather(World world) {
        String weatherText;
        boolean isStorming = world.hasStorm();
        boolean isThundering = world.isThundering();

        if (isThundering) {
            weatherText = getConfig().getString("forecast-words.weather.stormy");
        } else if (isStorming) {
            weatherText = getConfig().getString("forecast-words.weather.rainy");
        } else {
            weatherText = getConfig().getString("forecast-words.weather.clear");
        }
        return weatherText;
    }

    private String getWeatherWithSun(World world) {
        String weatherText = getWeather(world);

        if (Objects.equals(getDayOrNight(world),getConfig().getString("forecast-words.time.day")) && Objects.equals(weatherText,getConfig().getString("forecast-words.weather.clear")))  {
            weatherText = getConfig().getString("forecast-words.weather.sunny");
        }

        return weatherText;
    }

    private String getDayOrNight(World world) {
        String timeText;

        long tickTime = world.getTime();

        if (tickTime < 12000) {
            timeText = getConfig().getString("forecast-words.time.day");
        } else {
            timeText = getConfig().getString("forecast-words.time.night");
        }
        return timeText;
    }

    private String getSpecificTime(World world) {
        return "ok";
    }

    private String gson(String[] pathArray, int index) { // Get string or null
        String result;
        try {
            result = pathArray[index];
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public String getTagsFromCode(String code) {
        return db.getTagsFromCode(code);
    }

    public void clearDeadCodes() {
        db.clearDeadCodes();
    }

    public void destroyCode(String code) {
        db.destroyCode(code);
    }

    public String createCode(String code, String tags, long expiration, boolean selfDestruct) {
        return db.newCode(code,tags,expiration,selfDestruct);
    }

    public boolean isSelfDestructive(String code) {
        return db.isSelfDestructive(code);
    }

    public String uuidToUsername(String uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    public String[] uuidConvertToUsernames(String[] uuidList) {
        String[] usernameList = new String[0];
        for (String uuid : uuidList) {
            usernameList = Arrays.copyOf(usernameList, usernameList.length + 1);
            usernameList[usernameList.length - 1] = uuidToUsername(uuid); // Add new element
        }
        return usernameList;
    }


//    @Override
//    public void onDisable() {
//
//    }
}
