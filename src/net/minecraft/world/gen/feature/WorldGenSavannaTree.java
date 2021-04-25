package net.minecraft.world.gen.feature;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WorldGenSavannaTree extends WorldGenAbstractTree {
   private static final IBlockState TRUNK;
   private static final IBlockState LEAF;

   public WorldGenSavannaTree(boolean doBlockNotify) {
      super(doBlockNotify);
   }

   public boolean generate(World worldIn, Random rand, BlockPos position) {
      int i = rand.nextInt(3) + rand.nextInt(3) + 5;
      boolean flag = true;
      if (position.getY() >= 1 && position.getY() + i + 1 <= 256) {
         int l;
         int i3;
         for(int j = position.getY(); j <= position.getY() + 1 + i; ++j) {
            int k = 1;
            if (j == position.getY()) {
               k = 0;
            }

            if (j >= position.getY() + 1 + i - 2) {
               k = 2;
            }

            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for(l = position.getX() - k; l <= position.getX() + k && flag; ++l) {
               for(i3 = position.getZ() - k; i3 <= position.getZ() + k && flag; ++i3) {
                  if (j >= 0 && j < 256) {
                     if (!this.canGrowInto(worldIn.getBlockState(blockpos$mutableblockpos.setPos(l, j, i3)).getBlock())) {
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
               EnumFacing enumfacing = EnumFacing.Plane.HORIZONTAL.random(rand);
               int k2 = i - rand.nextInt(4) - 1;
               l = 3 - rand.nextInt(3);
               i3 = position.getX();
               int j1 = position.getZ();
               int k1 = 0;

               int k3;
               for(int l1 = 0; l1 < i; ++l1) {
                  k3 = position.getY() + l1;
                  if (l1 >= k2 && l > 0) {
                     i3 += enumfacing.getFrontOffsetX();
                     j1 += enumfacing.getFrontOffsetZ();
                     --l;
                  }

                  BlockPos blockpos = new BlockPos(i3, k3, j1);
                  Material material = worldIn.getBlockState(blockpos).getMaterial();
                  if (material == Material.AIR || material == Material.LEAVES) {
                     this.placeLogAt(worldIn, blockpos);
                     k1 = k3;
                  }
               }

               BlockPos blockpos2 = new BlockPos(i3, k1, j1);

               int l3;
               for(k3 = -3; k3 <= 3; ++k3) {
                  for(l3 = -3; l3 <= 3; ++l3) {
                     if (Math.abs(k3) != 3 || Math.abs(l3) != 3) {
                        this.placeLeafAt(worldIn, blockpos2.add(k3, 0, l3));
                     }
                  }
               }

               blockpos2 = blockpos2.up();

               for(k3 = -1; k3 <= 1; ++k3) {
                  for(l3 = -1; l3 <= 1; ++l3) {
                     this.placeLeafAt(worldIn, blockpos2.add(k3, 0, l3));
                  }
               }

               this.placeLeafAt(worldIn, blockpos2.east(2));
               this.placeLeafAt(worldIn, blockpos2.west(2));
               this.placeLeafAt(worldIn, blockpos2.south(2));
               this.placeLeafAt(worldIn, blockpos2.north(2));
               i3 = position.getX();
               j1 = position.getZ();
               EnumFacing enumfacing1 = EnumFacing.Plane.HORIZONTAL.random(rand);
               if (enumfacing1 != enumfacing) {
                  l3 = k2 - rand.nextInt(2) - 1;
                  int k4 = 1 + rand.nextInt(3);
                  k1 = 0;

                  int j5;
                  for(int l4 = l3; l4 < i && k4 > 0; --k4) {
                     if (l4 >= 1) {
                        j5 = position.getY() + l4;
                        i3 += enumfacing1.getFrontOffsetX();
                        j1 += enumfacing1.getFrontOffsetZ();
                        BlockPos blockpos1 = new BlockPos(i3, j5, j1);
                        Material material1 = worldIn.getBlockState(blockpos1).getMaterial();
                        if (material1 == Material.AIR || material1 == Material.LEAVES) {
                           this.placeLogAt(worldIn, blockpos1);
                           k1 = j5;
                        }
                     }

                     ++l4;
                  }

                  if (k1 > 0) {
                     BlockPos blockpos3 = new BlockPos(i3, k1, j1);

                     int l5;
                     for(j5 = -2; j5 <= 2; ++j5) {
                        for(l5 = -2; l5 <= 2; ++l5) {
                           if (Math.abs(j5) != 2 || Math.abs(l5) != 2) {
                              this.placeLeafAt(worldIn, blockpos3.add(j5, 0, l5));
                           }
                        }
                     }

                     blockpos3 = blockpos3.up();

                     for(j5 = -1; j5 <= 1; ++j5) {
                        for(l5 = -1; l5 <= 1; ++l5) {
                           this.placeLeafAt(worldIn, blockpos3.add(j5, 0, l5));
                        }
                     }
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

   private void placeLogAt(World worldIn, BlockPos pos) {
      this.setBlockAndNotifyAdequately(worldIn, pos, TRUNK);
   }

   private void placeLeafAt(World worldIn, BlockPos pos) {
      Material material = worldIn.getBlockState(pos).getMaterial();
      if (material == Material.AIR || material == Material.LEAVES) {
         this.setBlockAndNotifyAdequately(worldIn, pos, LEAF);
      }

   }

   static {
      TRUNK = Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.ACACIA);
      LEAF = Blocks.LEAVES2.getDefaultState().withProperty(BlockNewLeaf.VARIANT, BlockPlanks.EnumType.ACACIA).withProperty(BlockLeaves.CHECK_DECAY, false);
   }
}
