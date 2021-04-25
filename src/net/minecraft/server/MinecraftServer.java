package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ICrashReportDetail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkSystem;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.profiler.ISnooperInfo;
import net.minecraft.profiler.Profiler;
import net.minecraft.profiler.Snooper;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.ITickable;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.GameType;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.ServerWorldEventHandler;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerDemo;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MinecraftServer implements ICommandSender, Runnable, IThreadListener, ISnooperInfo {
   private static final Logger LOG = LogManager.getLogger();
   public static final File USER_CACHE_FILE = new File("usercache.json");
   private final ISaveFormat anvilConverterForAnvilFile;
   public static MinecraftServer instance;
   private final Snooper usageSnooper = new Snooper("server", this, getCurrentTimeMillis());
   private final File anvilFile;
   private final List<ITickable> tickables = Lists.newArrayList();
   public final ICommandManager commandManager;
   public final Profiler theProfiler = new Profiler();
   private final NetworkSystem networkSystem;
   private final ServerStatusResponse statusResponse = new ServerStatusResponse();
   private final Random random = new Random();
   private final DataFixer dataFixer;
   private int serverPort = -1;
   public WorldServer[] worldServers;
   private PlayerList playerList;
   private boolean serverRunning = true;
   private boolean serverStopped;
   private int tickCounter;
   protected final Proxy serverProxy;
   public String currentTask;
   public int percentDone;
   private boolean onlineMode;
   private boolean field_190519_A;
   private boolean canSpawnAnimals;
   private boolean canSpawnNPCs;
   private boolean pvpEnabled;
   private boolean allowFlight;
   private String motd;
   private int buildLimit;
   private int maxPlayerIdleMinutes;
   public final long[] tickTimeArray = new long[100];
   public long[][] timeOfLastDimensionTick;
   private KeyPair serverKeyPair;
   private String serverOwner;
   private String folderName;
   private String worldName;
   private boolean isDemo;
   private boolean enableBonusChest;
   private String resourcePackUrl = "";
   private String resourcePackHash = "";
   private boolean serverIsRunning;
   private long timeOfLastWarning;
   private String userMessage;
   private boolean startProfiling;
   private boolean isGamemodeForced;
   private final YggdrasilAuthenticationService authService;
   private final MinecraftSessionService sessionService;
   private final GameProfileRepository profileRepo;
   private final PlayerProfileCache profileCache;
   private long nanoTimeSinceStatusRefresh;
   public final Queue<FutureTask<?>> futureTaskQueue = Queues.newArrayDeque();
   private Thread serverThread;
   private long currentTime = getCurrentTimeMillis();
   private boolean worldIconSet;

   public MinecraftServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
      this.serverProxy = proxyIn;
      this.authService = authServiceIn;
      this.sessionService = sessionServiceIn;
      this.profileRepo = profileRepoIn;
      this.profileCache = profileCacheIn;
      this.anvilFile = anvilFileIn;
      this.networkSystem = new NetworkSystem(this);
      this.commandManager = this.createNewCommandManager();
      this.anvilConverterForAnvilFile = new AnvilSaveConverter(anvilFileIn, dataFixerIn);
      this.dataFixer = dataFixerIn;
   }

   public ServerCommandManager createNewCommandManager() {
      return new ServerCommandManager(this);
   }

   public abstract boolean startServer() throws IOException;

   public void convertMapIfNeeded(String worldNameIn) {
      if (this.getActiveAnvilConverter().isOldMapFormat(worldNameIn)) {
         LOG.info("Converting map!");
         this.setUserMessage("menu.convertingLevel");
         this.getActiveAnvilConverter().convertMapFormat(worldNameIn, new IProgressUpdate() {
            private long startTime = System.currentTimeMillis();

            public void displaySavingString(String message) {
            }

            public void resetProgressAndMessage(String message) {
            }

            public void setLoadingProgress(int progress) {
               if (System.currentTimeMillis() - this.startTime >= 1000L) {
                  this.startTime = System.currentTimeMillis();
                  MinecraftServer.LOG.info("Converting... {}%", progress);
               }

            }

            public void setDoneWorking() {
            }

            public void displayLoadingString(String message) {
            }
         });
      }

   }

   protected synchronized void setUserMessage(String message) {
      this.userMessage = message;
   }

   @Nullable
   public synchronized String getUserMessage() {
      return this.userMessage;
   }

   public void loadAllWorlds(String saveName, String worldNameIn, long seed, WorldType type, String generatorOptions) {
      this.convertMapIfNeeded(saveName);
      this.setUserMessage("menu.loadingLevel");
      this.worldServers = new WorldServer[3];
      this.timeOfLastDimensionTick = new long[this.worldServers.length][100];
      ISaveHandler isavehandler = this.anvilConverterForAnvilFile.getSaveLoader(saveName, true);
      this.setResourcePackFromWorld(this.getFolderName(), isavehandler);
      WorldInfo worldinfo = isavehandler.loadWorldInfo();
      WorldSettings worldsettings;
      if (worldinfo == null) {
         if (this.isDemo()) {
            worldsettings = WorldServerDemo.DEMO_WORLD_SETTINGS;
         } else {
            worldsettings = new WorldSettings(seed, this.getGameType(), this.canStructuresSpawn(), this.isHardcore(), type);
            worldsettings.setGeneratorOptions(generatorOptions);
            if (this.enableBonusChest) {
               worldsettings.enableBonusChest();
            }
         }

         worldinfo = new WorldInfo(worldsettings, worldNameIn);
      } else {
         worldinfo.setWorldName(worldNameIn);
         worldsettings = new WorldSettings(worldinfo);
      }

      for(int i = 0; i < this.worldServers.length; ++i) {
         int j = 0;
         if (i == 1) {
            j = -1;
         }

         if (i == 2) {
            j = 1;
         }

         if (i == 0) {
            if (this.isDemo()) {
               this.worldServers[i] = (WorldServer)(new WorldServerDemo(this, isavehandler, worldinfo, j, this.theProfiler)).init();
            } else {
               this.worldServers[i] = (WorldServer)(new WorldServer(this, isavehandler, worldinfo, j, this.theProfiler)).init();
            }

            this.worldServers[i].initialize(worldsettings);
         } else {
            this.worldServers[i] = (WorldServer)(new WorldServerMulti(this, isavehandler, j, this.worldServers[0], this.theProfiler)).init();
         }

         this.worldServers[i].addEventListener(new ServerWorldEventHandler(this, this.worldServers[i]));
         if (!this.isSinglePlayer()) {
            this.worldServers[i].getWorldInfo().setGameType(this.getGameType());
         }
      }

      this.playerList.setPlayerManager(this.worldServers);
      this.setDifficultyForAllWorlds(this.getDifficulty());
      this.initialWorldChunkLoad();
   }

   public void initialWorldChunkLoad() {
      int i = true;
      int j = true;
      int k = true;
      int l = true;
      int i1 = 0;
      this.setUserMessage("menu.generatingTerrain");
      int j1 = false;
      LOG.info("Preparing start region for level 0");
      WorldServer worldserver = this.worldServers[0];
      BlockPos blockpos = worldserver.getSpawnPoint();
      long k1 = getCurrentTimeMillis();

      for(int l1 = -192; l1 <= 192 && this.isServerRunning(); l1 += 16) {
         for(int i2 = -192; i2 <= 192 && this.isServerRunning(); i2 += 16) {
            long j2 = getCurrentTimeMillis();
            if (j2 - k1 > 1000L) {
               this.outputPercentRemaining("Preparing spawn area", i1 * 100 / 625);
               k1 = j2;
            }

            ++i1;
            worldserver.getChunkProvider().provideChunk(blockpos.getX() + l1 >> 4, blockpos.getZ() + i2 >> 4);
         }
      }

      this.clearCurrentTask();
   }

   public void setResourcePackFromWorld(String worldNameIn, ISaveHandler saveHandlerIn) {
      File file1 = new File(saveHandlerIn.getWorldDirectory(), "resources.zip");
      if (file1.isFile()) {
         try {
            this.setResourcePack("level://" + URLEncoder.encode(worldNameIn, StandardCharsets.UTF_8.toString()) + "/resources.zip", "");
         } catch (UnsupportedEncodingException var5) {
            LOG.warn("Something went wrong url encoding {}", worldNameIn);
         }
      }

   }

   public abstract boolean canStructuresSpawn();

   public abstract GameType getGameType();

   public abstract EnumDifficulty getDifficulty();

   public abstract boolean isHardcore();

   public abstract int getOpPermissionLevel();

   public abstract boolean shouldBroadcastRconToOps();

   public abstract boolean shouldBroadcastConsoleToOps();

   protected void outputPercentRemaining(String message, int percent) {
      this.currentTask = message;
      this.percentDone = percent;
      LOG.info("{}: {}%", message, percent);
   }

   protected void clearCurrentTask() {
      this.currentTask = null;
      this.percentDone = 0;
   }

   public void saveAllWorlds(boolean isSilent) {
      WorldServer[] var2 = this.worldServers;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         WorldServer worldserver = var2[var4];
         if (worldserver != null) {
            if (!isSilent) {
               LOG.info("Saving chunks for level '{}'/{}", worldserver.getWorldInfo().getWorldName(), worldserver.provider.getDimensionType().getName());
            }

            try {
               worldserver.saveAllChunks(true, (IProgressUpdate)null);
            } catch (MinecraftException var7) {
               LOG.warn(var7.getMessage());
            }
         }
      }

   }

   public void stopServer() {
      LOG.info("Stopping server");
      if (this.getNetworkSystem() != null) {
         this.getNetworkSystem().terminateEndpoints();
      }

      if (this.playerList != null) {
         LOG.info("Saving players");
         this.playerList.saveAllPlayerData();
         this.playerList.removeAllPlayers();
      }

      if (this.worldServers != null) {
         LOG.info("Saving worlds");
         WorldServer[] var1 = this.worldServers;
         int var2 = var1.length;

         int var3;
         WorldServer worldserver1;
         for(var3 = 0; var3 < var2; ++var3) {
            worldserver1 = var1[var3];
            if (worldserver1 != null) {
               worldserver1.disableLevelSaving = false;
            }
         }

         this.saveAllWorlds(false);
         var1 = this.worldServers;
         var2 = var1.length;

         for(var3 = 0; var3 < var2; ++var3) {
            worldserver1 = var1[var3];
            if (worldserver1 != null) {
               worldserver1.flush();
            }
         }
      }

      if (this.usageSnooper.isSnooperRunning()) {
         this.usageSnooper.stopSnooper();
      }

   }

   public boolean isServerRunning() {
      return this.serverRunning;
   }

   public void initiateShutdown() {
      this.serverRunning = false;
   }

   public void run() {
      try {
         if (this.startServer()) {
            this.currentTime = getCurrentTimeMillis();
            long i = 0L;
            this.statusResponse.setServerDescription(new TextComponentString(this.motd));
            this.statusResponse.setVersion(new ServerStatusResponse.Version("1.12.2", 340));
            this.applyServerIconToResponse(this.statusResponse);

            while(this.serverRunning) {
               long k = getCurrentTimeMillis();
               long j = k - this.currentTime;
               if (j > 2000L && this.currentTime - this.timeOfLastWarning >= 15000L) {
                  LOG.warn("Can't keep up! Did the system time change, or is the server overloaded? Running {}ms behind, skipping {} tick(s)", j, j / 50L);
                  j = 2000L;
                  this.timeOfLastWarning = this.currentTime;
               }

               if (j < 0L) {
                  LOG.warn("Time ran backwards! Did the system time change?");
                  j = 0L;
               }

               i += j;
               this.currentTime = k;
               if (this.worldServers[0].areAllPlayersAsleep()) {
                  this.tick();
                  i = 0L;
               } else {
                  while(i > 50L) {
                     i -= 50L;
                     this.tick();
                  }
               }

               Thread.sleep(Math.max(1L, 50L - i));
               this.serverIsRunning = true;
            }
         } else {
            this.finalTick((CrashReport)null);
         }
      } catch (Throwable var46) {
         LOG.error("Encountered an unexpected exception", var46);
         CrashReport crashreport = null;
         if (var46 instanceof ReportedException) {
            crashreport = this.addServerInfoToCrashReport(((ReportedException)var46).getCrashReport());
         } else {
            crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", var46));
         }

         File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
         if (crashreport.saveToFile(file1)) {
            LOG.error("This crash report has been saved to: {}", file1.getAbsolutePath());
         } else {
            LOG.error("We were unable to save this crash report to disk.");
         }

         this.finalTick(crashreport);
      } finally {
         try {
            this.serverStopped = true;
            this.stopServer();
         } catch (Throwable var44) {
            LOG.error("Exception stopping the server", var44);
         } finally {
            this.systemExitNow();
         }

      }

   }

   public void applyServerIconToResponse(ServerStatusResponse response) {
      File file1 = this.getFile("server-icon.png");
      if (!file1.exists()) {
         file1 = this.getActiveAnvilConverter().getFile(this.getFolderName(), "icon.png");
      }

      if (file1.isFile()) {
         ByteBuf bytebuf = Unpooled.buffer();

         try {
            BufferedImage bufferedimage = ImageIO.read(file1);
            Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide", new Object[0]);
            Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high", new Object[0]);
            ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
            ByteBuf bytebuf1 = Base64.encode(bytebuf);
            response.setFavicon("data:image/png;base64," + bytebuf1.toString(StandardCharsets.UTF_8));
         } catch (Exception var9) {
            LOG.error("Couldn't load server icon", var9);
         } finally {
            bytebuf.release();
         }
      }

   }

   public boolean isWorldIconSet() {
      this.worldIconSet = this.worldIconSet || this.getWorldIconFile().isFile();
      return this.worldIconSet;
   }

   public File getWorldIconFile() {
      return this.getActiveAnvilConverter().getFile(this.getFolderName(), "icon.png");
   }

   public File getDataDirectory() {
      return new File(".");
   }

   public void finalTick(CrashReport report) {
   }

   public void systemExitNow() {
   }

   public void tick() {
      long i = System.nanoTime();
      ++this.tickCounter;
      if (this.startProfiling) {
         this.startProfiling = false;
         this.theProfiler.profilingEnabled = true;
         this.theProfiler.clearProfiling();
      }

      this.theProfiler.startSection("root");
      this.updateTimeLightAndEntities();
      if (i - this.nanoTimeSinceStatusRefresh >= 5000000000L) {
         this.nanoTimeSinceStatusRefresh = i;
         this.statusResponse.setPlayers(new ServerStatusResponse.Players(this.getMaxPlayers(), this.getCurrentPlayerCount()));
         GameProfile[] agameprofile = new GameProfile[Math.min(this.getCurrentPlayerCount(), 12)];
         int j = MathHelper.getInt((Random)this.random, 0, this.getCurrentPlayerCount() - agameprofile.length);

         for(int k = 0; k < agameprofile.length; ++k) {
            agameprofile[k] = ((EntityPlayerMP)this.playerList.getPlayerList().get(j + k)).getGameProfile();
         }

         Collections.shuffle(Arrays.asList(agameprofile));
         this.statusResponse.getPlayers().setPlayers(agameprofile);
      }

      if (this.tickCounter % 900 == 0) {
         this.theProfiler.startSection("save");
         this.playerList.saveAllPlayerData();
         this.saveAllWorlds(true);
         this.theProfiler.endSection();
      }

      this.theProfiler.startSection("tallying");
      this.tickTimeArray[this.tickCounter % 100] = System.nanoTime() - i;
      this.theProfiler.endSection();
      this.theProfiler.startSection("snooper");
      if (!this.usageSnooper.isSnooperRunning() && this.tickCounter > 100) {
         this.usageSnooper.startSnooper();
      }

      if (this.tickCounter % 6000 == 0) {
         this.usageSnooper.addMemoryStatsToSnooper();
      }

      this.theProfiler.endSection();
      this.theProfiler.endSection();
   }

   public void updateTimeLightAndEntities() {
      this.theProfiler.startSection("jobs");
      synchronized(this.futureTaskQueue) {
         while(!this.futureTaskQueue.isEmpty()) {
            Util.runTask((FutureTask)this.futureTaskQueue.poll(), LOG);
         }
      }

      this.theProfiler.endStartSection("levels");

      int k;
      for(k = 0; k < this.worldServers.length; ++k) {
         long i = System.nanoTime();
         if (k == 0 || this.getAllowNether()) {
            WorldServer worldserver = this.worldServers[k];
            this.theProfiler.func_194340_a(() -> {
               return worldserver.getWorldInfo().getWorldName();
            });
            if (this.tickCounter % 20 == 0) {
               this.theProfiler.startSection("timeSync");
               this.playerList.sendPacketToAllPlayersInDimension(new SPacketTimeUpdate(worldserver.getTotalWorldTime(), worldserver.getWorldTime(), worldserver.getGameRules().getBoolean("doDaylightCycle")), worldserver.provider.getDimensionType().getId());
               this.theProfiler.endSection();
            }

            this.theProfiler.startSection("tick");

            CrashReport crashreport1;
            try {
               worldserver.tick();
            } catch (Throwable var8) {
               crashreport1 = CrashReport.makeCrashReport(var8, "Exception ticking world");
               worldserver.addWorldInfoToCrashReport(crashreport1);
               throw new ReportedException(crashreport1);
            }

            try {
               worldserver.updateEntities();
            } catch (Throwable var7) {
               crashreport1 = CrashReport.makeCrashReport(var7, "Exception ticking world entities");
               worldserver.addWorldInfoToCrashReport(crashreport1);
               throw new ReportedException(crashreport1);
            }

            this.theProfiler.endSection();
            this.theProfiler.startSection("tracker");
            worldserver.getEntityTracker().updateTrackedEntities();
            this.theProfiler.endSection();
            this.theProfiler.endSection();
         }

         this.timeOfLastDimensionTick[k][this.tickCounter % 100] = System.nanoTime() - i;
      }

      this.theProfiler.endStartSection("connection");
      this.getNetworkSystem().networkTick();
      this.theProfiler.endStartSection("players");
      this.playerList.onTick();
      this.theProfiler.endStartSection("commandFunctions");
      this.func_193030_aL().update();
      this.theProfiler.endStartSection("tickables");

      for(k = 0; k < this.tickables.size(); ++k) {
         ((ITickable)this.tickables.get(k)).update();
      }

      this.theProfiler.endSection();
   }

   public boolean getAllowNether() {
      return true;
   }

   public void startServerThread() {
      this.serverThread = new Thread(this, "Server thread");
      this.serverThread.start();
   }

   public File getFile(String fileName) {
      return new File(this.getDataDirectory(), fileName);
   }

   public void logWarning(String msg) {
      LOG.warn(msg);
   }

   public WorldServer worldServerForDimension(int dimension) {
      if (dimension == -1) {
         return this.worldServers[1];
      } else {
         return dimension == 1 ? this.worldServers[2] : this.worldServers[0];
      }
   }

   public String getMinecraftVersion() {
      return "1.12.2";
   }

   public int getCurrentPlayerCount() {
      return this.playerList.getCurrentPlayerCount();
   }

   public int getMaxPlayers() {
      return this.playerList.getMaxPlayers();
   }

   public String[] getAllUsernames() {
      return this.playerList.getAllUsernames();
   }

   public GameProfile[] getGameProfiles() {
      return this.playerList.getAllProfiles();
   }

   public String getServerModName() {
      return "vanilla";
   }

   public CrashReport addServerInfoToCrashReport(CrashReport report) {
      report.getCategory().setDetail("Profiler Position", new ICrashReportDetail<String>() {
         public String call() throws Exception {
            return MinecraftServer.this.theProfiler.profilingEnabled ? MinecraftServer.this.theProfiler.getNameOfLastSection() : "N/A (disabled)";
         }
      });
      if (this.playerList != null) {
         report.getCategory().setDetail("Player Count", new ICrashReportDetail<String>() {
            public String call() {
               return MinecraftServer.this.playerList.getCurrentPlayerCount() + " / " + MinecraftServer.this.playerList.getMaxPlayers() + "; " + MinecraftServer.this.playerList.getPlayerList();
            }
         });
      }

      return report;
   }

   public List<String> getTabCompletions(ICommandSender sender, String input, @Nullable BlockPos pos, boolean hasTargetBlock) {
      List<String> list = Lists.newArrayList();
      boolean flag = input.startsWith("/");
      if (flag) {
         input = input.substring(1);
      }

      if (!flag && !hasTargetBlock) {
         String[] astring = input.split(" ", -1);
         String s2 = astring[astring.length - 1];
         String[] var15 = this.playerList.getAllUsernames();
         int var16 = var15.length;

         for(int var11 = 0; var11 < var16; ++var11) {
            String s1 = var15[var11];
            if (CommandBase.doesStringStartWith(s2, s1)) {
               list.add(s1);
            }
         }

         return list;
      } else {
         boolean flag1 = !input.contains(" ");
         List<String> list1 = this.commandManager.getTabCompletionOptions(sender, input, pos);
         if (!list1.isEmpty()) {
            Iterator var9 = list1.iterator();

            while(true) {
               while(var9.hasNext()) {
                  String s = (String)var9.next();
                  if (flag1 && !hasTargetBlock) {
                     list.add("/" + s);
                  } else {
                     list.add(s);
                  }
               }

               return list;
            }
         } else {
            return list;
         }
      }
   }

   public boolean isAnvilFileSet() {
      return this.anvilFile != null;
   }

   public String getName() {
      return "Server";
   }

   public void addChatMessage(ITextComponent component) {
      LOG.info(component.getUnformattedText());
   }

   public boolean canCommandSenderUseCommand(int permLevel, String commandName) {
      return true;
   }

   public ICommandManager getCommandManager() {
      return this.commandManager;
   }

   public KeyPair getKeyPair() {
      return this.serverKeyPair;
   }

   public String getServerOwner() {
      return this.serverOwner;
   }

   public void setServerOwner(String owner) {
      this.serverOwner = owner;
   }

   public boolean isSinglePlayer() {
      return this.serverOwner != null;
   }

   public String getFolderName() {
      return this.folderName;
   }

   public void setFolderName(String name) {
      this.folderName = name;
   }

   public void setWorldName(String worldNameIn) {
      this.worldName = worldNameIn;
   }

   public String getWorldName() {
      return this.worldName;
   }

   public void setKeyPair(KeyPair keyPair) {
      this.serverKeyPair = keyPair;
   }

   public void setDifficultyForAllWorlds(EnumDifficulty difficulty) {
      WorldServer[] var2 = this.worldServers;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         WorldServer worldserver1 = var2[var4];
         if (worldserver1 != null) {
            if (worldserver1.getWorldInfo().isHardcoreModeEnabled()) {
               worldserver1.getWorldInfo().setDifficulty(EnumDifficulty.HARD);
               worldserver1.setAllowedSpawnTypes(true, true);
            } else if (this.isSinglePlayer()) {
               worldserver1.getWorldInfo().setDifficulty(difficulty);
               worldserver1.setAllowedSpawnTypes(worldserver1.getDifficulty() != EnumDifficulty.PEACEFUL, true);
            } else {
               worldserver1.getWorldInfo().setDifficulty(difficulty);
               worldserver1.setAllowedSpawnTypes(this.allowSpawnMonsters(), this.canSpawnAnimals);
            }
         }
      }

   }

   public boolean allowSpawnMonsters() {
      return true;
   }

   public boolean isDemo() {
      return this.isDemo;
   }

   public void setDemo(boolean demo) {
      this.isDemo = demo;
   }

   public void canCreateBonusChest(boolean enable) {
      this.enableBonusChest = enable;
   }

   public ISaveFormat getActiveAnvilConverter() {
      return this.anvilConverterForAnvilFile;
   }

   public String getResourcePackUrl() {
      return this.resourcePackUrl;
   }

   public String getResourcePackHash() {
      return this.resourcePackHash;
   }

   public void setResourcePack(String url, String hash) {
      this.resourcePackUrl = url;
      this.resourcePackHash = hash;
   }

   public void addServerStatsToSnooper(Snooper playerSnooper) {
      playerSnooper.addClientStat("whitelist_enabled", false);
      playerSnooper.addClientStat("whitelist_count", 0);
      if (this.playerList != null) {
         playerSnooper.addClientStat("players_current", this.getCurrentPlayerCount());
         playerSnooper.addClientStat("players_max", this.getMaxPlayers());
         playerSnooper.addClientStat("players_seen", this.playerList.getAvailablePlayerDat().length);
      }

      playerSnooper.addClientStat("uses_auth", this.onlineMode);
      playerSnooper.addClientStat("gui_state", this.getGuiEnabled() ? "enabled" : "disabled");
      playerSnooper.addClientStat("run_time", (getCurrentTimeMillis() - playerSnooper.getMinecraftStartTimeMillis()) / 60L * 1000L);
      playerSnooper.addClientStat("avg_tick_ms", (int)(MathHelper.average(this.tickTimeArray) * 1.0E-6D));
      int l = 0;
      if (this.worldServers != null) {
         WorldServer[] var3 = this.worldServers;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            WorldServer worldserver1 = var3[var5];
            if (worldserver1 != null) {
               WorldInfo worldinfo = worldserver1.getWorldInfo();
               playerSnooper.addClientStat("world[" + l + "][dimension]", worldserver1.provider.getDimensionType().getId());
               playerSnooper.addClientStat("world[" + l + "][mode]", worldinfo.getGameType());
               playerSnooper.addClientStat("world[" + l + "][difficulty]", worldserver1.getDifficulty());
               playerSnooper.addClientStat("world[" + l + "][hardcore]", worldinfo.isHardcoreModeEnabled());
               playerSnooper.addClientStat("world[" + l + "][generator_name]", worldinfo.getTerrainType().getWorldTypeName());
               playerSnooper.addClientStat("world[" + l + "][generator_version]", worldinfo.getTerrainType().getGeneratorVersion());
               playerSnooper.addClientStat("world[" + l + "][height]", this.buildLimit);
               playerSnooper.addClientStat("world[" + l + "][chunks_loaded]", worldserver1.getChunkProvider().getLoadedChunkCount());
               ++l;
            }
         }
      }

      playerSnooper.addClientStat("worlds", l);
   }

   public void addServerTypeToSnooper(Snooper playerSnooper) {
      playerSnooper.addStatToSnooper("singleplayer", this.isSinglePlayer());
      playerSnooper.addStatToSnooper("server_brand", this.getServerModName());
      playerSnooper.addStatToSnooper("gui_supported", GraphicsEnvironment.isHeadless() ? "headless" : "supported");
      playerSnooper.addStatToSnooper("dedicated", this.isDedicatedServer());
   }

   public boolean isSnooperEnabled() {
      return true;
   }

   public abstract boolean isDedicatedServer();

   public boolean isServerInOnlineMode() {
      return this.onlineMode;
   }

   public void setOnlineMode(boolean online) {
      this.onlineMode = online;
   }

   public boolean func_190518_ac() {
      return this.field_190519_A;
   }

   public boolean getCanSpawnAnimals() {
      return this.canSpawnAnimals;
   }

   public void setCanSpawnAnimals(boolean spawnAnimals) {
      this.canSpawnAnimals = spawnAnimals;
   }

   public boolean getCanSpawnNPCs() {
      return this.canSpawnNPCs;
   }

   public abstract boolean shouldUseNativeTransport();

   public void setCanSpawnNPCs(boolean spawnNpcs) {
      this.canSpawnNPCs = spawnNpcs;
   }

   public boolean isPVPEnabled() {
      return this.pvpEnabled;
   }

   public void setAllowPvp(boolean allowPvp) {
      this.pvpEnabled = allowPvp;
   }

   public boolean isFlightAllowed() {
      return this.allowFlight;
   }

   public void setAllowFlight(boolean allow) {
      this.allowFlight = allow;
   }

   public abstract boolean isCommandBlockEnabled();

   public String getMOTD() {
      return this.motd;
   }

   public void setMOTD(String motdIn) {
      this.motd = motdIn;
   }

   public int getBuildLimit() {
      return this.buildLimit;
   }

   public void setBuildLimit(int maxBuildHeight) {
      this.buildLimit = maxBuildHeight;
   }

   public boolean isServerStopped() {
      return this.serverStopped;
   }

   public PlayerList getPlayerList() {
      return this.playerList;
   }

   public void setPlayerList(PlayerList list) {
      this.playerList = list;
   }

   public void setGameType(GameType gameMode) {
      WorldServer[] var2 = this.worldServers;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         WorldServer worldserver1 = var2[var4];
         worldserver1.getWorldInfo().setGameType(gameMode);
      }

   }

   public NetworkSystem getNetworkSystem() {
      return this.networkSystem;
   }

   public boolean serverIsInRunLoop() {
      return this.serverIsRunning;
   }

   public boolean getGuiEnabled() {
      return false;
   }

   public abstract String shareToLAN(GameType var1, boolean var2);

   public int getTickCounter() {
      return this.tickCounter;
   }

   public void enableProfiling() {
      this.startProfiling = true;
   }

   public Snooper getPlayerUsageSnooper() {
      return this.usageSnooper;
   }

   public World getEntityWorld() {
      return this.worldServers[0];
   }

   public boolean isBlockProtected(World worldIn, BlockPos pos, EntityPlayer playerIn) {
      return false;
   }

   public boolean getForceGamemode() {
      return this.isGamemodeForced;
   }

   public Proxy getServerProxy() {
      return this.serverProxy;
   }

   public static long getCurrentTimeMillis() {
      return System.currentTimeMillis();
   }

   public int getMaxPlayerIdleMinutes() {
      return this.maxPlayerIdleMinutes;
   }

   public void setPlayerIdleTimeout(int idleTimeout) {
      this.maxPlayerIdleMinutes = idleTimeout;
   }

   public MinecraftSessionService getMinecraftSessionService() {
      return this.sessionService;
   }

   public GameProfileRepository getGameProfileRepository() {
      return this.profileRepo;
   }

   public PlayerProfileCache getPlayerProfileCache() {
      return this.profileCache;
   }

   public ServerStatusResponse getServerStatusResponse() {
      return this.statusResponse;
   }

   public void refreshStatusNextTick() {
      this.nanoTimeSinceStatusRefresh = 0L;
   }

   @Nullable
   public Entity getEntityFromUuid(UUID uuid) {
      WorldServer[] var2 = this.worldServers;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         WorldServer worldserver1 = var2[var4];
         if (worldserver1 != null) {
            Entity entity = worldserver1.getEntityFromUuid(uuid);
            if (entity != null) {
               return entity;
            }
         }
      }

      return null;
   }

   public boolean sendCommandFeedback() {
      return this.worldServers[0].getGameRules().getBoolean("sendCommandFeedback");
   }

   public MinecraftServer getServer() {
      return this;
   }

   public int getMaxWorldSize() {
      return 29999984;
   }

   public <V> ListenableFuture<V> callFromMainThread(Callable<V> callable) {
      Validate.notNull(callable);
      if (!this.isCallingFromMinecraftThread() && !this.isServerStopped()) {
         ListenableFutureTask<V> listenablefuturetask = ListenableFutureTask.create(callable);
         synchronized(this.futureTaskQueue) {
            this.futureTaskQueue.add(listenablefuturetask);
            return listenablefuturetask;
         }
      } else {
         try {
            return Futures.immediateFuture(callable.call());
         } catch (Exception var6) {
            return Futures.immediateFailedCheckedFuture(var6);
         }
      }
   }

   public ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
      Validate.notNull(runnableToSchedule);
      return this.callFromMainThread(Executors.callable(runnableToSchedule));
   }

   public boolean isCallingFromMinecraftThread() {
      return Thread.currentThread() == this.serverThread;
   }

   public int getNetworkCompressionThreshold() {
      return 256;
   }

   public int getSpawnRadius(@Nullable WorldServer worldIn) {
      return worldIn != null ? worldIn.getGameRules().getInt("spawnRadius") : 10;
   }

   public AdvancementManager func_191949_aK() {
      return this.worldServers[0].func_191952_z();
   }

   public FunctionManager func_193030_aL() {
      return this.worldServers[0].func_193037_A();
   }

   public void func_193031_aM() {
      if (this.isCallingFromMinecraftThread()) {
         this.getPlayerList().saveAllPlayerData();
         this.worldServers[0].getLootTableManager().reloadLootTables();
         this.func_191949_aK().func_192779_a();
         this.func_193030_aL().func_193059_f();
         this.getPlayerList().func_193244_w();
      } else {
         this.addScheduledTask(this::func_193031_aM);
      }

   }
}
