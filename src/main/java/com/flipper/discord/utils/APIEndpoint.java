package com.flipper.discord.utils;

public enum APIEndpoint {
    CFTOOLS("cf"), DZSA("dz");

    String shortName;

    APIEndpoint(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }
}
