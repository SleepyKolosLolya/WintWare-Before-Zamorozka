package net.minecraft.block;

import java.util.IdentityHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAir extends Block {
   private static Map mapOriginalOpacity = new IdentityHashMap();

   protected BlockAir() {
      super(Material.AIR);
   }

   public EnumBlockRenderType getRenderType(IBlockState state) {
      return EnumBlockRenderType.INVISIBLE;
   }

   @Nullable
   public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
      return NULL_AABB;
   }

   public boolean isOpaqueCube(IBlockState state) {
      return false;
   }

   public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
      return false;
   }

   public void dropBlockAsItemWithChance(World worldIn, BlockPos pos, IBlockState state, float chance, int fortune) {
   }

   public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos) {
      return true;
   }

   public boolean isFullCube(IBlockState state) {
      return false;
   }

   public static void setLightOpacity(Block p_setLightOpacity_0_, int p_setLightOpacity_1_) {
      if (!mapOriginalOpacity.containsKey(p_setLightOpacity_0_)) {
         mapOriginalOpacity.put(p_setLightOpacity_0_, p_setLightOpacity_0_.lightOpacity);
      }

      p_setLightOpacity_0_.lightOpacity = p_setLightOpacity_1_;
   }

   public static void restoreLightOpacity(Block p_restoreLightOpacity_0_) {
      if (mapOriginalOpacity.containsKey(p_restoreLightOpacity_0_)) {
         int i = (Integer)mapOriginalOpacity.get(p_restoreLightOpacity_0_);
         setLightOpacity(p_restoreLightOpacity_0_, i);
      }

   }

   public BlockFaceShape func_193383_a(IBlockAccess p_193383_1_, IBlockState p_193383_2_, BlockPos p_193383_3_, EnumFacing p_193383_4_) {
      return BlockFaceShape.UNDEFINED;
   }
}
