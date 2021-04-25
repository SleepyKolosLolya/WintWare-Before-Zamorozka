package me.wintware.client.module.visual;

import me.wintware.client.Main;
import me.wintware.client.clickgui.settings.Setting;
import me.wintware.client.event.EventTarget;
import me.wintware.client.event.impl.EventReceivePacket;
import me.wintware.client.event.impl.EventUpdate;
import me.wintware.client.module.Category;
import me.wintware.client.module.Module;
import net.minecraft.network.play.server.SPacketTimeUpdate;

public class WorldTime extends Module {
   Setting time;
   long lol = 0L;
   private long yaw;

   public WorldTime() {
      super("WorldTime", Category.Visuals);
      Main.instance.setmgr.rSetting(this.time = new Setting("Speed", this, 20.0D, 0.1D, 1000.0D, false));
   }

   @EventTarget
   public void onPacket(EventReceivePacket event) {
      if (event.getPacket() instanceof SPacketTimeUpdate) {
         event.setCancelled(true);
      }

   }

   @EventTarget
   public void onUpdate(EventUpdate e) {
      mc.world.setWorldTime(this.lol);
      this.lol += this.time.getValLong();
   }
}
