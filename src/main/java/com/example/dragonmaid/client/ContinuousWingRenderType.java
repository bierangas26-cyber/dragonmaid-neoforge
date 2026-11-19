package com.example.dragonmaid.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.RenderType;

final class ContinuousWingRenderType extends RenderType {
   private static final Map<RenderType, RenderType> CACHE = new LinkedHashMap<>(16, 0.75F, true);
   final RenderType original;

   private ContinuousWingRenderType(RenderType original) {
      super(
         "dragonmaid_continuous_wing",
         original.format(),
         original.mode(),
         original.bufferSize(),
         original.affectsCrumbling(),
         original.sortOnUpload(),
         () -> {
            original.setupRenderState();
            RenderSystem.enableCull();
         },
         original::clearRenderState
      );
      this.original = original;
   }

   static RenderType of(RenderType type) {
      if (type instanceof ContinuousWingRenderType) {
         return type;
      } else {
         RenderType found = CACHE.get(type);
         if (found != null) {
            return found;
         } else {
            RenderType var3 = new ContinuousWingRenderType(type);
            CACHE.put(type, var3);
            if (CACHE.size() > 96) {
               Iterator<RenderType> oldest = CACHE.keySet().iterator();
               oldest.next();
               oldest.remove();
            }

            return var3;
         }
      }
   }

   static void clear() {
      CACHE.clear();
   }

   public Optional<RenderType> outline() {
      return this.original.outline().map(ContinuousWingRenderType::of);
   }

   public boolean isOutline() {
      return this.original.isOutline();
   }
}
