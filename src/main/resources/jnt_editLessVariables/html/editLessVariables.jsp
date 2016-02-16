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
    <template:addResources type="javascript" resources="jquery.min.js,jquery.blockUI.js,workInProgress.js"/>
    <template:addResources type="javascript" resources="jscolor.min.js"/>
    <fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
    <fmt:message key="siteSettings.bootstrap.reset.confirm" var="resetConfirm"/>

    <c:set var="formview" value="${currentNode.properties.formview.string}"/>
    <c:if test="${formview ne 'advanced'}">
        <c:set var="switchFormUrl" value="${url.base}${renderContext.mainResource.node.path}.${renderContext.mainResource.template}.html"/>
        <c:if test="${empty param.formview}">
            <a class="btn btn-small pull-right" href="<c:url value='${switchFormUrl}?formview=advanced'/>"><i class="icon-resize-full"></i> <fmt:message key="siteSettings.label.bootstrap.form.advanced"/></a>
        </c:if>
        <c:if test="${not empty param.formview}">
            <a class="btn btn-small pull-right" href="<c:url value='${switchFormUrl}'/>"><i class="icon-resize-small"></i> <fmt:message key="siteSettings.label.bootstrap.form.simple"/></a>
        </c:if>
    </c:if>

    <div class="page-header">
        <jcr:node path="/modules/${renderContext.site.templatePackage.idWithVersion}/templates" var="templatesNode"/>
        <h1 id="header${renderContext.mainResource.node.identifier}"><fmt:message key="siteSettings.label.bootstrap"/><small style="color:black;"> - Bootstrap version ${templatesNode.properties['bootstrapVersion'].string eq 'less3' ? '3' : '2'}</small></h1>
    </div>


    <div class="box-1">
        <form id="customizeBootstrap${renderContext.mainResource.node.identifier}" class="form-horizontal"
              action="<c:url value='${url.base}${renderContext.site.path}.customizeBootstrap.do'/>" method="post"
              onsubmit="workInProgress('${i18nWaiting}')">

            <c:if test="${not empty param.formview}">
                <c:set var="formview" value="${param.formview}"/>
            </c:if>
            <c:if test="${formview ne 'default'}">
                <template:include view="${formview}" />
            </c:if>

            <input type="hidden" name="jcrRedirectTo"
                   value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
            <input type="hidden" name="jcrNewNodeOutputFormat"
                   value="<c:url value='${renderContext.mainResource.template}.html'/>">

            <div class="form-actions">
                <button class="btn btn-primary" type="submit">
                    <i class="icon-ok icon-white"></i> <fmt:message key='siteSettings.bootstrap.saveAndCompile'/>
                </button>
                <button class="btn btn-danger" type="button" onclick="if (confirm('${resetConfirm}')) {$('#resetVariables${renderContext.mainResource.node.identifier}').submit()}">
                    <i class="icon-refresh icon-white"></i> <fmt:message key='siteSettings.bootstrap.reset'/>
                </button>
                <jcr:node path="${renderContext.site.path}/files/bootstrap" var="bootstrapFolder"/>
                <c:set var="needPublication" value="${jcr:needPublication(bootstrapFolder, null, false, true, true)}"/>
                <button class="btn${needPublication ? '' : ' disabled'}" type="button" name="publish"
                        <c:if test="${needPublication}">onclick="$('#publishBootstrap${renderContext.mainResource.node.identifier}').submit()"</c:if>>
                    <i class="icon-globe"></i> <fmt:message key='siteSettings.bootstrap.publishCSS'/>
                </button>
            </div>
        </form>
    </div>

    <form id="resetVariables${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.customizeBootstrap.do'/>" method="post"
          onsubmit="workInProgress('${i18nWaiting}')">
        <input type="hidden" name="resetVariables" value="true"/>
        <input type="hidden" name="jcrRedirectTo"
               value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat"
               value="<c:url value='${renderContext.mainResource.template}.html'/>">
    </form>

    <form id="publishBootstrap${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.publishBootstrap.do'/>" method="post"
          onsubmit="workInProgress('${i18nWaiting}')">
        <input type="hidden" name="jcrRedirectTo"
               value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat"
               value="<c:url value='${renderContext.mainResource.template}.html'/>">
    </form>

</c:if>
<c:if test="${renderContext.mode == 'studiovisual'}">
    ${fn:escapeXml(currentNode.displayableName)}
</c:if>