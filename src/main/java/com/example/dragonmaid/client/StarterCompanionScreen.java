package com.example.dragonmaid.client;

import com.example.dragonmaid.accessory.DragonAccessories;
import com.example.dragonmaid.data.DragonForms;
import com.example.dragonmaid.item.DragonAccessoryItem;
import com.example.dragonmaid.starter.StarterConfig;
import com.example.dragonmaid.starter.StarterSettings;
import com.iafenvoy.iceandfire.entity.DragonBaseEntity;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import org.joml.Matrix4f;

public final class StarterCompanionScreen extends Screen {
   private final CreateWorldScreen parent;
   public final StarterSettings draft;
   private final StarterConfig.Limits limits;
   private StarterPreviewLevel previewLevel;
   private DragonBaseEntity preview;
   private ItemStack previewEgg = ItemStack.EMPTY;
   private CompoundTag previewSettings;
   private int previewSpecies = 1;
   private int previewColor;
   private boolean previewDirty = true;
   private int category;
   private int page;
   private EditBox days;
   private EditBox knowledge;
   private EditBox name;
   private final List<Component> labels = new ArrayList<>();
   private final List<int[]> labelPositions = new ArrayList<>();
   private final List<Button> accessoryButtons = new ArrayList<>();
   private int left;
   private int right;
   private int contentWidth;
   private int previewWidth;
   private int gridY;
   private int rowHeight;
   private Button form;

   public StarterCompanionScreen(CreateWorldScreen parent) {
      super(text("title"));
      this.parent = parent;
      this.limits = StarterConfig.limits(true);
      this.draft = ((StarterWorldOptions)parent).dragonmaid$starter().copy();
      this.draft.normalize(this.limits);
   }

   static Component text(String key, Object... args) {
      return Component.translatable("starter.dragonmaid." + key, args);
   }

   public static boolean isPreview(Entity entity) {
      return entity.level() instanceof StarterPreviewLevel;
   }

