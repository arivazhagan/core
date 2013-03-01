/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.resource;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.forge.container.AddonRegistry;
import org.jboss.forge.container.services.ExportedInstance;
import org.jboss.forge.resource.events.ResourceEvent;
import org.jboss.forge.resource.transaction.ResourceTransaction;
import org.jboss.forge.resource.transaction.ResourceTransactionManager;

/**
 * @author <a href="http://community.jboss.org/people/kenfinni">Ken Finnigan</a>
 * @author Mike Brock <cbrock@redhat.com>
 */
@Singleton
public class ResourceFactoryImpl implements ResourceFactory, ResourceTransactionManager
{
   @Inject
   private BeanManager manager;

   @Inject
   private AddonRegistry registry;

   private ResourceTransactionImpl transaction;

   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public <E, T extends Resource<E>> T create(final Class<T> type, final E underlyingResource)
   {
      T result = null;
      synchronized (this)
      {
         List<Resource<?>> generated = new ArrayList<Resource<?>>();
         for (ExportedInstance<ResourceGenerator> instance : getRegisteredResourceGenerators())
         {
            ResourceGenerator generator = instance.get();
            if (generator.handles(type, underlyingResource))
            {
               if (type.isAssignableFrom(generator.getResourceType(type, underlyingResource)))
               {
                  generated.add(generator.getResource(this, type, underlyingResource));
               }
            }
            instance.release(generator);
         }

         /*
          * Choose the resource that is most specialized. TODO This needs to handle forks in the type hierarchy.
          */
         for (Resource<?> resource : generated)
         {
            if (result == null || result.getClass().isAssignableFrom(resource.getClass()))
               result = (T) resource;
         }
      }
      return result;
   }

   @Override
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public <E> Resource<E> create(E underlyingResource)
   {
      Resource<E> result = null;
      synchronized (this)
      {
         List<Resource<?>> generated = new ArrayList<Resource<?>>();
         for (ExportedInstance<ResourceGenerator> instance : getRegisteredResourceGenerators())
         {
            ResourceGenerator generator = instance.get();
            if (generator.handles(Resource.class, underlyingResource))
            {
               Resource resource = generator.getResource(this, Resource.class, underlyingResource);
               generated.add(resource);
            }
            instance.release(generator);
         }

         /*
          * Choose the resource that is most specialized. TODO This needs to handle forks in the type hierarchy.
          */
         for (Resource<?> resource : generated)
         {
            if (result == null || result.getClass().isAssignableFrom(resource.getClass()))
            {
               result = (Resource<E>) resource;
            }
         }
      }
      // Transaction Hook
      if (result != null && transaction != null)
      {
         result = transaction.decorateResource(result);
      }

      return result;
   }

   @SuppressWarnings("rawtypes")
   private Iterable<ExportedInstance<ResourceGenerator>> getRegisteredResourceGenerators()
   {
      return registry.getExportedInstances(ResourceGenerator.class);
   }

   @Override
   public ResourceFactory fireEvent(ResourceEvent event)
   {
      manager.fireEvent(event, new Annotation[] {});
      return this;
   }

   @Override
   public ResourceTransaction getCurrentTransaction()
   {
      return transaction;
   }

   @Override
   public ResourceTransaction startTransaction() throws ResourceException
   {
      if (transaction != null)
      {
         throw new ResourceException("Transaction already exists!");
      }
      transaction = new ResourceTransactionImpl(this);
      return transaction;
   }

   void unsetTransaction()
   {
      transaction = null;
   }

}