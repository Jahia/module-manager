<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<template:addResources type="javascript" resources="jquery.min.js,jquery.blockUI.js,workInProgress.js"/>
<fmt:message key="label.workInProgressTitle" var="i18nWaiting"/><c:set var="i18nWaiting" value="${functions:escapeJavaScript(i18nWaiting)}"/>
<c:if test="${not empty branchTagInfos}">
    <fmt:message key="serverSettings.manageModules.duplicateModule.scm.master" var="scmMaster"/>
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">
            $(document).ready(function() {
                $("#newScmUri").change(function() {
                    var selectedTag = $(this).find('option:selected').text();
                    if (selectedTag == "${scmMaster}") {
                        selectedTag = "";
                    }
                    $('#branchOrTag').val(selectedTag);
                });
            });
        </script>
    </template:addResources>
</c:if>
<template:addResources type="inlinejavascript">
    <script type="text/javascript">
        $(document).ready(function() {
            $("#duplicateModuleForm").submit(function(event) {
                workInProgress('${i18nWaiting}');
                var $this = $(this);
                $.ajax({
                    type: $this.attr('method'),
                    dataType: 'json',
                    data: $this.serialize(),
                    url: $this.attr('action')
                }).done(function (data) {
                    if ('error' in data) {
                        $("#errors").append("<div class=\"alert alert-danger\">" +
                                "<button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>" +
                                data.error + "</div>");
                    } else if ('bundleError' in data) {
                        alert(data.bundleError);
                        var url = window.location.href;
                        var paramStart = url.indexOf('?');
                        if (paramStart > -1) {
                            url = url.substring(0, paramStart);
                        }
                        window.location.assign(url);
                    } else {
                        window.parent.location.assign(data.newModuleStudioUrl);
                    }
                }).fail(function (jqXHR, textStatus, errorThrown) {
                    alert(errorThrown);
                }).always(function () {
                    window.parent.hideMask();
                });
                event.preventDefault();
                return false;
            });
        });
    </script>
</template:addResources>

<div class="page-header">
    <h2>
        <fmt:message key='serverSettings.manageModules.duplicateModule.title'>
            <fmt:param value="${moduleName}"/>
        </fmt:message>
    </h2>
</div>


<div id="errors">
    <c:forEach items="${flowRequestContext.messageContext.allMessages}" var="message">
        <c:if test="${message.severity eq 'ERROR'}">
            <div class="alert alert-danger">
                <button type="button" class="close" data-dismiss="alert">&times;</button>
                    ${message.text}
            </div>
        </c:if>
    </c:forEach>
</div>

