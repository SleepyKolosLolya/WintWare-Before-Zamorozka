package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;

public class GuiControls extends GuiScreen {
   private static final GameSettings.Options[] OPTIONS_ARR;
   private final GuiScreen parentScreen;
   protected String screenTitle = "Controls";
   private final GameSettings options;
   public KeyBinding buttonId;
   public long time;
   private GuiKeyBindingList keyBindingList;
   private GuiButton buttonReset;

   public GuiControls(GuiScreen screen, GameSettings settings) {
      this.parentScreen = screen;
      this.options = settings;
   }

   public void initGui() {
      this.keyBindingList = new GuiKeyBindingList(this, this.mc);
      this.buttonList.add(new GuiButton(200, width / 2 - 155 + 160, height - 29, 150, 20, I18n.format("gui.done")));
      this.buttonReset = this.addButton(new GuiButton(201, width / 2 - 155, height - 29, 150, 20, I18n.format("controls.resetAll")));
      this.screenTitle = I18n.format("controls.title");
      int i = 0;
      GameSettings.Options[] var2 = OPTIONS_ARR;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         GameSettings.Options gamesettings$options = var2[var4];
         if (gamesettings$options.getEnumFloat()) {
            this.buttonList.add(new GuiOptionSlider(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, 18 + 24 * (i >> 1), gamesettings$options));
         } else {
            this.buttonList.add(new GuiOptionButton(gamesettings$options.returnEnumOrdinal(), width / 2 - 155 + i % 2 * 160, 18 + 24 * (i >> 1), gamesettings$options, this.options.getKeyBinding(gamesettings$options)));
         }

         ++i;
      }

   }

   public void handleMouseInput() throws IOException {
      super.handleMouseInput();
      this.keyBindingList.handleMouseInput();
   }

   protected void actionPerformed(GuiButton button) throws IOException {
      if (button.id == 200) {
         this.mc.displayGuiScreen(this.parentScreen);
      } else if (button.id == 201) {
         KeyBinding[] var2 = this.mc.gameSettings.keyBindings;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            KeyBinding keybinding = var2[var4];
            keybinding.setKeyCode(keybinding.getKeyCodeDefault());
         }

         KeyBinding.resetKeyBindingArrayAndHash();
      } else if (button.id < 100 && button instanceof GuiOptionButton) {
         this.options.setOptionValue(((GuiOptionButton)button).returnEnumOptions(), 1);
         button.displayString = this.options.getKeyBinding(GameSettings.Options.getEnumOptions(button.id));
      }

   }

   protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
      if (this.buttonId != null) {
         this.options.setOptionKeyBinding(this.buttonId, -100 + mouseButton);
         this.buttonId = null;
         KeyBinding.resetKeyBindingArrayAndHash();
      } else if (mouseButton != 0 || !this.keyBindingList.mouseClicked(mouseX, mouseY, mouseButton)) {
         super.mouseClicked(mouseX, mouseY, mouseButton);
      }

   }

   protected void mouseReleased(int mouseX, int mouseY, int state) {
      if (state != 0 || !this.keyBindingList.mouseReleased(mouseX, mouseY, state)) {
         super.mouseReleased(mouseX, mouseY, state);
      }

   }

   protected void keyTyped(char typedChar, int keyCode) throws IOException {
      if (this.buttonId != null) {
         if (keyCode == 1) {
            this.options.setOptionKeyBinding(this.buttonId, 0);
         } else if (keyCode != 0) {
            this.options.setOptionKeyBinding(this.buttonId, keyCode);
         } else if (typedChar > 0) {
            this.options.setOptionKeyBinding(this.buttonId, typedChar + 256);
         }

         this.buttonId = null;
         this.time = Minecraft.getSystemTime();
         KeyBinding.resetKeyBindingArrayAndHash();
      } else {
         super.keyTyped(typedChar, keyCode);
      }

   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drawDefaultBackground();
      this.keyBindingList.drawScreen(mouseX, mouseY, partialTicks);
      drawCenteredString(this.fontRendererObj, this.screenTitle, width / 2, 8, 16777215);
      boolean flag = false;
      KeyBinding[] var5 = this.options.keyBindings;
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         KeyBinding keybinding = var5[var7];
         if (keybinding.getKeyCode() != keybinding.getKeyCodeDefault()) {
            flag = true;
            break;
         }
      }

      this.buttonReset.enabled = flag;
      super.drawScreen(mouseX, mouseY, partialTicks);
   }

   static {
      OPTIONS_ARR = new GameSettings.Options[]{GameSettings.Options.INVERT_MOUSE, GameSettings.Options.SENSITIVITY, GameSettings.Options.TOUCHSCREEN, GameSettings.Options.AUTO_JUMP};
   }
}
