package net.minecraft.client.renderer;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.Container;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

public abstract class InventoryEffectRenderer extends GuiContainer {
   protected boolean hasActivePotionEffects;

   public InventoryEffectRenderer(Container inventorySlotsIn) {
      super(inventorySlotsIn);
   }

   public void initGui() {
      super.initGui();
      this.updateActivePotionEffects();
   }

   protected void updateActivePotionEffects() {
      if (this.mc.player.getActivePotionEffects().isEmpty()) {
         this.guiLeft = (width - this.xSize) / 2;
         this.hasActivePotionEffects = false;
      } else {
         this.guiLeft = 160 + (width - this.xSize - 200) / 2;
         this.hasActivePotionEffects = true;
      }

   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      super.drawScreen(mouseX, mouseY, partialTicks);
      if (this.hasActivePotionEffects) {
         this.drawActivePotionEffects();
      }

   }

   private void drawActivePotionEffects() {
      int i = this.guiLeft - 124;
      int j = this.guiTop;
      int k = true;
      Collection<PotionEffect> collection = this.mc.player.getActivePotionEffects();
      if (!collection.isEmpty()) {
         GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager.disableLighting();
         int l = 33;
         if (collection.size() > 5) {
            l = 132 / (collection.size() - 1);
         }

         for(Iterator var6 = Ordering.natural().sortedCopy(collection).iterator(); var6.hasNext(); j += l) {
            PotionEffect potioneffect = (PotionEffect)var6.next();
            Potion potion = potioneffect.getPotion();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(INVENTORY_BACKGROUND);
            this.drawTexturedModalRect(i, j, 0, 166, 140, 32);
            if (potion.hasStatusIcon()) {
               int i1 = potion.getStatusIconIndex();
               this.drawTexturedModalRect(i + 6, j + 7, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18);
            }

            String s1 = I18n.format(potion.getName());
            if (potioneffect.getAmplifier() == 1) {
               s1 = s1 + " " + I18n.format("enchantment.level.2");
            } else if (potioneffect.getAmplifier() == 2) {
               s1 = s1 + " " + I18n.format("enchantment.level.3");
            } else if (potioneffect.getAmplifier() == 3) {
               s1 = s1 + " " + I18n.format("enchantment.level.4");
            }

            this.fontRendererObj.drawStringWithShadow(s1, (float)(i + 10 + 18), (float)(j + 6), 16777215);
            String s = Potion.getPotionDurationString(potioneffect, 1.0F);
            this.fontRendererObj.drawStringWithShadow(s, (float)(i + 10 + 18), (float)(j + 6 + 10), 8355711);
         }
      }

   }
}
