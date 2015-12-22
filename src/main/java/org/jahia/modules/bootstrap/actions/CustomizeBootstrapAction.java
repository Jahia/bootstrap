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
package org.jahia.modules.bootstrap.actions;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.modules.bootstrap.rules.BootstrapCompiler;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRFileNode;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class CustomizeBootstrapAction extends Action {

    public static final String BOOTSTRAP_VARIABLES = "bootstrapVariables";

    private BootstrapCompiler bootstrapCompiler;

    @Override
    public ActionResult doExecute(HttpServletRequest request, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters,
                                  URLResolver urlResolver) throws Exception {
        JCRSiteNode site = renderContext.getSite();
        if (parameters.keySet().contains("resetVariables")) {
            if (site.hasNode(BOOTSTRAP_VARIABLES)) {
                site.getNode(BOOTSTRAP_VARIABLES).remove();
                session.save();
            }
            bootstrapCompiler.compileBootstrapWithVariables(site, null);
            return ActionResult.OK;
        }

        JCRNodeWrapper variablesNode;
        if (site.hasNode(BOOTSTRAP_VARIABLES)) {
            variablesNode = site.getNode(BOOTSTRAP_VARIABLES);
        } else {
            variablesNode = site.addNode(BOOTSTRAP_VARIABLES, "jnt:lessVariables");
        }
        StringBuilder variablesLessBuilder = new StringBuilder("\n");
        for (String varName : parameters.keySet()) {
            if ("jcrRedirectTo".equals(varName) || "jcrNewNodeOutputFormat".equals(varName)) {
                continue;
            }
            List<String> varValues = parameters.get(varName);
            if (varValues == null || varValues.isEmpty() || StringUtils.isBlank(varValues.get(0))) {
                if (variablesNode.hasProperty(varName)) {
                    variablesNode.getProperty(varName).remove();
                }
            } else {
                String varValue = varValues.get(0);
                variablesNode.setProperty(varName, varValue);
                variablesLessBuilder.append("@").append(varName).append(":\t").append(varValue).append(";\n");
            }
        }
        session.save();
        bootstrapCompiler.compileBootstrapWithVariables(site, variablesLessBuilder.toString());
        return ActionResult.OK;
    }

    public void setBootstrapCompiler(BootstrapCompiler bootstrapCompiler) {
        this.bootstrapCompiler = bootstrapCompiler;
    }
}
