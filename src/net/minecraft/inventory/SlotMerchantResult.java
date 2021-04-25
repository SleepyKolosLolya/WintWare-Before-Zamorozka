package net.minecraft.inventory;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.village.MerchantRecipe;

public class SlotMerchantResult extends Slot {
   private final InventoryMerchant theMerchantInventory;
   private final EntityPlayer thePlayer;
   private int removeCount;
   private final IMerchant theMerchant;

   public SlotMerchantResult(EntityPlayer player, IMerchant merchant, InventoryMerchant merchantInventory, int slotIndex, int xPosition, int yPosition) {
      super(merchantInventory, slotIndex, xPosition, yPosition);
      this.thePlayer = player;
      this.theMerchant = merchant;
      this.theMerchantInventory = merchantInventory;
   }

   public boolean isItemValid(ItemStack stack) {
      return false;
   }

   public ItemStack decrStackSize(int amount) {
      if (this.getHasStack()) {
         this.removeCount += Math.min(amount, this.getStack().func_190916_E());
      }

      return super.decrStackSize(amount);
   }

   protected void onCrafting(ItemStack stack, int amount) {
      this.removeCount += amount;
      this.onCrafting(stack);
   }

   protected void onCrafting(ItemStack stack) {
      stack.onCrafting(this.thePlayer.world, this.thePlayer, this.removeCount);
      this.removeCount = 0;
   }

   public ItemStack func_190901_a(EntityPlayer p_190901_1_, ItemStack p_190901_2_) {
      this.onCrafting(p_190901_2_);
      MerchantRecipe merchantrecipe = this.theMerchantInventory.getCurrentRecipe();
      if (merchantrecipe != null) {
         ItemStack itemstack = this.theMerchantInventory.getStackInSlot(0);
         ItemStack itemstack1 = this.theMerchantInventory.getStackInSlot(1);
         if (this.doTrade(merchantrecipe, itemstack, itemstack1) || this.doTrade(merchantrecipe, itemstack1, itemstack)) {
            this.theMerchant.useRecipe(merchantrecipe);
            p_190901_1_.addStat(StatList.TRADED_WITH_VILLAGER);
            this.theMerchantInventory.setInventorySlotContents(0, itemstack);
            this.theMerchantInventory.setInventorySlotContents(1, itemstack1);
         }
      }

      return p_190901_2_;
   }

   private boolean doTrade(MerchantRecipe trade, ItemStack firstItem, ItemStack secondItem) {
      ItemStack itemstack = trade.getItemToBuy();
      ItemStack itemstack1 = trade.getSecondItemToBuy();
      if (firstItem.getItem() == itemstack.getItem() && firstItem.func_190916_E() >= itemstack.func_190916_E()) {
         if (!itemstack1.func_190926_b() && !secondItem.func_190926_b() && itemstack1.getItem() == secondItem.getItem() && secondItem.func_190916_E() >= itemstack1.func_190916_E()) {
            firstItem.func_190918_g(itemstack.func_190916_E());
            secondItem.func_190918_g(itemstack1.func_190916_E());
            return true;
         }

         if (itemstack1.func_190926_b() && secondItem.func_190926_b()) {
            firstItem.func_190918_g(itemstack.func_190916_E());
            return true;
         }
      }

      return false;
   }
}
