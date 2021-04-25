package javax.vecmath;

import java.awt.Color;
import java.io.Serializable;

public class Color4f extends Tuple4f implements Serializable {
   static final long serialVersionUID = 8577680141580006740L;

   public Color4f(float x, float y, float z, float w) {
      super(x, y, z, w);
   }

   public Color4f(float[] c) {
      super(c);
   }

   public Color4f(Color4f c1) {
      super((Tuple4f)c1);
   }

   public Color4f(Tuple4f t1) {
      super(t1);
   }

   public Color4f(Tuple4d t1) {
      super(t1);
   }

   public Color4f(Color color) {
      super((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
   }

   public Color4f() {
   }

   public final void set(Color color) {
      this.x = (float)color.getRed() / 255.0F;
      this.y = (float)color.getGreen() / 255.0F;
      this.z = (float)color.getBlue() / 255.0F;
      this.w = (float)color.getAlpha() / 255.0F;
   }

   public final Color get() {
      int r = Math.round(this.x * 255.0F);
      int g = Math.round(this.y * 255.0F);
      int b = Math.round(this.z * 255.0F);
      int a = Math.round(this.w * 255.0F);
      return new Color(r, g, b, a);
   }
}
