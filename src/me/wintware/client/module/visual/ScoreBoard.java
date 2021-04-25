package me.wintware.client.module.visual;

import me.wintware.client.Main;
import me.wintware.client.clickgui.settings.Setting;
import me.wintware.client.event.EventTarget;
import me.wintware.client.event.impl.EventRenderScoreboard;
import me.wintware.client.module.Category;
import me.wintware.client.module.Module;
import net.minecraft.client.renderer.GlStateManager;

public class ScoreBoard extends Module {
   public Setting x;
   public Setting y;

   public ScoreBoard() {
      super("Scoreboard", Category.Visuals);
      Main.instance.setmgr.rSetting(new Setting("NoScoreBoard", this, false));
      Main.instance.setmgr.rSetting(this.x = new Setting("Scoreboard X", this, 0.0D, 0.0D, 500.0D, false));
      Main.instance.setmgr.rSetting(this.y = new Setting("Scoreboard Y", this, 0.0D, 0.0D, 500.0D, false));
   }

   @EventTarget
   public void onRenderScoreboard(EventRenderScoreboard event) {
      if (event.isPre()) {
         GlStateManager.translate(-this.x.getValDouble(), this.y.getValDouble(), 1.0D);
      } else {
         GlStateManager.translate(this.x.getValDouble(), -this.y.getValDouble(), 1.0D);
      }

   }
}
