package net.minecraft.world.chunk.storage;

import java.io.File;
import javax.annotation.Nullable;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraft.world.storage.WorldInfo;

public class AnvilSaveHandler extends SaveHandler {
   public AnvilSaveHandler(File p_i46650_1_, String p_i46650_2_, boolean p_i46650_3_, DataFixer dataFixerIn) {
      super(p_i46650_1_, p_i46650_2_, p_i46650_3_, dataFixerIn);
   }

   public IChunkLoader getChunkLoader(WorldProvider provider) {
      File file1 = this.getWorldDirectory();
      File file2;
      if (provider instanceof WorldProviderHell) {
         file2 = new File(file1, "DIM-1");
         file2.mkdirs();
         return new AnvilChunkLoader(file2, this.dataFixer);
      } else if (provider instanceof WorldProviderEnd) {
         file2 = new File(file1, "DIM1");
         file2.mkdirs();
         return new AnvilChunkLoader(file2, this.dataFixer);
      } else {
         return new AnvilChunkLoader(file1, this.dataFixer);
      }
   }

   public void saveWorldInfoWithPlayer(WorldInfo worldInformation, @Nullable NBTTagCompound tagCompound) {
      worldInformation.setSaveVersion(19133);
      super.saveWorldInfoWithPlayer(worldInformation, tagCompound);
   }

   public void flush() {
      try {
         ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
      } catch (InterruptedException var2) {
         var2.printStackTrace();
      }

      RegionFileCache.clearRegionFileReferences();
   }
}
