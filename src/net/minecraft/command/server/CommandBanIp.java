package net.minecraft.command.server;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.PlayerNotFoundException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListIPBansEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandBanIp extends CommandBase {
   public static final Pattern IP_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

   public String getCommandName() {
      return "ban-ip";
   }

   public int getRequiredPermissionLevel() {
      return 3;
   }

   public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
      return server.getPlayerList().getBannedIPs().isLanServer() && super.checkPermission(server, sender);
   }

   public String getCommandUsage(ICommandSender sender) {
      return "commands.banip.usage";
   }

   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length >= 1 && args[0].length() > 1) {
         ITextComponent itextcomponent = args.length >= 2 ? getChatComponentFromNthArg(sender, args, 1) : null;
         Matcher matcher = IP_PATTERN.matcher(args[0]);
         if (matcher.matches()) {
            this.banIp(server, sender, args[0], itextcomponent == null ? null : itextcomponent.getUnformattedText());
         } else {
            EntityPlayerMP entityplayermp = server.getPlayerList().getPlayerByUsername(args[0]);
            if (entityplayermp == null) {
               throw new PlayerNotFoundException("commands.banip.invalid");
            }

            this.banIp(server, sender, entityplayermp.getPlayerIP(), itextcomponent == null ? null : itextcomponent.getUnformattedText());
         }

      } else {
         throw new WrongUsageException("commands.banip.usage", new Object[0]);
      }
   }

   public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
      return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getAllUsernames()) : Collections.emptyList();
   }

   protected void banIp(MinecraftServer server, ICommandSender sender, String ipAddress, @Nullable String banReason) {
      UserListIPBansEntry userlistipbansentry = new UserListIPBansEntry(ipAddress, (Date)null, sender.getName(), (Date)null, banReason);
      server.getPlayerList().getBannedIPs().addEntry(userlistipbansentry);
      List<EntityPlayerMP> list = server.getPlayerList().getPlayersMatchingAddress(ipAddress);
      String[] astring = new String[list.size()];
      int i = 0;

      EntityPlayerMP entityplayermp;
      for(Iterator var9 = list.iterator(); var9.hasNext(); astring[i++] = entityplayermp.getName()) {
         entityplayermp = (EntityPlayerMP)var9.next();
         entityplayermp.connection.func_194028_b(new TextComponentTranslation("multiplayer.disconnect.ip_banned", new Object[0]));
      }

      if (list.isEmpty()) {
         notifyCommandListener(sender, this, "commands.banip.success", new Object[]{ipAddress});
      } else {
         notifyCommandListener(sender, this, "commands.banip.success.players", new Object[]{ipAddress, joinNiceString(astring)});
      }

   }
}
