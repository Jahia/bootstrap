/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.bootstrap.tags;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.URLGenerator;
import org.jahia.taglibs.AbstractJahiaTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.jsp.JspException;
import java.util.List;

public class AddBootstrapCSSTag extends AbstractJahiaTag {
    private static final Logger logger = LoggerFactory.getLogger(AddBootstrapCSSTag.class);

    public static final String BOOTSTRAP2_CSS_NAME = "bootstrap.css";
    public static final String BOOTSTRAP3_CSS_NAME = "bootstrap3.css";
    public static final String BOOTSTRAP2_CSS_PATH = "/files/bootstrap/css/" + BOOTSTRAP2_CSS_NAME;
    public static final String BOOTSTRAP3_CSS_PATH = "/files/bootstrap/css/" + BOOTSTRAP3_CSS_NAME;

    @Override
    public int doEndTag() throws JspException {
        try {
            RenderContext renderContext = getRenderContext();
            JCRSiteNode site = renderContext.getSite();
            String basePath = site.getPath();
            boolean isVersion2 = true;
            if (basePath.startsWith("/modules/")) {
                basePath = "/modules/" + site.getTemplatePackage().getIdWithVersion();
                if (!site.getSession().nodeExists(basePath + BOOTSTRAP2_CSS_PATH)) {
                    List<JahiaTemplatesPackage> dependencies = site.getTemplatePackage().getDependencies();
                    for (JahiaTemplatesPackage dependency : dependencies) {
                        if ("bootstrap".equals(dependency.getId())) {
                            isVersion2 = isBootstrapVersion2(site, basePath);
                            basePath = "/modules/" + dependency.getIdWithVersion();
                            break;
                        }
                    }
                }
            }
            URLGenerator urlGenerator = renderContext.getURLGenerator();
            String path = urlGenerator.getContext() + urlGenerator.getFiles() + basePath + ((isVersion2)?BOOTSTRAP2_CSS_PATH:BOOTSTRAP3_CSS_PATH);
            String tag = "<jahia:resource type=\"css\" path=\"" + path + "\" insert=\"true\" resource=\"" + ((isVersion2)?BOOTSTRAP2_CSS_NAME:BOOTSTRAP3_CSS_NAME) + "\" title=\"\" key=\"\" />\n";
            pageContext.getOut().print(tag);
        } catch (Exception e) {
            throw new JspException("Failed to write jahia:resource tag for bootstrap", e);
        }
        return super.doEndTag();
    }

    private boolean isBootstrapVersion2(JCRSiteNode site, String basePath) {
        try {
            JCRNodeWrapper currentModule = site.getSession().getNode(basePath);
            if (currentModule.hasNode("templates")) {
                JCRNodeWrapper templatesFolder = currentModule.getNode("templates");
                return !(templatesFolder.hasProperty("bootstrapVersion") && templatesFolder.getProperty("bootstrapVersion").getString().equals("less3"));
            }
        } catch (RepositoryException e) {
            logger.error(e.getMessage());
        }
        return true;
    }
}
