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
<c:set var="developmentMode" value="<%= SettingsBean.getInstance().isDevelopmentMode() %>"/>
<template:addResources type="javascript"
                       resources="jquery.min.js,jquery.blockUI.js,jquery.metadata.js,workInProgress.js,jquery.cuteTime.js"/>
<template:addResources type="javascript" resources="jquery.cuteTime.settings.${currentResource.locale}.js"/>
<template:addResources type="javascript"
                       resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js,settings/dataTables.initializer.js"/>
<template:addResources type="javascript" resources="jquery-ui.min.js"/>
<template:addResources type="javascript" resources="dt-module-manager.js"/>
<template:addResources type="css" resources="datatables/css/bootstrap-theme.css,tablecloth.css"/>
<template:addResources type="css" resources="manageModules.css"/>
<template:addResources type="css" resources="mdModuleManager.css"/>

<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/>
<c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
<fmt:message key="serverSettings.manageModules.checkForUpdates" var="i18nRefreshModules"/>
<fmt:message var="lastUpdateTooltip" key="serverSettings.manageModules.lastUpdate"/>
<fmt:message var="i18nSearchModule" key="serverSettings.manageModules.searchModule"/>
<fmt:message var="i18nRowsPerPage" key="serverSettings.manageModules.rowsPerPage"/>
<fmt:message var="i18nOf" key="serverSettings.manageModules.of"/>

<div class="mg-page-header">
    <h2><fmt:message key="serverSettings.manageModules"/></h2>
</div>

<c:set var="moduleTableId" value="module_table_${adminModuleTableUUID}"/>

<template:addResources>
    <script type="text/javascript">
        $(document).ready(function () {
            var tableId = "${moduleTableId}";

            function save_dt_view(oSettings, oData) {
                localStorage.setItem('DataTables_adminModulesView', JSON.stringify({id: tableId, data: oData}));
            }

            function load_dt_view(oSettings) {
                var item = localStorage.getItem('DataTables_adminModulesView');
                if (item) {
                    var itemJSON = JSON.parse(item);
                    if (itemJSON.data && itemJSON.id == tableId) {
                        return itemJSON.data;
                    }
                }
                return undefined;
            }

            var oldStart = 0;

            var customOptions = {
                "fnStateSave": function (oSettings, oData) {
                    save_dt_view(oSettings, oData);
                },
                "fnStateLoad": function (oSettings) {
                    return load_dt_view(oSettings);
                },
                "sDom": "<'row'<'col-sm-12 searchBox'f>r>t<'row lip pull-right'<l><i><p>>",
                "oLanguage": {
                    "sLengthMenu": "${i18nRowsPerPage}  _MENU_",

                    "sSearch": "",
                    "sInfo": " _START_-_END_ ${i18nOf}  _TOTAL_"
                },
                "sPaginationType": "simple_numbers",
                "bAutoWidth": false,
                "aoColumns": [
                    {sWidth: '38%'},
                    {sWidth: '23%'},
                    {sWidth: '23%'},
                    {sWidth: '15%'}
                ]
            };

            dataTablesSettings.init(tableId, 25, [], true,
                function (o) {
                    // auto scroll to top on paginate
                    if (o._iDisplayStart != oldStart) {
                        var targetOffset = $('#' + tableId).offset().top;
                        $('html,body').animate({scrollTop: targetOffset}, 350);
                        oldStart = o._iDisplayStart;
                    }
                },
                customOptions);

            $('.dataTables_filter input')
                .attr("placeholder", "${i18nSearchModule}");


            $('#' + tableId).on('click', 'tr', function () {
                $(this).find('td:first form').submit();
            });

            $('.timestamp').cuteTime({refresh: 60000});
        });
    </script>
</template:addResources>

<form id="viewAvailableModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST">
    <input type="hidden" name="_eventId" value="viewAvailableModules"/>
</form>
<form id="reloadModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
    <input type="hidden" name="_eventId" value="reloadModules"/>
</form>

<%@include file="common/moduleLabels.jspf" %>

<ul class="nav nav-tabs" id="myTab" role="tablist">
    <li class="nav-item">
        <a class="nav-link active" id="installed-modules-tab" data-toggle="tab" href="#installed-modules" role="tab"
           aria-controls="installed-modules"
           aria-selected="true"><fmt:message key="serverSettings.manageModules.installedModules"/></a>
    </li>
    <li class="nav-item">
        <a class="nav-link" id="available-modules-tab" data-toggle="tab" href="#available-modules" role="tab"
           aria-controls="available-modules"
           aria-selected="false" onclick="$('#viewAvailableModulesForm').submit()"><fmt:message
                key="serverSettings.manageModules.availableModules"/></a>
    </li>
    <span class="pull-right"><fmt:message key="serverSettings.manageModules.lastUpdate"/>:&nbsp;<span class="timestamp"><fmt:formatDate
            value="${lastModulesUpdate}" pattern="yyyy/MM/dd HH:mm"/></span>
        <form id="reloadModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST"
              onsubmit="workInProgress('${i18nWaiting}');">
    <input type="hidden" name="_eventId" value="reloadModules"/>
