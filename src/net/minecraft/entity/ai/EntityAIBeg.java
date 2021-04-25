package net.minecraft.entity.ai;

import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EntityAIBeg extends EntityAIBase {
   private final EntityWolf theWolf;
   private EntityPlayer thePlayer;
   private final World worldObject;
   private final float minPlayerDistance;
   private int timeoutCounter;

   public EntityAIBeg(EntityWolf wolf, float minDistance) {
      this.theWolf = wolf;
      this.worldObject = wolf.world;
      this.minPlayerDistance = minDistance;
      this.setMutexBits(2);
   }

   public boolean shouldExecute() {
      this.thePlayer = this.worldObject.getClosestPlayerToEntity(this.theWolf, (double)this.minPlayerDistance);
      return this.thePlayer == null ? false : this.hasPlayerGotBoneInHand(this.thePlayer);
   }

   public boolean continueExecuting() {
      if (!this.thePlayer.isEntityAlive()) {
         return false;
      } else if (this.theWolf.getDistanceSqToEntity(this.thePlayer) > (double)(this.minPlayerDistance * this.minPlayerDistance)) {
         return false;
      } else {
         return this.timeoutCounter > 0 && this.hasPlayerGotBoneInHand(this.thePlayer);
      }
   }

   public void startExecuting() {
      this.theWolf.setBegging(true);
      this.timeoutCounter = 40 + this.theWolf.getRNG().nextInt(40);
   }

   public void resetTask() {
      this.theWolf.setBegging(false);
      this.thePlayer = null;
   }

   public void updateTask() {
      this.theWolf.getLookHelper().setLookPosition(this.thePlayer.posX, this.thePlayer.posY + (double)this.thePlayer.getEyeHeight(), this.thePlayer.posZ, 10.0F, (float)this.theWolf.getVerticalFaceSpeed());
      --this.timeoutCounter;
   }

   private boolean hasPlayerGotBoneInHand(EntityPlayer player) {
      EnumHand[] var2 = EnumHand.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         EnumHand enumhand = var2[var4];
         ItemStack itemstack = player.getHeldItem(enumhand);
         if (this.theWolf.isTamed() && itemstack.getItem() == Items.BONE) {
            return true;
         }

         if (this.theWolf.isBreedingItem(itemstack)) {
            return true;
         }
      }

      return false;
   }
}
