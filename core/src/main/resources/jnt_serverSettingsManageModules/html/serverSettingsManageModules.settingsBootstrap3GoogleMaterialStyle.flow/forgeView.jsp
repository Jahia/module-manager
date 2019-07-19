<%@ page import="java.util.Date" %>
<%@ page import="org.jahia.settings.SettingsBean" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="utility" uri="http://www.jahia.org/tags/utilityLib" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%@ taglib prefix="user" uri="http://www.jahia.org/tags/user" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>
<%--@elvariable id="module" type="org.jahia.modules.modulemanager.forge.Module"--%>
<c:set var="developmentMode" value="<%= SettingsBean.getInstance().isDevelopmentMode() %>"/>
<template:addResources type="javascript" resources="jquery.min.js,jquery.blockUI.js,workInProgress.js,jquery.cuteTime.js"/>
<template:addResources type="javascript" resources="jquery.cuteTime.settings.${currentResource.locale}.js"/>
<template:addResources type="javascript"
                       resources="datatables/jquery.dataTables.js,i18n/jquery.dataTables-${currentResource.locale}.js,datatables/dataTables.bootstrap-ext.js,settings/dataTables.initializer.js"/>
<template:addResources type="javascript" resources="jquery-ui.min.js"/>
<template:addResources type="javascript" resources="dt-module-manager.js"/>
<template:addResources type="css" resources="datatables/css/bootstrap-theme.css,tablecloth.css"/>
<template:addResources type="css" resources="manageModules.css"/>
<template:addResources type="css" resources="module-manager.css"/>
<template:addResources type="css" resources="mdModuleManager.css"/>

<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting"
                                                                       value="${functions:escapeJavaScript(i18nWaiting)}"/>
<fmt:message key="serverSettings.manageModules.details" var="i18nModuleDetails"/>
<fmt:message key="serverSettings.manageModules.checkForUpdates" var="i18nRefreshModules"/>
<fmt:message var="lastUpdateTooltip" key="serverSettings.manageModules.lastUpdate">
    <fmt:param value="${lastModulesUpdate}"/>
</fmt:message>
<fmt:message var="i18nRowsPerPage" key="serverSettings.manageModules.rowsPerPage" />
<fmt:message var="i18nOf" key="serverSettings.manageModules.of"/>
<fmt:message var="i18nSearchModule" key="serverSettings.manageModules.searchModule" />
<fmt:message key="serverSettings.manageModules.moduleRefresh" var="i18nModuleRefresh"/>

