package net.minecraft.client.renderer.block.model;

import java.util.List;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

public interface IBakedModel {
   List<BakedQuad> getQuads(IBlockState var1, EnumFacing var2, long var3);

   boolean isAmbientOcclusion();

   boolean isGui3d();

   boolean isBuiltInRenderer();

   TextureAtlasSprite getParticleTexture();

   ItemCameraTransforms getItemCameraTransforms();

   ItemOverrideList getOverrides();
}
