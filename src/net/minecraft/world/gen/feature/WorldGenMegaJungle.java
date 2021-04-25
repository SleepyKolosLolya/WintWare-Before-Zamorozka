package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class WorldGenMegaJungle extends WorldGenHugeTrees {
   public WorldGenMegaJungle(boolean p_i46448_1_, int p_i46448_2_, int p_i46448_3_, IBlockState p_i46448_4_, IBlockState p_i46448_5_) {
      super(p_i46448_1_, p_i46448_2_, p_i46448_3_, p_i46448_4_, p_i46448_5_);
   }

   public boolean generate(World worldIn, Random rand, BlockPos position) {
      int i = this.getHeight(rand);
      if (!this.ensureGrowable(worldIn, rand, position, i)) {
         return false;
      } else {
         this.createCrown(worldIn, position.up(i), 2);

         int i2;
         for(i2 = position.getY() + i - 2 - rand.nextInt(4); i2 > position.getY() + i / 2; i2 -= 2 + rand.nextInt(4)) {
            float f = rand.nextFloat() * 6.2831855F;
            int k = position.getX() + (int)(0.5F + MathHelper.cos(f) * 4.0F);
            int l = position.getZ() + (int)(0.5F + MathHelper.sin(f) * 4.0F);

            int j2;
            for(j2 = 0; j2 < 5; ++j2) {
               k = position.getX() + (int)(1.5F + MathHelper.cos(f) * (float)j2);
               l = position.getZ() + (int)(1.5F + MathHelper.sin(f) * (float)j2);
               this.setBlockAndNotifyAdequately(worldIn, new BlockPos(k, i2 - 3 + j2 / 2, l), this.woodMetadata);
            }

            j2 = 1 + rand.nextInt(2);
            int j1 = i2;

            for(int k1 = i2 - j2; k1 <= j1; ++k1) {
               int l1 = k1 - j1;
               this.growLeavesLayer(worldIn, new BlockPos(k, k1, l), 1 - l1);
            }
         }

         for(i2 = 0; i2 < i; ++i2) {
            BlockPos blockpos = position.up(i2);
            if (this.canGrowInto(worldIn.getBlockState(blockpos).getBlock())) {
               this.setBlockAndNotifyAdequately(worldIn, blockpos, this.woodMetadata);
               if (i2 > 0) {
                  this.placeVine(worldIn, rand, blockpos.west(), BlockVine.EAST);
                  this.placeVine(worldIn, rand, blockpos.north(), BlockVine.SOUTH);
               }
            }

            if (i2 < i - 1) {
               BlockPos blockpos1 = blockpos.east();
               if (this.canGrowInto(worldIn.getBlockState(blockpos1).getBlock())) {
                  this.setBlockAndNotifyAdequately(worldIn, blockpos1, this.woodMetadata);
                  if (i2 > 0) {
                     this.placeVine(worldIn, rand, blockpos1.east(), BlockVine.WEST);
                     this.placeVine(worldIn, rand, blockpos1.north(), BlockVine.SOUTH);
                  }
               }

               BlockPos blockpos2 = blockpos.south().east();
               if (this.canGrowInto(worldIn.getBlockState(blockpos2).getBlock())) {
                  this.setBlockAndNotifyAdequately(worldIn, blockpos2, this.woodMetadata);
                  if (i2 > 0) {
                     this.placeVine(worldIn, rand, blockpos2.east(), BlockVine.WEST);
                     this.placeVine(worldIn, rand, blockpos2.south(), BlockVine.NORTH);
                  }
               }

               BlockPos blockpos3 = blockpos.south();
               if (this.canGrowInto(worldIn.getBlockState(blockpos3).getBlock())) {
                  this.setBlockAndNotifyAdequately(worldIn, blockpos3, this.woodMetadata);
                  if (i2 > 0) {
                     this.placeVine(worldIn, rand, blockpos3.west(), BlockVine.EAST);
                     this.placeVine(worldIn, rand, blockpos3.south(), BlockVine.NORTH);
                  }
               }
            }
         }

         return true;
      }
   }

   private void placeVine(World p_181632_1_, Random p_181632_2_, BlockPos p_181632_3_, PropertyBool p_181632_4_) {
      if (p_181632_2_.nextInt(3) > 0 && p_181632_1_.isAirBlock(p_181632_3_)) {
         this.setBlockAndNotifyAdequately(p_181632_1_, p_181632_3_, Blocks.VINE.getDefaultState().withProperty(p_181632_4_, true));
      }

   }

   private void createCrown(World worldIn, BlockPos p_175930_2_, int p_175930_3_) {
      int i = true;

      for(int j = -2; j <= 0; ++j) {
         this.growLeavesLayerStrict(worldIn, p_175930_2_.up(j), p_175930_3_ + 1 - j);
      }

   }
}