</form>
        <span class="refreshModule">
            <button class="btn btn-primary" data-toggle="tooltip" data-placement="bottom" title="${i18nModuleRefresh}"
                    data-container="body"
                    data-original-title="${i18nModuleRefresh}"
                    style="display: inline-block;"
                    data-container="body"
                    onclick="$('#reloadModulesForm').submit()">
                <i class="material-icons">update</i>
          </button>
        </span>
    </span>
</ul>


<div class="panel-body">
    <div class="card">
        <form:form modelAttribute="moduleFile" class="form" enctype="multipart/form-data" method="post">
            <%--onsubmit="workInProgress('${i18nWaiting}');">--%>
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
                <c:if test="${message.source eq 'moduleExists'}">
                    <c:set var="forceUpdateDisplay" value="true"/>

                </c:if>
            </c:forEach>
            <c:if test="${forceUpdateDisplay eq 'true'}">
                <div class="alert alert-warning">
                    <button type="button" class="close" data-dismiss="alert">&times;</button>
                    <fmt:message key="serverSettings.manageModules.upload.force.info"/>
                </div>
            </c:if>

            <div class="row">
                <div class="col-md-3">
                    <div class="form-group is-empty label-floating selectModule">
                        <div class="input-group">
                            <div class="input-group-btn">
                                <label class="btn btn-primary" for="moduleFileUpload">
                                    <input type="file" class="form-control-file" id="moduleFileUpload" name="moduleFile">
                                    <fmt:message key="serverSettings.manageModules.select.module"/>
                                </label>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="form-group">
                        <div class="checkbox">
                            <label for="moduleAutoStart">
                                <input id="moduleAutoStart" class="filled-in cyan" type="checkbox"
                                       name="moduleAutoStart" ${developmentMode ? 'checked="checked"' : ''}/>
                                <fmt:message key="serverSettings.manageModules.upload.autoStart"/>
                            </label>

                            <c:if test="${forceUpdateDisplay eq 'true'}">
                                <label for="moduleForceUpdate">
                                    <input type="checkbox" class="filled-in cyan" name="moduleForceUpdate" id="moduleForceUpdate"/>
                                    <fmt:message key="serverSettings.manageModules.upload.force"/>
                                </label>
                            </c:if>
                        </div>
                    </div>
                </div>
                <div class="col-md-5">
                    <div class="form-group is-empty label-floating">
                        <div class="input-group">
                             <span class="input-group-btn">
                                 <button id="btnUpload" class="btn cyan" type="submit" name="_eventId_upload">
                                     <fmt:message key='label.upload'/>
                                 </button>
                             </span>
                        </div>
                    </div>
                </div>
            </div>
        </form:form>
    </div>

    <div class="card material-table">

        <table id="${moduleTableId}" class="display table table-bordered hover no-ver-margin" style="width: 100%">
            <thead>
            <tr>
                <th><fmt:message key='serverSettings.manageModules.moduleName'/></th>
                <th><fmt:message key='serverSettings.manageModules.versions'/></th>
                <th><fmt:message key='serverSettings.manageModules.status'/></th>
                <th><fmt:message key='serverSettings.manageModules.usedInSites'/></th>
            </tr>
            </thead>
            <tbody>
            <c:set var="isStudio" value="${false}"/>
            <c:forEach items="${allModuleVersions}" var="entry">
                <%@include file="common/currentModuleVars.jspf" %>
                <c:if test="${!isMandatoryDependency && sourcesDownloadable}">
                    <%@include file="common/modulesTableRow.jspf" %>
                </c:if>
            </c:forEach>
            <c:forEach items="${allModuleVersions}" var="entry">
                <%@include file="common/currentModuleVars.jspf" %>
                <c:if test="${!isMandatoryDependency && !sourcesDownloadable}">
                    <%@include file="common/modulesTableRow.jspf" %>
                </c:if>
            </c:forEach>
            <c:forEach items="${allModuleVersions}" var="entry">
                <%@include file="common/currentModuleVars.jspf" %>
                <c:if test="${isMandatoryDependency}">
                    <%@include file="common/modulesTableRow.jspf" %>
                </c:if>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<p><a id="mandatory-dependency">&nbsp;</a><span class="text-error"><strong>*</strong></span>&nbsp;-&nbsp;${i18nMandatoryDependency}
</p>
</div>
