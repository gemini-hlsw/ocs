<%@ include file="taglibs.jsp" %>
<head>
    <title>GOL: Time Accounting:<s:property value="planID"/></title>
</head>

<div id="content">

    <h3>Time Analysis Log for: <s:property value="planID"/> (<a href="textExportTAPlan.action?planID=<s:property value='planID'/>">export</a> as text)</h3>
    <s:set name="segments" value="observingLog.logSegments" scope="request"/>
    <s:iterator value="observingLog.logSegments" id="seg">
        <table class="log" id="rowID" width="100%" cellpadding="0">
            <caption><s:property value="segmentCaption"/></caption>
            <thead>
                <tr>
                    <th width="1%">Views</th>
                    <th>Observation&#160;ID</th>
                    <s:iterator value="visibleTableInfo">
                        <th><s:property value="columnHeading"/></th>
                    </s:iterator>
                </tr>
            </thead>
            <s:iterator value="rows" status="rowStatus">
                <s:if test="#rowStatus.odd == true">
                    <tbody class="even">
                </s:if>
                <s:else>
                    <tbody class="odd">
                </s:else>

                <tr>
                    <td rowspan="2" nowrap>
                        <a href='<s:url value="fetchProgram.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            </s:url>'>
                            prog</a>
                        <a href='<s:url value="fetchPlan.action" includeParams="none">
                            <s:param name="planID" value="planID"/>
                            </s:url>'>
                            log</a><br>
                        <a href='<s:url value="fetchObservation.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            </s:url>'>
                            obs</a>
                         <a href='<s:url value="fetchPlanQADisplay.action" includeParams="none">
                            <s:param  name="planID" value="planID"/>
                            </s:url>'>
                            QA</a>
                    </td>
                    <td rowspan="2" nowrap><s:property value="observationID"/></td>
                    <s:iterator value="[1].visibleTableInfo">
                        <s:set name="pname" value="property"/>
                        <td><s:property value="get(#pname)"/></td>
                    </s:iterator>
                </tr>
                <tr>
                    <td colspan="<s:property value='[1].visibleTableInfo.size'/>">
                        <textarea style="width:100%;" onfocus="growText('<s:property value="observationID"/>','<s:property value="configID"/>');" id="<s:property value='configID'/>" wrap="physical" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
                </tr>
                <tr>
                    <td bgcolor="#BFBFBF" colspan="<s:property value='[1].visibleTableInfo.size+2'/>">Time gap:  <s:property value="get('visitgap')"/></td>
                </tr>
                </tbody>
            </s:iterator>
        </table>
        </s:iterator>
</div>