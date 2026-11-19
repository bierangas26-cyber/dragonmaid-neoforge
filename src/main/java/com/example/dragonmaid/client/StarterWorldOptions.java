package com.example.dragonmaid.client;

import com.example.dragonmaid.starter.StarterSettings;
import net.minecraft.client.gui.components.Button;

public interface StarterWorldOptions {
   StarterSettings dragonmaid$starter();

   void dragonmaid$starter(StarterSettings var1);

   Button dragonmaid$starterEntry();
}
