package net.minecraft.client.resources.data;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.resources.Language;
import net.minecraft.util.JsonUtils;

public class LanguageMetadataSectionSerializer extends BaseMetadataSectionSerializer<LanguageMetadataSection> {
   public LanguageMetadataSection deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
      JsonObject jsonobject = p_deserialize_1_.getAsJsonObject();
      Set<Language> set = Sets.newHashSet();
      Iterator var6 = jsonobject.entrySet().iterator();

      String s;
      String s1;
      String s2;
      boolean flag;
      do {
         if (!var6.hasNext()) {
            return new LanguageMetadataSection(set);
         }

         Entry<String, JsonElement> entry = (Entry)var6.next();
         s = (String)entry.getKey();
         if (s.length() > 16) {
            throw new JsonParseException("Invalid language->'" + s + "': language code must not be more than " + 16 + " characters long");
         }

         JsonObject jsonobject1 = JsonUtils.getJsonObject((JsonElement)entry.getValue(), "language");
         s1 = JsonUtils.getString(jsonobject1, "region");
         s2 = JsonUtils.getString(jsonobject1, "name");
         flag = JsonUtils.getBoolean(jsonobject1, "bidirectional", false);
         if (s1.isEmpty()) {
            throw new JsonParseException("Invalid language->'" + s + "'->region: empty value");
         }

         if (s2.isEmpty()) {
            throw new JsonParseException("Invalid language->'" + s + "'->name: empty value");
         }
      } while(set.add(new Language(s, s1, s2, flag)));

      throw new JsonParseException("Duplicate language->'" + s + "' defined");
   }

   public String getSectionName() {
      return "language";
   }
}
