package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import optifine.Config;
import optifine.TextureUtils;
import shadersmod.client.Shaders;

public class TextureAtlasSprite {
   private final String iconName;
   protected List<int[][]> framesTextureData = Lists.newArrayList();
   protected int[][] interpolatedFrameData;
   private AnimationMetadataSection animationMetadata;
   protected boolean rotated;
   protected int originX;
   protected int originY;
   protected int width;
   protected int height;
   private float minU;
   private float maxU;
   private float minV;
   private float maxV;
   protected int frameCounter;
   protected int tickCounter;
   private int indexInMap = -1;
   public float baseU;
   public float baseV;
   public int sheetWidth;
   public int sheetHeight;
   public int glSpriteTextureId = -1;
   public TextureAtlasSprite spriteSingle = null;
   public boolean isSpriteSingle = false;
   public int mipmapLevels = 0;
   public TextureAtlasSprite spriteNormal = null;
   public TextureAtlasSprite spriteSpecular = null;
   public boolean isShadersSprite = false;
   public boolean isDependencyParent = false;

   private TextureAtlasSprite(TextureAtlasSprite p_i2_1_) {
      this.iconName = p_i2_1_.iconName;
      this.isSpriteSingle = true;
   }

   protected TextureAtlasSprite(String spriteName) {
      this.iconName = spriteName;
      if (Config.isMultiTexture()) {
         this.spriteSingle = new TextureAtlasSprite(this);
      }

   }

   protected static TextureAtlasSprite makeAtlasSprite(ResourceLocation spriteResourceLocation) {
      return new TextureAtlasSprite(spriteResourceLocation.toString());
   }

   public void initSprite(int inX, int inY, int originInX, int originInY, boolean rotatedIn) {
      this.originX = originInX;
      this.originY = originInY;
      this.rotated = rotatedIn;
      float f = (float)(0.009999999776482582D / (double)inX);
      float f1 = (float)(0.009999999776482582D / (double)inY);
      this.minU = (float)originInX / (float)((double)inX) + f;
      this.maxU = (float)(originInX + this.width) / (float)((double)inX) - f;
      this.minV = (float)originInY / (float)inY + f1;
      this.maxV = (float)(originInY + this.height) / (float)inY - f1;
      this.baseU = Math.min(this.minU, this.maxU);
      this.baseV = Math.min(this.minV, this.maxV);
      if (this.spriteSingle != null) {
         this.spriteSingle.initSprite(this.width, this.height, 0, 0, false);
      }

   }

   public void copyFrom(TextureAtlasSprite atlasSpirit) {
      this.originX = atlasSpirit.originX;
      this.originY = atlasSpirit.originY;
      this.width = atlasSpirit.width;
      this.height = atlasSpirit.height;
      this.rotated = atlasSpirit.rotated;
      this.minU = atlasSpirit.minU;
      this.maxU = atlasSpirit.maxU;
      this.minV = atlasSpirit.minV;
      this.maxV = atlasSpirit.maxV;
      if (this.spriteSingle != null) {
         this.spriteSingle.initSprite(this.width, this.height, 0, 0, false);
      }

   }

   public int getOriginX() {
      return this.originX;
   }

   public int getOriginY() {
      return this.originY;
   }

   public int getIconWidth() {
      return this.width;
   }

   public int getIconHeight() {
      return this.height;
   }

   public float getMinU() {
      return this.minU;
   }

   public float getMaxU() {
      return this.maxU;
   }

   public float getInterpolatedU(double u) {
      float f = this.maxU - this.minU;
      return this.minU + f * (float)u / 16.0F;
   }

   public float getUnInterpolatedU(float p_188537_1_) {
      float f = this.maxU - this.minU;
      return (p_188537_1_ - this.minU) / f * 16.0F;
   }

   public float getMinV() {
      return this.minV;
   }

   public float getMaxV() {
      return this.maxV;
   }

   public float getInterpolatedV(double v) {
      float f = this.maxV - this.minV;
      return this.minV + f * (float)v / 16.0F;
   }

