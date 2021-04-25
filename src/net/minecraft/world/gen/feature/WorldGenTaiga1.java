package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldGenTaiga1 extends WorldGenAbstractTree {
   private static final IBlockState TRUNK;
   private static final IBlockState LEAF;

   public WorldGenTaiga1() {
      super(false);
   }

   public boolean generate(World worldIn, Random rand, BlockPos position) {
      int i = rand.nextInt(5) + 7;
      int j = i - rand.nextInt(2) - 3;
      int k = i - j;
      int l = 1 + rand.nextInt(k + 1);
      if (position.getY() >= 1 && position.getY() + i + 1 <= 256) {
         boolean flag = true;

         int j3;
         int k3;
         int k2;
         for(int i1 = position.getY(); i1 <= position.getY() + 1 + i && flag; ++i1) {
            int j1 = true;
            if (i1 - position.getY() < j) {
               k2 = 0;
            } else {
               k2 = l;
            }

            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for(j3 = position.getX() - k2; j3 <= position.getX() + k2 && flag; ++j3) {
               for(k3 = position.getZ() - k2; k3 <= position.getZ() + k2 && flag; ++k3) {
                  if (i1 >= 0 && i1 < 256) {
                     if (!this.canGrowInto(worldIn.getBlockState(blockpos$mutableblockpos.setPos(j3, i1, k3)).getBlock())) {
                        flag = false;
                     }
                  } else {
                     flag = false;
                  }
               }
            }
         }

         if (!flag) {
            return false;
         } else {
            Block block = worldIn.getBlockState(position.down()).getBlock();
            if ((block == Blocks.GRASS || block == Blocks.DIRT) && position.getY() < 256 - i - 1) {
               this.setDirtAt(worldIn, position.down());
               k2 = 0;

               int l2;
               for(l2 = position.getY() + i; l2 >= position.getY() + j; --l2) {
                  for(j3 = position.getX() - k2; j3 <= position.getX() + k2; ++j3) {
                     k3 = j3 - position.getX();

                     for(int i2 = position.getZ() - k2; i2 <= position.getZ() + k2; ++i2) {
                        int j2 = i2 - position.getZ();
                        if (Math.abs(k3) != k2 || Math.abs(j2) != k2 || k2 <= 0) {
                           BlockPos blockpos = new BlockPos(j3, l2, i2);
                           if (!worldIn.getBlockState(blockpos).isFullBlock()) {
                              this.setBlockAndNotifyAdequately(worldIn, blockpos, LEAF);
                           }
                        }
                     }
                  }

                  if (k2 >= 1 && l2 == position.getY() + j + 1) {
                     --k2;
                  } else if (k2 < l) {
                     ++k2;
                  }
               }

               for(l2 = 0; l2 < i - 1; ++l2) {
                  Material material = worldIn.getBlockState(position.up(l2)).getMaterial();
                  if (material == Material.AIR || material == Material.LEAVES) {
                     this.setBlockAndNotifyAdequately(worldIn, position.up(l2), TRUNK);
                  }
               }

               return true;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   static {
      TRUNK = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE);
      LEAF = Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.SPRUCE).withProperty(BlockLeaves.CHECK_DECAY, false);
   }
}
