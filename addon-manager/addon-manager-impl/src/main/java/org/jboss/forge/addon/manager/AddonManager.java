/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.jboss.forge.addon.manager;

import javax.inject.Inject;

import org.jboss.forge.addon.dependency.DependencyNode;
import org.jboss.forge.addon.dependency.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.dependency.spi.DependencyResolver;
import org.jboss.forge.container.AddonId;
import org.jboss.forge.container.AddonRepository;
import org.jboss.forge.container.services.Remote;

/**
 * Installs addons into an {@link AddonRepository}
 *
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 *
 */
@Remote
public class AddonManager
{
   private AddonRepository repository;
   private DependencyResolver resolver;

   @Inject
   public AddonManager(AddonRepository repository, DependencyResolver resolver)
   {
      this.repository = repository;
      this.resolver = resolver;
   }

   public InstallRequest install(AddonId addonId)
   {
      String coordinates = addonId.getName() + ":jar:forge-addon:" + addonId.getVersion();
      DependencyNode requestedAddonNode = resolver.resolveDependencyHierarchy(DependencyQueryBuilder
               .create(coordinates));

      return new InstallRequest(repository, requestedAddonNode);
   }

   public boolean remove(AddonId entry)
   {
      return repository.disable(entry);
   }
}