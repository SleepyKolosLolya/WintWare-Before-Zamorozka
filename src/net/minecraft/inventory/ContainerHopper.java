package net.minecraft.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

public class ContainerHopper extends Container {
   private final IInventory hopperInventory;

   public ContainerHopper(InventoryPlayer playerInventory, IInventory hopperInventoryIn, EntityPlayer player) {
      this.hopperInventory = hopperInventoryIn;
      hopperInventoryIn.openInventory(player);
      int i = true;

      int i1;
      for(i1 = 0; i1 < hopperInventoryIn.getSizeInventory(); ++i1) {
         this.addSlotToContainer(new Slot(hopperInventoryIn, i1, 44 + i1 * 18, 20));
      }

      for(i1 = 0; i1 < 3; ++i1) {
         for(int k = 0; k < 9; ++k) {
            this.addSlotToContainer(new Slot(playerInventory, k + i1 * 9 + 9, 8 + k * 18, i1 * 18 + 51));
         }
      }

      for(i1 = 0; i1 < 9; ++i1) {
         this.addSlotToContainer(new Slot(playerInventory, i1, 8 + i1 * 18, 109));
      }

   }

   public boolean canInteractWith(EntityPlayer playerIn) {
      return this.hopperInventory.isUsableByPlayer(playerIn);
   }

   public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
      ItemStack itemstack = ItemStack.field_190927_a;
      Slot slot = (Slot)this.inventorySlots.get(index);
      if (slot != null && slot.getHasStack()) {
         ItemStack itemstack1 = slot.getStack();
         itemstack = itemstack1.copy();
         if (index < this.hopperInventory.getSizeInventory()) {
            if (!this.mergeItemStack(itemstack1, this.hopperInventory.getSizeInventory(), this.inventorySlots.size(), true)) {
               return ItemStack.field_190927_a;
            }
         } else if (!this.mergeItemStack(itemstack1, 0, this.hopperInventory.getSizeInventory(), false)) {
            return ItemStack.field_190927_a;
         }

         if (itemstack1.func_190926_b()) {
            slot.putStack(ItemStack.field_190927_a);
         } else {
            slot.onSlotChanged();
         }
      }

      return itemstack;
   }

   public void onContainerClosed(EntityPlayer playerIn) {
      super.onContainerClosed(playerIn);
      this.hopperInventory.closeInventory(playerIn);
   }
}
