package net.minecraft.client.model;

import net.minecraft.entity.Entity;

public class ModelMinecart extends ModelBase {
   public ModelRenderer[] sideModels = new ModelRenderer[7];

   public ModelMinecart() {
      this.sideModels[0] = new ModelRenderer(this, 0, 10);
      this.sideModels[1] = new ModelRenderer(this, 0, 0);
      this.sideModels[2] = new ModelRenderer(this, 0, 0);
      this.sideModels[3] = new ModelRenderer(this, 0, 0);
      this.sideModels[4] = new ModelRenderer(this, 0, 0);
      this.sideModels[5] = new ModelRenderer(this, 44, 10);
      int i = true;
      int j = true;
      int k = true;
      int l = true;
      this.sideModels[0].addBox(-10.0F, -8.0F, -1.0F, 20, 16, 2, 0.0F);
      this.sideModels[0].setRotationPoint(0.0F, 4.0F, 0.0F);
      this.sideModels[5].addBox(-9.0F, -7.0F, -1.0F, 18, 14, 1, 0.0F);
      this.sideModels[5].setRotationPoint(0.0F, 4.0F, 0.0F);
      this.sideModels[1].addBox(-8.0F, -9.0F, -1.0F, 16, 8, 2, 0.0F);
      this.sideModels[1].setRotationPoint(-9.0F, 4.0F, 0.0F);
      this.sideModels[2].addBox(-8.0F, -9.0F, -1.0F, 16, 8, 2, 0.0F);
      this.sideModels[2].setRotationPoint(9.0F, 4.0F, 0.0F);
      this.sideModels[3].addBox(-8.0F, -9.0F, -1.0F, 16, 8, 2, 0.0F);
      this.sideModels[3].setRotationPoint(0.0F, 4.0F, -7.0F);
      this.sideModels[4].addBox(-8.0F, -9.0F, -1.0F, 16, 8, 2, 0.0F);
      this.sideModels[4].setRotationPoint(0.0F, 4.0F, 7.0F);
      this.sideModels[0].rotateAngleX = 1.5707964F;
      this.sideModels[1].rotateAngleY = 4.712389F;
      this.sideModels[2].rotateAngleY = 1.5707964F;
      this.sideModels[3].rotateAngleY = 3.1415927F;
      this.sideModels[5].rotateAngleX = -1.5707964F;
   }

   public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      this.sideModels[5].rotationPointY = 4.0F - ageInTicks;

      for(int i = 0; i < 6; ++i) {
         this.sideModels[i].render(scale);
      }

   }
}
