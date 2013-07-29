/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.ui.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.forge.addon.convert.ConverterFactory;
import org.jboss.forge.addon.environment.Environment;
import org.jboss.forge.addon.ui.InputComponentFactory;
import org.jboss.forge.addon.ui.facets.HintsFacet;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.impl.facets.HintsFacetImpl;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.SelectComponent;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UIInputMany;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.util.InputComponents;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.services.Exported;
import org.jboss.forge.furnace.services.ExportedInstance;
import org.jboss.forge.furnace.util.Annotations;

/**
 * Produces UIInput objects
 * 
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 * 
 */
public class InputComponentProducer implements InputComponentFactory
{
   private Environment environment;
   private AddonRegistry addonRegistry;

   @Inject
   public InputComponentProducer(Environment environment, AddonRegistry addonRegistry)
   {
      this.environment = environment;
      this.addonRegistry = addonRegistry;
   }

   @Produces
   @SuppressWarnings("unchecked")
   public <T> UISelectOne<T> produceSelectOne(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         UISelectOne<T> input = createSelectOne(name, valueType);
         preconfigureInput(input, withAttributes);
         return input;
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UISelectOne.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UISelectMany<T> produceSelectMany(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         UISelectMany<T> input = createSelectMany(name, valueType);
         preconfigureInput(input, withAttributes);
         return input;
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UISelectMany.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UIInput<T> produceInput(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         UIInput<T> input = createInput(name, valueType);
         preconfigureInput(input, withAttributes);
         return input;
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UIInput.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Produces
   @SuppressWarnings({ "unchecked" })
   public <T> UIInputMany<T> produceInputMany(InjectionPoint injectionPoint)
   {
      String name = injectionPoint.getMember().getName();
      Type type = injectionPoint.getAnnotated().getBaseType();

      if (type instanceof ParameterizedType)
      {
         ParameterizedType parameterizedType = (ParameterizedType) type;

         Type[] typeArguments = parameterizedType.getActualTypeArguments();
         Class<T> valueType = (Class<T>) typeArguments[0];
         WithAttributes withAttributes = injectionPoint.getAnnotated().getAnnotation(WithAttributes.class);
         UIInputMany<T> input = createInputMany(name, valueType);
         preconfigureInput(input, withAttributes);
         return input;
      }
      else
      {
         throw new IllegalStateException("Cannot inject a generic instance of type " + UIInputMany.class.getName()
                  + "<?,?> without specifying concrete generic types at injection point " + injectionPoint + ".");
      }
   }

   @Override
   public <T> UIInput<T> createInput(String name, Class<T> valueType)
   {
      UIInputImpl<T> input = new UIInputImpl<T>(name, valueType);
      configureRequiredFacets(input);
      return input;
   }

   @Override
   public <T> UIInputMany<T> createInputMany(String name, Class<T> valueType)
   {
      UIInputManyImpl<T> input = new UIInputManyImpl<T>(name, valueType);
      configureRequiredFacets(input);
      return input;
   }

   @Override
   public <T> UISelectOne<T> createSelectOne(String name, Class<T> valueType)
   {
      UISelectOneImpl<T> input = new UISelectOneImpl<T>(name, valueType);
      configureRequiredFacets(input);
      return input;
   }

   @Override
   public <T> UISelectMany<T> createSelectMany(String name, Class<T> valueType)
   {
      UISelectManyImpl<T> input = new UISelectManyImpl<T>(name, valueType);
      configureRequiredFacets(input);
      return input;
   }

   /**
    * Pre-configure input based on WithAttributes info if annotation exists
    */
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void preconfigureInput(InputComponent<?, ?> input, WithAttributes atts)
   {
      if (atts != null)
      {
         input.setEnabled(atts.enabled());
         input.setLabel(atts.label());
         input.setRequired(atts.required());
         input.setRequiredMessage(atts.requiredMessage());
         input.setDescription(atts.description());

         // Set input type
         if (atts.type() != InputType.DEFAULT)
         {
            input.getFacet(HintsFacet.class).setInputType(atts.type());
         }

         // Set Default Value
         if (!"".equals(atts.defaultValue()))
         {
            ExportedInstance<ConverterFactory> exportedInstance = addonRegistry
                     .getExportedInstance(ConverterFactory.class);
            ConverterFactory converterFactory = exportedInstance.get();
            try
            {
               InputComponents.setDefaultValueFor(converterFactory, (InputComponent<?, Object>) input,
                        atts.defaultValue());
            }
            finally
            {
               exportedInstance.release(converterFactory);
            }

         }
      }

      if (input instanceof SelectComponent)
      {
         SelectComponent selectComponent = (SelectComponent) input;
         Class<?> valueType = input.getValueType();
         Iterable<?> choices = null;
         // Auto-populate Enums on SelectComponents
         if (valueType.isEnum())
         {
            Class<? extends Enum> enumClass = valueType.asSubclass(Enum.class);
            choices = EnumSet.allOf(enumClass);
         }
         // Auto-populate Exported values on SelectComponents
         else if (Annotations.isAnnotationPresent(valueType, Exported.class))
         {
            List<Object> choiceList = new ArrayList<Object>();
            for (ExportedInstance exportedInstance : addonRegistry.getExportedInstances(valueType))
            {
               Object instance = exportedInstance.get();
               choiceList.add(instance);
               exportedInstance.release(instance);
            }
            choices = choiceList;
         }
         selectComponent.setValueChoices(choices);
      }
   }

   private void configureRequiredFacets(InputComponent<?, ?> input)
   {
      HintsFacetImpl hintsFacet = new HintsFacetImpl(input, environment);
      input.install(hintsFacet);
   }
}
