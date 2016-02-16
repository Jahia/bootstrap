import org.apache.commons.io.IOUtils
import org.apache.taglibs.standard.functions.Functions
import org.jahia.data.templates.JahiaTemplatesPackage
import org.jahia.modules.bootstrap.actions.CustomizeBootstrapAction
import org.jahia.modules.bootstrap.rules.BootstrapCompiler
import org.jahia.registries.ServicesRegistry
import org.jahia.services.content.JCRNodeWrapper
import org.jahia.services.content.decorator.JCRSiteNode
import org.jahia.services.templates.JahiaTemplateManagerService
import org.jahia.services.templates.TemplatePackageRegistry
import org.springframework.core.io.Resource

import java.text.CharacterIterator
import java.text.StringCharacterIterator

def getVariables(JahiaTemplatesPackage aPackage, String resource) {
    Resource r = aPackage.getResource(resource)
    if (r != null && r.exists()) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(r.getInputStream(), writer);
        return writer.toString();
    }
    return null
}

def escpaceXMLAndAddCodeTag(String text) {
    StringBuilder stringBuilder = new StringBuilder();
    CharacterIterator it = new StringCharacterIterator(text)
    def isFirst = true;
    for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()) {
        if (ch == '`') {
            if (isFirst) {
                stringBuilder.append("<code>")
                isFirst = false
            } else {
                stringBuilder.append("</code>")
                isFirst = true
            }
        } else {
            stringBuilder.append(ch);
        }
    }
    return stringBuilder.toString()
}

def printInput(String line, JCRNodeWrapper variablesNode) {
    matcher = (line =~ /@([\w-]+):\s+([^;]+);(?:\s*\/\/\s*(.+))?/)
    if (matcher.matches()) {
        println '<div class="control-group">'

        String variableName = matcher[0][1]
        println '<label class="control-label">' + variableName + '</label>'

        def value
        if (variablesNode != null && variablesNode.hasProperty(variableName)) {
            value = variablesNode.getProperty(variableName).getString()
        } else {
            value = matcher[0][2]
        }

        println '<div class="controls">'
        if (value.startsWith("#")) {
            println '<input class="input-xxlarge jscolor {hash:true, refine:false, required:false}" type="text" name="' + variableName + '" value="' + value.replace('"', '&quot;') + '"/>'
        } else {
            println '<input class="input-xxlarge" type="text" name="' + variableName + '" value="' + value.replace('"', '&quot;') + '"/>'
        }
        if (matcher[0][3] != null) {
            println '<span class="help-inline">' + matcher[0][3] + '</span>'
        }
        println '</div>'

        println '</div>'
    }
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
String lessRessoucesfolder = "less2"
for (JahiaTemplatesPackage aPackage : packages) {
    variables = getVariables(aPackage,"less/variables.less")
    if (variables != null) {
        break;
    }
}

if (variables == null) {
    if (bootstrapModule != null) {
        JCRNodeWrapper templatesSetNode = site.session.getNode("/modules/" + site.getTemplatePackage().getIdWithVersion());
        lessRessoucesfolder = templatesSetNode.hasNode("templates") && templatesSetNode.getNode("templates").hasProperty("bootstrapVersion")?templatesSetNode.getNode("templates").getPropertyAsString("bootstrapVersion"):BootstrapCompiler.defaultLessRessoucesfolder;
        variables = getVariables(bootstrapModule, lessRessoucesfolder + "/variables.less")
    }
}

if (variables != null) {
    JCRNodeWrapper variablesNode
    if (site.hasNode(CustomizeBootstrapAction.BOOTSTRAP_VARIABLES)) {
        variablesNode = site.getNode(CustomizeBootstrapAction.BOOTSTRAP_VARIABLES)
    }

    def isFieldsetOpen = false
    if (lessRessoucesfolder.equals("less2")) {
        def previousLine
        variables.eachLine { line, count ->
            if (line.startsWith("//") && line.length() > 2) {
                def text = line.substring(2).trim()
                if (previousLine != null) {
                    if (text == "--------------------------------------------------") {
                        if (isFieldsetOpen) {
                            println "</fieldset>"
                            isFieldsetOpen = false
                        }
                        println "<h3>$previousLine</h3>"
                        previousLine = null
                    } else if (text == "-------------------------") {
                        if (isFieldsetOpen) {
                            println "</fieldset>"
                        }
                        println "<fieldset>"
                        println "<legend>$previousLine</legend>"
                        isFieldsetOpen = true
                        previousLine = null
                    } else {
                        previousLine += "<br/>\n" + text
                    }
                } else {
                    previousLine = text
                }
            } else {
                if (previousLine != null) {
                    println '<span class="help-block"><em>' + previousLine + '</em></span>'
                    previousLine = null
                }
                if (line.startsWith("@")) {
                    printInput(line, variablesNode)
                }
            }
        }
        if (isFieldsetOpen) {
            println "</fieldset>"
        }
    } else if (lessRessoucesfolder.equals("less3")) {
        println '<div class="accordion" id="accordionParent">'
        def isFirst = true
        variables.eachLine { line, count ->
            if (line.startsWith("//") &&  line.length() > 2
                && !line.startsWith("// TODO") && !line.startsWith("//==") && !line.startsWith("//##") && !line.startsWith("//**") && !line.startsWith("// -") && !line.startsWith("//--")) {
                def text = line.substring(3).trim()
                println '<h3>' + text + '</h3>'
            } else if ((line.startsWith("//==") || line.startsWith("//--")) && line.length() > 4) {
                if (!isFirst) {
                    if (isFieldsetOpen) {
                        println '</fieldset>'
                        isFieldsetOpen = false
                    }
                    println '</div>'
                    println '</div>'
                    println '</div>'
                }
                def text = line.substring(5).trim()
                println '<div class="accordion-group">'
                println '<div class="accordion-heading">'
                println '<a class="accordion-toggle" data-toggle="collapse" data-parent="#accordionParent" href="#collapse' + count + '">'
                println Functions.escapeXml(text)
                println '</a>'
                println '</div>'
                if (isFirst) {
                    println '<div id="collapse' + count + '" class="accordion-body collapse in">'
                } else {
                    println '<div id="collapse' + count + '" class="accordion-body collapse">'
                }
                println '<div class="accordion-inner">'
                isFirst = false
            } else if (line.startsWith("//##") && line.length() > 4) {
                if (isFieldsetOpen) {
                    println '</fieldset>'
                }
                def text = line.substring(5).trim()
                println '<fieldset>'
                println '<legend>' + Functions.escapeXml(text) + '</legend>'
                isFieldsetOpen = true
            } else if (line.startsWith("//**") && line.length() > 4) {
                def text = line.substring(5).trim()
                text = Functions.escapeXml(text)
                text = escpaceXMLAndAddCodeTag(text)
                println '<span class="help-block"><em>' + text + '</em></span>'

            } else if (line.startsWith("@")) {
                printInput(line, variablesNode)
            }
        }
        if (isFieldsetOpen) {
            println '</fieldset>'
            isFieldsetOpen = false
        }
        println '</div>'
        println '</div>'
        println '</div>'
        println '</div>'
    }
}