   public float getUnInterpolatedV(float p_188536_1_) {
      float f = this.maxV - this.minV;
      return (p_188536_1_ - this.minV) / f * 16.0F;
   }

   public String getIconName() {
      return this.iconName;
   }

   public void updateAnimation() {
      if (this.animationMetadata != null) {
         ++this.tickCounter;
         if (this.tickCounter >= this.animationMetadata.getFrameTimeSingle(this.frameCounter)) {
            int i = this.animationMetadata.getFrameIndex(this.frameCounter);
            int j = this.animationMetadata.getFrameCount() == 0 ? this.framesTextureData.size() : this.animationMetadata.getFrameCount();
            this.frameCounter = (this.frameCounter + 1) % j;
            this.tickCounter = 0;
            int k = this.animationMetadata.getFrameIndex(this.frameCounter);
            boolean flag = false;
            boolean flag1 = this.isSpriteSingle;
            if (i != k && k >= 0 && k < this.framesTextureData.size()) {
               TextureUtil.uploadTextureMipmap((int[][])this.framesTextureData.get(k), this.width, this.height, this.originX, this.originY, flag, flag1);
            }
         } else if (this.animationMetadata.isInterpolate()) {
            this.updateAnimationInterpolated();
         }
      }

   }

   private void updateAnimationInterpolated() {
      double d0 = 1.0D - (double)this.tickCounter / (double)this.animationMetadata.getFrameTimeSingle(this.frameCounter);
      int i = this.animationMetadata.getFrameIndex(this.frameCounter);
      int j = this.animationMetadata.getFrameCount() == 0 ? this.framesTextureData.size() : this.animationMetadata.getFrameCount();
      int k = this.animationMetadata.getFrameIndex((this.frameCounter + 1) % j);
      if (i != k && k >= 0 && k < this.framesTextureData.size()) {
         int[][] aint = (int[][])this.framesTextureData.get(i);
         int[][] aint1 = (int[][])this.framesTextureData.get(k);
         if (this.interpolatedFrameData == null || this.interpolatedFrameData.length != aint.length) {
            this.interpolatedFrameData = new int[aint.length][];
         }

         for(int l = 0; l < aint.length; ++l) {
            if (this.interpolatedFrameData[l] == null) {
               this.interpolatedFrameData[l] = new int[aint[l].length];
            }

            if (l < aint1.length && aint1[l].length == aint[l].length) {
               for(int i1 = 0; i1 < aint[l].length; ++i1) {
                  int j1 = aint[l][i1];
                  int k1 = aint1[l][i1];
                  int l1 = this.interpolateColor(d0, j1 >> 16 & 255, k1 >> 16 & 255);
                  int i2 = this.interpolateColor(d0, j1 >> 8 & 255, k1 >> 8 & 255);
                  int j2 = this.interpolateColor(d0, j1 & 255, k1 & 255);
                  this.interpolatedFrameData[l][i1] = j1 & -16777216 | l1 << 16 | i2 << 8 | j2;
               }
            }
         }

         TextureUtil.uploadTextureMipmap(this.interpolatedFrameData, this.width, this.height, this.originX, this.originY, false, false);
      }

   }

   private int interpolateColor(double p_188535_1_, int p_188535_3_, int p_188535_4_) {
      return (int)(p_188535_1_ * (double)p_188535_3_ + (1.0D - p_188535_1_) * (double)p_188535_4_);
   }

   public int[][] getFrameTextureData(int index) {
      return (int[][])this.framesTextureData.get(index);
   }

   public int getFrameCount() {
      return this.framesTextureData.size();
   }

   public void setIconWidth(int newWidth) {
      this.width = newWidth;
      if (this.spriteSingle != null) {
         this.spriteSingle.setIconWidth(this.width);
      }

   }

   public void setIconHeight(int newHeight) {
      this.height = newHeight;
      if (this.spriteSingle != null) {
         this.spriteSingle.setIconHeight(this.height);
      }

   }

