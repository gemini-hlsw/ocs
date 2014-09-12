<%@ include file="taglibs.jsp" %>
<head>
<title>GOL: <s:property value="observationID"/></title>
</head>

<div id="content">
<h3>Observing Log information for observation: <s:property value="observationID"/></h3>

<p/>

<s:set name="segments" value="observingLog.logSegments" scope="request"/>
<s:iterator value="observingLog.logSegments" id="seg">
    <table class="log" id="rowID">
        <caption><s:property value="segmentCaption"/></caption>
        <thead>
            <tr>
                <th>Views</th>
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
                    <a href='<s:url value="fetchLogFromObservation.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            <s:param name="utstart" value="rawUT"/>
                            </s:url>'>
                    log</a>
                </td>

                <td rowspan="2" nowrap><s:property value="observationID"/></td>
                <s:iterator value="[1].visibleTableInfo">
                    <s:set name="pname" value="property"/>
                    <td><s:property value="get(#pname)"/></td>
                </s:iterator>
            </tr>
            <tr>
                <td colspan="<s:property value='[1].visibleTableInfo.size'/>" ><textarea cols="120" wrap="physical" style="width:99%;" onfocus="growText('<s:property value="observationID"/>','<s:property value="configID"/>');" id="<s:property value='configID'/>" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
            </tr>
            </tbody>
        </s:iterator>
    </table>
</s:iterator>

</div>