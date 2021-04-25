package net.minecraft.util;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public enum EnumFacing implements IStringSerializable {
   DOWN(0, 1, -1, "down", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.Y, new Vec3i(0, -1, 0)),
   UP(1, 0, -1, "up", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.Y, new Vec3i(0, 1, 0)),
   NORTH(2, 3, 2, "north", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.Z, new Vec3i(0, 0, -1)),
   SOUTH(3, 2, 0, "south", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.Z, new Vec3i(0, 0, 1)),
   WEST(4, 5, 1, "west", EnumFacing.AxisDirection.NEGATIVE, EnumFacing.Axis.X, new Vec3i(-1, 0, 0)),
   EAST(5, 4, 3, "east", EnumFacing.AxisDirection.POSITIVE, EnumFacing.Axis.X, new Vec3i(1, 0, 0));

   private final int index;
   private final int opposite;
   private final int horizontalIndex;
   private final String name;
   private final EnumFacing.Axis axis;
   private final EnumFacing.AxisDirection axisDirection;
   private final Vec3i directionVec;
   public static final EnumFacing[] VALUES = new EnumFacing[6];
   private static final EnumFacing[] HORIZONTALS = new EnumFacing[4];
   private static final Map<String, EnumFacing> NAME_LOOKUP = Maps.newHashMap();

   private EnumFacing(int indexIn, int oppositeIn, int horizontalIndexIn, String nameIn, EnumFacing.AxisDirection axisDirectionIn, EnumFacing.Axis axisIn, Vec3i directionVecIn) {
      this.index = indexIn;
      this.horizontalIndex = horizontalIndexIn;
      this.opposite = oppositeIn;
      this.name = nameIn;
      this.axis = axisIn;
      this.axisDirection = axisDirectionIn;
      this.directionVec = directionVecIn;
   }

   public int getIndex() {
      return this.index;
   }

   public int getHorizontalIndex() {
      return this.horizontalIndex;
   }

   public EnumFacing.AxisDirection getAxisDirection() {
      return this.axisDirection;
   }

   public EnumFacing getOpposite() {
      return VALUES[this.opposite];
   }

   public EnumFacing rotateAround(EnumFacing.Axis axis) {
      switch(axis) {
      case X:
         if (this != WEST && this != EAST) {
            return this.rotateX();
         }

         return this;
      case Y:
         if (this != UP && this != DOWN) {
            return this.rotateY();
         }

         return this;
      case Z:
         if (this != NORTH && this != SOUTH) {
            return this.rotateZ();
         }

         return this;
      default:
         throw new IllegalStateException("Unable to get CW facing for axis " + axis);
      }
   }

   public EnumFacing rotateY() {
      switch(this) {
      case NORTH:
         return EAST;
      case EAST:
         return SOUTH;
      case SOUTH:
         return WEST;
      case WEST:
         return NORTH;
      default:
         throw new IllegalStateException("Unable to get Y-rotated facing of " + this);
      }
   }

   private EnumFacing rotateX() {
      switch(this) {
      case NORTH:
         return DOWN;
      case EAST:
      case WEST:
      default:
         throw new IllegalStateException("Unable to get X-rotated facing of " + this);
      case SOUTH:
         return UP;
      case UP:
         return NORTH;
      case DOWN:
         return SOUTH;
      }
   }

   private EnumFacing rotateZ() {
      switch(this) {
      case EAST:
         return DOWN;
      case SOUTH:
      default:
         throw new IllegalStateException("Unable to get Z-rotated facing of " + this);
      case WEST:
         return UP;
      case UP:
         return EAST;
      case DOWN:
         return WEST;
      }
   }

   public EnumFacing rotateYCCW() {
      switch(this) {
      case NORTH:
         return WEST;
      case EAST:
         return NORTH;
      case SOUTH:
         return EAST;
      case WEST:
         return SOUTH;
      default:
         throw new IllegalStateException("Unable to get CCW facing of " + this);
      }
   }

   public int getFrontOffsetX() {
      return this.axis == EnumFacing.Axis.X ? this.axisDirection.getOffset() : 0;
   }

   public int getFrontOffsetY() {
      return this.axis == EnumFacing.Axis.Y ? this.axisDirection.getOffset() : 0;
   }

   public int getFrontOffsetZ() {
      return this.axis == EnumFacing.Axis.Z ? this.axisDirection.getOffset() : 0;
   }

   public String getName2() {
      return this.name;
   }

   public EnumFacing.Axis getAxis() {
      return this.axis;
   }

   @Nullable
   public static EnumFacing byName(String name) {
      return name == null ? null : (EnumFacing)NAME_LOOKUP.get(name.toLowerCase(Locale.ROOT));
   }

   public static EnumFacing getFront(int index) {
      return VALUES[MathHelper.abs(index % VALUES.length)];
   }

   public static EnumFacing getHorizontal(int horizontalIndexIn) {
      return HORIZONTALS[MathHelper.abs(horizontalIndexIn % HORIZONTALS.length)];
   }

   public static EnumFacing fromAngle(double angle) {
      return getHorizontal(MathHelper.floor(angle / 90.0D + 0.5D) & 3);
   }

   public float getHorizontalAngle() {
      return (float)((this.horizontalIndex & 3) * 90);
   }

   public static EnumFacing random(Random rand) {
      return values()[rand.nextInt(values().length)];
   }

   public static EnumFacing getFacingFromVector(float x, float y, float z) {
      EnumFacing enumfacing = NORTH;
      float f = Float.MIN_VALUE;
      EnumFacing[] var5 = values();
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         EnumFacing enumfacing1 = var5[var7];
         float f1 = x * (float)enumfacing1.directionVec.getX() + y * (float)enumfacing1.directionVec.getY() + z * (float)enumfacing1.directionVec.getZ();
         if (f1 > f) {
            f = f1;
            enumfacing = enumfacing1;
         }
      }

      return enumfacing;
   }

   public String toString() {
      return this.name;
   }

   public String getName() {
      return this.name;
   }

   public static EnumFacing getFacingFromAxis(EnumFacing.AxisDirection axisDirectionIn, EnumFacing.Axis axisIn) {
      EnumFacing[] var2 = values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         EnumFacing enumfacing = var2[var4];
         if (enumfacing.getAxisDirection() == axisDirectionIn && enumfacing.getAxis() == axisIn) {
            return enumfacing;
         }
      }

      throw new IllegalArgumentException("No such direction: " + axisDirectionIn + " " + axisIn);
   }

   public static EnumFacing func_190914_a(BlockPos p_190914_0_, EntityLivingBase p_190914_1_) {
      if (Math.abs(p_190914_1_.posX - (double)((float)p_190914_0_.getX() + 0.5F)) < 2.0D && Math.abs(p_190914_1_.posZ - (double)((float)p_190914_0_.getZ() + 0.5F)) < 2.0D) {
         double d0 = p_190914_1_.posY + (double)p_190914_1_.getEyeHeight();
         if (d0 - (double)p_190914_0_.getY() > 2.0D) {
            return UP;
         }

         if ((double)p_190914_0_.getY() - d0 > 0.0D) {
            return DOWN;
         }
      }

      return p_190914_1_.getHorizontalFacing().getOpposite();
   }

   public Vec3i getDirectionVec() {
      return this.directionVec;
   }

   static {
      EnumFacing[] var0 = values();
      int var1 = var0.length;

      for(int var2 = 0; var2 < var1; ++var2) {
         EnumFacing enumfacing = var0[var2];
         VALUES[enumfacing.index] = enumfacing;
         if (enumfacing.getAxis().isHorizontal()) {
            HORIZONTALS[enumfacing.horizontalIndex] = enumfacing;
         }

         NAME_LOOKUP.put(enumfacing.getName2().toLowerCase(Locale.ROOT), enumfacing);
      }

   }

   public static enum Plane implements Predicate<EnumFacing>, Iterable<EnumFacing> {
      HORIZONTAL,
      VERTICAL;

      public EnumFacing[] facings() {
         switch(this) {
         case HORIZONTAL:
            return new EnumFacing[]{EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST};
         case VERTICAL:
            return new EnumFacing[]{EnumFacing.UP, EnumFacing.DOWN};
         default:
            throw new Error("Someone's been tampering with the universe!");
         }
      }

      public EnumFacing random(Random rand) {
         EnumFacing[] aenumfacing = this.facings();
         return aenumfacing[rand.nextInt(aenumfacing.length)];
      }

      public boolean apply(@Nullable EnumFacing p_apply_1_) {
         return p_apply_1_ != null && p_apply_1_.getAxis().getPlane() == this;
      }

      public Iterator<EnumFacing> iterator() {
         return Iterators.forArray(this.facings());
      }
   }

   public static enum AxisDirection {
      POSITIVE(1, "Towards positive"),
      NEGATIVE(-1, "Towards negative");

      private final int offset;
      private final String description;

      private AxisDirection(int offset, String description) {
         this.offset = offset;
         this.description = description;
      }

      public int getOffset() {
         return this.offset;
      }

      public String toString() {
         return this.description;
      }
   }

   public static enum Axis implements Predicate<EnumFacing>, IStringSerializable {
      X("x", EnumFacing.Plane.HORIZONTAL),
      Y("y", EnumFacing.Plane.VERTICAL),
      Z("z", EnumFacing.Plane.HORIZONTAL);

      private static final Map<String, EnumFacing.Axis> NAME_LOOKUP = Maps.newHashMap();
      private final String name;
      private final EnumFacing.Plane plane;

      private Axis(String name, EnumFacing.Plane plane) {
         this.name = name;
         this.plane = plane;
      }

      @Nullable
      public static EnumFacing.Axis byName(String name) {
         return name == null ? null : (EnumFacing.Axis)NAME_LOOKUP.get(name.toLowerCase(Locale.ROOT));
      }

      public String getName2() {
         return this.name;
      }

      public boolean isVertical() {
         return this.plane == EnumFacing.Plane.VERTICAL;
      }

      public boolean isHorizontal() {
         return this.plane == EnumFacing.Plane.HORIZONTAL;
      }

      public String toString() {
         return this.name;
      }

      public boolean apply(@Nullable EnumFacing p_apply_1_) {
         return p_apply_1_ != null && p_apply_1_.getAxis() == this;
      }

      public EnumFacing.Plane getPlane() {
         return this.plane;
      }

      public String getName() {
         return this.name;
      }

      static {
         EnumFacing.Axis[] var0 = values();
         int var1 = var0.length;

         for(int var2 = 0; var2 < var1; ++var2) {
            EnumFacing.Axis enumfacing$axis = var0[var2];
            NAME_LOOKUP.put(enumfacing$axis.getName2().toLowerCase(Locale.ROOT), enumfacing$axis);
         }

      }
   }
}
