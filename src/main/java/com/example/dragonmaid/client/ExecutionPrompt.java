package com.example.dragonmaid.client;

import com.example.dragonmaid.network.DragonNetwork;
import com.example.dragonmaid.network.ExecutionPromptMessage;
import com.example.dragonmaid.network.ExecutionResponseMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;

public final class ExecutionPrompt {
    private ExecutionPrompt() {
    }

    public static void open(ExecutionPromptMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.level != null) {
            minecraft.setScreen(
                new ConfirmScreen(
                    confirmed -> {
                        if (minecraft.getConnection() != null) {
                            DragonNetwork.CHANNEL.sendToServer(new ExecutionResponseMessage(message.token(), confirmed));
                        }

                        minecraft.setScreen(null);
                    },
                    Component.translatable("screen.dragonmaid.execution.title"),
                    Component.translatable("screen.dragonmaid.execution.body", new Object[]{message.name()})
                        .append("\n\n")
                        .append(Component.translatable("screen.dragonmaid.execution.recovery").withStyle(ChatFormatting.GOLD)),
                    Component.translatable("button.dragonmaid.execute_confirm"),
                    Component.translatable("gui.cancel")
                )
            );
        }
    }
}
