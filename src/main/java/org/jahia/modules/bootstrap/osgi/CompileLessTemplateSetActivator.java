/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.bootstrap.osgi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.bootstrap.rules.BootstrapCompiler;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.ModuleVersion;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Activator for modules data provider.
 * Mount and unmount sources at startup/stop of modules
 */
public class CompileLessTemplateSetActivator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(CompileLessTemplateSetActivator.class);
        private BundleContext context;

    /**
     * Called when this bundle is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle. This method
     * can be used to register services or to allocate any resources that this
     * bundle needs.
     * <p/>
     * <p/>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being started.
     */
    @Override
    public void start(BundleContext context) {
        if (this.context == null) {
            this.context = context;
        }

        context.addBundleListener(new SynchronousBundleListener() {
            private List<String> getDependenciesIds(JahiaTemplatesPackage jahiaTemplatesPackage) {
                String depends = jahiaTemplatesPackage.getBundle().getHeaders().get("Jahia-Depends");
                ArrayList<String> l = new ArrayList<String>();
                for (String dep : StringUtils.split(depends, ",")) {
                    l.add(dep.trim());
                }
                return l;
            }

            @Override
            public void bundleChanged(BundleEvent bundleEvent) {
                if (bundleEvent.getType() == BundleEvent.STARTED) {
                    JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
                    if(jahiaTemplateManagerService == null) {
                        return;
                    }

                    String version = bundleEvent.getBundle().getHeaders().get("Implementation-Version");
                    if(version == null) {
                        return;
                    }

                    JahiaTemplatesPackage jahiaTemplatesPackage = jahiaTemplateManagerService.getTemplatePackageRegistry().lookupByIdAndVersion(bundleEvent.getBundle().getSymbolicName(), new ModuleVersion(version));
                    if(jahiaTemplatesPackage == null){
                        return;
                    }

                    if(jahiaTemplatesPackage.getModuleType().equals(JahiaTemplateManagerService.MODULE_TYPE_TEMPLATES_SET) &&
                            getDependenciesIds(jahiaTemplatesPackage).contains("bootstrap")){
                        BootstrapCompiler bootstrapCompiler = null;
                        try {
                            bootstrapCompiler = (BootstrapCompiler) SpringContextSingleton.getBean("BootstrapCompiler");
                        } catch (NoSuchBeanDefinitionException e) {
                            log.debug("Failed to find BootstrapCompiler", e);
                        }
                        if (bootstrapCompiler != null) {
                            try {
                                bootstrapCompiler.compile(jahiaTemplatesPackage);
                            } catch (RepositoryException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle. In general, this
     * method should undo the work that the <code>BundleActivator.start</code>
     * method started. There should be no active threads that were started by
     * this bundle when this bundle returns. A stopped bundle must not call any
     * Framework objects.
     * <p/>
     * <p/>
     * This method must complete and return to its caller in a timely manner.
     *
     * @param context The execution context of the bundle being stopped.
     */
    @Override
    public void stop(BundleContext context) {

    }
}
