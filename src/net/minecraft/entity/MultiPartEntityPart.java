package net.minecraft.entity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class MultiPartEntityPart extends Entity {
   public final IEntityMultiPart entityDragonObj;
   public final String partName;

   public MultiPartEntityPart(IEntityMultiPart parent, String partName, float base, float sizeHeight) {
      super(parent.getWorld());
      this.setSize(base, sizeHeight);
      this.entityDragonObj = parent;
      this.partName = partName;
   }

   protected void entityInit() {
   }

   protected void readEntityFromNBT(NBTTagCompound compound) {
   }

   protected void writeEntityToNBT(NBTTagCompound compound) {
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public boolean attackEntityFrom(DamageSource source, float amount) {
      return this.isEntityInvulnerable(source) ? false : this.entityDragonObj.attackEntityFromPart(this, source, amount);
   }

   public boolean isEntityEqual(Entity entityIn) {
      return this == entityIn || this.entityDragonObj == entityIn;
   }
}
