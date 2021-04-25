package net.minecraft.network.play.server;

import java.io.IOException;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.INetHandlerPlayClient;

public class SPacketEntityEquipment implements Packet<INetHandlerPlayClient> {
   private int entityID;
   private EntityEquipmentSlot equipmentSlot;
   private ItemStack itemStack;

   public SPacketEntityEquipment() {
      this.itemStack = ItemStack.field_190927_a;
   }

   public SPacketEntityEquipment(int entityIdIn, EntityEquipmentSlot equipmentSlotIn, ItemStack itemStackIn) {
      this.itemStack = ItemStack.field_190927_a;
      this.entityID = entityIdIn;
      this.equipmentSlot = equipmentSlotIn;
      this.itemStack = itemStackIn.copy();
   }

   public void readPacketData(PacketBuffer buf) throws IOException {
      this.entityID = buf.readVarIntFromBuffer();
      this.equipmentSlot = (EntityEquipmentSlot)buf.readEnumValue(EntityEquipmentSlot.class);
      this.itemStack = buf.readItemStackFromBuffer();
   }

   public void writePacketData(PacketBuffer buf) throws IOException {
      buf.writeVarIntToBuffer(this.entityID);
      buf.writeEnumValue(this.equipmentSlot);
      buf.writeItemStackToBuffer(this.itemStack);
   }

   public void processPacket(INetHandlerPlayClient handler) {
      handler.handleEntityEquipment(this);
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public int getEntityID() {
      return this.entityID;
   }

   public EntityEquipmentSlot getEquipmentSlot() {
      return this.equipmentSlot;
   }
}
