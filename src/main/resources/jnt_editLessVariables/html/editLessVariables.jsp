<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<template:addResources type="javascript" resources="jquery.min.js,codemirror/lib/codemirror.js,codemirror/mode/less/less.js"/>
<template:addResources type="css" resources="codemirror/codemirror.css"/>

<script type="text/javascript">
    function resetVariables() {
        if (confirm('<fmt:message key="confirm.reset" />')) {
            var data = {};
            data["reset"] = "true";
            $.post("<c:url value="${url.base}${renderContext.site.path}.updateVariables.do"/>", data,
                    function (data, result) {
                        window.location.reload();
                    });
        }
    }
</script>

<h2><fmt:message key="siteSettings.label.bootstrap" /></h2>

<form action="<c:url value='${url.base}${renderContext.site.path}.updateVariables.do'/>" method="post">
    <jcr:node path="${renderContext.site.path}/files/variables.less/jcr:content" var="lessVariables"/>
    <jcr:nodeProperty node="${lessVariables}" name="jcr:data" var="data"/>
    <textarea id="variables" name="variables"><c:out value="${data.string}"/></textarea>
    <input type="hidden" name="jcrRedirectTo" value="<c:url value='${url.base}${renderContext.mainResource.node.path}'/>"/>
    <input type="hidden" name="jcrNewNodeOutputFormat" value="<c:url value='${renderContext.mainResource.template}.html'/>">
    <div style="margin-top: 16px; text-align: center">
        <button class="btn btn-primary" type="submit" name="save">
            <i class="icon-ok icon-white"></i> <fmt:message key='label.save'/>
        </button>
        <button class="btn btn-danger" type="button" name="reset" onclick="resetVariables()">
            <i class="icon-remove icon-white"></i> <fmt:message key='label.reset'/>
        </button>
    </div>
</form>
<script type="text/javascript">
    var myCodeMirror = CodeMirror.fromTextArea(document.getElementById("variables"), {mode:"less", lineNumbers:true, matchBrackets:true});

    function setCodeMirrorSize() {
        myCodeMirror.setSize("100%", $(window).height() - 150);
    }
    setCodeMirrorSize();
    $(window).resize(setCodeMirrorSize);
</script>
