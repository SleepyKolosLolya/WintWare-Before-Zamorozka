package net.minecraft.profiler;

import com.google.common.collect.Maps;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.HttpUtil;

public class Snooper {
   private final Map<String, Object> snooperStats = Maps.newHashMap();
   private final Map<String, Object> clientStats = Maps.newHashMap();
   private final String uniqueID = UUID.randomUUID().toString();
   private final URL serverUrl;
   private final ISnooperInfo playerStatsCollector;
   private final Timer threadTrigger = new Timer("Snooper Timer", true);
   private final Object syncLock = new Object();
   private final long minecraftStartTimeMilis;
   private boolean isRunning;
   private int selfCounter;

   public Snooper(String side, ISnooperInfo playerStatCollector, long startTime) {
      try {
         this.serverUrl = new URL("http://snoop.minecraft.net/" + side + "?version=" + 2);
      } catch (MalformedURLException var6) {
         throw new IllegalArgumentException();
      }

      this.playerStatsCollector = playerStatCollector;
      this.minecraftStartTimeMilis = startTime;
   }

   public void startSnooper() {
      if (!this.isRunning) {
         this.isRunning = true;
         this.addOSData();
         this.threadTrigger.schedule(new TimerTask() {
            public void run() {
               if (Snooper.this.playerStatsCollector.isSnooperEnabled()) {
                  HashMap map;
                  synchronized(Snooper.this.syncLock) {
                     map = Maps.newHashMap(Snooper.this.clientStats);
                     if (Snooper.this.selfCounter == 0) {
                        map.putAll(Snooper.this.snooperStats);
                     }

                     map.put("snooper_count", Snooper.this.selfCounter++);
                     map.put("snooper_token", Snooper.this.uniqueID);
                  }

                  MinecraftServer minecraftserver = Snooper.this.playerStatsCollector instanceof MinecraftServer ? (MinecraftServer)Snooper.this.playerStatsCollector : null;
                  HttpUtil.postMap(Snooper.this.serverUrl, map, true, minecraftserver == null ? null : minecraftserver.getServerProxy());
               }

            }
         }, 0L, 900000L);
      }

   }

   private void addOSData() {
      this.addJvmArgsToSnooper();
      this.addClientStat("snooper_token", this.uniqueID);
      this.addStatToSnooper("snooper_token", this.uniqueID);
      this.addStatToSnooper("os_name", System.getProperty("os.name"));
      this.addStatToSnooper("os_version", System.getProperty("os.version"));
      this.addStatToSnooper("os_architecture", System.getProperty("os.arch"));
      this.addStatToSnooper("java_version", System.getProperty("java.version"));
      this.addClientStat("version", "1.12.2");
      this.playerStatsCollector.addServerTypeToSnooper(this);
   }

   private void addJvmArgsToSnooper() {
      RuntimeMXBean runtimemxbean = ManagementFactory.getRuntimeMXBean();
      List<String> list = runtimemxbean.getInputArguments();
      int i = 0;
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         String s = (String)var4.next();
         if (s.startsWith("-X")) {
            this.addClientStat("jvm_arg[" + i++ + "]", s);
         }
      }

      this.addClientStat("jvm_args", i);
   }

   public void addMemoryStatsToSnooper() {
      this.addStatToSnooper("memory_total", Runtime.getRuntime().totalMemory());
      this.addStatToSnooper("memory_max", Runtime.getRuntime().maxMemory());
      this.addStatToSnooper("memory_free", Runtime.getRuntime().freeMemory());
      this.addStatToSnooper("cpu_cores", Runtime.getRuntime().availableProcessors());
      this.playerStatsCollector.addServerStatsToSnooper(this);
   }

   public void addClientStat(String statName, Object statValue) {
      synchronized(this.syncLock) {
         this.clientStats.put(statName, statValue);
      }
   }

   public void addStatToSnooper(String statName, Object statValue) {
      synchronized(this.syncLock) {
         this.snooperStats.put(statName, statValue);
      }
   }

   public Map<String, String> getCurrentStats() {
      Map<String, String> map = Maps.newLinkedHashMap();
      synchronized(this.syncLock) {
         this.addMemoryStatsToSnooper();
         Iterator var3 = this.snooperStats.entrySet().iterator();

         Entry entry1;
         while(var3.hasNext()) {
            entry1 = (Entry)var3.next();
            map.put(entry1.getKey(), entry1.getValue().toString());
         }

         var3 = this.clientStats.entrySet().iterator();

         while(var3.hasNext()) {
            entry1 = (Entry)var3.next();
            map.put(entry1.getKey(), entry1.getValue().toString());
         }

         return map;
      }
   }

   public boolean isSnooperRunning() {
      return this.isRunning;
   }

   public void stopSnooper() {
      this.threadTrigger.cancel();
   }

   public String getUniqueID() {
      return this.uniqueID;
   }

   public long getMinecraftStartTimeMillis() {
      return this.minecraftStartTimeMilis;
   }
}
