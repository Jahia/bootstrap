<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="query" uri="http://www.jahia.org/tags/queryLib" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="propertyDefinition" type="org.jahia.services.content.nodetypes.ExtendedPropertyDefinition"--%>
<%--@elvariable id="type" type="org.jahia.services.content.nodetypes.ExtendedNodeType"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<c:if test="${renderContext.mode != 'studiovisual'}">
<template:addResources type="javascript"
                       resources="jquery.min.js,jquery.blockUI.js,workInProgress.js,codemirror/lib/codemirror.js,codemirror/mode/less/less.js"/>
<template:addResources type="css" resources="codemirror/codemirror.css"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>

<h2 id="header${renderContext.mainResource.node.identifier}"><fmt:message key="siteSettings.label.bootstrap"/></h2>

<c:set var="selectedFile" value="variables.less"/>
<c:if test="${not empty param.fileNameSelector}">
    <c:set var="selectedFile" value="${param.fileNameSelector}"/>
</c:if>
<template:tokenizedForm>
    <form id="fileSelector" method="get" action="<c:url value='${url.base}${renderContext.site.path}.bootstrapCustomization.html'/>">
        <select name="fileNameSelector">
            <query:definition var="listQuery" statement="select * from [jnt:file] where ischildnode([${renderContext.site.path}/files/less]) order by localname() asc" />
            <jcr:jqom var="result" qomBeanName="listQuery"/>
            <c:forEach items="${result.nodes}" var="fileNodes">
                <option value="${fileNodes.name}" <c:if test="${selectedFile eq fileNodes.name}">selected="selected" </c:if>>${fileNodes.displayableName}</option>
            </c:forEach>
        </select>
        <button class="btn btn-primary" type="submit"><fmt:message key='siteSettings.bootstrap.select.file'/></button>
    </form>
</template:tokenizedForm>
<template:tokenizedForm disableXSSFiltering="true">
    <form id="customizeBootstrap${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.customizeBootstrap.do'/>" method="post">
        <div id="customizeButtons${renderContext.mainResource.node.identifier}">
            <label class="checkbox pull-left" style="margin-right: 5px">
                <jcr:node path="${renderContext.site.path}/files/less/bootstrap.less" var="bootstrapFile"/>
                <input type="checkbox" name="responsive"
                       value="true"${fn:contains(bootstrapFile.fileContent.text,"\"responsive.less\"" ) ? ' checked="checked"' : ''} /><fmt:message
                    key="jmix_bootstrapSite.responsive" />
            </label>
            <button class="btn btn-primary" type="submit" name="save" onclick="workInProgress('${i18nWaiting}')">
                <i class="icon-ok icon-white"></i> <fmt:message key='siteSettings.bootstrap.saveAndCompile'><fmt:param value="${selectedFile}"/></fmt:message>
            </button>
            <jcr:node path="${renderContext.site.path}/files/bootstrap" var="bootstrapFolder"/>
            <c:set var="needPublication" value="${jcr:needPublication(bootstrapFolder, null, false, true, true)}"/>
            <button class="btn${needPublication ? '' : ' disabled'}" type="button" name="publish"
                    <c:if test="${needPublication}">onclick="workInProgress('${i18nWaiting}'); $('#publishBootstrap${renderContext.mainResource.node.identifier}').submit()"</c:if>>
                <i class="icon-globe"></i> <fmt:message key='siteSettings.bootstrap.publishCSS'/>
            </button>
        </div>
        <h2 id="currentFile${renderContext.mainResource.node.identifier}"><fmt:message key="siteSettings.bootstrap.currentFile" /> : <span class="text-success">${selectedFile}</span></h2>
        <jcr:node path="${renderContext.site.path}/files/less/${selectedFile}/jcr:content" var="lessVariables"/>
        <jcr:nodeProperty node="${lessVariables}" name="jcr:data" var="data"/>
        <textarea id="variables" name="variables"><c:out value="${data.string}"/></textarea>
        <input type="hidden" name="jcrRedirectTo"
               value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat"
               value="<c:url value='${renderContext.mainResource.template}.html'/>">
        <input type="hidden" value="${selectedFile}" name="selectedFile"/>
    </form>
</template:tokenizedForm>
<template:tokenizedForm>
    <form id="publishBootstrap${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.publishBootstrap.do'/>" method="post">
        <input type="hidden" name="jcrRedirectTo"
               value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat"
               value="<c:url value='${renderContext.mainResource.template}.html'/>">
    </form>
</template:tokenizedForm>
<script type="text/javascript">
    var myCodeMirror = CodeMirror.fromTextArea(document.getElementById("variables"),
            {mode: "less", lineNumbers: true, matchBrackets: true});

    function setCodeMirrorSize() {
        myCodeMirror.setSize("100%", $(window).height() - $(".page-header").outerHeight(true) -
                                     $("#header${renderContext.mainResource.node.identifier}").outerHeight(true) -
                                     $("#customizeButtons${renderContext.mainResource.node.identifier}").outerHeight(true) -
                                     $("#currentFile${renderContext.mainResource.node.identifier}").outerHeight(true) -
                                     $("#publishBootstrap${renderContext.mainResource.node.identifier}").outerHeight(true) -
                                     $("#fileSelector").outerHeight(true));
    }
    setCodeMirrorSize();
    $(window).resize(setCodeMirrorSize);
</script>
</c:if>
<c:if test="${renderContext.mode == 'studiovisual'}">
${fn:escapeXml(currentNode.displayableName)}
</c:if>