package net.minecraft.server.management;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.network.play.server.SPacketEntityEffect;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketHeldItemChange;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.network.play.server.SPacketPlayerAbilities;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.network.play.server.SPacketServerDifficulty;
import net.minecraft.network.play.server.SPacketSetExperience;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.network.play.server.SPacketTeams;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.network.play.server.SPacketWorldBorder;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.stats.StatisticsManagerServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.border.IBorderListener;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.WorldInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PlayerList {
   public static final File FILE_PLAYERBANS = new File("banned-players.json");
   public static final File FILE_IPBANS = new File("banned-ips.json");
   public static final File FILE_OPS = new File("ops.json");
   public static final File FILE_WHITELIST = new File("whitelist.json");
   private static final Logger LOG = LogManager.getLogger();
   private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
   private final MinecraftServer mcServer;
   private final List<EntityPlayerMP> playerEntityList = Lists.newArrayList();
   private final Map<UUID, EntityPlayerMP> uuidToPlayerMap = Maps.newHashMap();
   private final UserListBans bannedPlayers;
   private final UserListIPBans bannedIPs;
   private final UserListOps ops;
   private final UserListWhitelist whiteListedPlayers;
   private final Map<UUID, StatisticsManagerServer> playerStatFiles;
   private final Map<UUID, PlayerAdvancements> field_192055_p;
   private IPlayerFileData playerNBTManagerObj;
   private boolean whiteListEnforced;
   protected int maxPlayers;
   private int viewDistance;
   private GameType gameType;
   private boolean commandsAllowedForAll;
   private int playerPingIndex;

   public PlayerList(MinecraftServer server) {
      this.bannedPlayers = new UserListBans(FILE_PLAYERBANS);
      this.bannedIPs = new UserListIPBans(FILE_IPBANS);
      this.ops = new UserListOps(FILE_OPS);
      this.whiteListedPlayers = new UserListWhitelist(FILE_WHITELIST);
      this.playerStatFiles = Maps.newHashMap();
      this.field_192055_p = Maps.newHashMap();
      this.mcServer = server;
      this.bannedPlayers.setLanServer(false);
      this.bannedIPs.setLanServer(false);
      this.maxPlayers = 8;
   }

   public void initializeConnectionToPlayer(NetworkManager netManager, EntityPlayerMP playerIn) {
      GameProfile gameprofile = playerIn.getGameProfile();
      PlayerProfileCache playerprofilecache = this.mcServer.getPlayerProfileCache();
      GameProfile gameprofile1 = playerprofilecache.getProfileByUUID(gameprofile.getId());
      String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();
      playerprofilecache.addEntry(gameprofile);
      NBTTagCompound nbttagcompound = this.readPlayerDataFromFile(playerIn);
      playerIn.setWorld(this.mcServer.worldServerForDimension(playerIn.dimension));
      playerIn.interactionManager.setWorld((WorldServer)playerIn.world);
      String s1 = "local";
      if (netManager.getRemoteAddress() != null) {
         s1 = netManager.getRemoteAddress().toString();
      }

      LOG.info("{}[{}] logged in with entity id {} at ({}, {}, {})", playerIn.getName(), s1, playerIn.getEntityId(), playerIn.posX, playerIn.posY, playerIn.posZ);
      WorldServer worldserver = this.mcServer.worldServerForDimension(playerIn.dimension);
      WorldInfo worldinfo = worldserver.getWorldInfo();
      this.setPlayerGameTypeBasedOnOther(playerIn, (EntityPlayerMP)null, worldserver);
      NetHandlerPlayServer nethandlerplayserver = new NetHandlerPlayServer(this.mcServer, netManager, playerIn);
      nethandlerplayserver.sendPacket(new SPacketJoinGame(playerIn.getEntityId(), playerIn.interactionManager.getGameType(), worldinfo.isHardcoreModeEnabled(), worldserver.provider.getDimensionType().getId(), worldserver.getDifficulty(), this.getMaxPlayers(), worldinfo.getTerrainType(), worldserver.getGameRules().getBoolean("reducedDebugInfo")));
      nethandlerplayserver.sendPacket(new SPacketCustomPayload("MC|Brand", (new PacketBuffer(Unpooled.buffer())).writeString(this.getServerInstance().getServerModName())));
      nethandlerplayserver.sendPacket(new SPacketServerDifficulty(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
      nethandlerplayserver.sendPacket(new SPacketPlayerAbilities(playerIn.capabilities));
      nethandlerplayserver.sendPacket(new SPacketHeldItemChange(playerIn.inventory.currentItem));
      this.updatePermissionLevel(playerIn);
      playerIn.getStatFile().markAllDirty();
      playerIn.func_192037_E().func_192826_c(playerIn);
      this.sendScoreboard((ServerScoreboard)worldserver.getScoreboard(), playerIn);
      this.mcServer.refreshStatusNextTick();
      TextComponentTranslation textcomponenttranslation;
      if (playerIn.getName().equalsIgnoreCase(s)) {
         textcomponenttranslation = new TextComponentTranslation("multiplayer.player.joined", new Object[]{playerIn.getDisplayName()});
      } else {
         textcomponenttranslation = new TextComponentTranslation("multiplayer.player.joined.renamed", new Object[]{playerIn.getDisplayName(), s});
      }

      textcomponenttranslation.getStyle().setColor(TextFormatting.YELLOW);
      this.sendChatMsg(textcomponenttranslation);
      this.playerLoggedIn(playerIn);
      nethandlerplayserver.setPlayerLocation(playerIn.posX, playerIn.posY, playerIn.posZ, playerIn.rotationYaw, playerIn.rotationPitch);
      this.updateTimeAndWeatherForPlayer(playerIn, worldserver);
      if (!this.mcServer.getResourcePackUrl().isEmpty()) {
         playerIn.loadResourcePack(this.mcServer.getResourcePackUrl(), this.mcServer.getResourcePackHash());
      }

      Iterator var13 = playerIn.getActivePotionEffects().iterator();

      while(var13.hasNext()) {
         PotionEffect potioneffect = (PotionEffect)var13.next();
         nethandlerplayserver.sendPacket(new SPacketEntityEffect(playerIn.getEntityId(), potioneffect));
      }

      if (nbttagcompound != null && nbttagcompound.hasKey("RootVehicle", 10)) {
         NBTTagCompound nbttagcompound1 = nbttagcompound.getCompoundTag("RootVehicle");
         Entity entity1 = AnvilChunkLoader.readWorldEntity(nbttagcompound1.getCompoundTag("Entity"), worldserver, true);
         if (entity1 != null) {
            UUID uuid = nbttagcompound1.getUniqueId("Attach");
            Iterator var16;
            Entity entity2;
            if (entity1.getUniqueID().equals(uuid)) {
               playerIn.startRiding(entity1, true);
            } else {
               var16 = entity1.getRecursivePassengers().iterator();

               while(var16.hasNext()) {
                  entity2 = (Entity)var16.next();
                  if (entity2.getUniqueID().equals(uuid)) {
                     playerIn.startRiding(entity2, true);
                     break;
                  }
               }
            }

            if (!playerIn.isRiding()) {
               LOG.warn("Couldn't reattach entity to player");
               worldserver.removeEntityDangerously(entity1);
               var16 = entity1.getRecursivePassengers().iterator();

               while(var16.hasNext()) {
                  entity2 = (Entity)var16.next();
                  worldserver.removeEntityDangerously(entity2);
               }
            }
         }
      }

      playerIn.addSelfToInternalCraftingInventory();
   }

   protected void sendScoreboard(ServerScoreboard scoreboardIn, EntityPlayerMP playerIn) {
      Set<ScoreObjective> set = Sets.newHashSet();
      Iterator var4 = scoreboardIn.getTeams().iterator();

      while(var4.hasNext()) {
         ScorePlayerTeam scoreplayerteam = (ScorePlayerTeam)var4.next();
         playerIn.connection.sendPacket(new SPacketTeams(scoreplayerteam, 0));
      }

      for(int i = 0; i < 19; ++i) {
         ScoreObjective scoreobjective = scoreboardIn.getObjectiveInDisplaySlot(i);
         if (scoreobjective != null && !set.contains(scoreobjective)) {
            Iterator var6 = scoreboardIn.getCreatePackets(scoreobjective).iterator();

            while(var6.hasNext()) {
               Packet<?> packet = (Packet)var6.next();
               playerIn.connection.sendPacket(packet);
            }

            set.add(scoreobjective);
         }
      }

   }

   public void setPlayerManager(WorldServer[] worldServers) {
      this.playerNBTManagerObj = worldServers[0].getSaveHandler().getPlayerNBTManager();
      worldServers[0].getWorldBorder().addListener(new IBorderListener() {
         public void onSizeChanged(WorldBorder border, double newSize) {
            PlayerList.this.sendPacketToAllPlayers(new SPacketWorldBorder(border, SPacketWorldBorder.Action.SET_SIZE));
         }

         public void onTransitionStarted(WorldBorder border, double oldSize, double newSize, long time) {
            PlayerList.this.sendPacketToAllPlayers(new SPacketWorldBorder(border, SPacketWorldBorder.Action.LERP_SIZE));
         }

         public void onCenterChanged(WorldBorder border, double x, double z) {
            PlayerList.this.sendPacketToAllPlayers(new SPacketWorldBorder(border, SPacketWorldBorder.Action.SET_CENTER));
         }

         public void onWarningTimeChanged(WorldBorder border, int newTime) {
            PlayerList.this.sendPacketToAllPlayers(new SPacketWorldBorder(border, SPacketWorldBorder.Action.SET_WARNING_TIME));
         }

         public void onWarningDistanceChanged(WorldBorder border, int newDistance) {
            PlayerList.this.sendPacketToAllPlayers(new SPacketWorldBorder(border, SPacketWorldBorder.Action.SET_WARNING_BLOCKS));
         }

         public void onDamageAmountChanged(WorldBorder border, double newAmount) {
         }

         public void onDamageBufferChanged(WorldBorder border, double newSize) {
         }
      });
   }

   public void preparePlayer(EntityPlayerMP playerIn, @Nullable WorldServer worldIn) {
      WorldServer worldserver = playerIn.getServerWorld();
      if (worldIn != null) {
         worldIn.getPlayerChunkMap().removePlayer(playerIn);
      }

      worldserver.getPlayerChunkMap().addPlayer(playerIn);
      worldserver.getChunkProvider().provideChunk((int)playerIn.posX >> 4, (int)playerIn.posZ >> 4);
      if (worldIn != null) {
         CriteriaTriggers.field_193134_u.func_193143_a(playerIn, worldIn.provider.getDimensionType(), worldserver.provider.getDimensionType());
         if (worldIn.provider.getDimensionType() == DimensionType.NETHER && playerIn.world.provider.getDimensionType() == DimensionType.OVERWORLD && playerIn.func_193106_Q() != null) {
            CriteriaTriggers.field_193131_B.func_193168_a(playerIn, playerIn.func_193106_Q());
         }
      }

   }

   public int getEntityViewDistance() {
      return PlayerChunkMap.getFurthestViewableBlock(this.getViewDistance());
   }

   @Nullable
   public NBTTagCompound readPlayerDataFromFile(EntityPlayerMP playerIn) {
      NBTTagCompound nbttagcompound = this.mcServer.worldServers[0].getWorldInfo().getPlayerNBTTagCompound();
      NBTTagCompound nbttagcompound1;
      if (playerIn.getName().equals(this.mcServer.getServerOwner()) && nbttagcompound != null) {
         nbttagcompound1 = nbttagcompound;
         playerIn.readFromNBT(nbttagcompound);
         LOG.debug("loading single player");
      } else {
         nbttagcompound1 = this.playerNBTManagerObj.readPlayerData(playerIn);
      }

      return nbttagcompound1;
   }

   protected void writePlayerData(EntityPlayerMP playerIn) {
      this.playerNBTManagerObj.writePlayerData(playerIn);
      StatisticsManagerServer statisticsmanagerserver = (StatisticsManagerServer)this.playerStatFiles.get(playerIn.getUniqueID());
      if (statisticsmanagerserver != null) {
         statisticsmanagerserver.saveStatFile();
      }

      PlayerAdvancements playeradvancements = (PlayerAdvancements)this.field_192055_p.get(playerIn.getUniqueID());
      if (playeradvancements != null) {
         playeradvancements.func_192749_b();
      }

   }

   public void playerLoggedIn(EntityPlayerMP playerIn) {
      this.playerEntityList.add(playerIn);
      this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
      this.sendPacketToAllPlayers(new SPacketPlayerListItem(SPacketPlayerListItem.Action.ADD_PLAYER, new EntityPlayerMP[]{playerIn}));
      WorldServer worldserver = this.mcServer.worldServerForDimension(playerIn.dimension);

      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         playerIn.connection.sendPacket(new SPacketPlayerListItem(SPacketPlayerListItem.Action.ADD_PLAYER, new EntityPlayerMP[]{(EntityPlayerMP)this.playerEntityList.get(i)}));
      }

      worldserver.spawnEntityInWorld(playerIn);
      this.preparePlayer(playerIn, (WorldServer)null);
   }

   public void serverUpdateMovingPlayer(EntityPlayerMP playerIn) {
      playerIn.getServerWorld().getPlayerChunkMap().updateMovingPlayer(playerIn);
   }

   public void playerLoggedOut(EntityPlayerMP playerIn) {
      WorldServer worldserver = playerIn.getServerWorld();
      playerIn.addStat(StatList.LEAVE_GAME);
      this.writePlayerData(playerIn);
      if (playerIn.isRiding()) {
         Entity entity = playerIn.getLowestRidingEntity();
         if (entity.getRecursivePassengersByType(EntityPlayerMP.class).size() == 1) {
            LOG.debug("Removing player mount");
            playerIn.dismountRidingEntity();
            worldserver.removeEntityDangerously(entity);
            Iterator var4 = entity.getRecursivePassengers().iterator();

            while(var4.hasNext()) {
               Entity entity1 = (Entity)var4.next();
               worldserver.removeEntityDangerously(entity1);
            }

            worldserver.getChunkFromChunkCoords(playerIn.chunkCoordX, playerIn.chunkCoordZ).setChunkModified();
         }
      }

      worldserver.removeEntity(playerIn);
      worldserver.getPlayerChunkMap().removePlayer(playerIn);
      playerIn.func_192039_O().func_192745_a();
      this.playerEntityList.remove(playerIn);
      UUID uuid = playerIn.getUniqueID();
      EntityPlayerMP entityplayermp = (EntityPlayerMP)this.uuidToPlayerMap.get(uuid);
      if (entityplayermp == playerIn) {
         this.uuidToPlayerMap.remove(uuid);
         this.playerStatFiles.remove(uuid);
         this.field_192055_p.remove(uuid);
      }

      this.sendPacketToAllPlayers(new SPacketPlayerListItem(SPacketPlayerListItem.Action.REMOVE_PLAYER, new EntityPlayerMP[]{playerIn}));
   }

   public String allowUserToConnect(SocketAddress address, GameProfile profile) {
      String s;
      if (this.bannedPlayers.isBanned(profile)) {
         UserListBansEntry userlistbansentry = (UserListBansEntry)this.bannedPlayers.getEntry(profile);
         s = "You are banned from this server!\nReason: " + userlistbansentry.getBanReason();
         if (userlistbansentry.getBanEndDate() != null) {
            s = s + "\nYour ban will be removed on " + DATE_FORMAT.format(userlistbansentry.getBanEndDate());
         }

         return s;
      } else if (!this.canJoin(profile)) {
         return "You are not white-listed on this server!";
      } else if (this.bannedIPs.isBanned(address)) {
         UserListIPBansEntry userlistipbansentry = this.bannedIPs.getBanEntry(address);
         s = "Your IP address is banned from this server!\nReason: " + userlistipbansentry.getBanReason();
         if (userlistipbansentry.getBanEndDate() != null) {
            s = s + "\nYour ban will be removed on " + DATE_FORMAT.format(userlistipbansentry.getBanEndDate());
         }

         return s;
      } else {
         return this.playerEntityList.size() >= this.maxPlayers && !this.bypassesPlayerLimit(profile) ? "The server is full!" : null;
      }
   }

   public EntityPlayerMP createPlayerForUser(GameProfile profile) {
      UUID uuid = EntityPlayer.getUUID(profile);
      List<EntityPlayerMP> list = Lists.newArrayList();

      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i);
         if (entityplayermp.getUniqueID().equals(uuid)) {
            list.add(entityplayermp);
         }
      }

      EntityPlayerMP entityplayermp2 = (EntityPlayerMP)this.uuidToPlayerMap.get(profile.getId());
      if (entityplayermp2 != null && !list.contains(entityplayermp2)) {
         list.add(entityplayermp2);
      }

      Iterator var8 = list.iterator();

      while(var8.hasNext()) {
         EntityPlayerMP entityplayermp1 = (EntityPlayerMP)var8.next();
         entityplayermp1.connection.func_194028_b(new TextComponentTranslation("multiplayer.disconnect.duplicate_login", new Object[0]));
      }

      Object playerinteractionmanager;
      if (this.mcServer.isDemo()) {
         playerinteractionmanager = new DemoPlayerInteractionManager(this.mcServer.worldServerForDimension(0));
      } else {
         playerinteractionmanager = new PlayerInteractionManager(this.mcServer.worldServerForDimension(0));
      }

      return new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(0), profile, (PlayerInteractionManager)playerinteractionmanager);
   }

   public EntityPlayerMP recreatePlayerEntity(EntityPlayerMP playerIn, int dimension, boolean conqueredEnd) {
      playerIn.getServerWorld().getEntityTracker().removePlayerFromTrackers(playerIn);
      playerIn.getServerWorld().getEntityTracker().untrackEntity(playerIn);
      playerIn.getServerWorld().getPlayerChunkMap().removePlayer(playerIn);
      this.playerEntityList.remove(playerIn);
      this.mcServer.worldServerForDimension(playerIn.dimension).removeEntityDangerously(playerIn);
      BlockPos blockpos = playerIn.getBedLocation();
      boolean flag = playerIn.isSpawnForced();
      playerIn.dimension = dimension;
      Object playerinteractionmanager;
      if (this.mcServer.isDemo()) {
         playerinteractionmanager = new DemoPlayerInteractionManager(this.mcServer.worldServerForDimension(playerIn.dimension));
      } else {
         playerinteractionmanager = new PlayerInteractionManager(this.mcServer.worldServerForDimension(playerIn.dimension));
      }

      EntityPlayerMP entityplayermp = new EntityPlayerMP(this.mcServer, this.mcServer.worldServerForDimension(playerIn.dimension), playerIn.getGameProfile(), (PlayerInteractionManager)playerinteractionmanager);
      entityplayermp.connection = playerIn.connection;
      entityplayermp.func_193104_a(playerIn, conqueredEnd);
      entityplayermp.setEntityId(playerIn.getEntityId());
      entityplayermp.setCommandStats(playerIn);
      entityplayermp.setPrimaryHand(playerIn.getPrimaryHand());
      Iterator var8 = playerIn.getTags().iterator();

      while(var8.hasNext()) {
         String s = (String)var8.next();
         entityplayermp.addTag(s);
      }

      WorldServer worldserver = this.mcServer.worldServerForDimension(playerIn.dimension);
      this.setPlayerGameTypeBasedOnOther(entityplayermp, playerIn, worldserver);
      BlockPos blockpos2;
      if (blockpos != null) {
         blockpos2 = EntityPlayer.getBedSpawnLocation(this.mcServer.worldServerForDimension(playerIn.dimension), blockpos, flag);
         if (blockpos2 != null) {
            entityplayermp.setLocationAndAngles((double)((float)blockpos2.getX() + 0.5F), (double)((float)blockpos2.getY() + 0.1F), (double)((float)blockpos2.getZ() + 0.5F), 0.0F, 0.0F);
            entityplayermp.setSpawnPoint(blockpos, flag);
         } else {
            entityplayermp.connection.sendPacket(new SPacketChangeGameState(0, 0.0F));
         }
      }

      worldserver.getChunkProvider().provideChunk((int)entityplayermp.posX >> 4, (int)entityplayermp.posZ >> 4);

      while(!worldserver.getCollisionBoxes(entityplayermp, entityplayermp.getEntityBoundingBox()).isEmpty() && entityplayermp.posY < 256.0D) {
         entityplayermp.setPosition(entityplayermp.posX, entityplayermp.posY + 1.0D, entityplayermp.posZ);
      }

      entityplayermp.connection.sendPacket(new SPacketRespawn(entityplayermp.dimension, entityplayermp.world.getDifficulty(), entityplayermp.world.getWorldInfo().getTerrainType(), entityplayermp.interactionManager.getGameType()));
      blockpos2 = worldserver.getSpawnPoint();
      entityplayermp.connection.setPlayerLocation(entityplayermp.posX, entityplayermp.posY, entityplayermp.posZ, entityplayermp.rotationYaw, entityplayermp.rotationPitch);
      entityplayermp.connection.sendPacket(new SPacketSpawnPosition(blockpos2));
      entityplayermp.connection.sendPacket(new SPacketSetExperience(entityplayermp.experience, entityplayermp.experienceTotal, entityplayermp.experienceLevel));
      this.updateTimeAndWeatherForPlayer(entityplayermp, worldserver);
      this.updatePermissionLevel(entityplayermp);
      worldserver.getPlayerChunkMap().addPlayer(entityplayermp);
      worldserver.spawnEntityInWorld(entityplayermp);
      this.playerEntityList.add(entityplayermp);
      this.uuidToPlayerMap.put(entityplayermp.getUniqueID(), entityplayermp);
      entityplayermp.addSelfToInternalCraftingInventory();
      entityplayermp.setHealth(entityplayermp.getHealth());
      return entityplayermp;
   }

   public void updatePermissionLevel(EntityPlayerMP player) {
      GameProfile gameprofile = player.getGameProfile();
      int i = this.canSendCommands(gameprofile) ? this.ops.getPermissionLevel(gameprofile) : 0;
      i = this.mcServer.isSinglePlayer() && this.mcServer.worldServers[0].getWorldInfo().areCommandsAllowed() ? 4 : i;
      i = this.commandsAllowedForAll ? 4 : i;
      this.sendPlayerPermissionLevel(player, i);
   }

   public void changePlayerDimension(EntityPlayerMP player, int dimensionIn) {
      int i = player.dimension;
      WorldServer worldserver = this.mcServer.worldServerForDimension(player.dimension);
      player.dimension = dimensionIn;
      WorldServer worldserver1 = this.mcServer.worldServerForDimension(player.dimension);
      player.connection.sendPacket(new SPacketRespawn(player.dimension, player.world.getDifficulty(), player.world.getWorldInfo().getTerrainType(), player.interactionManager.getGameType()));
      this.updatePermissionLevel(player);
      worldserver.removeEntityDangerously(player);
      player.isDead = false;
      this.transferEntityToWorld(player, i, worldserver, worldserver1);
      this.preparePlayer(player, worldserver);
      player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
      player.interactionManager.setWorld(worldserver1);
      player.connection.sendPacket(new SPacketPlayerAbilities(player.capabilities));
      this.updateTimeAndWeatherForPlayer(player, worldserver1);
      this.syncPlayerInventory(player);
      Iterator var6 = player.getActivePotionEffects().iterator();

      while(var6.hasNext()) {
         PotionEffect potioneffect = (PotionEffect)var6.next();
         player.connection.sendPacket(new SPacketEntityEffect(player.getEntityId(), potioneffect));
      }

   }

   public void transferEntityToWorld(Entity entityIn, int lastDimension, WorldServer oldWorldIn, WorldServer toWorldIn) {
      double d0 = entityIn.posX;
      double d1 = entityIn.posZ;
      double d2 = 8.0D;
      float f = entityIn.rotationYaw;
      oldWorldIn.theProfiler.startSection("moving");
      if (entityIn.dimension == -1) {
         d0 = MathHelper.clamp(d0 / 8.0D, toWorldIn.getWorldBorder().minX() + 16.0D, toWorldIn.getWorldBorder().maxX() - 16.0D);
         d1 = MathHelper.clamp(d1 / 8.0D, toWorldIn.getWorldBorder().minZ() + 16.0D, toWorldIn.getWorldBorder().maxZ() - 16.0D);
         entityIn.setLocationAndAngles(d0, entityIn.posY, d1, entityIn.rotationYaw, entityIn.rotationPitch);
         if (entityIn.isEntityAlive()) {
            oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
         }
      } else if (entityIn.dimension == 0) {
         d0 = MathHelper.clamp(d0 * 8.0D, toWorldIn.getWorldBorder().minX() + 16.0D, toWorldIn.getWorldBorder().maxX() - 16.0D);
         d1 = MathHelper.clamp(d1 * 8.0D, toWorldIn.getWorldBorder().minZ() + 16.0D, toWorldIn.getWorldBorder().maxZ() - 16.0D);
         entityIn.setLocationAndAngles(d0, entityIn.posY, d1, entityIn.rotationYaw, entityIn.rotationPitch);
         if (entityIn.isEntityAlive()) {
            oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
         }
      } else {
         BlockPos blockpos;
         if (lastDimension == 1) {
            blockpos = toWorldIn.getSpawnPoint();
         } else {
            blockpos = toWorldIn.getSpawnCoordinate();
         }

         d0 = (double)blockpos.getX();
         entityIn.posY = (double)blockpos.getY();
         d1 = (double)blockpos.getZ();
         entityIn.setLocationAndAngles(d0, entityIn.posY, d1, 90.0F, 0.0F);
         if (entityIn.isEntityAlive()) {
            oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
         }
      }

      oldWorldIn.theProfiler.endSection();
      if (lastDimension != 1) {
         oldWorldIn.theProfiler.startSection("placing");
         d0 = (double)MathHelper.clamp((int)d0, -29999872, 29999872);
         d1 = (double)MathHelper.clamp((int)d1, -29999872, 29999872);
         if (entityIn.isEntityAlive()) {
            entityIn.setLocationAndAngles(d0, entityIn.posY, d1, entityIn.rotationYaw, entityIn.rotationPitch);
            toWorldIn.getDefaultTeleporter().placeInPortal(entityIn, f);
            toWorldIn.spawnEntityInWorld(entityIn);
            toWorldIn.updateEntityWithOptionalForce(entityIn, false);
         }

         oldWorldIn.theProfiler.endSection();
      }

      entityIn.setWorld(toWorldIn);
   }

   public void onTick() {
      if (++this.playerPingIndex > 600) {
         this.sendPacketToAllPlayers(new SPacketPlayerListItem(SPacketPlayerListItem.Action.UPDATE_LATENCY, this.playerEntityList));
         this.playerPingIndex = 0;
      }

   }

   public void sendPacketToAllPlayers(Packet<?> packetIn) {
      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         ((EntityPlayerMP)this.playerEntityList.get(i)).connection.sendPacket(packetIn);
      }

   }

   public void sendPacketToAllPlayersInDimension(Packet<?> packetIn, int dimension) {
      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i);
         if (entityplayermp.dimension == dimension) {
            entityplayermp.connection.sendPacket(packetIn);
         }
      }

   }

   public void sendMessageToAllTeamMembers(EntityPlayer player, ITextComponent message) {
      Team team = player.getTeam();
      if (team != null) {
         Iterator var4 = team.getMembershipCollection().iterator();

         while(var4.hasNext()) {
            String s = (String)var4.next();
            EntityPlayerMP entityplayermp = this.getPlayerByUsername(s);
            if (entityplayermp != null && entityplayermp != player) {
               entityplayermp.addChatMessage(message);
            }
         }
      }

   }

   public void sendMessageToTeamOrAllPlayers(EntityPlayer player, ITextComponent message) {
      Team team = player.getTeam();
      if (team == null) {
         this.sendChatMsg(message);
      } else {
         for(int i = 0; i < this.playerEntityList.size(); ++i) {
            EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i);
            if (entityplayermp.getTeam() != team) {
               entityplayermp.addChatMessage(message);
            }
         }
      }

   }

   public String getFormattedListOfPlayers(boolean includeUUIDs) {
      String s = "";
      List<EntityPlayerMP> list = Lists.newArrayList(this.playerEntityList);

      for(int i = 0; i < list.size(); ++i) {
         if (i > 0) {
            s = s + ", ";
         }

         s = s + ((EntityPlayerMP)list.get(i)).getName();
         if (includeUUIDs) {
            s = s + " (" + ((EntityPlayerMP)list.get(i)).getCachedUniqueIdString() + ")";
         }
      }

      return s;
   }

   public String[] getAllUsernames() {
      String[] astring = new String[this.playerEntityList.size()];

      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         astring[i] = ((EntityPlayerMP)this.playerEntityList.get(i)).getName();
      }

      return astring;
   }

   public GameProfile[] getAllProfiles() {
      GameProfile[] agameprofile = new GameProfile[this.playerEntityList.size()];

      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         agameprofile[i] = ((EntityPlayerMP)this.playerEntityList.get(i)).getGameProfile();
      }

      return agameprofile;
   }

   public UserListBans getBannedPlayers() {
      return this.bannedPlayers;
   }

   public UserListIPBans getBannedIPs() {
      return this.bannedIPs;
   }

   public void addOp(GameProfile profile) {
      int i = this.mcServer.getOpPermissionLevel();
      this.ops.addEntry(new UserListOpsEntry(profile, this.mcServer.getOpPermissionLevel(), this.ops.bypassesPlayerLimit(profile)));
      this.sendPlayerPermissionLevel(this.getPlayerByUUID(profile.getId()), i);
   }

   public void removeOp(GameProfile profile) {
      this.ops.removeEntry(profile);
      this.sendPlayerPermissionLevel(this.getPlayerByUUID(profile.getId()), 0);
   }

   private void sendPlayerPermissionLevel(EntityPlayerMP player, int permLevel) {
      if (player != null && player.connection != null) {
         byte b0;
         if (permLevel <= 0) {
            b0 = 24;
         } else if (permLevel >= 4) {
            b0 = 28;
         } else {
            b0 = (byte)(24 + permLevel);
         }

         player.connection.sendPacket(new SPacketEntityStatus(player, b0));
      }

   }

   public boolean canJoin(GameProfile profile) {
      return !this.whiteListEnforced || this.ops.hasEntry(profile) || this.whiteListedPlayers.hasEntry(profile);
   }

   public boolean canSendCommands(GameProfile profile) {
      return this.ops.hasEntry(profile) || this.mcServer.isSinglePlayer() && this.mcServer.worldServers[0].getWorldInfo().areCommandsAllowed() && this.mcServer.getServerOwner().equalsIgnoreCase(profile.getName()) || this.commandsAllowedForAll;
   }

   @Nullable
   public EntityPlayerMP getPlayerByUsername(String username) {
      Iterator var2 = this.playerEntityList.iterator();

      EntityPlayerMP entityplayermp;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         entityplayermp = (EntityPlayerMP)var2.next();
      } while(!entityplayermp.getName().equalsIgnoreCase(username));

      return entityplayermp;
   }

   public void sendToAllNearExcept(@Nullable EntityPlayer except, double x, double y, double z, double radius, int dimension, Packet<?> packetIn) {
      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         EntityPlayerMP entityplayermp = (EntityPlayerMP)this.playerEntityList.get(i);
         if (entityplayermp != except && entityplayermp.dimension == dimension) {
            double d0 = x - entityplayermp.posX;
            double d1 = y - entityplayermp.posY;
            double d2 = z - entityplayermp.posZ;
            if (d0 * d0 + d1 * d1 + d2 * d2 < radius * radius) {
               entityplayermp.connection.sendPacket(packetIn);
            }
         }
      }

   }

   public void saveAllPlayerData() {
      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         this.writePlayerData((EntityPlayerMP)this.playerEntityList.get(i));
      }

   }

   public void addWhitelistedPlayer(GameProfile profile) {
      this.whiteListedPlayers.addEntry(new UserListWhitelistEntry(profile));
   }

   public void removePlayerFromWhitelist(GameProfile profile) {
      this.whiteListedPlayers.removeEntry(profile);
   }

   public UserListWhitelist getWhitelistedPlayers() {
      return this.whiteListedPlayers;
   }

   public String[] getWhitelistedPlayerNames() {
      return this.whiteListedPlayers.getKeys();
   }

   public UserListOps getOppedPlayers() {
      return this.ops;
   }

   public String[] getOppedPlayerNames() {
      return this.ops.getKeys();
   }

   public void reloadWhitelist() {
   }

   public void updateTimeAndWeatherForPlayer(EntityPlayerMP playerIn, WorldServer worldIn) {
      WorldBorder worldborder = this.mcServer.worldServers[0].getWorldBorder();
      playerIn.connection.sendPacket(new SPacketWorldBorder(worldborder, SPacketWorldBorder.Action.INITIALIZE));
      playerIn.connection.sendPacket(new SPacketTimeUpdate(worldIn.getTotalWorldTime(), worldIn.getWorldTime(), worldIn.getGameRules().getBoolean("doDaylightCycle")));
      BlockPos blockpos = worldIn.getSpawnPoint();
      playerIn.connection.sendPacket(new SPacketSpawnPosition(blockpos));
      if (worldIn.isRaining()) {
         playerIn.connection.sendPacket(new SPacketChangeGameState(1, 0.0F));
         playerIn.connection.sendPacket(new SPacketChangeGameState(7, worldIn.getRainStrength(1.0F)));
         playerIn.connection.sendPacket(new SPacketChangeGameState(8, worldIn.getThunderStrength(1.0F)));
      }

   }

   public void syncPlayerInventory(EntityPlayerMP playerIn) {
      playerIn.sendContainerToPlayer(playerIn.inventoryContainer);
      playerIn.setPlayerHealthUpdated();
      playerIn.connection.sendPacket(new SPacketHeldItemChange(playerIn.inventory.currentItem));
   }

   public int getCurrentPlayerCount() {
      return this.playerEntityList.size();
   }

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public String[] getAvailablePlayerDat() {
      return this.mcServer.worldServers[0].getSaveHandler().getPlayerNBTManager().getAvailablePlayerDat();
   }

   public void setWhiteListEnabled(boolean whitelistEnabled) {
      this.whiteListEnforced = whitelistEnabled;
   }

   public List<EntityPlayerMP> getPlayersMatchingAddress(String address) {
      List<EntityPlayerMP> list = Lists.newArrayList();
      Iterator var3 = this.playerEntityList.iterator();

      while(var3.hasNext()) {
         EntityPlayerMP entityplayermp = (EntityPlayerMP)var3.next();
         if (entityplayermp.getPlayerIP().equals(address)) {
            list.add(entityplayermp);
         }
      }

      return list;
   }

   public int getViewDistance() {
      return this.viewDistance;
   }

   public MinecraftServer getServerInstance() {
      return this.mcServer;
   }

   public NBTTagCompound getHostPlayerData() {
      return null;
   }

   public void setGameType(GameType gameModeIn) {
      this.gameType = gameModeIn;
   }

   private void setPlayerGameTypeBasedOnOther(EntityPlayerMP target, EntityPlayerMP source, World worldIn) {
      if (source != null) {
         target.interactionManager.setGameType(source.interactionManager.getGameType());
      } else if (this.gameType != null) {
         target.interactionManager.setGameType(this.gameType);
      }

      target.interactionManager.initializeGameType(worldIn.getWorldInfo().getGameType());
   }

   public void setCommandsAllowedForAll(boolean p_72387_1_) {
      this.commandsAllowedForAll = p_72387_1_;
   }

   public void removeAllPlayers() {
      for(int i = 0; i < this.playerEntityList.size(); ++i) {
         ((EntityPlayerMP)this.playerEntityList.get(i)).connection.func_194028_b(new TextComponentTranslation("multiplayer.disconnect.server_shutdown", new Object[0]));
      }

   }

   public void sendChatMsgImpl(ITextComponent component, boolean isSystem) {
      this.mcServer.addChatMessage(component);
      ChatType chattype = isSystem ? ChatType.SYSTEM : ChatType.CHAT;
      this.sendPacketToAllPlayers(new SPacketChat(component, chattype));
   }

   public void sendChatMsg(ITextComponent component) {
      this.sendChatMsgImpl(component, true);
   }

   public StatisticsManagerServer getPlayerStatsFile(EntityPlayer playerIn) {
      UUID uuid = playerIn.getUniqueID();
      StatisticsManagerServer statisticsmanagerserver = uuid == null ? null : (StatisticsManagerServer)this.playerStatFiles.get(uuid);
      if (statisticsmanagerserver == null) {
         File file1 = new File(this.mcServer.worldServerForDimension(0).getSaveHandler().getWorldDirectory(), "stats");
         File file2 = new File(file1, uuid + ".json");
         if (!file2.exists()) {
            File file3 = new File(file1, playerIn.getName() + ".json");
            if (file3.exists() && file3.isFile()) {
               file3.renameTo(file2);
            }
         }

         statisticsmanagerserver = new StatisticsManagerServer(this.mcServer, file2);
         statisticsmanagerserver.readStatFile();
         this.playerStatFiles.put(uuid, statisticsmanagerserver);
      }

      return statisticsmanagerserver;
   }

   public PlayerAdvancements func_192054_h(EntityPlayerMP p_192054_1_) {
      UUID uuid = p_192054_1_.getUniqueID();
      PlayerAdvancements playeradvancements = (PlayerAdvancements)this.field_192055_p.get(uuid);
      if (playeradvancements == null) {
         File file1 = new File(this.mcServer.worldServerForDimension(0).getSaveHandler().getWorldDirectory(), "advancements");
         File file2 = new File(file1, uuid + ".json");
         playeradvancements = new PlayerAdvancements(this.mcServer, file2, p_192054_1_);
         this.field_192055_p.put(uuid, playeradvancements);
      }

      playeradvancements.func_192739_a(p_192054_1_);
      return playeradvancements;
   }

   public void setViewDistance(int distance) {
      this.viewDistance = distance;
      if (this.mcServer.worldServers != null) {
         WorldServer[] var2 = this.mcServer.worldServers;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            WorldServer worldserver = var2[var4];
            if (worldserver != null) {
               worldserver.getPlayerChunkMap().setPlayerViewRadius(distance);
               worldserver.getEntityTracker().setViewDistance(distance);
            }
         }
      }

   }

   public List<EntityPlayerMP> getPlayerList() {
      return this.playerEntityList;
   }

   public EntityPlayerMP getPlayerByUUID(UUID playerUUID) {
      return (EntityPlayerMP)this.uuidToPlayerMap.get(playerUUID);
   }

   public boolean bypassesPlayerLimit(GameProfile profile) {
      return false;
   }

   public void func_193244_w() {
      Iterator var1 = this.field_192055_p.values().iterator();

      while(var1.hasNext()) {
         PlayerAdvancements playeradvancements = (PlayerAdvancements)var1.next();
         playeradvancements.func_193766_b();
      }

   }
}
