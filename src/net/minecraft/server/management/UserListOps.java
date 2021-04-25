package net.minecraft.server.management;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.io.File;
import java.util.Iterator;

public class UserListOps extends UserList<GameProfile, UserListOpsEntry> {
   public UserListOps(File saveFile) {
      super(saveFile);
   }

   protected UserListEntry<GameProfile> createEntry(JsonObject entryData) {
      return new UserListOpsEntry(entryData);
   }

   public String[] getKeys() {
      String[] astring = new String[this.getValues().size()];
      int i = 0;

      UserListOpsEntry userlistopsentry;
      for(Iterator var3 = this.getValues().values().iterator(); var3.hasNext(); astring[i++] = ((GameProfile)userlistopsentry.getValue()).getName()) {
         userlistopsentry = (UserListOpsEntry)var3.next();
      }

      return astring;
   }

   public int getPermissionLevel(GameProfile profile) {
      UserListOpsEntry userlistopsentry = (UserListOpsEntry)this.getEntry(profile);
      return userlistopsentry != null ? userlistopsentry.getPermissionLevel() : 0;
   }

   public boolean bypassesPlayerLimit(GameProfile profile) {
      UserListOpsEntry userlistopsentry = (UserListOpsEntry)this.getEntry(profile);
      return userlistopsentry != null ? userlistopsentry.bypassesPlayerLimit() : false;
   }

   protected String getObjectKey(GameProfile obj) {
      return obj.getId().toString();
   }

   public GameProfile getGameProfileFromName(String username) {
      Iterator var2 = this.getValues().values().iterator();

      UserListOpsEntry userlistopsentry;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         userlistopsentry = (UserListOpsEntry)var2.next();
      } while(!username.equalsIgnoreCase(((GameProfile)userlistopsentry.getValue()).getName()));

      return (GameProfile)userlistopsentry.getValue();
   }
}