   public void loadSprite(PngSizeInfo sizeInfo, boolean p_188538_2_) throws IOException {
      this.resetSprite();
      this.width = sizeInfo.pngWidth;
      this.height = sizeInfo.pngHeight;
      if (p_188538_2_) {
         this.height = this.width;
      } else if (sizeInfo.pngHeight != sizeInfo.pngWidth) {
         throw new RuntimeException("broken aspect ratio and not an animation");
      }

      if (this.spriteSingle != null) {
         this.spriteSingle.width = this.width;
         this.spriteSingle.height = this.height;
      }

   }

   public void loadSpriteFrames(IResource resource, int mipmaplevels) throws IOException {
      BufferedImage bufferedimage = TextureUtil.readBufferedImage(resource.getInputStream());
      if (this.width != bufferedimage.getWidth()) {
         bufferedimage = TextureUtils.scaleImage(bufferedimage, this.width);
      }

      AnimationMetadataSection animationmetadatasection = (AnimationMetadataSection)resource.getMetadata("animation");
      int[][] aint = new int[mipmaplevels][];
      aint[0] = new int[bufferedimage.getWidth() * bufferedimage.getHeight()];
      bufferedimage.getRGB(0, 0, bufferedimage.getWidth(), bufferedimage.getHeight(), aint[0], 0, bufferedimage.getWidth());
      int k;
      int i1;
      if (animationmetadatasection == null) {
         this.framesTextureData.add(aint);
      } else {
         k = bufferedimage.getHeight() / this.width;
         if (animationmetadatasection.getFrameCount() > 0) {
            Iterator iterator = animationmetadatasection.getFrameIndexSet().iterator();

            while(iterator.hasNext()) {
               i1 = (Integer)iterator.next();
               if (i1 >= k) {
                  throw new RuntimeException("invalid frameindex " + i1);
               }

               this.allocateFrameTextureData(i1);
               this.framesTextureData.set(i1, getFrameTextureData(aint, this.width, this.width, i1));
            }

            this.animationMetadata = animationmetadatasection;
         } else {
            List<AnimationFrame> list = Lists.newArrayList();

            for(i1 = 0; i1 < k; ++i1) {
               this.framesTextureData.add(getFrameTextureData(aint, this.width, this.width, i1));
               list.add(new AnimationFrame(i1, -1));
            }

            this.animationMetadata = new AnimationMetadataSection(list, this.width, this.height, animationmetadatasection.getFrameTime(), animationmetadatasection.isInterpolate());
         }
      }

      if (!this.isShadersSprite) {
         if (Config.isShaders()) {
            this.loadShadersSprites();
         }

         for(k = 0; k < this.framesTextureData.size(); ++k) {
            int[][] aint2 = (int[][])this.framesTextureData.get(k);
            if (aint2 != null && !this.iconName.startsWith("minecraft:blocks/leaves_")) {
               for(i1 = 0; i1 < aint2.length; ++i1) {
                  int[] aint1 = aint2[i1];
                  this.fixTransparentColor(aint1);
               }
            }
         }

         if (this.spriteSingle != null) {
            IResource iresource = Config.getResourceManager().getResource(resource.getResourceLocation());
            this.spriteSingle.loadSpriteFrames(iresource, mipmaplevels);
         }
      }

   }

