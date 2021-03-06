/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.bootstrap.rules;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.bootstrap.actions.CustomizeBootstrapAction;
import org.jahia.services.content.*;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.jahia.services.content.rules.AddedNodeFact;
import org.jahia.services.templates.JahiaModuleAware;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.templates.TemplatePackageRegistry;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
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
        long timer = System.currentTimeMillis();
        try {
            JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
                public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                    JCRNodeWrapper moduleVersion = session.getNode("/modules/" + module.getIdWithVersion());
                    Resource[] lessResources = module.getResources(LESS_RESOURCES_FOLDER);
                    try {
                        compileBootstrap(moduleVersion, Arrays.asList(lessResources), null);
                    } catch (IOException e) {
                        throw new RepositoryException(e);
                    } catch (LessException e) {
                        throw new RepositoryException(e);
                    }
                    return null;
                }
            });
        } catch (RepositoryException e) {
            log.error("Failed to compile bootstrap.css", e);
        }
        log.info("Bootstrap initialization completed in {} ms", System.currentTimeMillis() - timer);
    }

    public void compile(final JahiaTemplatesPackage templateSet) throws RepositoryException {
        JCRTemplate.getInstance().doExecuteWithSystemSession(new JCRCallback<Object>() {
            @Override
            public Object doInJCR(JCRSessionWrapper session) throws RepositoryException {
                Resource[] templatesSetLessResources = templateSet.getResources(LESS_RESOURCES_FOLDER);
                if (templatesSetLessResources.length == 0) {
                    // no need to compile bootstrap.css if the templatesSet doesn't contain any less files, just copy it from bootstrap module
                    copyBootstrapCSS(module.getRootFolderPath() + "/" + module.getVersion().toString() + "/" + CSS_FOLDER_PATH + "/" + BOOTSTRAP_CSS,
                            session.getNode(templateSet.getRootFolderPath() + "/" + templateSet.getVersion().toString()),
                            session);
                } else {
                    ArrayList<Resource> lessResources = new ArrayList<Resource>(Arrays.asList(templatesSetLessResources));
                    lessResources.addAll(Arrays.asList(module.getResources(LESS_RESOURCES_FOLDER)));
                    try {
                        compileBootstrap(session.getNode(templateSet.getRootFolderPath() + "/" + templateSet.getVersion().toString()), lessResources, null);
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    } catch (LessException e) {
                        log.error(e.getMessage(), e);
                    }
                }

                // copy on all sites using this templateSet, that don't have any custom variables
                QueryManager qm = session.getWorkspace().getQueryManager();
                QueryResult result = qm.createQuery("SELECT * FROM [jnt:virtualsite] WHERE [j:templatesSet] = '" + templateSet.getId() + "'", Query.JCR_SQL2).execute();
                NodeIterator sites = result.getNodes();
                while (sites.hasNext()) {
                    JCRSiteNode site = (JCRSiteNode) sites.nextNode();
                    if (!site.getAllInstalledModules().contains("bootstrap") || !site.hasNode(CustomizeBootstrapAction.BOOTSTRAP_VARIABLES)) {
                        copyBootstrapCSS(templateSet.getRootFolderPath() + "/" + templateSet.getVersion().toString() + "/" + CSS_FOLDER_PATH + "/" + BOOTSTRAP_CSS,
                                site, session);
                    }
                }
                if (sites.getSize() > 0) {
                    session.save();
                }
                return null;
            }
        });
    }

    private void copyBootstrapCSS(String srcCssPath, JCRNodeWrapper siteOrModuleVersion, JCRSessionWrapper session) throws RepositoryException {
        if (session.itemExists(srcCssPath)) {
            JCRNodeWrapper dstCss = siteOrModuleVersion;
            for (String pathPart : StringUtils.split(CSS_FOLDER_PATH, '/')) {
                if (dstCss.hasNode(pathPart)) {
                    dstCss = dstCss.getNode(pathPart);
                } else {
                    dstCss = dstCss.addNode(pathPart, "jnt:folder");
                }
            }
            if (dstCss.hasNode(BOOTSTRAP_CSS)) {
                dstCss.getNode(BOOTSTRAP_CSS).remove();
            }
            session.getNode(srcCssPath).copy(dstCss.getPath());
            session.save();
        }
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
                List<String> allContent = new ArrayList<String>();
                for (Resource lessResource : lessResources) {
                    File lessFile = new File(tmpLessFolder, lessResource.getFilename());
                    if (!lessFile.exists()) {
                        InputStream inputStream;
                        if (variables != null && VARIABLES_LESS.equals(lessResource.getFilename())) {
                            inputStream = new SequenceInputStream(lessResource.getInputStream(), new ByteArrayInputStream(variables.getBytes()));
                        } else {
                            inputStream = lessResource.getInputStream();
                        }
                        final FileOutputStream output = new FileOutputStream(lessFile);
                        IOUtils.copy(inputStream, output);
                        IOUtils.closeQuietly(inputStream);
                        IOUtils.closeQuietly(output);
                    }
                    final FileInputStream input = new FileInputStream(lessFile);
                    allContent.addAll(IOUtils.readLines(input));
                    IOUtils.closeQuietly(input);
                }
                String md5 = DigestUtils.md5Hex(StringUtils.join(allContent, '\n'));

                JCRNodeWrapper node = siteOrModuleVersion;
                for (String pathPart : StringUtils.split(CSS_FOLDER_PATH, '/')) {
                    if (node.hasNode(pathPart)) {
                        node = node.getNode(pathPart);
                    } else {
                        node = node.addNode(pathPart, "jnt:folder");
                    }
                }

                boolean compileCss = true;
                JCRNodeWrapper bootstrapCssNode;

                if (node.hasNode(BOOTSTRAP_CSS)) {
                    bootstrapCssNode = node.getNode(BOOTSTRAP_CSS);
                    String content = bootstrapCssNode.getFileContent().getText();
                    String timestamp = StringUtils.substringBetween(content,"/* sources hash "," */");
                    if (timestamp != null && md5.equals(timestamp)) {
                        compileCss = false;
                    }
                } else {
                    bootstrapCssNode = node.addNode(BOOTSTRAP_CSS, "jnt:file");
                }
                if (compileCss) {
                    File bootstrapCss = new File(tmpLessFolder, BOOTSTRAP_CSS);
                    lessCompiler.compile(new File(tmpLessFolder, "bootstrap.less"), bootstrapCss);
                    FileOutputStream f = new FileOutputStream(bootstrapCss,true);
                    IOUtils.write("\n/* sources hash "+ md5 + " */\n",f);
                    IOUtils.closeQuietly(f);
                    FileInputStream inputStream = new FileInputStream(bootstrapCss);
                    bootstrapCssNode.getFileContent().uploadFile(inputStream,"text/css");
                    bootstrapCssNode.getSession().save();
                    IOUtils.closeQuietly(inputStream);
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
