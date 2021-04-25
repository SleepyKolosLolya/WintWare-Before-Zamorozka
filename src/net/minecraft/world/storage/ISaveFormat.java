package net.minecraft.world.storage;

import java.io.File;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.AnvilConverterException;
import net.minecraft.util.IProgressUpdate;

public interface ISaveFormat {
   String getName();

   ISaveHandler getSaveLoader(String var1, boolean var2);

   List<WorldSummary> getSaveList() throws AnvilConverterException;

   void flushCache();

   @Nullable
   WorldInfo getWorldInfo(String var1);

   boolean isNewLevelIdAcceptable(String var1);

   boolean deleteWorldDirectory(String var1);

   void renameWorld(String var1, String var2);

   boolean isConvertible(String var1);

   boolean isOldMapFormat(String var1);

   boolean convertMapFormat(String var1, IProgressUpdate var2);

   boolean canLoadWorld(String var1);

   File getFile(String var1, String var2);
}