   public void generateMipmaps(int level) {
      List<int[][]> list = Lists.newArrayList();

      for(int i = 0; i < this.framesTextureData.size(); ++i) {
         final int[][] aint = (int[][])this.framesTextureData.get(i);
         if (aint != null) {
            try {
               list.add(TextureUtil.generateMipmapData(level, this.width, aint));
            } catch (Throwable var8) {
               CrashReport crashreport = CrashReport.makeCrashReport(var8, "Generating mipmaps for frame");
               CrashReportCategory crashreportcategory = crashreport.makeCategory("Frame being iterated");
               crashreportcategory.addCrashSection("Frame index", i);
               crashreportcategory.setDetail("Frame sizes", new ICrashReportDetail<String>() {
                  public String call() throws Exception {
                     StringBuilder stringbuilder = new StringBuilder();
                     int[][] var2 = aint;
                     int var3 = var2.length;

                     for(int var4 = 0; var4 < var3; ++var4) {
                        int[] aint1 = var2[var4];
                        if (stringbuilder.length() > 0) {
                           stringbuilder.append(", ");
                        }

                        stringbuilder.append(aint1 == null ? "null" : aint1.length);
                     }

                     return stringbuilder.toString();
                  }
               });
               throw new ReportedException(crashreport);
            }
         }
      }

      this.setFramesTextureData(list);
      if (this.spriteSingle != null) {
         this.spriteSingle.generateMipmaps(level);
      }

   }

   private void allocateFrameTextureData(int index) {
      if (this.framesTextureData.size() <= index) {
         for(int i = this.framesTextureData.size(); i <= index; ++i) {
            this.framesTextureData.add((Object)null);
         }
      }

      if (this.spriteSingle != null) {
         this.spriteSingle.allocateFrameTextureData(index);
      }

   }

   private static int[][] getFrameTextureData(int[][] data, int rows, int columns, int p_147962_3_) {
      int[][] aint = new int[data.length][];

      for(int i = 0; i < data.length; ++i) {
         int[] aint1 = data[i];
         if (aint1 != null) {
            aint[i] = new int[(rows >> i) * (columns >> i)];
            System.arraycopy(aint1, p_147962_3_ * aint[i].length, aint[i], 0, aint[i].length);
         }
      }

      return aint;
   }

   public void clearFramesTextureData() {
      this.framesTextureData.clear();
      if (this.spriteSingle != null) {
         this.spriteSingle.clearFramesTextureData();
      }

   }

   public boolean hasAnimationMetadata() {
      return this.animationMetadata != null;
   }

   public void setFramesTextureData(List<int[][]> arrayList) {
      this.framesTextureData = arrayList;
      if (this.spriteSingle != null) {
         this.spriteSingle.setFramesTextureData(arrayList);
      }

   }

   private void resetSprite() {
      this.animationMetadata = null;
      this.setFramesTextureData(Lists.newArrayList());
      this.frameCounter = 0;
      this.tickCounter = 0;
      if (this.spriteSingle != null) {
         this.spriteSingle.resetSprite();
      }

   }

   public String toString() {
      return "TextureAtlasSprite{name='" + this.iconName + '\'' + ", frameCount=" + this.framesTextureData.size() + ", rotated=" + this.rotated + ", x=" + this.originX + ", y=" + this.originY + ", height=" + this.height + ", width=" + this.width + ", u0=" + this.minU + ", u1=" + this.maxU + ", v0=" + this.minV + ", v1=" + this.maxV + '}';
   }

   public boolean hasCustomLoader(IResourceManager p_hasCustomLoader_1_, ResourceLocation p_hasCustomLoader_2_) {
      return false;
   }

   public boolean load(IResourceManager p_load_1_, ResourceLocation p_load_2_, Function<ResourceLocation, TextureAtlasSprite> p_load_3_) {
      return true;
   }

   public Collection<ResourceLocation> getDependencies() {
      return ImmutableList.of();
   }

   public int getIndexInMap() {
      return this.indexInMap;
   }

   public void setIndexInMap(int p_setIndexInMap_1_) {
      this.indexInMap = p_setIndexInMap_1_;
   }

