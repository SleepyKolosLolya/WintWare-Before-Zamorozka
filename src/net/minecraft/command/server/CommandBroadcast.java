package net.minecraft.command.server;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandBroadcast extends CommandBase {
   public String getCommandName() {
      return "say";
   }

   public int getRequiredPermissionLevel() {
      return 1;
   }

   public String getCommandUsage(ICommandSender sender) {
      return "commands.say.usage";
   }

   public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
      if (args.length > 0 && args[0].length() > 0) {
         ITextComponent itextcomponent = getChatComponentFromNthArg(sender, args, 0, true);
         server.getPlayerList().sendChatMsg(new TextComponentTranslation("chat.type.announcement", new Object[]{sender.getDisplayName(), itextcomponent}));
      } else {
         throw new WrongUsageException("commands.say.usage", new Object[0]);
      }
   }

   public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
      return args.length >= 1 ? getListOfStringsMatchingLastWord(args, server.getAllUsernames()) : Collections.emptyList();
   }
}
