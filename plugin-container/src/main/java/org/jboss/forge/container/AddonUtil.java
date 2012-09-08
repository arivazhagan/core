/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.container;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.forge.container.util.Assert;
import org.jboss.forge.container.util.OSUtils;
import org.jboss.forge.container.util.Streams;
import org.jboss.forge.container.util.Strings;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.parser.xml.XMLParserException;

/**
 * Used to perform Addon installation/registration operations.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:koen.aers@gmail.com">Koen Aers</a>
 */
public final class AddonUtil
{
   private static final String DEFAULT_SLOT = "main";
   private static final String ATTR_SLOT = "slot";
   private static final String ATTR_API_VERSION = "api-version";
   private static final String ATTR_NAME = "name";
   private static final String PLUGIN_DIR_DEFAULT = "/.forge/addons";
   private static final String REGISTRY_FILE = "/installed.xml";

   private static String PLUGIN_DIR = null;
   private static String REGISTRY = null;

   public static String getAddonDirName()
   {
      if (PLUGIN_DIR == null)
      {
         PLUGIN_DIR = System.getProperty(Bootstrap.PROP_PLUGIN_DIR);
         if (PLUGIN_DIR == null)
         {
            PLUGIN_DIR = OSUtils.getUserHomePath() + PLUGIN_DIR_DEFAULT;
         }
      }
      return PLUGIN_DIR;
   }

   public static String getRegistryFileName()
   {
      if (REGISTRY == null)
      {
         REGISTRY = getAddonDirName() + REGISTRY_FILE;
      }
      return REGISTRY;
   }

   public static File getRegistryFile()
   {
      return new File(getRegistryFileName());
   }

   public static List<AddonEntry> listByAPICompatibleVersion(final String version)
   {
      List<AddonEntry> list = list();
      List<AddonEntry> result = list;

      if (version != null)
      {
         result = new ArrayList<AddonUtil.AddonEntry>();
         for (AddonEntry entry : list)
         {
            if (isApiCompatible(version, entry))
            {
               result.add(entry);
            }
         }
      }

      return result;
   }

   public static List<AddonEntry> list()
   {
      List<AddonEntry> result = new ArrayList<AddonEntry>();
      // File registryFile = new File(OSUtils.getUserHomePath() + getRegistry());
      File registryFile = getRegistryFile();
      try
      {
         Node installed = XMLParser.parse(new FileInputStream(registryFile));
         List<Node> list = installed.get("plugin");
         for (Node plugin : list)
         {
            AddonEntry entry = new AddonEntry(plugin.getAttribute(ATTR_NAME),
                     plugin.getAttribute(ATTR_API_VERSION),
                     plugin.getAttribute(ATTR_SLOT));
            result.add(entry);
         }
      }
      catch (XMLParserException e)
      {
         throw new RuntimeException("Invalid syntax in [" + registryFile.getAbsolutePath()
                  + "] - Please delete this file and restart Forge", e);
      }
      catch (FileNotFoundException e)
      {
         // this is OK, no plugins installed
      }
      return result;
   }

   public static AddonEntry install(final String name, final String apiVersion, String slot)
   {
      if (Strings.isNullOrEmpty(name))
      {
         throw new RuntimeException("Plugin must not be null");
      }
      if (Strings.isNullOrEmpty(apiVersion))
      {
         throw new RuntimeException("API version must not be null");
      }
      if (Strings.isNullOrEmpty(slot))
      {
         slot = DEFAULT_SLOT;
      }

      List<AddonEntry> installedPlugins = list();
      for (AddonEntry e : installedPlugins)
      {
         if (name.equals(e.getName()))
         {
            remove(e);
         }
      }

      Node installed = null;
      File registryFile = getRegistryFile();
      try
      {

         if (registryFile.exists())
         {
            installed = XMLParser.parse(new FileInputStream(registryFile));
         }
         else
         {
            registryFile.mkdirs();
            registryFile.delete();
            registryFile.createNewFile();

            installed = XMLParser.parse("<installed></installed>");
         }

         installed.getOrCreate("plugin@" + ATTR_NAME + "=" + name + "&" + ATTR_API_VERSION + "=" + apiVersion)
                  .attribute(ATTR_SLOT, slot);
         Streams.write(XMLParser.toXMLInputStream(installed), new FileOutputStream(registryFile));

         return new AddonEntry(name, apiVersion, slot);
      }
      catch (FileNotFoundException e)
      {
         throw new RuntimeException("Could not read [" + registryFile.getAbsolutePath()
                  + "] - ", e);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Error manipulating [" + registryFile.getAbsolutePath()
                  + "] - ", e);
      }
   }

   public static void remove(final AddonEntry plugin)
   {
      if (plugin == null)
      {
         throw new RuntimeException("Plugin must not be null");
      }

      File registryFile = getRegistryFile();
      if (registryFile.exists())
      {
         try
         {
            Node installed = XMLParser.parse(new FileInputStream(registryFile));

            Node child = installed.getSingle("plugin@" + ATTR_NAME + "=" + plugin.getName() + "&"
                     + ATTR_API_VERSION
                     + "=" + plugin.getApiVersion());
            installed.removeChild(child);
            Streams.write(XMLParser.toXMLInputStream(installed), new FileOutputStream(registryFile));
         }
         catch (FileNotFoundException e)
         {
            // already removed
         }
      }
   }

