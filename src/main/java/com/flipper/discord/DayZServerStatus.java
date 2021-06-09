package com.flipper.discord;

import com.flipper.discord.utils.APIEndpoint;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DayZServerStatus extends ListenerAdapter {

    //API ENDPOINTS IN USE:
    // https://dayzsalauncher.com/api/v1/query/149.56.29.5/27016
    // https://data.cftools.cloud/v1/gameserver/2a68bf9364037a5b8dc1ce7805d3211c9696cb33

    public final String ipAddress, port, queryPort, token, cfToolsServerID;
    public final APIEndpoint endpointInUse;
    public String apiURI, dataURI;
    public JDA discordAPI;

    public String cachedServerResponse = null;
    public String dayZVersionString, dayZMapString, dayZServerNameString;
    public boolean online;
    public int currentPlayers, maxPlayers, cfServerRank, cfServerRating;

    public Runnable heartbeatTask;
    public ScheduledExecutorService executor;

    public DayZServerStatus(String addr, String port, String queryPort, String token, APIEndpoint endpoint) {
        this.ipAddress = addr;
        this.port = port;
        this.queryPort = queryPort;
        this.token = token;
        this.endpointInUse = endpoint;
        this.cfToolsServerID = DigestUtils.sha1Hex(1 + ipAddress + port);


        if(endpointInUse == APIEndpoint.CFTOOLS) {
            this.apiURI = "https://data.cftools.cloud/";
            this.dataURI =  apiURI + "v1/gameserver/" + cfToolsServerID;
        } else if(endpointInUse == APIEndpoint.DZSA) {
            this.apiURI = "https://dayzsalauncher.com/";
            this.dataURI =  apiURI + "api/v1/query/" + ipAddress + "/" + queryPort;
        }


        initializeDiscordBot();
        startHeartBeat();
    }

    private void startHeartBeat() {
        System.out.println(cfToolsServerID);

        executor = Executors.newScheduledThreadPool(1);
        heartbeatTask = () -> {
            JSONObject obj = getServerInfo();
            System.out.println("[beating] ");
            switch (endpointInUse) {
                case CFTOOLS:
                    //Server Status
                    online = obj.getJSONObject(cfToolsServerID).getBoolean("online");

                    currentPlayers = obj.getJSONObject(cfToolsServerID).getJSONObject("status").getInt("players");
                    maxPlayers = obj.getJSONObject(cfToolsServerID).getJSONObject("status").getInt("slots");

                    //DayZ Variables
                    dayZVersionString = obj.getJSONObject(cfToolsServerID).getString("version");
                    dayZMapString = obj.getJSONObject(cfToolsServerID).getString("map");
                    dayZServerNameString = obj.getJSONObject(cfToolsServerID).getString("name");

                    cfServerRank = obj.getJSONObject(cfToolsServerID).getInt("rank");
                    cfServerRating = obj.getJSONObject(cfToolsServerID).getInt("rating");


                    if(online) {
                        discordAPI.getPresence().setStatus(OnlineStatus.ONLINE);
                        discordAPI.getPresence().setActivity(Activity.watching(currentPlayers + "/" + maxPlayers));
                    } else {
                        discordAPI.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.watching("my DayZ server startup."));
                    }
                    System.out.println(online);
                    break;
                case DZSA:
                    if(obj.getInt("status") != 0) online = false;

                    currentPlayers = obj.getJSONObject("result").getInt("players");
                    maxPlayers = obj.getJSONObject("result").getInt("maxPlayers");

                    dayZVersionString = obj.getJSONObject("result").getString("version");
                    dayZMapString = obj.getJSONObject("result").getString("map");
                    dayZServerNameString = obj.getJSONObject("result").getString("name");

                    cfServerRating = 0;
                    cfServerRank = 0;
            }
        };
        executor.scheduleWithFixedDelay(heartbeatTask, 0, 20, TimeUnit.SECONDS);
    }

    private void restartHeartbeat() {
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
            executor.shutdownNow();
            System.out.println("Awaiting termination of Runnable.");
            if(executor.isShutdown()) executor.scheduleWithFixedDelay(heartbeatTask, 0, 20, TimeUnit.SECONDS);
            System.out.println("Runnable is restarting.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Message msg = event.getMessage();
        if (msg.getContentRaw().equals("!serverrank")) {
            MessageChannel channel = event.getChannel();
            channel.sendMessage("Current CFTools rank is: " + cfServerRating).queue();
        }
        if(msg.getContentRaw().equals("!refreshstatus")) {
            cachedServerResponse = null;
            MessageChannel channel = event.getChannel();
            restartHeartbeat();
            channel.sendMessage("OK");

        }
    }

    public void initializeDiscordBot() {
        try {
            JDABuilder builder = JDABuilder.createDefault(token);
            builder.disableCache(CacheFlag.ACTIVITY);
            builder.setChunkingFilter(ChunkingFilter.NONE);
            builder.setStatus(OnlineStatus.OFFLINE);
            builder.addEventListeners(this);
            discordAPI = builder.build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String inputStreamToString(InputStream streamReader) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(streamReader));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line + "/n");
        }
        bufferedReader.close();
        return stringBuilder.toString().replace("/n", "");
    }

    public JSONObject getServerInfo() {
        try {
            URL serverInfoResponse = new URL(dataURI);
            HttpURLConnection connection = (HttpURLConnection) serverInfoResponse.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36");
            connection.connect();
            cachedServerResponse = getJSONResponse(connection);
            return new JSONObject(cachedServerResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String getJSONResponse(HttpURLConnection connection) throws Exception {
        return inputStreamToString(connection.getInputStream());
    }
}