   private void fixTransparentColor(int[] p_fixTransparentColor_1_) {
      if (p_fixTransparentColor_1_ != null) {
         long i = 0L;
         long j = 0L;
         long k = 0L;
         long l = 0L;

         int l2;
         int i3;
         int j3;
         int k3;
         int l3;
         int i4;
         for(l2 = 0; l2 < p_fixTransparentColor_1_.length; ++l2) {
            i3 = p_fixTransparentColor_1_[l2];
            j3 = i3 >> 24 & 255;
            if (j3 >= 16) {
               k3 = i3 >> 16 & 255;
               l3 = i3 >> 8 & 255;
               i4 = i3 & 255;
               i += (long)k3;
               j += (long)l3;
               k += (long)i4;
               ++l;
            }
         }

         if (l > 0L) {
            l2 = (int)(i / l);
            i3 = (int)(j / l);
            j3 = (int)(k / l);
            k3 = l2 << 16 | i3 << 8 | j3;

            for(l3 = 0; l3 < p_fixTransparentColor_1_.length; ++l3) {
               i4 = p_fixTransparentColor_1_[l3];
               int k2 = i4 >> 24 & 255;
               if (k2 <= 16) {
                  p_fixTransparentColor_1_[l3] = k3;
               }
            }
         }
      }

   }

   public double getSpriteU16(float p_getSpriteU16_1_) {
      float f = this.maxU - this.minU;
      return (double)((p_getSpriteU16_1_ - this.minU) / f * 16.0F);
   }

   public double getSpriteV16(float p_getSpriteV16_1_) {
      float f = this.maxV - this.minV;
      return (double)((p_getSpriteV16_1_ - this.minV) / f * 16.0F);
   }

   public void bindSpriteTexture() {
      if (this.glSpriteTextureId < 0) {
         this.glSpriteTextureId = TextureUtil.glGenTextures();
         TextureUtil.allocateTextureImpl(this.glSpriteTextureId, this.mipmapLevels, this.width, this.height);
         TextureUtils.applyAnisotropicLevel();
      }

      TextureUtils.bindTexture(this.glSpriteTextureId);
   }

   public void deleteSpriteTexture() {
      if (this.glSpriteTextureId >= 0) {
         TextureUtil.deleteTexture(this.glSpriteTextureId);
         this.glSpriteTextureId = -1;
      }

   }

   public float toSingleU(float p_toSingleU_1_) {
      p_toSingleU_1_ -= this.baseU;
      float f = (float)this.sheetWidth / (float)this.width;
      p_toSingleU_1_ *= f;
      return p_toSingleU_1_;
   }

   public float toSingleV(float p_toSingleV_1_) {
      p_toSingleV_1_ -= this.baseV;
      float f = (float)this.sheetHeight / (float)this.height;
      p_toSingleV_1_ *= f;
      return p_toSingleV_1_;
   }

   public List<int[][]> getFramesTextureData() {
      List<int[][]> list = new ArrayList();
      list.addAll(this.framesTextureData);
      return list;
   }

   public AnimationMetadataSection getAnimationMetadata() {
      return this.animationMetadata;
   }

   public void setAnimationMetadata(AnimationMetadataSection p_setAnimationMetadata_1_) {
      this.animationMetadata = p_setAnimationMetadata_1_;
   }

   private void loadShadersSprites() {
      String s1;
      ResourceLocation resourcelocation1;
      if (Shaders.configNormalMap) {
         s1 = this.iconName + "_n";
         resourcelocation1 = new ResourceLocation(s1);
         resourcelocation1 = Config.getTextureMap().completeResourceLocation(resourcelocation1);
         if (Config.hasResource(resourcelocation1)) {
            this.spriteNormal = new TextureAtlasSprite(s1);
            this.spriteNormal.isShadersSprite = true;
            this.spriteNormal.copyFrom(this);
            Config.getTextureMap().generateMipmaps(Config.getResourceManager(), this.spriteNormal);
         }
      }

      if (Shaders.configSpecularMap) {
         s1 = this.iconName + "_s";
         resourcelocation1 = new ResourceLocation(s1);
         resourcelocation1 = Config.getTextureMap().completeResourceLocation(resourcelocation1);
         if (Config.hasResource(resourcelocation1)) {
            this.spriteSpecular = new TextureAtlasSprite(s1);
            this.spriteSpecular.isShadersSprite = true;
            this.spriteSpecular.copyFrom(this);
            Config.getTextureMap().generateMipmaps(Config.getResourceManager(), this.spriteSpecular);
         }
      }

   }
}
