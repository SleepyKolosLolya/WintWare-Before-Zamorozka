package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.resources.I18n;

public class GuiYesNo extends GuiScreen {
   protected GuiYesNoCallback parentScreen;
   protected String messageLine1;
   private final String messageLine2;
   private final List<String> listLines = Lists.newArrayList();
   protected String confirmButtonText;
   protected String cancelButtonText;
   protected int parentButtonClickedId;
   private int ticksUntilEnable;

   public GuiYesNo(GuiYesNoCallback p_i1082_1_, String p_i1082_2_, String p_i1082_3_, int p_i1082_4_) {
      this.parentScreen = p_i1082_1_;
      this.messageLine1 = p_i1082_2_;
      this.messageLine2 = p_i1082_3_;
      this.parentButtonClickedId = p_i1082_4_;
      this.confirmButtonText = I18n.format("gui.yes");
      this.cancelButtonText = I18n.format("gui.no");
   }

   public GuiYesNo(GuiYesNoCallback p_i1083_1_, String p_i1083_2_, String p_i1083_3_, String p_i1083_4_, String p_i1083_5_, int p_i1083_6_) {
      this.parentScreen = p_i1083_1_;
      this.messageLine1 = p_i1083_2_;
      this.messageLine2 = p_i1083_3_;
      this.confirmButtonText = p_i1083_4_;
      this.cancelButtonText = p_i1083_5_;
      this.parentButtonClickedId = p_i1083_6_;
   }

   public void initGui() {
      this.buttonList.add(new GuiOptionButton(0, width / 2 - 155, height / 6 + 96, this.confirmButtonText));
      this.buttonList.add(new GuiOptionButton(1, width / 2 - 155 + 160, height / 6 + 96, this.cancelButtonText));
      this.listLines.clear();
      this.listLines.addAll(this.fontRendererObj.listFormattedStringToWidth(this.messageLine2, width - 50));
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      this.parentScreen.confirmClicked(button.id == 0, this.parentButtonClickedId);
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      drawCenteredString(this.fontRendererObj, this.messageLine1, width / 2, 70, 16777215);
      int i = 90;

      for(Iterator var5 = this.listLines.iterator(); var5.hasNext(); i += this.fontRendererObj.FONT_HEIGHT) {
         String s = (String)var5.next();
         drawCenteredString(this.fontRendererObj, s, width / 2, i, 16777215);
      }

      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   public void setButtonDelay(int p_146350_1_) {
      this.ticksUntilEnable = p_146350_1_;

      GuiButton guibutton;
      for(Iterator var2 = this.buttonList.iterator(); var2.hasNext(); guibutton.enabled = false) {
         guibutton = (GuiButton)var2.next();
      }

   }

   public void updateScreen() {
      super.updateScreen();
      GuiButton guibutton;
      if (--this.ticksUntilEnable == 0) {
         for(Iterator var1 = this.buttonList.iterator(); var1.hasNext(); guibutton.enabled = true) {
            guibutton = (GuiButton)var1.next();
         }
      }

   }
}