<div class="row">
    <div class="col-md-6 col-md-offset-3">
        <div class="panel panel-default">
            <div class="panel-body">
                <template:tokenizedForm allowsMultipleSubmits="true">
                <form id="duplicateModuleForm" action="<c:url value='${url.base}${renderContext.mainResource.node.path}.duplicateModule.do'/>" method="POST">
                    <c:choose>
                        <c:when test="${empty srcPath}">
                            <c:choose>
                                <c:when test="${not empty branchTagInfos}">
                                    <div class="form-group label-floating">
                                        <label class="control-label" for="newScmUri"><fmt:message key="serverSettings.manageModules.downloadSources.scm.${fn:endsWith(version,'-SNAPSHOT') ? 'branch' : 'tag'}" /></label>
                                        <input type="hidden" id="branchOrTag" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                                        <select name="newScmUri" id="newScmUri">
                                            <c:forEach var="branchTagInfo" items="${branchTagInfos}">
                                                <option value="${branchTagInfo.value}" ${branchTagInfo.key eq branchOrTag ? 'selected' : ''}>${branchTagInfo.key}</option>
                                            </c:forEach>
                                        </select>
                                    </div>
                                </c:when>
                                <c:when test="${hasError}">
                                    <div class="form-group label-floating">
                                        <label class="control-label" for="newScmUriText"><fmt:message key="serverSettings.manageModules.downloadSources.scmUri" /></label>
                                        <input class="form-control" type="text" id="newScmUriText" name="newScmUri" value="${not empty newScmUri ? newScmUri : scmUri}"/>
                                    </div>
                                    <div class="form-group label-floating">
                                        <label class="control-label" for="branchOrTagText"><fmt:message key="serverSettings.manageModules.downloadSources.branchOrTag" /></label>
                                        <input class="form-control" type="text" id="branchOrTagText" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                                    </div>
                                </c:when>
                                <c:when test="${not empty newScmUri}">
                                    <input type="hidden" name="newScmUri" value="${newScmUri}"/>
                                    <input type="hidden" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                                </c:when>
                            </c:choose>
                        </c:when>
                        <c:otherwise>
                            <input type="hidden" name="srcPath" value="${srcPath}"/>
                            <input type="hidden" name="newScmUri" value="${scmUri}"/>
                            <input type="hidden" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                        </c:otherwise>
                    </c:choose>

                    <div class="form-group label-floating">
                        <div class="input-group">
                            <label for="newModuleName"><fmt:message key='label.moduleName'/>
                                <fmt:message key='label.moduleName.copy' var="moduleNameCopy">
                                    <fmt:param value="${moduleName}"/>
                                </fmt:message>
                            </label>
                            <input class="form-control" type="text" id="newModuleName" name="newModuleName" value="${not empty newModuleName ? newModuleName : moduleNameCopy}" required />
                        </div>
                    </div>

                    <div class="form-group label-floating">
                        <div class="input-group">
                            <fmt:message key='label.moduleId.empty' var="moduleIdEmpty"/>
                            <label for="newModuleId"><fmt:message key='label.moduleId'/></label>
                            <input class="form-control" type="text" id="newModuleId" name="newModuleId" placeholder="${moduleIdEmpty}" value="${newModuleId}" />
                        </div>
                    </div>
    
                    <div class="form-group label-floating">
                        <div class="input-group">
                            <label for="newGroupId"><fmt:message key='label.groupId'/></label>
                            <input class="form-control" type="text" id="newGroupId" name="newGroupId" placeholder="org.jahia.modules" value="${newGroupId ? newGroupId : groupId}" />
                        </div>
                    </div>

                    <div class="form-group label-floating">
                        <div class="input-group">
                            <label for="newDstPath"><fmt:message key='label.sources.folder'/></label>
                            <input class="form-control" type="text" id="newDstPath" name="newDstPath" placeholder="${dstPath}" value="${newDstPath}" />
                        </div>
                    </div>

                    <c:if test="${not empty moduleNodetypes}">
                        <div class="alert alert-danger">
                            <fmt:message key="serverSettings.manageModules.duplicateModule.uninstallSrcModuleWarning">
                                <fmt:param value="${fn:join(moduleNodetypes, ', ')}" />
                            </fmt:message>
                        </div>
                        <input type="hidden" name="containsTypeDefinitions" value="true" />
                    </c:if>

                    <div class="form-group form-group-sm">
                        <c:if test="${not empty tempSources}">
                            <input type="hidden" name="areSourcesTemporary" value="true" />
                        </c:if>
                        <input type="hidden" name="moduleId" value="${moduleId}" />
                        <input type="hidden" name="version" value="${version}" />
                        <button class="btn btn-primary btn-raised pull-right" type="submit">
                            <fmt:message key='label.next'/>
                        </button>
                        <button class="btn btn-default pull-right" type="button" onclick="$('#${currentNode.identifier}CancelForm').submit()">
                            <fmt:message key='label.cancel' />
                        </button>
                    </div>
                </form>
                </template:tokenizedForm>
                
                <form id="${currentNode.identifier}CancelForm" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
                    <input type="hidden" name="_eventId" value="cancelDuplicate" />
                </form>
            </div>
        </div>
    </div>
</div>