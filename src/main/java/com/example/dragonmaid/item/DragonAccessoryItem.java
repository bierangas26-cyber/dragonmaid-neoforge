package com.example.dragonmaid.item;

import com.example.dragonmaid.accessory.DragonAccessoryBehavior;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class DragonAccessoryItem extends Item implements DragonAccessoryBehavior {
    public DragonAccessoryItem() {
        super(new Properties().stacksTo(1));
    }
}