   protected void init() {
      this.labels.clear();
      this.labelPositions.clear();
      this.accessoryButtons.clear();
      this.days = this.knowledge = this.name = null;
      this.form = null;
      int panel = Math.min(this.width - 24, 660);
      this.left = (this.width - panel) / 2;
      this.previewWidth = Math.max(78, (int)((double)panel * 0.3));
      this.right = this.left + this.previewWidth + 12;
      this.contentWidth = panel - this.previewWidth - 12;
      this.gridY = 91;
      this.rowHeight = Math.max(34, Math.min(58, (this.height - 128) / 3));
      int half = (this.contentWidth - 10) / 2;
      boolean egg = this.draft.isEgg();
      this.addRenderableWidget(Button.builder(text("attributes"), b -> {
         this.commitInputs();
         this.category = 0;
         this.page = 0;
         this.rebuildWidgets();
      }).bounds(this.right, 32, half, 20).build());
      this.addRenderableWidget(Button.builder(text("appearance"), b -> {
         this.commitInputs();
         this.category = 1;
         this.page = 0;
         this.rebuildWidgets();
      }).bounds(this.right + half + 10, 32, half, 20).build());
      this.addRenderableWidget(Button.builder(text(this.draft.enabled ? "enabled" : "disabled"), b -> {
         this.commitInputs();
         this.draft.enabled = !this.draft.enabled;
         this.rebuildWidgets();
      }).bounds(this.right, 61, this.contentWidth, 20).build());
      if (this.category == 0 && this.page == 0) {
         this.days = this.number("days", 0, 0, this.draft.days, this.limits.minDays(), this.limits.maxDays(), v -> this.draft.days = v);
         this.knowledge = this.number(
            "knowledge", 1, 0, this.draft.knowledge, this.limits.minKnowledge(), this.limits.maxKnowledge(), v -> this.draft.knowledge = v
         );
         this.choice(
            "species",
            0,
            1,
            () -> text(this.draft.species == -1 ? "random" : "species." + this.draft.species),
            delta -> this.draft.species = cycleAppearance(this.draft.species, delta, 3)
         );
         this.choice("horn", 1, 1, () -> text(!egg && this.draft.horn ? "yes" : "no"), delta -> this.draft.horn = !this.draft.horn, !egg);
         this.form = this.choice("form", 0, 2, () -> text(this.draft.form.key()), this::cycleForm);
         if (!egg) {
            this.label(text("stage", StarterSettings.stage(this.draft.days)), 1, 2);
         }
      } else if (this.category == 0) {
         this.label(text("accessories", this.draft.slots()), 0, 0);
         int size = 22;
         int gap = 5;
         int start = this.right + (this.contentWidth - (size + gap) * 5 + gap) / 2;

         for (int i = 0; i < 5; i++) {
            int slot = i;
            Button button = Button.builder(Component.empty(), b -> {
               this.commitInputs();
               this.minecraft.setScreen(new StarterCompanionScreen.Picker(this, slot));
            }).bounds(start + i * (size + gap), this.gridY + 32, size, size).build();
            button.active = i < this.draft.slots();
            button.setTooltip(Tooltip.create(text(button.active ? "choose_accessory" : "locked")));
            this.accessoryButtons.add((Button)this.addRenderableWidget(button));
         }
      } else {
         Button gender = this.choice(
            "gender", 0, 0, () -> text(egg ? "random" : (this.draft.male ? "male" : "female")), delta -> this.draft.male = !this.draft.male, !egg
         );
         if (egg) {
            gender.setTooltip(Tooltip.create(text("egg_gender")));
         }

         this.choice("color", 1, 0, this::colorLabel, delta -> this.draft.color = cycleAppearance(this.draft.color, delta, 4));
         this.label(text("name"), 0, 1);
         this.name = new EditBox(this.font, this.right, this.gridY + this.rowHeight + 12, this.contentWidth, 20, text("name"));
         this.name.setMaxLength(48);
         this.name.setValue(egg ? "" : this.draft.name);
         this.name.setHint((Component)(egg ? Component.literal("—") : text("default_name")));
         this.name.active = !egg;
         this.name.setEditable(!egg);
         this.name.setResponder(v -> {
            this.draft.name = v;
            this.previewDirty = true;
         });
         this.addRenderableWidget(this.name);
      }

      int footer = this.height - 28;
      Button previous = (Button)this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
         this.commitInputs();
         this.page--;
         this.rebuildWidgets();
      }).bounds(this.right, footer, 24, 20).build());
      previous.active = this.category == 0 && this.page > 0;
      Button next = (Button)this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
         this.commitInputs();
         this.page++;
         this.rebuildWidgets();
      }).bounds(this.right + 76, footer, 24, 20).build());
      next.active = !egg && this.category == 0 && this.page < 1;
      this.addRenderableWidget(
         Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.right + this.contentWidth - 76, footer, 76, 20).build()
      );
      this.changed();
   }

   private void label(Component text, int column, int row) {
      this.labels.add(text);
      this.labelPositions.add(new int[]{this.right + column * (this.contentWidth + 10) / 2, this.gridY + row * this.rowHeight});
   }

   private EditBox number(String key, int column, int row, int value, int min, int max, IntConsumer setter) {
      int x = this.right + column * (this.contentWidth + 10) / 2;
      int y = this.gridY + row * this.rowHeight;
      int w = (this.contentWidth - 10) / 2;
      this.label(text(key), column, row);
      boolean editable = !this.draft.isEgg();
      EditBox box = new EditBox(this.font, x + 21, y + 12, w - 42, 20, text(key));
      box.setMaxLength(5);
      box.setFilter(v -> v.isEmpty() || v.chars().allMatch(Character::isDigit));
      box.setValue(editable ? Integer.toString(value) : "");
      box.active = editable;
      box.setEditable(editable);
      if (editable) {
         box.setTooltip(Tooltip.create(text(key.equals("days") ? "days_range" : "knowledge_range", min, max)));
      } else {
         box.setHint(Component.literal("—"));
      }

      this.addRenderableWidget(box);

      for (int delta : new int[]{-1, 1}) {
         ((Button)this.addRenderableWidget(Button.builder(Component.literal(delta < 0 ? "<" : ">"), b -> {
            this.commitInputs();
            setter.accept(Math.max(min, Math.min(max, Integer.parseInt(box.getValue()) + delta)));
            box.setValue(Integer.toString(key.equals("days") ? this.draft.days : this.draft.knowledge));
            this.changed();
         }).bounds(delta < 0 ? x : x + w - 20, y + 12, 20, 20).build())).active = editable;
      }

      return box;
   }

   private Button choice(String key, int column, int row, Supplier<Component> value, IntConsumer setter) {
      return this.choice(key, column, row, value, setter, true);
   }

   private Button choice(String key, int column, int row, Supplier<Component> value, IntConsumer setter, boolean selectable) {
      int x = this.right + column * (this.contentWidth + 10) / 2;
      int y = this.gridY + row * this.rowHeight;
      int w = (this.contentWidth - 10) / 2;
      this.label(text(key), column, row);
      Button main = (Button)this.addRenderableWidget(Button.builder(value.get().copy().withStyle(ChatFormatting.WHITE), b -> {
      }).bounds(x + 19, y + 12, w - 38, 20).build());
      main.active = false;

      for (int delta : new int[]{-1, 1}) {
         ((Button)this.addRenderableWidget(Button.builder(Component.literal(delta < 0 ? "<" : ">"), b -> {
            this.commitInputs();
            setter.accept(delta);
            this.rebuildWidgets();
         }).bounds(delta < 0 ? x : x + w - 18, y + 12, 18, 20).build())).active = selectable;
      }

      return main;
   }

   private void cycleForm(int delta) {
      List<StarterSettings.Form> forms = this.draft.canBeHuman()
         ? List.of(StarterSettings.Form.HUMANOID, StarterSettings.Form.DRAGON, StarterSettings.Form.EGG)
         : List.of(StarterSettings.Form.DRAGON, StarterSettings.Form.EGG);
      this.draft.form = forms.get(Math.floorMod(forms.indexOf(this.draft.form) + delta, forms.size()));
   }

   private static int cycleAppearance(int current, int delta, int count) {
      return Math.floorMod(current + 1 + delta, count + 1) - 1;
   }

   private Component colorLabel() {
      if (this.draft.color == -1) {
         return text("random");
      } else {
         return this.draft.species == -1 ? text("color_variant", this.draft.color + 1) : text("color." + this.draft.species + "." + this.draft.color);
      }
   }

   private int number(EditBox box, int fallback) {
      try {
         return box == null ? fallback : Integer.parseInt(box.getValue());
      } catch (NumberFormatException var4) {
         return fallback;
      }
   }

   private void commitInputs() {
      if (!this.draft.isEgg()) {
         this.draft.days = this.limits.days(this.number(this.days, this.draft.days));
         this.draft.knowledge = this.limits.knowledge(this.number(this.knowledge, this.draft.knowledge));
         if (this.name != null) {
            this.draft.name = this.name.getValue();
         }

         if (this.days != null) {
            this.days.setValue(Integer.toString(this.draft.days));
         }

         if (this.knowledge != null) {
            this.knowledge.setValue(Integer.toString(this.draft.knowledge));
         }
      }

      this.draft.normalize(this.limits);
      this.previewDirty = true;
   }

   private void changed() {
      this.draft.normalize(this.limits);
      this.previewDirty = true;
      if (this.form != null) {
         this.form.setMessage(text(this.draft.form.key()));
         this.form.setTooltip(Tooltip.create(text(this.draft.isEgg() ? "egg_settings" : (this.draft.canBeHuman() ? "human_ready" : "human_requirement"))));
      }

      if (this.previewLevel != null) {
         this.rebuildPreview();
      }
   }

   private void rebuildPreview() {
      if (this.previewDirty) {
         this.previewDirty = false;
         if (this.draft.species != -1) {
            this.previewSpecies = this.draft.species;
         }

         if (this.draft.color != -1) {
            this.previewColor = this.draft.color;
         }

         CompoundTag snapshot = this.draft.save();
         snapshot.putInt("Species", this.previewSpecies);
         snapshot.putInt("Color", this.previewColor);
         if (!snapshot.equals(this.previewSettings)) {
            StarterSettings appearance = StarterSettings.load(snapshot);
            if (appearance.isEgg()) {
               this.preview = null;
               this.previewEgg = appearance.createEgg();
            } else {
               this.previewEgg = ItemStack.EMPTY;
               if (this.previewLevel == null) {
                  // 1.21.1: CreateWorldScreen 重构，getUiState() 返回 WorldCreationUiState，注册表从 getSettings().worldgenLoadContext() 取。
                   this.previewLevel = new StarterPreviewLevel(this.parent.getUiState().getSettings().worldgenLoadContext());
               }

               this.preview = appearance.create(this.previewLevel, this.minecraft.getUser().getProfileId());
               this.preview.setNoAi(true);
               this.preview.setNoGravity(true);
               this.preview.setOnGround(true);
               this.preview.setPos(0.0, 0.0, 0.0);
               if (this.draft.species != -1) {
                  Collections.copy(this.draft.accessories, appearance.accessories);
               }
            }

            this.previewSettings = snapshot;
         }
      }
   }

   public void tick() {
      if (this.days != null && this.days.active) {
         if (!this.days.isFocused() && !this.days.getValue().equals(Integer.toString(this.draft.days))) {
            this.draft.days = this.limits.days(this.number(this.days, this.draft.days));
            this.days.setValue(Integer.toString(this.draft.days));
            this.changed();
         }
      }

      if (this.knowledge != null && this.knowledge.active) {
         if (!this.knowledge.isFocused() && !this.knowledge.getValue().equals(Integer.toString(this.draft.knowledge))) {
            this.draft.knowledge = this.limits.knowledge(this.number(this.knowledge, this.draft.knowledge));
            this.knowledge.setValue(Integer.toString(this.draft.knowledge));
            this.changed();
         }
      }

      if (this.name != null) {
      }

      if (this.preview != null) {
         this.preview.tickCount++;
      }
   }

   public boolean keyPressed(int key, int scan, int modifiers) {
      if (key != 257 && key != 335) {
         return super.keyPressed(key, scan, modifiers);
      } else {
         this.commitInputs();
         this.changed();
         this.setFocused(null);
         return true;
      }
   }

   private void renderPreview(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
      this.rebuildPreview();
      int top = 61;
      int bottom = this.height - 34;
      if (bottom > top) {
         double centerX = (double)this.left + (double)this.previewWidth / 2.0;
         double centerY = (double)(top + bottom) / 2.0;
         if (!this.previewEgg.isEmpty()) {
            float size = Math.min(5.0F, (float)Math.min(this.previewWidth - 10, bottom - top - 10) / 16.0F);
            if (!(size <= 0.0F)) {
               PoseStack pose = graphics.pose();
               pose.pushPose();

               try {
                  pose.translate(centerX, centerY, 0.0);
                  pose.scale(size, size, 1.0F);
                  graphics.renderItem(this.previewEgg, -8, -8);
               } finally {
                  pose.popPose();
               }
            }
         } else if (this.preview != null) {
            float turn = (float)Math.atan((centerX - (double)mouseX) / 40.0);
            float pitch = (float)Math.atan((centerY - (double)mouseY) / 40.0) * 20.0F;
            float bodyYaw = 180.0F + turn * 20.0F;
            float size;
            float pivotHeight;
            if (this.draft.form == StarterSettings.Form.HUMANOID) {
               float modelScale = DragonForms.modelScale(this.preview);
               size = (float)Math.min((double)(this.previewWidth - 12) / (2.7 * (double)modelScale), (double)(bottom - top - 16) / (2.8 * (double)modelScale));
               pivotHeight = 1.25F * modelScale;
            } else {
               double bodyWidth = Math.max((double)this.preview.getBbWidth() * 2.5, (double)this.preview.getRenderSize() * 1.6);
               size = (float)Math.min(
                  (double)(this.previewWidth - 10) / Math.max(1.0, bodyWidth), (double)((float)(bottom - top - 16) / Math.max(1.0F, this.preview.getBbHeight()))
               );
               pivotHeight = this.preview.getBbHeight() * 0.5F;
            }

            if (!(size <= 0.0F)) {
               PoseStack pose = new PoseStack();
               pose.last().pose().set(graphics.pose().last().pose());
               pose.last().normal().set(graphics.pose().last().normal());
               pose.translate(centerX, centerY, 50.0);
               pose.mulPose(new Matrix4f().scaling(size, size, -size));
               pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
               pose.mulPose(Axis.XP.rotationDegrees(pitch));
               pose.translate(0.0F, -pivotHeight, 0.0F);
               graphics.enableScissor(this.left, top, this.left + this.previewWidth, bottom);
               graphics.flush();
               Lighting.setupForEntityInInventory();

               try {
                  if (this.draft.form == StarterSettings.Form.HUMANOID) {
                     IceFemaleRenderer[] stages = ClientRegistration.dragonRenderers.get(FemaleDragonAssets.of(this.preview));
                     stages[FemaleDragonAssets.stageIndex(this.preview.getDragonStage())]
                        .renderMenuPreview(this.preview, bodyYaw, pose, graphics.bufferSource(), 15728880, partial);
                  } else {
                     this.preview.yBodyRotO = this.preview.yBodyRot = bodyYaw;
                     this.preview.yHeadRotO = this.preview.yHeadRot = 180.0F + turn * 40.0F;
                     this.preview.setYRot(this.preview.yHeadRot);
                     this.preview.setXRot(-pitch);
                     this.preview.xRotO = -pitch;
                     this.minecraft.getEntityRenderDispatcher().getRenderer(this.preview).render(this.preview, bodyYaw, partial, pose, graphics.bufferSource(), 15728880);
                  }

                  graphics.flush();
               } finally {
                  Lighting.setupFor3DItems();
                  graphics.disableScissor();
               }
            }
         }
      }
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.renderBackground(g, 0, 0, this.minecraft.getTimer().getGameTimeDeltaPartialTick(false));
      g.drawCenteredString(this.font, this.title, this.width / 2, 12, 16777215);
      this.renderPreview(g, mouseX, mouseY, partial);

      for (int i = 0; i < this.labels.size(); i++) {
         int[] pos = this.labelPositions.get(i);
         Component value = this.labels.get(i);
         if (this.category == 0 && this.page == 0 && i == 5) {
            value = text("stage", StarterSettings.stage(this.draft.days));
         }

         g.drawString(this.font, value, pos[0], pos[1], 16777215);
      }

      super.render(g, mouseX, mouseY, partial);
      if (this.category == 0 && this.page == 0 && !this.draft.isEgg()) {
         for (int i = 0; i < 2; i++) {
            int[] p = this.labelPositions.get(i);
            if (mouseX >= p[0] && mouseX < p[0] + (this.contentWidth - 10) / 2 && mouseY >= p[1] - 2 && mouseY < p[1] + 10) {
               g.renderTooltip(
                  this.font,
                  text(
                     i == 0 ? "days_range" : "knowledge_range",
                     i == 0 ? this.limits.minDays() : this.limits.minKnowledge(),
                     i == 0 ? this.limits.maxDays() : this.limits.maxKnowledge()
                  ),
                  mouseX,
                  mouseY
               );
            }
         }
      }

      g.drawCenteredString(this.font, Component.literal(Integer.toString(this.page + 1)), this.right + 50, this.height - 22, 16777215);

      for (int ix = 0; ix < this.accessoryButtons.size(); ix++) {
         Button b = this.accessoryButtons.get(ix);
         ItemStack stack = this.draft.accessories.get(ix);
         if (ix >= this.draft.slots()) {
            DragonPanel.lockedSlot(g, b.getX() + 3, b.getY() + 3);
         } else if (!stack.isEmpty()) {
            g.renderItem(stack, b.getX() + 3, b.getY() + 3);
            if (b.isHoveredOrFocused()) {
               g.renderTooltip(this.font, stack, mouseX, mouseY);
            }
         } else {
            g.drawCenteredString(this.font, Component.literal("+"), b.getX() + 11, b.getY() + 7, -4473925);
         }
      }
   }

   public void onClose() {
      this.commitInputs();
      ((StarterWorldOptions)this.parent).dragonmaid$starter(this.draft);
      this.preview = null;
      this.previewLevel = null;
      ClientRegistration.clearWorldState();
      this.minecraft.setScreen(this.parent);
   }

   private static final class Picker extends Screen {
      private final StarterCompanionScreen parent;
      private final int slot;
      private final List<ItemStack> items;
      private final List<Button> cells = new ArrayList<>();

      Picker(StarterCompanionScreen parent, int slot) {
         super(StarterCompanionScreen.text("choose_accessory"));
         this.parent = parent;
         this.slot = slot;
         this.items = BuiltInRegistries.ITEM
            .stream()
            .filter(i -> i instanceof DragonAccessoryItem && "dragonmaid".equals(BuiltInRegistries.ITEM.getKey(i).getNamespace()))
            .<ItemStack>map(ItemStack::new)
            .sorted(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i.getItem()).toString()))
            .toList();
      }

      protected void init() {
         this.cells.clear();
         this.parent.rebuildPreview();
         int cols = Math.max(4, Math.min(8, (this.width - 48) / 30));
         int start = (this.width - cols * 30) / 2;

         for (int i = 0; i < this.items.size(); i++) {
            ItemStack stack = this.items.get(i);
            Button cell = Button.builder(Component.empty(), b -> {
               this.parent.draft.accessories.set(this.slot, stack.copy());
               this.parent.changed();
               this.minecraft.setScreen(this.parent);
            }).bounds(start + i % cols * 30, 48 + i / cols * 30, 24, 24).build();
            cell.active = DragonAccessories.accepts(stack, this.parent.preview, this.slot);
            this.cells.add((Button)this.addRenderableWidget(cell));
         }

         this.addRenderableWidget(Button.builder(StarterCompanionScreen.text("clear"), b -> {
            this.parent.draft.accessories.set(this.slot, ItemStack.EMPTY);
            this.parent.changed();
            this.minecraft.setScreen(this.parent);
         }).bounds(this.width / 2 - 105, this.height - 30, 100, 20).build());
         this.addRenderableWidget(
            Button.builder(Component.translatable("gui.back"), b -> this.onClose()).bounds(this.width / 2 + 5, this.height - 30, 100, 20).build()
         );
      }

      public void onClose() {
         this.minecraft.setScreen(this.parent);
      }

      public void render(GuiGraphics g, int x, int y, float partial) {
         this.renderBackground(g, 0, 0, this.minecraft.getTimer().getGameTimeDeltaPartialTick(false));
         g.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
         super.render(g, x, y, partial);

         for (int i = 0; i < this.cells.size(); i++) {
            Button b = this.cells.get(i);
            g.renderItem(this.items.get(i), b.getX() + 4, b.getY() + 4);
            if (b.isHoveredOrFocused()) {
               if (b.active) {
                  g.renderTooltip(this.font, this.items.get(i), x, y);
               } else {
                  g.renderTooltip(
                     this.font, this.items.get(i).getHoverName().copy().append("\n").append(StarterCompanionScreen.text("accessory_unavailable")), x, y
                  );
               }
            }
         }
      }
   }
}
