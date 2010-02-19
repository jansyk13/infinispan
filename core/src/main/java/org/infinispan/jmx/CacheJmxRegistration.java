/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2000 - 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.jmx;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.CacheDelegate;
import org.infinispan.CacheException;
import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.factories.AbstractComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.AbstractComponentRegistry.Component;
import org.infinispan.factories.annotations.Inject;
import org.infinispan.factories.annotations.SurvivesRestarts;
import org.infinispan.factories.annotations.Start;
import org.infinispan.factories.annotations.Stop;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.util.HashSet;
import java.util.Set;

/**
 * If {@link Configuration#isExposeJmxStatistics()} is true, then class will register all the MBeans from cache local's
 * ConfigurationRegistry to the MBean server.
 *
 * @author Mircea.Markus@jboss.com
 * @author Galder Zamarreño
 * @see java.lang.management.ManagementFactory#getPlatformMBeanServer()
 * @since 4.0
 */
@SurvivesRestarts
public class CacheJmxRegistration extends AbstractJmxRegistration {
   private static final Log log = LogFactory.getLog(CacheJmxRegistration.class);

   private AdvancedCache cache;
   private Set<Component> nonCacheComponents;

   @Inject
   public void initialize(AdvancedCache cache, GlobalConfiguration globalConfig) {
      this.cache = cache;
      this.globalConfig = globalConfig;
   }

   /**
    * Here is where the registration is being performed.
    */
   @Start(priority = 14)
   public void start() {
      if (cache == null)
         throw new IllegalStateException("The cache should had been injected before a call to this method");
      Configuration config = cache.getConfiguration();
      if (config.isExposeJmxStatistics()) {
         Set<Component> components = cache.getComponentRegistry().getRegisteredComponents();
         nonCacheComponents = getNonCacheComponents(components);
         registerMBeans(components, cache.getConfiguration().getGlobalConfiguration());
         log.info("MBeans were successfully registered to the platform mbean server.");
      }
   }

   /**
    * Unregister when the cache is being stoped.
    */
   @Stop
   public void stop() {
      // This method might get called several times.
      // After the first call the cache will become null, so we guard this
      if (cache == null) return;
      Configuration config = cache.getConfiguration();
      if (config.isExposeJmxStatistics()) {
         // Only unregister the non cache MBean so that it can be restarted
         unregisterMBeans(nonCacheComponents);
         log.trace("MBeans were successfully unregistered from the mbean server.");
      }

      // make sure we don't set cache to null, in case it needs to be restarted via JMX.
   }
   
   public void unregisterCacheMBean() {
      String pattern = jmxDomain + ":" + ComponentsJmxRegistration.JMX_RESOURCE_KEY + "=" + CacheDelegate.OBJECT_NAME + ",*";
      try {
         Set<ObjectName> names = mBeanServer.queryNames(new ObjectName(pattern), null);
         for (ObjectName name : names) {
            mBeanServer.unregisterMBean(name);
         }
      } catch (MBeanRegistrationException e) {
         String message = "Unable to unregister Cache MBeans with pattern " + pattern;
         log.warn(message, e);
      } catch (InstanceNotFoundException e) {
         // Ignore if Cache MBeans not present
      } catch (MalformedObjectNameException e) {
         String message = "Malformed pattern " + pattern;
         log.error(message, e);
         throw new CacheException(message, e);
      }
   }


   @Override
   protected ComponentsJmxRegistration buildRegistrar(Set<AbstractComponentRegistry.Component> components) {
      ComponentsJmxRegistration registrar = new ComponentsJmxRegistration(mBeanServer, components, getGroupName());
      updateDomain(registrar, cache.getComponentRegistry().getGlobalComponentRegistry(), mBeanServer);
      return registrar;
   }
   
   protected void updateDomain(ComponentsJmxRegistration registrar, GlobalComponentRegistry componentRegistry, MBeanServer mBeanServer) {
      GlobalConfiguration gc = componentRegistry.getComponent(GlobalConfiguration.class);
      CacheManagerJmxRegistration managerJmxReg = componentRegistry.getComponent(CacheManagerJmxRegistration.class);
      if (!gc.isExposeGlobalJmxStatistics() && jmxDomain == null) {
         String tmpJmxDomain = getJmxDomain(gc.getJmxDomain(), mBeanServer);
         synchronized (managerJmxReg) {
            if (managerJmxReg.jmxDomain == null) {
               if (!tmpJmxDomain.equals(gc.getJmxDomain()) && !gc.isAllowDuplicateDomains()) {
                  String message = "There's already an cache manager instance registered under '" + gc.getJmxDomain() +
                        "' JMX domain. If you want to allow multiple instances configured with same JMX domain enable " +
                        "'allowDuplicateDomains' attribute in 'globalJmxStatistics' config element";
                  if (log.isErrorEnabled()) log.error(message);
                  throw new JmxDomainConflictException(message);
               }
               // Set manager component's jmx domain so that other caches under same manager 
               // can see it, particularly important when jmx is only enabled at the cache level
               managerJmxReg.jmxDomain = tmpJmxDomain;
            }
            // So that all caches share the same domain, regardless of whether dups are 
            // allowed or not, simply assign the manager's calculated jmxDomain
            jmxDomain = managerJmxReg.jmxDomain;
         }
      } else {
         // If global stats were enabled, manager's jmxDomain would have been populated 
         // when cache manager was started, so no need for synchronization here.
         jmxDomain = managerJmxReg.jmxDomain == null ? gc.getJmxDomain() : managerJmxReg.jmxDomain;
      }
      registrar.setJmxDomain(jmxDomain);
   }

   protected Set<Component> getNonCacheComponents(Set<Component> components) {
      Set<Component> componentsExceptCache = new HashSet<AbstractComponentRegistry.Component>();
      for (AbstractComponentRegistry.Component component : components) {
         String name = component.getName();
         if (!name.equals(Cache.class.getName()) && !name.equals(AdvancedCache.class.getName())) {
            componentsExceptCache.add(component);
         }
      }
      return componentsExceptCache;
   }

   private String getGroupName() {
      return cache.getName() + "(" + cache.getConfiguration().getCacheModeString().toLowerCase() + ")";
   }

}
