/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.taglibs.AbstractJahiaTag;
import org.slf4j.Logger;

import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.servlet.jsp.JspException;
import java.util.List;

public class AddBootstrapThemeJavascriptTag extends AbstractJahiaTag {
    private static final transient Logger logger = org.slf4j.LoggerFactory.getLogger(AddBootstrapThemeJavascriptTag.class);
    public static final String BOOTSTRAP_THEME_JAVASCRIPT_PATH = "/files/bootstrap/javascript";

    @Override
    public int doEndTag() throws JspException {
        try {
            RenderContext renderContext = getRenderContext();
            JCRSiteNode site = renderContext.getSite();
            JCRSessionWrapper session = site.getSession();
            String basePath = site.getPath();
            if (basePath.startsWith("/modules/")) {
                basePath = "/modules/" + site.getTemplatePackage().getIdWithVersion();
                if (!session.nodeExists(basePath + BOOTSTRAP_THEME_JAVASCRIPT_PATH)) {
                    List<JahiaTemplatesPackage> dependencies = site.getTemplatePackage().getDependencies();
                    for (JahiaTemplatesPackage dependency : dependencies) {
                        if ("bootstrap".equals(dependency.getId())) {
                            basePath = "/modules/" + dependency.getIdWithVersion();
                            break;
                        }
                    }
                }
            }
            try {
                if (session.nodeExists(basePath + BOOTSTRAP_THEME_JAVASCRIPT_PATH)) {
                    JCRNodeWrapper node = session.getNode(basePath + BOOTSTRAP_THEME_JAVASCRIPT_PATH);
                    if(node.hasNodes()) {
                        NodeIterator nodes = node.getNodes();
                        while (nodes.hasNext()) {
                            JCRNodeWrapper next = (JCRNodeWrapper) nodes.next();
                            if(next.isNodeType("nt:file")) {
                                String path = renderContext.getURLGenerator().getFiles() + next.getPath();
                                String tag = "<jahia:resource type=\"javascript\" path=\"" + path + "\" resource=\"" + next.getName() + "\" title=\"\" key=\"\" />\n";
                                pageContext.getOut().print(tag);
                            }
                        }
                    }
                }
            } catch (PathNotFoundException e) {
                logger.debug(e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new JspException("Failed to write jahia:resource tag for bootstrap", e);
        }
        return super.doEndTag();
    }
}
