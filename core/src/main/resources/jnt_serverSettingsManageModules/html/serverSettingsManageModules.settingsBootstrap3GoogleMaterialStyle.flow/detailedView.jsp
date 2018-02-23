<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="flowRequestContext" type="org.springframework.webflow.execution.RequestContext"--%>
<%--@elvariable id="otherVersions" type="java.util.Map<org.jahia.services.templates.ModuleVersion,org.jahia.data.templates.JahiaTemplatesPackage>"--%>
<%--@elvariable id="bundleInfo" type="java.util.Map<java.lang.String, java.lang.String>"--%>
<%--@elvariable id="activeVersion" type="org.jahia.data.templates.JahiaTemplatesPackage"--%>
<c:set var="developmentMode" value="<%= SettingsBean.getInstance().isDevelopmentMode() %>"/>
<template:addResources type="javascript" resources="jquery.js,jquery-ui.min.js,jquery.blockUI.js,workInProgress.js"/>
<template:addResources type="css" resources="jquery-ui.smoothness.css,jquery-ui.smoothness-jahia.css"/>

<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>

<template:addResources>
<script type="text/javascript">
    $(document).ready(function() { $('.button-download').click(function() { workInProgress('${i18nWaiting}') }) });
</script>
</template:addResources>

<c:set value="${renderContext.editModeConfigName eq 'studiomode' or renderContext.editModeConfigName eq 'studiovisualmode'}" var="isStudio"/>

<template:addResources>
<script type="text/javascript">
    $.fn.bootstrapBtn = $.fn.button.noConflict();
    $(function() {
        var selectedForm;
        $("#disable-confirm").dialog({
            autoOpen: false,
            closeText: "",
            resizable: false,
            height:300,
            modal: true,
            open: function(event, ui) {
                $('#cancelDialog').focus();
            },
            buttons: [
                {
                    text: "<fmt:message key='serverSettings.manageModules.module.disable.purgeContent.button.yes'/>",
                    click: function() {
                        $('#'+selectedForm + ' input[name=purge]').val(true);
                        $('#'+selectedForm).submit();
                        $( this ).dialog( "close" );
                    }
                },
                {
                    text: "<fmt:message key='serverSettings.manageModules.module.disable.purgeContent.button.no'/>",
                    click: function () {
                        $('#' + selectedForm).submit();
                        $(this).dialog("close");
                    }
                },
                {
                    id: "cancelDialog",
                    text: "<fmt:message key='label.cancel'/>",
                    click: function() {
                        $(this).dialog("close");
                    }
                }]
        });
        $(".disable-button").click(function() {
            selectedForm = 'disable' + $(this).attr('id').replace("disableButton-","");
            $( "#disable-confirm" ).dialog( "open" );
        });
    });
</script>
</template:addResources>

<div id="disable-confirm" title="<fmt:message key='serverSettings.manageModules.module.disable.purgeContent.title'/>">
    <p><span class="ui-icon ui-icon-alert" style="float: left; margin: 0 7px 20px 0;"></span><fmt:message key="serverSettings.manageModules.module.disable.purgeContent.message"/></p>
</div>

<form id="viewInstalledModulesForm" action="${flowExecutionUrl}" method="POST">
    <input type="hidden" name="_eventId" value="viewInstalledModules"/>
    <button class="btn" name="_eventId_viewInstalledModules"><i class=" icon-chevron-left"></i>&nbsp;<fmt:message key="backToPreviousPage"/></button>
</form>

<c:if test="${not empty otherVersions}">
    <c:forEach items="${otherVersions}" var="version">
        <c:if test="${version.value.state.state eq 'STARTED'}">
            <c:set var="hasStartedVersion" value="true"/>
        </c:if>
        <c:if test="${not empty version.value.sourcesFolder}">
            <c:set var="sourcesFound" value="${version.key}"/>
        </c:if>
    </c:forEach>
</c:if>

