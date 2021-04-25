package net.minecraft.inventory;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

public class InventoryBasic implements IInventory {
   private String inventoryTitle;
   private final int slotsCount;
   private final NonNullList<ItemStack> inventoryContents;
   private List<IInventoryChangedListener> changeListeners;
   private boolean hasCustomName;

   public InventoryBasic(String title, boolean customName, int slotCount) {
      this.inventoryTitle = title;
      this.hasCustomName = customName;
      this.slotsCount = slotCount;
      this.inventoryContents = NonNullList.func_191197_a(slotCount, ItemStack.field_190927_a);
   }

   public InventoryBasic(ITextComponent title, int slotCount) {
      this(title.getUnformattedText(), true, slotCount);
   }

   public void addInventoryChangeListener(IInventoryChangedListener listener) {
      if (this.changeListeners == null) {
         this.changeListeners = Lists.newArrayList();
      }

      this.changeListeners.add(listener);
   }

   public void removeInventoryChangeListener(IInventoryChangedListener listener) {
      this.changeListeners.remove(listener);
   }

   public ItemStack getStackInSlot(int index) {
      return index >= 0 && index < this.inventoryContents.size() ? (ItemStack)this.inventoryContents.get(index) : ItemStack.field_190927_a;
   }

   public ItemStack decrStackSize(int index, int count) {
      ItemStack itemstack = ItemStackHelper.getAndSplit(this.inventoryContents, index, count);
      if (!itemstack.func_190926_b()) {
         this.markDirty();
      }

      return itemstack;
   }

   public ItemStack addItem(ItemStack stack) {
      ItemStack itemstack = stack.copy();

      for(int i = 0; i < this.slotsCount; ++i) {
         ItemStack itemstack1 = this.getStackInSlot(i);
         if (itemstack1.func_190926_b()) {
            this.setInventorySlotContents(i, itemstack);
            this.markDirty();
            return ItemStack.field_190927_a;
         }

         if (ItemStack.areItemsEqual(itemstack1, itemstack)) {
            int j = Math.min(this.getInventoryStackLimit(), itemstack1.getMaxStackSize());
            int k = Math.min(itemstack.func_190916_E(), j - itemstack1.func_190916_E());
            if (k > 0) {
               itemstack1.func_190917_f(k);
               itemstack.func_190918_g(k);
               if (itemstack.func_190926_b()) {
                  this.markDirty();
                  return ItemStack.field_190927_a;
               }
            }
         }
      }

      if (itemstack.func_190916_E() != stack.func_190916_E()) {
         this.markDirty();
      }

      return itemstack;
   }

   public ItemStack removeStackFromSlot(int index) {
      ItemStack itemstack = (ItemStack)this.inventoryContents.get(index);
      if (itemstack.func_190926_b()) {
         return ItemStack.field_190927_a;
      } else {
         this.inventoryContents.set(index, ItemStack.field_190927_a);
         return itemstack;
      }
   }

   public void setInventorySlotContents(int index, ItemStack stack) {
      this.inventoryContents.set(index, stack);
      if (!stack.func_190926_b() && stack.func_190916_E() > this.getInventoryStackLimit()) {
         stack.func_190920_e(this.getInventoryStackLimit());
      }

      this.markDirty();
   }

   public int getSizeInventory() {
      return this.slotsCount;
   }

   public boolean func_191420_l() {
      Iterator var1 = this.inventoryContents.iterator();

      ItemStack itemstack;
      do {
         if (!var1.hasNext()) {
            return true;
         }

         itemstack = (ItemStack)var1.next();
      } while(itemstack.func_190926_b());

      return false;
   }

   public String getName() {
      return this.inventoryTitle;
   }

   public boolean hasCustomName() {
      return this.hasCustomName;
   }

   public void setCustomName(String inventoryTitleIn) {
      this.hasCustomName = true;
      this.inventoryTitle = inventoryTitleIn;
   }

   public ITextComponent getDisplayName() {
      return (ITextComponent)(this.hasCustomName() ? new TextComponentString(this.getName()) : new TextComponentTranslation(this.getName(), new Object[0]));
   }

   public int getInventoryStackLimit() {
      return 64;
   }

   public void markDirty() {
      if (this.changeListeners != null) {
         for(int i = 0; i < this.changeListeners.size(); ++i) {
            ((IInventoryChangedListener)this.changeListeners.get(i)).onInventoryChanged(this);
         }
      }

   }

   public boolean isUsableByPlayer(EntityPlayer player) {
      return true;
   }

   public void openInventory(EntityPlayer player) {
   }

   public void closeInventory(EntityPlayer player) {
   }

   public boolean isItemValidForSlot(int index, ItemStack stack) {
      return true;
   }

   public int getField(int id) {
      return 0;
   }

   public void setField(int id, int value) {
   }

   public int getFieldCount() {
      return 0;
   }

   public void clear() {
      this.inventoryContents.clear();
   }
}
