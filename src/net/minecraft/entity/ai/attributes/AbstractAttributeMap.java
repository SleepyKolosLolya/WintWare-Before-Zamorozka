package net.minecraft.entity.ai.attributes;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.util.LowerStringMap;

public abstract class AbstractAttributeMap {
   protected final Map<IAttribute, IAttributeInstance> attributes = Maps.newHashMap();
   protected final Map<String, IAttributeInstance> attributesByName = new LowerStringMap();
   protected final Multimap<IAttribute, IAttribute> descendantsByParent = HashMultimap.create();

   public IAttributeInstance getAttributeInstance(IAttribute attribute) {
      return (IAttributeInstance)this.attributes.get(attribute);
   }

   @Nullable
   public IAttributeInstance getAttributeInstanceByName(String attributeName) {
      return (IAttributeInstance)this.attributesByName.get(attributeName);
   }

   public IAttributeInstance registerAttribute(IAttribute attribute) {
      if (this.attributesByName.containsKey(attribute.getAttributeUnlocalizedName())) {
         throw new IllegalArgumentException("Attribute is already registered!");
      } else {
         IAttributeInstance iattributeinstance = this.createInstance(attribute);
         this.attributesByName.put(attribute.getAttributeUnlocalizedName(), iattributeinstance);
         this.attributes.put(attribute, iattributeinstance);

         for(IAttribute iattribute = attribute.getParent(); iattribute != null; iattribute = iattribute.getParent()) {
            this.descendantsByParent.put(iattribute, attribute);
         }

         return iattributeinstance;
      }
   }

   protected abstract IAttributeInstance createInstance(IAttribute var1);

   public Collection<IAttributeInstance> getAllAttributes() {
      return this.attributesByName.values();
   }

   public void onAttributeModified(IAttributeInstance instance) {
   }

   public void removeAttributeModifiers(Multimap<String, AttributeModifier> modifiers) {
      Iterator var2 = modifiers.entries().iterator();

      while(var2.hasNext()) {
         Entry<String, AttributeModifier> entry = (Entry)var2.next();
         IAttributeInstance iattributeinstance = this.getAttributeInstanceByName((String)entry.getKey());
         if (iattributeinstance != null) {
            iattributeinstance.removeModifier((AttributeModifier)entry.getValue());
         }
      }

   }

   public void applyAttributeModifiers(Multimap<String, AttributeModifier> modifiers) {
      Iterator var2 = modifiers.entries().iterator();

      while(var2.hasNext()) {
         Entry<String, AttributeModifier> entry = (Entry)var2.next();
         IAttributeInstance iattributeinstance = this.getAttributeInstanceByName((String)entry.getKey());
         if (iattributeinstance != null) {
            iattributeinstance.removeModifier((AttributeModifier)entry.getValue());
            iattributeinstance.applyModifier((AttributeModifier)entry.getValue());
         }
      }

   }
}
