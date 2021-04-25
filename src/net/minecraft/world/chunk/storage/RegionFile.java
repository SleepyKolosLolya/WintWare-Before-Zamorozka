package net.minecraft.world.chunk.storage;

import com.google.common.collect.Lists;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;

public class RegionFile {
   private static final byte[] EMPTY_SECTOR = new byte[4096];
   private final File fileName;
   private RandomAccessFile dataFile;
   private final int[] offsets = new int[1024];
   private final int[] chunkTimestamps = new int[1024];
   private List<Boolean> sectorFree;
   private int sizeDelta;
   private long lastModified;

   public RegionFile(File fileNameIn) {
      this.fileName = fileNameIn;
      this.sizeDelta = 0;

      try {
         if (fileNameIn.exists()) {
            this.lastModified = fileNameIn.lastModified();
         }

         this.dataFile = new RandomAccessFile(fileNameIn, "rw");
         if (this.dataFile.length() < 4096L) {
            this.dataFile.write(EMPTY_SECTOR);
            this.dataFile.write(EMPTY_SECTOR);
            this.sizeDelta += 8192;
         }

         int i1;
         if ((this.dataFile.length() & 4095L) != 0L) {
            for(i1 = 0; (long)i1 < (this.dataFile.length() & 4095L); ++i1) {
               this.dataFile.write(0);
            }
         }

         i1 = (int)this.dataFile.length() / 4096;
         this.sectorFree = Lists.newArrayListWithCapacity(i1);

         int k1;
         for(k1 = 0; k1 < i1; ++k1) {
            this.sectorFree.add(true);
         }

         this.sectorFree.set(0, false);
         this.sectorFree.set(1, false);
         this.dataFile.seek(0L);

         int k;
         for(k1 = 0; k1 < 1024; ++k1) {
            k = this.dataFile.readInt();
            this.offsets[k1] = k;
            if (k != 0 && (k >> 8) + (k & 255) <= this.sectorFree.size()) {
               for(int l = 0; l < (k & 255); ++l) {
                  this.sectorFree.set((k >> 8) + l, false);
               }
            }
         }

         for(k1 = 0; k1 < 1024; ++k1) {
            k = this.dataFile.readInt();
            this.chunkTimestamps[k1] = k;
         }
      } catch (IOException var6) {
         var6.printStackTrace();
      }

   }

   @Nullable
   public synchronized DataInputStream getChunkDataInputStream(int x, int z) {
      if (this.outOfBounds(x, z)) {
         return null;
      } else {
         try {
            int i = this.getOffset(x, z);
            if (i == 0) {
               return null;
            } else {
               int j = i >> 8;
               int k = i & 255;
               if (j + k > this.sectorFree.size()) {
                  return null;
               } else {
                  this.dataFile.seek((long)(j * 4096));
                  int l = this.dataFile.readInt();
                  if (l > 4096 * k) {
                     return null;
                  } else if (l <= 0) {
                     return null;
                  } else {
                     byte b0 = this.dataFile.readByte();
                     byte[] abyte;
                     if (b0 == 1) {
                        abyte = new byte[l - 1];
                        this.dataFile.read(abyte);
                        return new DataInputStream(new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(abyte))));
                     } else if (b0 == 2) {
                        abyte = new byte[l - 1];
                        this.dataFile.read(abyte);
                        return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(abyte))));
                     } else {
                        return null;
                     }
                  }
               }
            }
         } catch (IOException var9) {
            return null;
         }
      }
   }

   @Nullable
   public DataOutputStream getChunkDataOutputStream(int x, int z) {
      return this.outOfBounds(x, z) ? null : new DataOutputStream(new BufferedOutputStream(new DeflaterOutputStream(new RegionFile.ChunkBuffer(x, z))));
   }

   protected synchronized void write(int x, int z, byte[] data, int length) {
      try {
         int i = this.getOffset(x, z);
         int j = i >> 8;
         int k = i & 255;
         int l = (length + 5) / 4096 + 1;
         if (l >= 256) {
            return;
         }

         if (j != 0 && k == l) {
            this.write(j, data, length);
         } else {
            int l1;
            for(l1 = 0; l1 < k; ++l1) {
               this.sectorFree.set(j + l1, true);
            }

            l1 = this.sectorFree.indexOf(true);
            int j1 = 0;
            int k1;
            if (l1 != -1) {
               for(k1 = l1; k1 < this.sectorFree.size(); ++k1) {
                  if (j1 != 0) {
                     if ((Boolean)this.sectorFree.get(k1)) {
                        ++j1;
                     } else {
                        j1 = 0;
                     }
                  } else if ((Boolean)this.sectorFree.get(k1)) {
                     l1 = k1;
                     j1 = 1;
                  }

                  if (j1 >= l) {
                     break;
                  }
               }
            }

            if (j1 >= l) {
               j = l1;
               this.setOffset(x, z, l1 << 8 | l);

               for(k1 = 0; k1 < l; ++k1) {
                  this.sectorFree.set(j + k1, false);
               }

               this.write(j, data, length);
            } else {
               this.dataFile.seek(this.dataFile.length());
               j = this.sectorFree.size();

               for(k1 = 0; k1 < l; ++k1) {
                  this.dataFile.write(EMPTY_SECTOR);
                  this.sectorFree.add(false);
               }

               this.sizeDelta += 4096 * l;
               this.write(j, data, length);
               this.setOffset(x, z, j << 8 | l);
            }
         }

         this.setChunkTimestamp(x, z, (int)(MinecraftServer.getCurrentTimeMillis() / 1000L));
      } catch (IOException var12) {
         var12.printStackTrace();
      }

   }

   private void write(int sectorNumber, byte[] data, int length) throws IOException {
      this.dataFile.seek((long)(sectorNumber * 4096));
      this.dataFile.writeInt(length + 1);
      this.dataFile.writeByte(2);
      this.dataFile.write(data, 0, length);
   }

   private boolean outOfBounds(int x, int z) {
      return x < 0 || x >= 32 || z < 0 || z >= 32;
   }

   private int getOffset(int x, int z) {
      return this.offsets[x + z * 32];
   }

   public boolean isChunkSaved(int x, int z) {
      return this.getOffset(x, z) != 0;
   }

   private void setOffset(int x, int z, int offset) throws IOException {
      this.offsets[x + z * 32] = offset;
      this.dataFile.seek((long)((x + z * 32) * 4));
      this.dataFile.writeInt(offset);
   }

   private void setChunkTimestamp(int x, int z, int timestamp) throws IOException {
      this.chunkTimestamps[x + z * 32] = timestamp;
      this.dataFile.seek((long)(4096 + (x + z * 32) * 4));
      this.dataFile.writeInt(timestamp);
   }

   public void close() throws IOException {
      if (this.dataFile != null) {
         this.dataFile.close();
      }

   }

   class ChunkBuffer extends ByteArrayOutputStream {
      private final int chunkX;
      private final int chunkZ;

      public ChunkBuffer(int x, int z) {
         super(8096);
         this.chunkX = x;
         this.chunkZ = z;
      }

      public void close() throws IOException {
         RegionFile.this.write(this.chunkX, this.chunkZ, this.buf, this.count);
      }
   }
}
