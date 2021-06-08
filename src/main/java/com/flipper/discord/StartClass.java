package com.flipper.discord;

import java.util.Arrays;

public class StartClass {

    public static DayZServerStatus instance;

    public static void main(String[] args) {
        String address = null;
        String port = null;
        String token = null;
        for(String str : args) {
            if(str.startsWith("ip=")) address = str.split("ip=")[1];
            if(str.startsWith("port=")) port = str.split("port=")[1];
            if(str.startsWith("token=")) token = str.split("token=")[1];
        }
        if(address == null || port == null || token == null) {
            System.out.println("Requires args ip= port= and token=");
            System.exit(0);
        }
        System.out.println(address + " " + port + " " + token);
        instance = new DayZServerStatus(address, port, token);
    }

    public static DayZServerStatus getInstance() {
        return instance;
    }
}
