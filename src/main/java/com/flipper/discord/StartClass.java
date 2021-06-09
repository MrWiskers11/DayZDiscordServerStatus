package com.flipper.discord;

import com.flipper.discord.utils.APIEndpoint;

import java.util.Arrays;

public class StartClass {

    public static DayZServerStatus instance;
    public static boolean cf, dz;
    public static APIEndpoint endpoint = null;

    public static void main(String[] args) {
        String address = null;
        String port = null;
        String token = null;
        String queryport = null;
        for(String str : args) {
            if(str.startsWith("ip=")) address = str.split("ip=")[1];
            if(str.startsWith("port=")) port = str.split("port=")[1];
            if(str.startsWith("queryport=")) port = str.split("queryport=")[1];

            if(str.startsWith("token=")) token = str.split("token=")[1];

            if(str.equalsIgnoreCase("-cf")) endpoint = APIEndpoint.CFTOOLS;
            if(str.equalsIgnoreCase("-dz") || str.equalsIgnoreCase("-dzsa")) endpoint = APIEndpoint.DZSA;
        }
        if(address == null || port == null || token == null || endpoint == null || queryport == null) {
            System.out.println("Requires args queryport= ip= port= and token= (You also have to specify which API to use via the arguments -cf and -cz)");
            System.exit(0);
        }
        System.out.println(address + " " + port + " " + token);
        instance = new DayZServerStatus(address, port, queryport, token, endpoint);
    }

    public static DayZServerStatus getInstance() {
        return instance;
    }
}
