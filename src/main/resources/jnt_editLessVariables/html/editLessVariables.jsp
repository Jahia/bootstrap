<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<template:addResources type="javascript" resources="jquery.min.js,codemirror/lib/codemirror.js,codemirror/mode/less/less.js"/>
<template:addResources type="css" resources="codemirror/codemirror.css"/>

<h2><fmt:message key="siteSettings.label.bootstrap"/></h2>

<template:tokenizedForm>
    <form id="customizeBootstrap${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.customizeBootstrap.do'/>" method="post">
        <jcr:node path="${renderContext.site.path}/files/less/variables.less/jcr:content" var="lessVariables"/>
        <jcr:nodeProperty node="${lessVariables}" name="jcr:data" var="data"/>
        <textarea id="variables" name="variables"><c:out value="${data.string}"/></textarea>
        <input type="hidden" name="jcrRedirectTo" value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat" value="<c:url value='${renderContext.mainResource.template}.html'/>">
        <div style="margin-top: 10px">
            <label class="checkbox">
                <input type="checkbox" name="responsive" value="true"${renderContext.site.properties.responsive.boolean ? ' checked="checked"' : ''} /><fmt:message key="jmix_bootstrapSite.responsive" />
            </label>
            <button class="btn btn-primary" type="submit" name="save" onclick="workInProgress()">
                <i class="icon-ok icon-white"></i> <fmt:message key='label.save'/>
            </button>
            <jcr:node path="${renderContext.site.path}/files/bootstrap" var="bootstrapFolder"/>
            <c:set var="needPublication" value="${jcr:needPublication(bootstrapFolder, null, false, true, true)}"/>
            <button class="btn${needPublication ? '' : ' disabled'}" type="button" name="publish"
                    <c:if test="${needPublication}">onclick="workInProgress(); $('#publishBootstrap${renderContext.mainResource.node.identifier}').submit()"</c:if>>
                <i class="icon-globe icon-white"></i> <fmt:message key='label.publish'/>
            </button>
        </div>
    </form>
</template:tokenizedForm>
<template:tokenizedForm>
    <form id="publishBootstrap${renderContext.mainResource.node.identifier}"
          action="<c:url value='${url.base}${renderContext.site.path}.publishBootstrap.do'/>" method="post">
        <input type="hidden" name="jcrRedirectTo" value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
        <input type="hidden" name="jcrNewNodeOutputFormat" value="<c:url value='${renderContext.mainResource.template}.html'/>">
    </form>
</template:tokenizedForm>
<script type="text/javascript">
    var myCodeMirror = CodeMirror.fromTextArea(document.getElementById("variables"), {mode:"less", lineNumbers:true, matchBrackets:true});

    function setCodeMirrorSize() {
        myCodeMirror.setSize("100%", $(window).height() - 170);
    }
    setCodeMirrorSize();
    $(window).resize(setCodeMirrorSize);
</script>
