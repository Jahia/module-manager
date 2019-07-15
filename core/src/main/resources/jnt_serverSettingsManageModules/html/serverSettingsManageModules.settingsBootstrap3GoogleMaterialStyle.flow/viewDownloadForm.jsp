<%@ page import="org.springframework.web.servlet.tags.form.FormTag" %>
<%@ page import="javax.servlet.jsp.tagext.*" %>
<%@ page import="java.util.Arrays" %>
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
    <template:addResources type="inlinejavascript">
        <script type="text/javascript">
            $(document).ready(function() {
                $("#scmUri").change(function() {
                    var selectedBranchTag = $(this).find('option:selected').text();
                    if (selectedBranchTag == "${scmMaster}") {
                        selectedBranchTag = "";
                    }
                    $('#branchOrTag').val(selectedBranchTag);
                });
            });
        </script>
    </template:addResources>
</c:if>
<div class="page-header">
    <h2>
        <fmt:message key='serverSettings.manageModules.downloadSources' />
    </h2>
</div>

<c:forEach items="${flowRequestContext.messageContext.allMessages}" var="message">
    <c:if test="${message.severity eq 'ERROR'}">
        <div class="alert alert-danger">
            <button type="button" class="close" data-dismiss="alert">&times;</button>
                ${message.text}
        </div>
    </c:if>
</c:forEach>

<div class="row">
    <div class="col-md-6 col-md-offset-3">
        <div class="panel panel-default">
            <div class="panel-body">
                <form action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
                    <c:choose>
                        <c:when test="${not empty branchTagInfos}">
                            <div class="form-group label-floating">
                                <label class="control-label" for="scmUri">
                                    <fmt:message key="serverSettings.manageModules.downloadSources.scm.${fn:endsWith(version,'-SNAPSHOT') ? 'branch' : 'tag'}" />
                                </label>
                                <input type="hidden" id="branchOrTag" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                                <select class="form-control" name="scmUri" id="scmUri">
                                    <c:forEach var="branchTagInfo" items="${branchTagInfos}">
                                        <option value="${branchTagInfo.value}" ${branchTagInfo.key eq branchOrTag ? 'selected' : ''}>${branchTagInfo.key}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <div class="form-group label-floating">
                                <label class="control-label" for="scmUriText">
                                    <fmt:message key="serverSettings.manageModules.downloadSources.scmUri" />
                                </label>
                                <input class="form-control" type="text" id="scmUriText" name="scmUri" value="${not empty scmUri ? scmUri : scmUri}"/>
                            </div>
                            <div class="form-group label-floating">
                                <label class="control-label" for="branchOrTagText">
                                    <fmt:message key="serverSettings.manageModules.downloadSources.branchOrTag" />
                                </label>
                                <input class="form-control" type="text" id="branchOrTagText" name="branchOrTag" value="${not empty branchOrTag ? branchOrTag : ''}"/>
                            </div>
                        </c:otherwise>
                    </c:choose>
                    <div class="form-group form-group-sm">
                        <button class="btn btn-primary btn-raised pull-right" type="submit" name="_eventId_downloadSources">
                            <i class="icon-chevron-right icon-white"></i>
                            &nbsp;<fmt:message key='label.next'/>
                        </button>
                        <button class="btn btn-default pull-right" type="button" onclick="$('#${currentNode.identifier}CancelForm').submit()">
                            <i class="icon-ban-circle"></i>
                            &nbsp;<fmt:message key='label.cancel' />
                        </button>
                    </div>
                </form>

                <form id="${currentNode.identifier}CancelForm" action="${flowExecutionUrl}" method="POST" onsubmit="workInProgress('${i18nWaiting}');">
                    <input type="hidden" name="_eventId" value="cancel" />
                </form>
            </div>
        </div>
    </div>
</div>
