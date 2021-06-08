package com.flipper.discord;

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

    public final String ipAddress, port, token, cfToolsServerID, apiURI;
    public JDA discordAPI;


    public String cachedServerResponse = null;
    public boolean online;
    public int currentPlayers, maxPlayers;

    public ScheduledExecutorService executor;

    public DayZServerStatus(String addr, String port, String token) {
        this.ipAddress = addr;
        this.port = port;
        this.token = token;
        this.cfToolsServerID = DigestUtils.sha1Hex(1 + ipAddress + port);
        this.apiURI = "https://data.cftools.cloud/";

        initializeDiscordBot();
        startHeartBeat();
    }

    private void startHeartBeat() {
        System.out.println(cfToolsServerID);

        executor = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            JSONObject obj = getServerInfo();
            System.out.println("[beating] ");
            online = obj.getJSONObject(cfToolsServerID).getBoolean("online");
            currentPlayers = obj.getJSONObject(cfToolsServerID).getJSONObject("status").getInt("players");
            maxPlayers = obj.getJSONObject(cfToolsServerID).getJSONObject("status").getInt("players");

            if(online) {
                discordAPI.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(currentPlayers + "/" + maxPlayers));
            } else {
                discordAPI.getPresence().setPresence(OnlineStatus.DO_NOT_DISTURB, Activity.watching("my DayZ server startup."));
            }
            System.out.print(online);
        };
        executor.scheduleWithFixedDelay(task, 0, 50, TimeUnit.SECONDS);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        Message msg = event.getMessage();
        if (msg.getContentRaw().equals("!serverrank")) {
            MessageChannel channel = event.getChannel();
            channel.sendMessage("Current CFTools rank is: " +new JSONObject(cachedServerResponse).getJSONObject(cfToolsServerID).getInt("rank")).queue();
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
            URL serverInfoResponse = new URL(apiURI + "v1/gameserver/" + cfToolsServerID);
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