<div id="detailActiveVersion" class="panel panel-default">
    <div class="panel-heading">
        <h2><fmt:message key="serverSettings.manageModules"/> - ${activeVersion.name}&nbsp;${activeVersion.version}</h2>
    </div>
    <div class="panel-body">
        <p>
            ${fn:escapeXml(bundleInfo['Bundle-Description'])}
        </p>
        <c:forEach items="${flowRequestContext.messageContext.allMessages}" var="message">
            <c:if test="${message.severity eq 'INFO'}">
                <div class="alert alert-success">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                        ${message.text}
                </div>
            </c:if>
            <c:if test="${message.severity eq 'ERROR'}">
                <div class="alert alert-danger">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                        ${message.text}
                </div>
            </c:if>
        </c:forEach>
        <c:if test="${not empty error}">
            <div class="alert alert-danger">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                <fmt:message key="${error}"/>
            </div>
        </c:if>
        <table class="table table-striped table-bordered table-hover">
            <thead>
            <tr>
                <th style="width:17%">
                    <fmt:message key="serverSettings.manageModules.moduleId"/>
                </th>
                <th style="width:17%">
                    <fmt:message key="serverSettings.manageModules.groupId"/>
                </th>
                <th style="width:17%">
                    <fmt:message key="serverSettings.manageModules.module.state"/>
                </th>
                <th style="width:17%">
                    <fmt:message key="serverSettings.manageModules.module.type"/>
                </th>
                <th style="width:17%">
                    <fmt:message key="serverSettings.manageModules.module.author"/>
                </th>
    
                <c:if test="${developmentMode}">
                    <th style="width:17%">
                        <fmt:message key="serverSettings.manageModules.module.source.uri"/>
                    </th>
                </c:if>
    
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>
                    ${activeVersion.id}
                </td>
                <td>
                    ${activeVersion.groupId}
                </td>
                <td>
                    <fmt:message key="serverSettings.manageModules.module.state.${fn:toLowerCase(activeVersion.state.state)}"/>
                </td>
                <td>
                    ${activeVersion.moduleType}
                </td>
                <td>
                    ${fn:escapeXml(activeVersion.provider)}
                </td>
                <c:if test="${developmentMode}">
                    <c:set value="${functions:contains(systemSiteRequiredModules, activeVersion.id)}" var="isMandatoryDependency"/>
                    <c:set value="${activeVersion.sourcesDownloadable and not isMandatoryDependency}" var="sourcesDownloadable"/>
                    <fmt:message var="i18nDownloadSources" key='serverSettings.manageModules.downloadSources'/>
                    <td>
                        <c:if test="${not isStudio and moduleStates[activeVersion.id][activeVersion.version].installed}">
                        <c:choose>
                            <c:when test="${not empty activeVersion.sourcesFolder}">
                                <c:url var="urlToStudio" value="/cms/studio/${currentResource.locale}/modules/${activeVersion.id}.html"/>
                                <button class="btn btn-block" type="button" onclick='window.parent.location.assign("${urlToStudio}")'>
                                    <i class="icon-circle-arrow-right"></i>
                                    &nbsp;<fmt:message key='serverSettings.manageModules.goToStudio' />
                                </button>
                            </c:when>
                            <c:when test="${not empty sourcesFound}">
                                <fmt:message key='serverSettings.manageModules.module.source.notAvailable' >
                                    <fmt:param value="${sourcesFound}" />
                                </fmt:message>
                            </c:when>
                            <c:when test="${not sourcesDownloadable}">
                                <fmt:message key="serverSettings.manageModules.notDownloadable"/>
                            </c:when>
                            <c:when test="${not empty activeVersion.scmURI}">
                                <c:if test="${functions:contains(sourceControls, fn:substringBefore(fn:substringAfter(activeVersion.scmURI, ':'),':'))}">
                                    <c:choose>
                                        <c:when test="${hasStartedVersion}">
                                            <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                                <input type="hidden" name="module" value="${activeVersion.id}"/>
                                                <input type="hidden" name="scmUri" value="${activeVersion.scmURI}"/>
                                                <input type="hidden" name="version" value="${activeVersion.version}"/>
                                                <input type="hidden" name="branchOrTag" value="${activeVersion.scmTag}"/>
                                                <button class="btn btn-block button-download" type="submit" name="_eventId_downloadSources" onclick="">
                                                    <i class="icon-download"></i>
                                                    &nbsp;${i18nDownloadSources}
                                                </button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <fmt:message key="serverSettings.manageModules.noStartedVersion"/>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                            </c:when>
    
                            <c:otherwise>
                                <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                    <input type="hidden" name="module" value="${activeVersion.id}"/>
                                    <input type="hidden" name="scmUri" value="scm:git:"/>
                                    <button class="btn btn-block" type="submit" name="_eventId_viewDownloadForm" onclick="">
                                        <i class="icon-download"></i>
                                        &nbsp;${i18nDownloadSources}
                                    </button>
                                </form>
                            </c:otherwise>
                        </c:choose>
                        </c:if>
    
                        <c:choose>
                            <c:when test="${not isMandatoryDependency and (not empty moduleStates[activeVersion.id][activeVersion.version].unresolvedDependencies or  not empty sitesTemplates[activeVersion.id] or not empty sitesDirect[activeVersion.id] or not empty sitesTransitive[activeVersion.id] or (empty activeVersion.sourcesFolder and not empty sourcesFound))}">
                                <%--<button class="btn btn-block button-download" disabled>--%>
                                    <%--<i class="icon-share"></i>--%>
                                    <%--&nbsp;<fmt:message key='serverSettings.manageModules.duplicateModule'/>--%>
                                <%--</button>--%>
                            </c:when>
                            <c:when test="${not empty activeVersion.sourcesFolder and not isMandatoryDependency}">
                                <form style="margin: 0;" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
                                    <input type="hidden" name="moduleName" value="${activeVersion.name}"/>
                                    <input type="hidden" name="moduleId" value="${activeVersion.id}"/>
                                    <input type="hidden" name="groupId" value="${activeVersion.groupId}"/>
                                    <input type="hidden" name="version" value="${activeVersion.version}"/>
                                    <input type="hidden" name="srcPath" value="${activeVersion.sourcesFolder.path}"/>
                                    <button class="btn btn-block button-download" type="submit" name="_eventId_duplicateModuleForm">
                                        <i class="icon-share"></i>
                                        &nbsp;<fmt:message key='serverSettings.manageModules.duplicateModule'/>
                                    </button>
                                </form>
                            </c:when>
                            <c:when test="${not empty activeVersion.scmURI and sourcesDownloadable}">
                                <c:if test="${functions:contains(sourceControls, fn:substringBefore(fn:substringAfter(activeVersion.scmURI, ':'),':')) and hasStartedVersion}">
                                    <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                        <input type="hidden" name="moduleName" value="${activeVersion.name}"/>
                                        <input type="hidden" name="moduleId" value="${activeVersion.id}"/>
                                        <input type="hidden" name="groupId" value="${activeVersion.groupId}"/>
                                        <input type="hidden" name="version" value="${activeVersion.version}"/>
                                        <input type="hidden" name="scmUri" value="${activeVersion.scmURI}"/>
                                        <input type="hidden" name="branchOrTag" value="${activeVersion.scmTag}"/>
                                        <button class="btn btn-block button-download" type="submit" name="_eventId_downloadTempSources">
                                            <i class="icon-share"></i>
                                            &nbsp;<fmt:message key='serverSettings.manageModules.duplicateModule'/>
                                        </button>
                                    </form>
                                </c:if>
                            </c:when>
                        </c:choose>
                    </td>
                </c:if>
            </tr>
            </tbody>
        </table>


    <c:if test="${not empty otherVersions}">
        <h3><fmt:message key="serverSettings.manageModules.versions"/></h3>
        <%@include file="common/moduleLabels.jspf" %>
        <table class="table table-striped table-bordered table-hover">
            <thead>
            <tr>
                <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.version"/></th>
                <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.state"/></th>
                <c:if test="${not isStudio}">
                    <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.manage"/></th>
                </c:if>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${otherVersions}" var="version">
                <tr>
                    <td>${version.key}</td>
                    <td>
                        <fmt:message key="serverSettings.manageModules.module.state.${fn:toLowerCase(version.value.state.state)}"/>
                    </td>
                    <c:if test="${not isStudio}">
                        <td>
                         <c:set var="isActiveVersion" value="${version.key == activeVersion.version}"/>
                            <c:set var="showWiring" value="true"/>
                            <%@include file="common/moduleVersionActions.jspf" %>
                        </td>
                    </c:if>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
    
    <h3><fmt:message key="serverSettings.manageModules.sites.management"/></h3>
    
    <table class="table table-striped table-bordered table-hover">
        <thead>
        <tr>
            <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.site"/></th>
            <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.dependency.type"/></th>
            <th style="width:33%"><fmt:message key="serverSettings.manageModules.module.manage"/></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${sites}" var="site" varStatus="status">
            <c:if test="${site eq 'systemsite' or activeVersion.moduleType ne 'system'}">
            <tr>
                <td>${site}</td>
                <td>
                    <c:choose>
                        <c:when test="${not empty sitesDirect[activeVersion.id] and functions:contains(sitesDirect[activeVersion.id],site)}">
                            <fmt:message key="serverSettings.manageModules.usedInSites.direct"/>
                        </c:when>
                        <c:when test="${not empty sitesTemplates[activeVersion.id] and functions:contains(sitesTemplates[activeVersion.id],site)}">
                            <fmt:message
                                    key="serverSettings.manageModules.usedInSites.templates"/>
                        </c:when>
                        <c:when test="${not empty sitesTransitive[activeVersion.id] and functions:contains(sitesTransitive[activeVersion.id],site)}">
                            <fmt:message
                                    key="serverSettings.manageModules.usedInSites.transitive"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="serverSettings.manageModules.module.no.dependency"/>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <c:set var="cellEmpty" value="true"/>
                    <c:if test="${activeVersion.moduleType ne 'templatesSet' and moduleStates[activeVersion.id][activeVersion.version].installed}">
                        <c:choose>
                            <c:when test="${not empty sitesDirect[activeVersion.id] and functions:contains(sitesDirect[activeVersion.id],site)}">
                                <form id="disable${status.index}" style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                    <input type="hidden" name="module" value="${activeVersion.id}"/>
                                    <input type="hidden" name="disableFrom" value="/sites/${site}"/>
                                    <input type="hidden" name="purge" value="false"/>
                                    <input type="hidden" name="_eventId_disable" value="true"/>
                                    <c:if test="${site ne 'systemsite' or not moduleStates[activeVersion.id][activeVersion.version].systemDependency}">
                                    <fmt:message var="label"
                                                 key='serverSettings.manageModules.module.disable'/>
                                    <button class="btn btn-danger disable-button" type="button" onclick="" id="disableButton-${status.index}">
                                        <i class=" icon-stop icon-white"></i>&nbsp;${label}
                                    </button>
                                    </c:if>
                                    <c:set var="usedOnce" value="true"/>
                                </form>
                                <c:set var="cellEmpty" value="false"/>
                            </c:when>
                            <c:otherwise>
                                <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                    <input type="hidden" name="module" value="${activeVersion.id}"/>
                                    <input type="hidden" name="version" value="${activeVersion.version}"/>
                                    <input type="hidden" name="enableOn" value="/sites/${site}"/>
                                    <c:if test="${site ne 'systemsite' or not moduleStates[activeVersion.id][activeVersion.version].systemDependency}">
                                    <fmt:message var="label"
                                                 key='serverSettings.manageModules.module.enable'/>
                                    <button class="btn btn-success" type="submit" name="_eventId_enable" onclick="">
                                        <i class=" icon-play icon-white"></i>
                                        &nbsp;${label}
                                    </button>
                                    </c:if>
                                </form>
                                <c:set var="cellEmpty" value="false"/>
                            </c:otherwise>
                        </c:choose>
                    </c:if>
                    <c:if test="${cellEmpty}">&nbsp;</c:if>
                </td>
            </tr>
            </c:if>
        </c:forEach>
        <c:if test="${not empty usedOnce}">
            <tr>
                <td align="right" colspan="3">
                    <form id="disableAll" style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                        <input type="hidden" name="module" value="${activeVersion.id}"/>
                        <input type="hidden" name="purge" value="false"/>
                        <input type="hidden" name="_eventId_disableAll" value="true"/>
                        <fmt:message var="label"
                                     key='serverSettings.manageModules.module.disable.all'/>
                        <button class="btn btn-danger disable-button" type="button" onclick="" id="disableButton-All">
                            <i class="icon-ban-circle icon-white"></i>&nbsp;${label}
                        </button>
                    </form>
                </td>
            </tr>
        </c:if>
        </tbody>
    </table>
    
    
    
    
    <%--@elvariable id="nodeTypes" type="java.util.Map<java.lang.String,java.lang.Boolean>"--%>
    <c:if test="${not empty nodeTypes}">
        <h3><fmt:message key="serverSettings.manageModules.module.nodetypes"/></h3>
        <table class="table table-striped table-bordered table-hover">
            <thead>
            <tr>
                <th style="width:50%"><fmt:message key="serverSettings.manageModules.module.nodetype.name"/></th>
                <th style="width:50%"><fmt:message key='serverSettings.manageModules.module.nodetype.component'/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${nodeTypes}" var="nodeType">
                <tr>
                    <td><span style="font: bold">${nodeType.key}</span></td>
                    <td>
                        <c:choose>
                            <c:when test="${nodeType.value}">
                                <fmt:message key="label.yes"/>
                            </c:when>
                            <c:otherwise>
                                <fmt:message key="label.no"/>
                            </c:otherwise>
                        </c:choose>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
    
    <c:if test="${not empty activeVersion.dependencies}">
        <h3><fmt:message key="serverSettings.manageModules.module.dependencies"/></h3>
        <table class="table table-striped table-bordered table-hover">
            <thead>
            <tr>
                <th style="width:50%"><fmt:message key="serverSettings.manageModules.module.dependency.name"/></th>
                <th style="width:50%"><fmt:message key='serverSettings.manageModules.details'/></th>
            </tr>
            </thead>
            <tbody>
            <c:forEach items="${activeVersion.dependencies}" var="dependency">
                <tr>
                    <td><span style="font: bold">${dependency.name}</span></td>
                    <td>
                        <c:if test="${isStudio and not empty dependency.sourcesFolder}">
                            <c:set var="urlDependencyDetails" value="${url.base}/modules/${dependency.id}.html"/>
                            <button class="btn btn-info" type="button" onclick='window.location.assign("${urlDependencyDetails}")'>
                                <i class="material-icons">info_outline</i>
                            </button>
                        </c:if>
                        <c:if test="${not isStudio}">
                            <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                <input type="hidden" name="selectedModule" value="${dependency.id}"/>
                                <button class="btn btn-info" type="submit" name="_eventId_viewDetails" onclick="">
                                    <i class="material-icons">info_outline</i>
                                </button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>
    <c:if test="${not empty dependantModules}">
        <h3><fmt:message key="serverSettings.manageModules.module.dependantModules"/></h3>
        <table class="table table-striped table-bordered table-hover">
            <thead>
            <tr>
                <th style="width:50%"><fmt:message key="serverSettings.manageModules.module.dependency.name"/></th>
                <th style="width:50%"><fmt:message key='serverSettings.manageModules.details'/></th>
            </tr>
            </thead>
            <tbody>
    
            <c:forEach items="${dependantModules}" var="dependency">
                <tr>
                    <td><span style="font: bold">${dependency.name}</span></td>
                    <td>
                        <c:if test="${isStudio}">
                            <c:choose>
                                <c:when test="${not empty dependency.sourcesFolder}">
                                    <c:url var="urlDependencyDetails" value="${url.base}/modules/${dependency.id}.html"/>
                                    <button class="btn btn-info" type="button" onclick='window.location.assign("${urlDependencyDetails}")'>
                                        <i class="material-icons">info_outline</i>
                                    </button>
                                </c:when>
                                <c:otherwise>&nbsp;</c:otherwise>
                            </c:choose>
                        </c:if>
                        <c:if test="${not isStudio}">
                            <form style="margin: 0;" action="${flowExecutionUrl}" method="POST">
                                <input type="hidden" name="selectedModule" value="${dependency.id}"/>
                                <button class="btn btn-info" type="submit" name="_eventId_viewDetails" onclick="">
                                    <i class="material-icons">info_outline</i>
                                </button>
                            </form>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </c:if>

    </div>
</div>