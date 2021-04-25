package net.minecraft.inventory;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;

public class SlotCrafting extends Slot {
   private final InventoryCrafting craftMatrix;
   private final EntityPlayer thePlayer;
   private int amountCrafted;

   public SlotCrafting(EntityPlayer player, InventoryCrafting craftingInventory, IInventory inventoryIn, int slotIndex, int xPosition, int yPosition) {
      super(inventoryIn, slotIndex, xPosition, yPosition);
      this.thePlayer = player;
      this.craftMatrix = craftingInventory;
   }

   public boolean isItemValid(ItemStack stack) {
      return false;
   }

   public ItemStack decrStackSize(int amount) {
      if (this.getHasStack()) {
         this.amountCrafted += Math.min(amount, this.getStack().func_190916_E());
      }

      return super.decrStackSize(amount);
   }

   protected void onCrafting(ItemStack stack, int amount) {
      this.amountCrafted += amount;
      this.onCrafting(stack);
   }

   protected void func_190900_b(int p_190900_1_) {
      this.amountCrafted += p_190900_1_;
   }

   protected void onCrafting(ItemStack stack) {
      if (this.amountCrafted > 0) {
         stack.onCrafting(this.thePlayer.world, this.thePlayer, this.amountCrafted);
      }

      this.amountCrafted = 0;
      InventoryCraftResult inventorycraftresult = (InventoryCraftResult)this.inventory;
      IRecipe irecipe = inventorycraftresult.func_193055_i();
      if (irecipe != null && !irecipe.func_192399_d()) {
         this.thePlayer.func_192021_a(Lists.newArrayList(new IRecipe[]{irecipe}));
         inventorycraftresult.func_193056_a((IRecipe)null);
      }

   }

   public ItemStack func_190901_a(EntityPlayer p_190901_1_, ItemStack p_190901_2_) {
      this.onCrafting(p_190901_2_);
      NonNullList<ItemStack> nonnulllist = CraftingManager.getRemainingItems(this.craftMatrix, p_190901_1_.world);

      for(int i = 0; i < nonnulllist.size(); ++i) {
         ItemStack itemstack = this.craftMatrix.getStackInSlot(i);
         ItemStack itemstack1 = (ItemStack)nonnulllist.get(i);
         if (!itemstack.func_190926_b()) {
            this.craftMatrix.decrStackSize(i, 1);
            itemstack = this.craftMatrix.getStackInSlot(i);
         }

         if (!itemstack1.func_190926_b()) {
            if (itemstack.func_190926_b()) {
               this.craftMatrix.setInventorySlotContents(i, itemstack1);
            } else if (ItemStack.areItemsEqual(itemstack, itemstack1) && ItemStack.areItemStackTagsEqual(itemstack, itemstack1)) {
               itemstack1.func_190917_f(itemstack.func_190916_E());
               this.craftMatrix.setInventorySlotContents(i, itemstack1);
            } else if (!this.thePlayer.inventory.addItemStackToInventory(itemstack1)) {
               this.thePlayer.dropItem(itemstack1, false);
            }
         }
      }

      return p_190901_2_;
   }
}