<section class="moduleManagerContainer">

    <div class="page-header">
        <h2><fmt:message key="serverSettings.manageModules"/></h2>
    </div>

    <c:forEach items="${flowRequestContext.messageContext.allMessages}" var="message">
        <c:if test="${message.severity eq 'INFO' || message.severity eq 'ERROR'}">
            <div class="alert alert-${message.severity eq 'INFO' ? 'success' : 'danger'}">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${message.text}
            </div>
        </c:if>
    </c:forEach>

    <c:set var="moduleTableId" value="module_table_${forgeModuleTableUUID}"/>

    <template:addResources>
        <script type="text/javascript">
            $(document).ready(function () {
                var tableId = "${moduleTableId}";

                function save_dt_view(oSettings, oData) {
                    localStorage.setItem('DataTables_adminModulesForgeView', JSON.stringify({id: tableId, data: oData}));
                }

                function load_dt_view(oSettings) {
                    var item = localStorage.getItem('DataTables_adminModulesForgeView');
                    if (item) {
                        var itemJSON = JSON.parse(item);
                        if (itemJSON.data && itemJSON.id == tableId) {
                            return itemJSON.data;
                        }
                    }
                    return undefined;
                }

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
                        "sSearch": '<i class="material-icons">search</i>',
                        "sInfo": " _START_-_END_ ${i18nOf}  _TOTAL_"
                    },
                    "sPaginationType": "simple_numbers",
                    "bAutoWidth": false,
                    "aoColumns" : [
                        { sWidth: '75%' },
                        { sWidth: '15%' },
                        { sWidth: '10%' }
                    ]
                };

                dataTablesSettings.init(tableId, 10, [], true, null, customOptions);

                $('.dataTables_filter input')
                    .attr("placeholder", "${i18nSearchModule}");

                $('#' + tableId).on('click', 'tr', function () {
                    var remoteUrl = $(this).find('td:first input[name="remoteUrl"]').val();
                    $('#modalframe').attr('src', remoteUrl);
                    $('#moduleDetailsModal').modal('show');
                });

                $('.timestamp').cuteTime({refresh: 60000});

            });
        </script>
    </template:addResources>

    <form id="viewInstalledModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST">
        <input type="hidden" name="_eventId" value="viewInstalledModules"/>
    </form>
    <form id="reloadModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
        <input type="hidden" name="_eventId" value="reloadModules"/>
    </form>

    <ul class="nav nav-tabs" id="availableModuleTabs" role="tablist">
        <li class="nav-item">
            <a class="nav-link" id="installed-modules-tab" data-toggle="tab" href="#installed-modules" role="tab"
               aria-controls="installed-modules"
               aria-selected="true"
               onclick="$('#viewInstalledModulesForm').submit()"
            ><fmt:message key="serverSettings.manageModules.installedModules"/></a>
        </li>
        <li class="nav-item active">
            <a class="nav-link" id="available-modules-tab" data-toggle="tab" href="#available-modules" role="tab"
               aria-controls="available-modules"
               aria-selected="false"><fmt:message
                    key="serverSettings.manageModules.availableModules"/></a>
        </li>
        <span class="pull-right"><fmt:message key="serverSettings.manageModules.lastUpdate"/>:&nbsp;<span class="timestamp"><fmt:formatDate
                value="${lastModulesUpdate}" pattern="yyyy/MM/dd HH:mm"/></span>
            <form id="reloadModulesForm" style="display: none" action="${flowExecutionUrl}" method="POST"
                  onsubmit="workInProgress('${i18nWaiting}');">
                <input type="hidden" name="_eventId" value="reloadModules"/>
            </form>
             <span class="refreshModule">
                <button  data-toggle="tooltip" data-placement="bottom" title="${i18nModuleRefresh}"
                         data-original-title="${i18nModuleRefresh}"
                         style="display: inline-block;"
                         data-container="body"
                         class="btn btn-primary btn-fab btn-fab-xs"
                         onclick="$('#reloadModulesForm').submit()">
                    <i class="material-icons">update</i>
              </button>
            </span>
        </span>
    </ul>

    <div class="panel-body">
        <div id="moduleDetailsModal" class="modal fade" tabindex="-1" role="dialog" aria-labelledby="moduleDetailsModalLabel"
             style="width:960px; margin-left:40px;">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h3 id="moduleDetailsModalLabel"><fmt:message key="serverSettings.manageModules.details"/></h3>
                </div>
                <div class="modal-body" style="padding:0; height:480px; max-height:480px">
                    <iframe id="modalframe" frameborder="0" style="width:100%; height:99%"></iframe>
                </div>
            </div>
        </div>
        <div class="checkbox">
            <label for="globalModuleAutoStart" class="autoStartModule black-text">
                <input type="checkbox" name="globalModuleAutoStart"
                       id="globalModuleAutoStart" ${developmentMode ? 'checked="checked"' : ''}/>
                <fmt:message key="serverSettings.manageModules.download.autoStart"/>&nbsp;
            </label>
        </div>
    </div>

    <div class="card material-table">
        <table class="table table-bordered table-striped table-hover no-ver-margin" id="${moduleTableId}">
            <thead>
            <tr>
                <th><fmt:message key='serverSettings.manageModules.moduleName'/></th>
                <th>
                    <fmt:message key="serverSettings.manageModules.version"/>
                </th>
                <th>
                    <fmt:message key="serverSettings.manageModules.download"/>
                </th>
            </tr>
            </thead>

            <tbody>
            <c:forEach items="${requestScope.modules}" var="module">
                <c:url value="${module.remoteUrl}" context="/" var="remoteUrl"/>
                <tr onclick="$('#modalframe').attr('src', '${remoteUrl}')">
                    <td>
                            <input type="hidden" name="remoteUrl" value="${remoteUrl}"/>
                            <c:if test="${not empty module.icon}">
                                <img style="width:32px; height:32px;margin-right: 5px" src="${module.icon}"/>
                            </c:if>
                            <span>
                                <b>${module.name}</b>&nbsp;(${module.id})
                            </span>
                        <input type="hidden" name="remoteUrl" value="${remoteUrl}"/>
                    </td>

                    <td> ${module.version}</td>

                    <td>
                        <c:choose>
                            <c:when test="${!module.installable}">
                                <fmt:message key="serverSettings.manageModules.module.canNotInstall"/>
                            </c:when>
                            <c:otherwise>
                                <c:remove var="alreadyInstalled"/>
                                <c:if test="${not empty allModuleVersions[module.id]}">
                                    <c:forEach items="${allModuleVersions[module.id]}" var="entry">
                                        <c:if test="${entry.key eq module.version}">
                                            <c:set var="alreadyInstalled" value="true"/>
                                        </c:if>
                                    </c:forEach>
                                </c:if>

                                <c:choose>
                                    <c:when test="${not empty alreadyInstalled}">
                                        <fmt:message key="serverSettings.manageModules.module.alreadyInstalled"/>
                                    </c:when>
                                    <c:otherwise>
                                        <fmt:message key="serverSettings.manageModules.download" var="downloadLabel"/>
                                        <form style="margin: 0;" action="${flowExecutionUrl}" method="POST"
                                              onsubmit="this.elements.namedItem('moduleAutoStart').value=document.getElementById('globalModuleAutoStart').checked; workInProgress('${i18nWaiting}');">
                                            <input type="hidden" name="forgeId" value="${module.forgeId}"/>
                                            <input type="hidden" name="moduleUrl" value="${module.downloadUrl}"/>
                                            <input type="hidden" name="moduleAutoStart" value="${developmentMode}"/>
                                            <button data-toggle="tooltip" data-placement="bottom" title="${downloadLabel}"
                                                    data-original-title="" class="btn btn-fab btn-fab-xs button-download" type="submit"
                                                    name="_eventId_installForgeModule">
                                                <i class="material-icons">file_download</i>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </c:otherwise>
                        </c:choose>

                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</section>
