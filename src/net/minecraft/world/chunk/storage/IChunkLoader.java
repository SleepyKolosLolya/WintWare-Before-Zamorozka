package net.minecraft.world.chunk.storage;

import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public interface IChunkLoader {
   @Nullable
   Chunk loadChunk(World var1, int var2, int var3) throws IOException;

   void saveChunk(World var1, Chunk var2) throws MinecraftException, IOException;

   void saveExtraChunkData(World var1, Chunk var2) throws IOException;

   void chunkTick();

   void saveExtraData();

   boolean func_191063_a(int var1, int var2);
}
