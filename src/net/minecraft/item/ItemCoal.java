package net.minecraft.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.NonNullList;

public class ItemCoal extends Item {
   public ItemCoal() {
      this.setHasSubtypes(true);
      this.setMaxDamage(0);
      this.setCreativeTab(CreativeTabs.MATERIALS);
   }

   public String getUnlocalizedName(ItemStack stack) {
      return stack.getMetadata() == 1 ? "item.charcoal" : "item.coal";
   }

   public void getSubItems(CreativeTabs itemIn, NonNullList<ItemStack> tab) {
      if (this.func_194125_a(itemIn)) {
         tab.add(new ItemStack(this, 1, 0));
         tab.add(new ItemStack(this, 1, 1));
      }

   }
}
