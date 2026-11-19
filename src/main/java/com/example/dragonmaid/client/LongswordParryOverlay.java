package com.example.dragonmaid.client;

import com.example.dragonmaid.combat.BladeMotion;
import com.example.dragonmaid.combat.LongswordParry;
import com.example.dragonmaid.combat.SpecialWeapons;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.movement.HumanoidFlight;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent.Post;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Start;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
   modid = "dragonmaid",
   value = {Dist.CLIENT}
)
public final class LongswordParryOverlay {
   private static final int CLOSE_DISPLAY_TICKS = 10;
   private static final int BAR_GAP = 11;
   private static final int BAR_HEIGHT = 15;
   private static final int SUCCESS_COLOR = 16752695;
   private static final LongswordParryFeedback PLAYER_FEEDBACK = new LongswordParryFeedback();
   private static LocalPlayer feedbackOwner;

   private LongswordParryOverlay() {
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void beginUse(Start event) {
      if (event.getEntity() instanceof LocalPlayer player
         && player == Minecraft.getInstance().player
         && event.getDuration() > 0
         && SpecialWeapons.isLongsword(event.getItem())) {
         selectPlayer(player);
         BlockStatePredictionHandler prediction = predictionHandler((ClientLevel)player.level());
         PLAYER_FEEDBACK.start(prediction.isPredicting() ? prediction.currentSequence() : -1);
         PlayerBladeAnimation.startLongswordGuard(player);
         return;
      }
   }

   static boolean playerRiposte(LocalPlayer player, int useSequence) {
      selectPlayer(player);
      return PLAYER_FEEDBACK.succeed(useSequence, player.tickCount);
   }

   /** 1.21.1: ClientLevel#getBlockStatePredictionHandler 包私有，本仓 mixin 尚未提供 accessor，这里用反射兜底。 */
   private static BlockStatePredictionHandler predictionHandler(ClientLevel level) {
      try {
         java.lang.reflect.Method method = ClientLevel.class.getDeclaredMethod("getBlockStatePredictionHandler");
         method.setAccessible(true);
         return (BlockStatePredictionHandler)method.invoke(level);
      } catch (ReflectiveOperationException | RuntimeException exception) {
         return new BlockStatePredictionHandler();
      }
   }

   private static void selectPlayer(LocalPlayer player) {
      if (feedbackOwner != player) {
         feedbackOwner = player;
         PLAYER_FEEDBACK.start(-1);
      }
   }

   @SubscribeEvent
   public static void render(Post event) {
      if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
         Minecraft minecraft = Minecraft.getInstance();
         LocalPlayer player = minecraft.player;
         selectPlayer(player);
         if (player != null
            && !player.isSpectator()
            && player.isAlive()
            && minecraft.screen == null
            && minecraft.getCameraEntity() == player
            && !minecraft.options.hideGui) {
            GuiGraphics gui = event.getGuiGraphics();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            int age = playerGuardAge(player);
            if (age < 0 && player.getVehicle() instanceof DragonBaseEntity dragon && HumanoidFlight.validRider(dragon, player)) {
               int action = DragonForms.state(dragon).dragonmaid$getAction();
               if (action == 9) {
                  drawSuccess(gui, (float)BladeMotion.age(dragon) + partialTick);
                  return;
               }

               if (dragon.isUsingItem() && dragon.getUsedItemHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isLongsword(dragon.getUseItem())) {
                  drawWindow(gui, dragon.getUseItem(), dragon.getTicksUsingItem(), partialTick);
                  return;
               }
            }

            if (PLAYER_FEEDBACK.spent()) {
               drawSuccess(gui, PLAYER_FEEDBACK.successAge(player.tickCount, partialTick));
            } else if (age >= 0) {
               drawWindow(gui, player.getUseItem(), age, partialTick);
            }
         }
      }
   }

   private static void drawWindow(GuiGraphics gui, ItemStack blade, int age, float partialTick) {
      int window = LongswordParry.windowTicks(blade);
      int windup = LongswordParry.windupTicks();
      int activeTicks = age - windup;
      if (activeTicks < window + 10) {
         float fill = LongswordParryFeedback.windowFill(age, partialTick, windup, window);
         int fade = activeTicks < 0 ? 120 : (activeTicks < window ? 255 : 220 - 16 * Math.min(10, activeTicks - window));
         int border = fade << 24 | (fill > 0.0F ? 5338498 : 9806507);
         draw(gui, Mth.ceil(15.0F * fill), border, activeTicks < 0 ? -9593946 : -5834497);
      }
   }

   private static void drawSuccess(GuiGraphics gui, float age) {
      int alpha = LongswordParryFeedback.fadeAlpha(age);
      if (alpha > 0) {
         draw(gui, 15, alpha << 24 | 16752695, alpha << 24 | 16752695);
      }
   }

   private static int playerGuardAge(LocalPlayer player) {
      return player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND && SpecialWeapons.isLongsword(player.getUseItem()) ? player.getTicksUsingItem() : -1;
   }

   private static void draw(GuiGraphics gui, int filled, int border, int color) {
      int x = gui.guiWidth() / 2;
      int y = gui.guiHeight() / 2;
      int top = y - 7;
      int bottom = top + 15;

      for (int side = -1; side <= 1; side += 2) {
         int center = x + side * 11;
         gui.fill(center - 2, top - 1, center + 3, bottom + 1, border);
         gui.fill(center - 1, top, center + 2, bottom, border & 0xFF000000 | 1515817);
         if (filled > 0) {
            gui.fill(center - 1, bottom - filled, center + 2, bottom, color);
         }
      }
   }
}
