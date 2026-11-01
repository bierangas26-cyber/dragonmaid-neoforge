package com.example.dragonmaid.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class GeoJson {
    public static final Gson GSON = new GsonBuilder().create();

    private GeoJson() { }
}
