package net.minecraft.client.tutorial;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.toasts.TutorialToast;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;

public class FindTreeStep implements ITutorialStep {
   private static final Set<Block> field_193268_a;
   private static final ITextComponent field_193269_b;
   private static final ITextComponent field_193270_c;
   private final Tutorial field_193271_d;
   private TutorialToast field_193272_e;
   private int field_193273_f;

   public FindTreeStep(Tutorial p_i47582_1_) {
      this.field_193271_d = p_i47582_1_;
   }

   public void func_193245_a() {
      ++this.field_193273_f;
      if (this.field_193271_d.func_194072_f() != GameType.SURVIVAL) {
         this.field_193271_d.func_193292_a(TutorialSteps.NONE);
      } else {
         if (this.field_193273_f == 1) {
            EntityPlayerSP entityplayersp = this.field_193271_d.func_193295_e().player;
            if (entityplayersp != null) {
               Iterator var2 = field_193268_a.iterator();

               while(var2.hasNext()) {
                  Block block = (Block)var2.next();
                  if (entityplayersp.inventory.hasItemStack(new ItemStack(block))) {
                     this.field_193271_d.func_193292_a(TutorialSteps.CRAFT_PLANKS);
                     return;
                  }
               }

               if (func_194070_a(entityplayersp)) {
                  this.field_193271_d.func_193292_a(TutorialSteps.CRAFT_PLANKS);
                  return;
               }
            }
         }

         if (this.field_193273_f >= 6000 && this.field_193272_e == null) {
            this.field_193272_e = new TutorialToast(TutorialToast.Icons.TREE, field_193269_b, field_193270_c, false);
            this.field_193271_d.func_193295_e().func_193033_an().func_192988_a(this.field_193272_e);
         }
      }

   }

   public void func_193248_b() {
      if (this.field_193272_e != null) {
         this.field_193272_e.func_193670_a();
         this.field_193272_e = null;
      }

   }

   public void func_193246_a(WorldClient p_193246_1_, RayTraceResult p_193246_2_) {
      if (p_193246_2_.typeOfHit == RayTraceResult.Type.BLOCK && p_193246_2_.getBlockPos() != null) {
         IBlockState iblockstate = p_193246_1_.getBlockState(p_193246_2_.getBlockPos());
         if (field_193268_a.contains(iblockstate.getBlock())) {
            this.field_193271_d.func_193292_a(TutorialSteps.PUNCH_TREE);
         }
      }

   }

   public void func_193252_a(ItemStack p_193252_1_) {
      Iterator var2 = field_193268_a.iterator();

      Block block;
      do {
         if (!var2.hasNext()) {
            return;
         }

         block = (Block)var2.next();
      } while(p_193252_1_.getItem() != Item.getItemFromBlock(block));

      this.field_193271_d.func_193292_a(TutorialSteps.CRAFT_PLANKS);
   }

   public static boolean func_194070_a(EntityPlayerSP p_194070_0_) {
      Iterator var1 = field_193268_a.iterator();

      StatBase statbase;
      do {
         if (!var1.hasNext()) {
            return false;
         }

         Block block = (Block)var1.next();
         statbase = StatList.getBlockStats(block);
      } while(statbase == null || p_194070_0_.getStatFileWriter().readStat(statbase) <= 0);

      return true;
   }

   static {
      field_193268_a = Sets.newHashSet(new Block[]{Blocks.LOG, Blocks.LOG2, Blocks.LEAVES, Blocks.LEAVES2});
      field_193269_b = new TextComponentTranslation("tutorial.find_tree.title", new Object[0]);
      field_193270_c = new TextComponentTranslation("tutorial.find_tree.description", new Object[0]);
   }
}
