package net.minecraft.inventory;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ContainerMerchant extends Container {
   private final IMerchant theMerchant;
   private final InventoryMerchant merchantInventory;
   private final World theWorld;

   public ContainerMerchant(InventoryPlayer playerInventory, IMerchant merchant, World worldIn) {
      this.theMerchant = merchant;
      this.theWorld = worldIn;
      this.merchantInventory = new InventoryMerchant(playerInventory.player, merchant);
      this.addSlotToContainer(new Slot(this.merchantInventory, 0, 36, 53));
      this.addSlotToContainer(new Slot(this.merchantInventory, 1, 62, 53));
      this.addSlotToContainer(new SlotMerchantResult(playerInventory.player, merchant, this.merchantInventory, 2, 120, 53));

      int k;
      for(k = 0; k < 3; ++k) {
         for(int j = 0; j < 9; ++j) {
            this.addSlotToContainer(new Slot(playerInventory, j + k * 9 + 9, 8 + j * 18, 84 + k * 18));
         }
      }

      for(k = 0; k < 9; ++k) {
         this.addSlotToContainer(new Slot(playerInventory, k, 8 + k * 18, 142));
      }

   }

   public InventoryMerchant getMerchantInventory() {
      return this.merchantInventory;
   }

   public void onCraftMatrixChanged(IInventory inventoryIn) {
      this.merchantInventory.resetRecipeAndSlots();
      super.onCraftMatrixChanged(inventoryIn);
   }

   public void setCurrentRecipeIndex(int currentRecipeIndex) {
      this.merchantInventory.setCurrentRecipeIndex(currentRecipeIndex);
   }

   public boolean canInteractWith(EntityPlayer playerIn) {
      return this.theMerchant.getCustomer() == playerIn;
   }

   public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
      ItemStack itemstack = ItemStack.field_190927_a;
      Slot slot = (Slot)this.inventorySlots.get(index);
      if (slot != null && slot.getHasStack()) {
         ItemStack itemstack1 = slot.getStack();
         itemstack = itemstack1.copy();
         if (index == 2) {
            if (!this.mergeItemStack(itemstack1, 3, 39, true)) {
               return ItemStack.field_190927_a;
            }

            slot.onSlotChange(itemstack1, itemstack);
         } else if (index != 0 && index != 1) {
            if (index >= 3 && index < 30) {
               if (!this.mergeItemStack(itemstack1, 30, 39, false)) {
                  return ItemStack.field_190927_a;
               }
            } else if (index >= 30 && index < 39 && !this.mergeItemStack(itemstack1, 3, 30, false)) {
               return ItemStack.field_190927_a;
            }
         } else if (!this.mergeItemStack(itemstack1, 3, 39, false)) {
            return ItemStack.field_190927_a;
         }

         if (itemstack1.func_190926_b()) {
            slot.putStack(ItemStack.field_190927_a);
         } else {
            slot.onSlotChanged();
         }

         if (itemstack1.func_190916_E() == itemstack.func_190916_E()) {
            return ItemStack.field_190927_a;
         }

         slot.func_190901_a(playerIn, itemstack1);
      }

      return itemstack;
   }

   public void onContainerClosed(EntityPlayer playerIn) {
      super.onContainerClosed(playerIn);
      this.theMerchant.setCustomer((EntityPlayer)null);
      super.onContainerClosed(playerIn);
      if (!this.theWorld.isRemote) {
         ItemStack itemstack = this.merchantInventory.removeStackFromSlot(0);
         if (!itemstack.func_190926_b()) {
            playerIn.dropItem(itemstack, false);
         }

         itemstack = this.merchantInventory.removeStackFromSlot(1);
         if (!itemstack.func_190926_b()) {
            playerIn.dropItem(itemstack, false);
         }
      }

   }
}
