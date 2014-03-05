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
import org.apache.commons.lang.StringUtils;
import org.drools.core.spi.KnowledgeHelper;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.templates.JahiaModuleAware;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.ModuleVersion;
import org.jahia.services.templates.TemplatePackageRegistry;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.jcr.RepositoryException;
import java.io.*;
import java.util.*;

public class BootstrapCompiler implements JahiaModuleAware {

    private static final Logger log = LoggerFactory.getLogger(BootstrapCompiler.class);

    public static final String CSS_FOLDER_PATH = "files/bootstrap/css";
    public static final String BOOTSTRAP_CSS = "bootstrap.css";
    public static final String BOOTSTRAP_CSS_IMPORT_PATH = "src/main/import/content/modules/%s/" + CSS_FOLDER_PATH + "/" + BOOTSTRAP_CSS;
    public static final String LESS_RESOURCES_FOLDER = "less";
    public static final String VARIABLES_LESS = "variables.less";

    private LessCompiler lessCompiler;
    private JahiaTemplateManagerService jahiaTemplateManagerService;
    private JCRPublicationService publicationService;
    private JahiaTemplatesPackage module;


    public void init() {
        if (module == null) {
            return;
        }
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper moduleVersion = session.getNode("/modules/" + module.getIdWithVersion());
                    Resource[] lessResources = module.getResources(LESS_RESOURCES_FOLDER);
                    try {
                        compileBootstrap(moduleVersion, Arrays.asList(lessResources), null);
                    } catch (IOException e) {
                        new RepositoryException(e);
                    } catch (LessException e) {
                        new RepositoryException(e);
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            log.error("Failed to compile bootstrap.css", e);
        }

    }

    public void compile(AddedNodeFact nodeFact)
            throws RepositoryException, IOException, LessException {
        if (module == null) {
            return;
        }
        JCRNodeWrapper moduleVersion = nodeFact.getNode();
        String templatesSetName = moduleVersion.getParent().getName();
        JahiaTemplatesPackage templatesSet = jahiaTemplateManagerService.getTemplatePackageRegistry().lookupByIdAndVersion(templatesSetName, new ModuleVersion(moduleVersion.getName()));
        Resource[] templatesSetLessResources = templatesSet.getResources(LESS_RESOURCES_FOLDER);
        // no need to compile bootstrap.css if the templatesSet doesn't contain any less files
        if (templatesSetLessResources.length == 0) {
            return;
        }
        ArrayList<Resource> lessResources = new ArrayList<Resource>(Arrays.asList(templatesSetLessResources));
        lessResources.addAll(Arrays.asList(module.getResources(LESS_RESOURCES_FOLDER)));
        compileBootstrap(moduleVersion, lessResources, null);
    }

    public void compileBootstrapWithVariables(JCRSiteNode site, String variables) throws RepositoryException, IOException, LessException {
        if (module == null) {
            return;
        }

        Set<JahiaTemplatesPackage> packages = new TreeSet<JahiaTemplatesPackage>(TemplatePackageRegistry.TEMPLATE_PACKAGE_COMPARATOR);
        for (String s : site.getInstalledModulesWithAllDependencies()) {
            packages.add(jahiaTemplateManagerService.getTemplatePackageById(s));
        }
        packages.remove(module);
        ArrayList<Resource> lessResources = new ArrayList<Resource>();
        for (JahiaTemplatesPackage aPackage : packages) {
            lessResources.addAll(Arrays.asList(aPackage.getResources(LESS_RESOURCES_FOLDER)));
        }
        lessResources.addAll(Arrays.asList(module.getResources(LESS_RESOURCES_FOLDER)));
        compileBootstrap(site, lessResources, variables);
    }

    private void compileBootstrap(JCRNodeWrapper siteOrModuleVersion, List<Resource> lessResources, String variables) throws IOException, LessException, RepositoryException {
        if (lessResources != null && !lessResources.isEmpty()) {
            File tmpLessFolder = new File(FileUtils.getTempDirectory(), "less-" + System.currentTimeMillis());
            tmpLessFolder.mkdir();
            try {
                for (Resource lessResource : lessResources) {
                    File lessFile = new File(tmpLessFolder, lessResource.getFilename());
                    if (!lessFile.exists()) {
                        InputStream inputStream;
                        if (variables != null && VARIABLES_LESS.equals(lessResource.getFilename())) {
                            inputStream = new SequenceInputStream(lessResource.getInputStream(), new ByteArrayInputStream(variables.getBytes()));
                        } else {
                            inputStream = lessResource.getInputStream();
                        }
                        IOUtils.copy(inputStream, new FileOutputStream(lessFile));
                    }
                }
                File bootstrapCss = new File(tmpLessFolder, BOOTSTRAP_CSS);
                lessCompiler.compile(new File(tmpLessFolder, "bootstrap.less"), bootstrapCss);
                JCRNodeWrapper node = siteOrModuleVersion;
                for (String pathPart : StringUtils.split(CSS_FOLDER_PATH, '/')) {
                    if (node.hasNode(pathPart)) {
                        node = node.getNode(pathPart);
                    } else {
                        node = node.addNode(pathPart, "jnt:folder");
                    }
                }
                boolean uploadCss = true;
                JCRNodeWrapper bootstrapCssNode;
                if (node.hasNode(BOOTSTRAP_CSS)) {
                    bootstrapCssNode = node.getNode(BOOTSTRAP_CSS);
                    uploadCss = !IOUtils.contentEquals(bootstrapCssNode.getFileContent().downloadFile(), new FileInputStream(bootstrapCss));
                } else {
                    bootstrapCssNode = node.addNode(BOOTSTRAP_CSS, "jnt:file");
                }
                if (uploadCss) {
                    FileInputStream inputStream = new FileInputStream(bootstrapCss);
                    bootstrapCssNode.getFileContent().uploadFile(inputStream,"text/css");
                    bootstrapCssNode.getSession().save();
                }
            } catch (IOException e) {
                throw new RepositoryException(e);
            } catch (LessException e) {
                throw new RepositoryException(e);
            } finally {
                FileUtils.deleteQuietly(tmpLessFolder);
            }
        }
    }

    public void publish(AddedNodeFact nodeFact) throws RepositoryException {
        publishBootstrapFolder(nodeFact.getNode());
    }

    public void publishBootstrapFolder(JCRNodeWrapper bootstrapFolder) throws RepositoryException {
        List<PublicationInfo> tree = publicationService.getPublicationInfo(bootstrapFolder.getIdentifier(), null, true, true, true, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE);
        publicationService.publishByInfoList(tree, Constants.EDIT_WORKSPACE, Constants.LIVE_WORKSPACE, false, new ArrayList<String>());
    }

    public void setLessCompiler(LessCompiler lessCompiler) {
        this.lessCompiler = lessCompiler;
    }

    public void setJahiaTemplateManagerService(JahiaTemplateManagerService jahiaTemplateManagerService) {
        this.jahiaTemplateManagerService = jahiaTemplateManagerService;
    }

    public void setPublicationService(JCRPublicationService publicationService) {
        this.publicationService = publicationService;
    }

    @Override
    public void setJahiaModule(JahiaTemplatesPackage module) {
        this.module = module;
    }
}