   public static AddonEntry get(final AddonEntry plugin)
   {
      if (plugin == null)
      {
         throw new RuntimeException("Plugin must not be null");
      }

      File registryFile = getRegistryFile();
      if (registryFile.exists())
      {
         try
         {
            Node installed = XMLParser.parse(new FileInputStream(registryFile));

            List<Node> children = installed.get("plugin@" + ATTR_NAME + "=" + plugin.getName());
            for (Node child : children)
            {
               if (child != null)
               {
                  if ((plugin.getApiVersion() == null)
                           || plugin.getApiVersion().equals(child.getAttribute(ATTR_API_VERSION)))
                  {
                     if ((plugin.getSlot() == null)
                              || plugin.getSlot().equals(child.getAttribute(ATTR_SLOT)))
                     {
                        return new AddonEntry(child.getAttribute(ATTR_NAME),
                                 child.getAttribute(ATTR_API_VERSION),
                                 child.getAttribute(ATTR_SLOT));
                     }
                  }
               }
            }
         }
         catch (FileNotFoundException e)
         {
            // already removed
         }
      }

      return null;
   }

   public static File getAddonDirectory(AddonEntry found)
   {
      Assert.notNull(found.getSlot(), "Plugin slot must be specified.");
      Assert.notNull(found.getName(), "Plugin name must be specified.");

      String path = found.getName().replaceAll("\\.", "/");
      File pluginDir = new File(getAddonDirName() + "/" + path + "/" + found.getSlot());
      return pluginDir;
   }

   public static boolean has(final AddonEntry plugin)
   {
      return get(plugin) != null;
   }

   public static class AddonEntry
   {
      private final String name;
      private final String apiVersion;
      private final String slot;

      public AddonEntry(final String name, final String apiVersion, final String slot)
      {
         this.name = name;
         this.apiVersion = apiVersion;
         this.slot = slot;
      }

      public AddonEntry(final String name, final String apiVersion)
      {
         this.name = name;
         this.apiVersion = apiVersion;
         this.slot = null;
      }

      public AddonEntry(final String name)
      {
         this.name = name;
         this.apiVersion = null;
         this.slot = null;
      }

      public String getName()
      {
         return name;
      }

      public String getApiVersion()
      {
         return apiVersion;
      }

      public String getSlot()
      {
         return slot;
      }

      @Override
      public String toString()
      {
         return name + ":" + apiVersion + ":" + slot;
      }

      public static AddonEntry fromCoordinates(final String coordinates)
      {
         String[] split = coordinates.split(":");
         List<String> tokens = Arrays.asList(split);

         if (tokens.size() == 3)
         {
            if (Strings.isNullOrEmpty(tokens.get(0)))
               throw new IllegalArgumentException("Name was empty [" + coordinates + "]");
            if (Strings.isNullOrEmpty(tokens.get(1)))
               throw new IllegalArgumentException("Version was empty [" + coordinates + "]");
            if (Strings.isNullOrEmpty(tokens.get(2)))
               throw new IllegalArgumentException("Slot was empty [" + coordinates + "]");

            return new AddonEntry(tokens.get(0), tokens.get(1), tokens.get(2));
         }
         else
         {
            throw new IllegalArgumentException("Coordinates must be of the form 'name:apiVersion:slot'");
         }

      }

      public String toModuleId()
      {
         return name + ":" + slot;
      }

      @Override
      public int hashCode()
      {
         final int prime = 31;
         int result = 1;
         result = (prime * result) + ((apiVersion == null) ? 0 : apiVersion.hashCode());
         result = (prime * result) + ((name == null) ? 0 : name.hashCode());
         result = (prime * result) + ((slot == null) ? 0 : slot.hashCode());
         return result;
      }

      @Override
      public boolean equals(final Object obj)
      {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         AddonEntry other = (AddonEntry) obj;
         if (apiVersion == null)
         {
            if (other.apiVersion != null)
               return false;
         }
         else if (!apiVersion.equals(other.apiVersion))
            return false;
         if (name == null)
         {
            if (other.name != null)
               return false;
         }
         else if (!name.equals(other.name))
            return false;
         if (slot == null)
         {
            if (other.slot != null)
               return false;
         }
         else if (!slot.equals(other.slot))
            return false;
         return true;
      }

      public String toCoordinates()
      {
         return toString();
      }

   }

   public static String getRuntimeAPIVersion()
   {
      return AddonUtil.class.getPackage()
               .getImplementationVersion();
   }

   public static boolean isApiCompatible(CharSequence runtimeVersion, AddonEntry entry)
   {
      Assert.notNull(runtimeVersion, "Runtime API version must not be null.");
      Assert.notNull(entry, "Plugin entry must not be null.");
      String pluginApiVersion = entry.getApiVersion();
      Assert.notNull(pluginApiVersion, "Plugin entry.getApiVersion() must not be null.");

      return isApiCompatible(runtimeVersion, pluginApiVersion);
   }

   public static boolean isApiCompatible(CharSequence runtimeVersion, String pluginApiVersion)
   {
      Pattern runtimeVersionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(\\.|-)(.*)");
      Matcher matcher = runtimeVersionPattern.matcher(runtimeVersion);
      if (matcher.matches())
      {
         if (pluginApiVersion.matches(matcher.group(1) + "\\." + matcher.group(2) + "\\.(\\d+).*"))
         {
            return true;
         }
      }

      return false;
   }

   public static List<File> getPluginResourceJars(AddonEntry found)
   {
      File dir = AddonUtil.getAddonDirectory(found);
      return Arrays.asList(dir.listFiles(new FilenameFilter()
      {
         @Override
         public boolean accept(File file, String name)
         {
            return name.endsWith(".jar");
         }
      }));
   }
}