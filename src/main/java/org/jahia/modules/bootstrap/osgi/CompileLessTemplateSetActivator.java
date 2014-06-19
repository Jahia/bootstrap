/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.bootstrap.osgi;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
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

import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

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
            private Function<JahiaTemplatesPackage, String> transformPackageToIds = new Function<JahiaTemplatesPackage, String>() {
                @Nullable
                @Override
                public String apply(@Nullable JahiaTemplatesPackage input) {
                    if (input != null){
                        return input.getId();
                    }
                    return null;
                }
            };

            @Override
            public void bundleChanged(BundleEvent bundleEvent) {
                if (bundleEvent.getType() == BundleEvent.STARTED) {
                    JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService();
                    if(jahiaTemplateManagerService == null) {
                        return;
                    }

                    String version = bundleEvent.getBundle().getHeaders().get("Implementation-Version");
                    JahiaTemplatesPackage jahiaTemplatesPackage = jahiaTemplateManagerService.getTemplatePackageRegistry().lookupByIdAndVersion(bundleEvent.getBundle().getSymbolicName(), new ModuleVersion(version));
                    if(jahiaTemplatesPackage == null) {
                        return;
                    }

                    if(jahiaTemplatesPackage.getModuleType().equals(JahiaTemplateManagerService.MODULE_TYPE_TEMPLATES_SET) &&
                            Collections2.transform(jahiaTemplatesPackage.getDependencies(), transformPackageToIds).contains("bootstrap")){
                        BootstrapCompiler bootstrapCompiler = (BootstrapCompiler) SpringContextSingleton.getBean("BootstrapCompiler");
                        if(bootstrapCompiler != null){
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
