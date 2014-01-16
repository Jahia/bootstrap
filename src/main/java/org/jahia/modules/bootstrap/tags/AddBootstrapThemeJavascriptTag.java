/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
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
                                String tag = String.format("<jahia:resource type=\"javascript\" path=\"%s\" resource=\"%s\" title=\"\" key=\"\" />\n",
                                        path, next.getName());
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
