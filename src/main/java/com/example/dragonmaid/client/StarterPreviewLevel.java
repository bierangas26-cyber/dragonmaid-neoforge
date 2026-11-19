package com.example.dragonmaid.client;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.LevelTickAccess;

final class StarterPreviewLevel extends Level {
   private final Scoreboard scores = new Scoreboard();
   private final RecipeManager recipes;
   private final TickRateManager tickRates = new TickRateManager();
   private float dayTimeFraction;
   private float dayTimePerTick = 1.0F;
   private final LevelEntityGetter<Entity> entities = new LevelEntityGetterAdapter(
      new EntityLookup(), new EntitySectionStorage(Entity.class, key -> Visibility.HIDDEN)
   );
   private final ChunkSource chunks = new ChunkSource() {
      private final LevelLightEngine light = new LevelLightEngine(this, false, false);

      public ChunkAccess getChunk(int x, int z, ChunkStatus status, boolean create) {
         return null;
      }

      public void tick(BooleanSupplier time, boolean spawn) {
      }

      public String gatherStats() {
         return "Dragon Maid preview";
      }

      public int getLoadedChunksCount() {
         return 0;
      }

      public LevelLightEngine getLightEngine() {
         return this.light;
      }

      public BlockGetter getLevel() {
         return StarterPreviewLevel.this;
      }
   };

   StarterPreviewLevel(RegistryAccess registry) {
      super(
         new ClientLevelData(Difficulty.NORMAL, false, false),
         Level.OVERWORLD,
         registry,
         registry.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
         () -> InactiveProfiler.INSTANCE,
         true,
         false,
         0L,
         0
      );
      this.recipes = new RecipeManager(registry);
   }

   public BlockState getBlockState(BlockPos pos) {
      return Blocks.AIR.defaultBlockState();
   }

   public FluidState getFluidState(BlockPos pos) {
      return Fluids.EMPTY.defaultFluidState();
   }

   public ChunkSource getChunkSource() {
      return this.chunks;
   }

   public List<? extends Player> players() {
      return List.of();
   }

   public void sendBlockUpdated(BlockPos p, BlockState a, BlockState b, int flags) {
   }

   public void playSeededSound(Player p, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
   }

   public void playSeededSound(Player p, Entity e, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {
   }

   public String gatherChunkSourceStats() {
      return "Dragon Maid preview";
   }

   public Entity getEntity(int id) {
      return null;
   }

   public MapItemSavedData getMapData(MapId id) {
      return null;
   }

   public void setMapData(MapId id, MapItemSavedData map) {
   }

   public MapId getFreeMapId() {
      return new MapId(0);
   }

   public TickRateManager tickRateManager() {
      return this.tickRates;
   }

   public PotionBrewing potionBrewing() {
      return PotionBrewing.EMPTY;
   }

   public void setDayTimeFraction(float fraction) {
      this.dayTimeFraction = fraction;
   }

   public float getDayTimeFraction() {
      return this.dayTimeFraction;
   }

   public float getDayTimePerTick() {
      return this.dayTimePerTick;
   }

   public void setDayTimePerTick(float ticks) {
      this.dayTimePerTick = ticks;
   }

   public void destroyBlockProgress(int id, BlockPos pos, int stage) {
   }

   public Scoreboard getScoreboard() {
      return this.scores;
   }

   public RecipeManager getRecipeManager() {
      return this.recipes;
   }

   protected LevelEntityGetter<Entity> getEntities() {
      return this.entities;
   }

   public LevelTickAccess<Block> getBlockTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public LevelTickAccess<Fluid> getFluidTicks() {
      return BlackholeTickAccess.emptyLevelList();
   }

   public float getShade(Direction direction, boolean shade) {
      return 1.0F;
   }

   public int getHeight(Types type, int x, int z) {
      return 0;
   }

   public void gameEvent(Holder<GameEvent> event, Vec3 pos, Context context) {
   }

   public void levelEvent(Player player, int event, BlockPos pos, int data) {
   }

   public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) {
      return this.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
   }

   public int getSeaLevel() {
      return 63;
   }

   public FeatureFlagSet enabledFeatures() {
      return FeatureFlags.DEFAULT_FLAGS;
   }
}
