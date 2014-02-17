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

package org.jahia.modules.bootstrap.rules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.rules.AddedNodeFact;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import java.io.*;

public class BootstrapCompilerRuleService {

    public static final String BOOTSTRAP_CSS = "bootstrap.css";
    public static final String RESPONSIVE_CSS = "responsive.css";
    public static final String CSS_FOLDER = "css";
    public static final String BOOTSTRAP_FOLDER = "bootstrap";

    private LessCompiler lessCompiler;

    public void compile(AddedNodeFact nodeFact, KnowledgeHelper drools)
            throws RepositoryException, IOException, LessException {
        JCRNodeWrapper node = nodeFact.getNode();
        File tmpLessFolder = new File(FileUtils.getTempDirectory(), "less-" + System.currentTimeMillis());
        tmpLessFolder.mkdir();
        try {
            JCRNodeWrapper lessFolder = node.getParent();
            NodeIterator nodes = lessFolder.getNodes();
            copyDirectory(tmpLessFolder, nodes);
            JCRSiteNode site = node.getResolveSite();
            File bootstrapCss = new File(tmpLessFolder, BOOTSTRAP_CSS);
            lessCompiler.compile(new File(tmpLessFolder, "bootstrap.less"), bootstrapCss);
            JCRNodeWrapper files = lessFolder.getParent();
            JCRNodeWrapper bootstrapFolder;
            if (files.hasNode(BOOTSTRAP_FOLDER)) {
                bootstrapFolder = files.getNode(BOOTSTRAP_FOLDER);
            } else {
                bootstrapFolder = files.addNode(BOOTSTRAP_FOLDER, "jnt:folder");
            }
            JCRNodeWrapper cssFolder;
            if (bootstrapFolder.hasNode(CSS_FOLDER)) {
                cssFolder = bootstrapFolder.getNode(CSS_FOLDER);
            } else {
                cssFolder = bootstrapFolder.addNode(CSS_FOLDER, "jnt:folder");
            }
            boolean uploadCss = true;
            JCRNodeWrapper bootstrapCssNode;
            if (cssFolder.hasNode(BOOTSTRAP_CSS)) {
                bootstrapCssNode = cssFolder.getNode(BOOTSTRAP_CSS);
                uploadCss = !IOUtils.contentEquals(bootstrapCssNode.getFileContent().downloadFile(), new FileInputStream(bootstrapCss));
            } else {
                bootstrapCssNode = cssFolder.addNode(BOOTSTRAP_CSS, "jnt:file");
            }
            if (uploadCss) {
                bootstrapCssNode.getFileContent().uploadFile(new FileInputStream(bootstrapCss),"text/css");
                node.getSession().save();
            }
        } finally {
            FileUtils.deleteQuietly(tmpLessFolder);
        }
    }

    private void copyDirectory(File tmpLessFolder, NodeIterator nodes) throws RepositoryException, IOException {
        while (nodes.hasNext()) {
            JCRNodeWrapper n = (JCRNodeWrapper) nodes.nextNode();
            if (n.isNodeType("nt:folder")) {
                File directory = new File(tmpLessFolder, n.getName());
                FileUtils.forceMkdir(directory);
                copyDirectory(directory, n.getNodes());
            } else if (n.isNodeType("nt:file")) {
                IOUtils.copy(n.getFileContent().downloadFile(), new FileOutputStream(new File(tmpLessFolder,
                        n.getName())));
            }
        }
    }

    public void setLessCompiler(LessCompiler lessCompiler) {
        this.lessCompiler = lessCompiler;
    }
}
