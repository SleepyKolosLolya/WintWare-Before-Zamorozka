package net.minecraft.client.gui.inventory;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerDispenser;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

public class GuiDispenser extends GuiContainer {
   private static final ResourceLocation DISPENSER_GUI_TEXTURES = new ResourceLocation("textures/gui/container/dispenser.png");
   private final InventoryPlayer playerInventory;
   public IInventory dispenserInventory;

   public GuiDispenser(InventoryPlayer playerInv, IInventory dispenserInv) {
      super(new ContainerDispenser(playerInv, dispenserInv));
      this.playerInventory = playerInv;
      this.dispenserInventory = dispenserInv;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      super.drawScreen(mouseX, mouseY, partialTicks);
      this.func_191948_b(mouseX, mouseY);
   }

   protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
      String s = this.dispenserInventory.getDisplayName().getUnformattedText();
      this.fontRendererObj.drawString(s, (float)(this.xSize / 2 - this.fontRendererObj.getStringWidth(s) / 2), 6.0F, 4210752);
      this.fontRendererObj.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8.0F, (float)(this.ySize - 96 + 2), 4210752);
   }

   protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(DISPENSER_GUI_TEXTURES);
      int i = (width - this.xSize) / 2;
      int j = (height - this.ySize) / 2;
      this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
   }
}
