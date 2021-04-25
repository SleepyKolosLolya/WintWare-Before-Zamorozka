package net.minecraft.client.gui;

import java.io.IOException;
import java.net.URI;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiScreenDemo extends GuiScreen {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final ResourceLocation DEMO_BACKGROUND_LOCATION = new ResourceLocation("textures/gui/demo_background.png");

   public void initGui() {
      this.buttonList.clear();
      int i = true;
      this.buttonList.add(new GuiButton(1, width / 2 - 116, height / 2 + 62 + -16, 114, 20, I18n.format("demo.help.buy")));
      this.buttonList.add(new GuiButton(2, width / 2 + 2, height / 2 + 62 + -16, 114, 20, I18n.format("demo.help.later")));
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      switch(button.id) {
      case 1:
         button.enabled = false;

         try {
            Class<?> oclass = Class.forName("java.awt.Desktop");
            Object object = oclass.getMethod("getDesktop").invoke((Object)null);
            oclass.getMethod("browse", URI.class).invoke(object, new URI("http://www.minecraft.net/store?source=demo"));
         } catch (Throwable var4) {
            LOGGER.error("Couldn't open link", var4);
         }
         break;
      case 2:
         this.mc.displayGuiScreen((GuiScreen)null);
         this.mc.setIngameFocus();
      }

   }

   public void drawDefaultBackground() {
      super.drawDefaultBackground();
      GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
      this.mc.getTextureManager().bindTexture(DEMO_BACKGROUND_LOCATION);
      int i = (width - 248) / 2;
      int j = (height - 166) / 2;
      this.drawTexturedModalRect(i, j, 0, 0, 248, 166);
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      int i = (width - 248) / 2 + 10;
      int j = (height - 166) / 2 + 8;
      this.fontRendererObj.drawString(I18n.format("demo.help.title"), (float)i, (float)j, 2039583);
      j += 12;
      GameSettings gamesettings = this.mc.gameSettings;
      this.fontRendererObj.drawString(I18n.format("demo.help.movementShort", GameSettings.getKeyDisplayString(gamesettings.keyBindForward.getKeyCode()), GameSettings.getKeyDisplayString(gamesettings.keyBindLeft.getKeyCode()), GameSettings.getKeyDisplayString(gamesettings.keyBindBack.getKeyCode()), GameSettings.getKeyDisplayString(gamesettings.keyBindRight.getKeyCode())), (float)i, (float)j, 5197647);
      this.fontRendererObj.drawString(I18n.format("demo.help.movementMouse"), (float)i, (float)(j + 12), 5197647);
      this.fontRendererObj.drawString(I18n.format("demo.help.jump", GameSettings.getKeyDisplayString(gamesettings.keyBindJump.getKeyCode())), (float)i, (float)(j + 24), 5197647);
      this.fontRendererObj.drawString(I18n.format("demo.help.inventory", GameSettings.getKeyDisplayString(gamesettings.keyBindInventory.getKeyCode())), (float)i, (float)(j + 36), 5197647);
      this.fontRendererObj.drawSplitString(I18n.format("demo.help.fullWrapped"), i, j + 68, 218, 2039583);
      super.drawScreen(mouseX, mouseY, partialTicks);
   }
}
