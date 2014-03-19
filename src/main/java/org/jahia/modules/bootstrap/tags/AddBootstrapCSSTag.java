/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
package org.jahia.modules.bootstrap.tags;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.URLGenerator;
import org.jahia.taglibs.AbstractJahiaTag;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.servlet.jsp.JspException;
import java.util.List;

public class AddBootstrapCSSTag extends AbstractJahiaTag {
    private static final transient Logger logger = org.slf4j.LoggerFactory.getLogger(AddBootstrapCSSTag.class);
    public static final String CSS_NAME = "bootstrap.css";
    public static final String CSS_PATH = "/files/bootstrap/css/" + CSS_NAME;

    @Override
    public int doEndTag() throws JspException {
        try {
            RenderContext renderContext = getRenderContext();
            JCRSiteNode site = renderContext.getSite();
            String basePath = site.getPath();
            if (basePath.startsWith("/modules/")) {
                basePath = "/modules/" + site.getTemplatePackage().getIdWithVersion();
                if (!site.getSession().nodeExists(basePath + CSS_PATH)) {
                    List<JahiaTemplatesPackage> dependencies = site.getTemplatePackage().getDependencies();
                    for (JahiaTemplatesPackage dependency : dependencies) {
                        if ("bootstrap".equals(dependency.getId())) {
                            basePath = "/modules/" + dependency.getIdWithVersion();
                            break;
                        }
                    }
                }
            }
            URLGenerator urlGenerator = renderContext.getURLGenerator();
            String path = urlGenerator.getContext() + urlGenerator.getFiles() + basePath + CSS_PATH;
            String tag = String.format("<jahia:resource type=\"css\" path=\"%s\" insert=\"true\" resource=\"%s\" title=\"\" key=\"\" />\n",
                    path, CSS_NAME);
            pageContext.getOut().print(tag);
        } catch (Exception e) {
            throw new JspException("Failed to write jahia:resource tag for bootstrap", e);
        }
        return super.doEndTag();
    }
}
