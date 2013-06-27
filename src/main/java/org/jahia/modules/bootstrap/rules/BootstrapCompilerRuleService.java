/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
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

package org.jahia.modules.bootstrap.rules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.drools.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.rules.AddedNodeFact;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BootstrapCompilerRuleService {

    public static final String BOOTSTRAP_CSS = "bootstrap.css";
    public static final String BOOTSTRAP_RESPONSIVE_CSS = "bootstrap-responsive.css";
    public static final String CSS_FOLDER = "css";

    private LessCompiler lessCompiler;

    public void compile(AddedNodeFact nodeFact, KnowledgeHelper drools) throws RepositoryException, IOException, LessException {
        JCRNodeWrapper node = nodeFact.getNode();
        File tmpLessFolder = new File(FileUtils.getTempDirectory(), "less-" + System.currentTimeMillis());
        tmpLessFolder.mkdir();
        JCRNodeWrapper lessFolder = node.getParent();
        NodeIterator nodes = lessFolder.getNodes();
        while (nodes.hasNext()) {
            JCRNodeWrapper n = (JCRNodeWrapper) nodes.nextNode();
            IOUtils.copy(n.getFileContent().downloadFile(), new FileOutputStream(new File(tmpLessFolder, n.getName())));
        }
        File bootstrapCss = new File(tmpLessFolder, BOOTSTRAP_CSS);
        lessCompiler.compile(new File(tmpLessFolder, "bootstrap.less"), bootstrapCss);
        File responsiveCss = new File(tmpLessFolder, BOOTSTRAP_RESPONSIVE_CSS);
        lessCompiler.compile(new File(tmpLessFolder, "responsive.less"), responsiveCss);
        JCRNodeWrapper files = lessFolder.getParent();
        JCRNodeWrapper cssFolder;
        if (files.hasNode(CSS_FOLDER)) {
            cssFolder = files.getNode(CSS_FOLDER);
        } else {
            cssFolder = files.addNode(CSS_FOLDER, "jnt:folder");
        }
        JCRNodeWrapper bootstrapCssNode;
        if (cssFolder.hasNode(BOOTSTRAP_CSS)) {
            bootstrapCssNode = cssFolder.getNode(BOOTSTRAP_CSS);
        } else {
            bootstrapCssNode = cssFolder.addNode(BOOTSTRAP_CSS, "jnt:file");
        }
        bootstrapCssNode.getFileContent().uploadFile(new FileInputStream(bootstrapCss), "text/css");
        JCRNodeWrapper responsiveCssNode;
        if (cssFolder.hasNode(BOOTSTRAP_RESPONSIVE_CSS)) {
            responsiveCssNode = cssFolder.getNode(BOOTSTRAP_RESPONSIVE_CSS);
        } else {
            responsiveCssNode = cssFolder.addNode(BOOTSTRAP_RESPONSIVE_CSS, "jnt:file");
        }
        responsiveCssNode.getFileContent().uploadFile(new FileInputStream(responsiveCss), "text/css");
        node.getSession().save();
    }

    public void setLessCompiler(LessCompiler lessCompiler) {
        this.lessCompiler = lessCompiler;
    }
}
