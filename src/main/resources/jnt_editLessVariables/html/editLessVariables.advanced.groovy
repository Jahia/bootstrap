import org.apache.commons.io.IOUtils
import org.jahia.data.templates.JahiaTemplatesPackage
import org.jahia.modules.bootstrap.actions.CustomizeBootstrapAction
import org.jahia.modules.bootstrap.rules.BootstrapCompiler
import org.jahia.registries.ServicesRegistry
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.decorator.JCRSiteNode
import org.jahia.services.templates.JahiaTemplateManagerService
import org.jahia.services.templates.TemplatePackageRegistry
import org.springframework.core.io.Resource

def getVariables(JahiaTemplatesPackage aPackage, String resource) {
    Resource r = aPackage.getResource(resource)
    if (r != null && r.exists()) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(r.getInputStream(), writer);
        return writer.toString();
    }
    return null
}

JCRSiteNode site = renderContext.mainResource.node.resolveSite
JahiaTemplateManagerService jahiaTemplateManagerService = ServicesRegistry.getInstance().getJahiaTemplateManagerService()
Set<JahiaTemplatesPackage> packages = new TreeSet<JahiaTemplatesPackage>(TemplatePackageRegistry.TEMPLATE_PACKAGE_COMPARATOR);
for (String s : site.getInstalledModulesWithAllDependencies()) {
    packages.add(jahiaTemplateManagerService.getTemplatePackageById(s));
}

JahiaTemplatesPackage bootstrapModule = jahiaTemplateManagerService.getTemplatePackageById("bootstrap")
packages.remove(bootstrapModule);
def variables
for (JahiaTemplatesPackage aPackage : packages) {
    variables = getVariables(aPackage,"less/variables.less")
    if (variables != null) {
        break;
    }
}

if (variables == null) {
    if (bootstrapModule != null) {
        JCRNodeWrapper templatesSetNode = site.session.getNode("/modules/" + site.getTemplatePackage().getIdWithVersion());
        String lessRessoucesfolder = templatesSetNode.hasNode("templates") && templatesSetNode.getNode("templates").hasProperty("bootstrapVersion")?templatesSetNode.getNode("templates").getPropertyAsString("bootstrapVersion"):BootstrapCompiler.defaultLessRessoucesfolder;
        variables = getVariables(bootstrapModule, lessRessoucesfolder + "/variables.less")
    }
}

if (variables != null) {
    JCRNodeWrapper variablesNode
    if (site.hasNode(CustomizeBootstrapAction.BOOTSTRAP_VARIABLES)) {
        variablesNode = site.getNode(CustomizeBootstrapAction.BOOTSTRAP_VARIABLES)
    }

    def previousLine
    def fieldsetOpened = false
    variables.eachLine { line ->
        if (line.startsWith("//") && line.length() > 2) {
            text = line.substring(2).trim()
            if (previousLine != null) {
                if (text == "--------------------------------------------------") {
                    if (fieldsetOpened) {
                        println "</fieldset>"
                        fieldsetOpened = false
                    }
                    println "<h2>$previousLine</h2>"
                    previousLine = null
                } else if (text == "-------------------------") {
                    if (fieldsetOpened) {
                        println "</fieldset>"
                    }
                    println "<fieldset class=\"box-1\">"
                    println "<legend>$previousLine</legend>"
                    fieldsetOpened = true
                    previousLine = null
                } else {
                    previousLine += "<br/>\n" + text
                }
            } else {
                previousLine = text
            }
        } else {
            if (previousLine != null) {
                println '<span class="help-block">' + previousLine + '</span>'
                previousLine = null
            }
            if (line.startsWith("@")) {
                matcher = ( line =~ /@([\w-]+):\s+([^;]+);(?:\s*\/\/\s*(.+))?/ )
                if (matcher.matches()) {
                    println '<label><div class="row-fluid">'
                    def variableName = matcher[0][1]
                    println '<div class="span3">' + variableName + '</div>'
                    def value
                    if (variablesNode != null && variablesNode.hasProperty(variableName)) {
                        value = variablesNode.getProperty(variableName).getString()
                    } else {
                        value = matcher[0][2]
                    }
                    println '<div class="span6"><input type="text" name="' + variableName + '" value="' + value.replace('"', '&quot;') + '" class="span12" /></div>'
                    if (matcher[0][3] != null) {
                        println '<div class="span3"><span class="help-inline">' + matcher[0][3] + '</span></div>'
                    }
                    println '</div></label>'
                }
            }
        }
    }
    if (fieldsetOpened) {
        println "</fieldset>"
    }
}
