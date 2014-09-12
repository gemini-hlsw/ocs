<%@ include file="taglibs.jsp" %>
<s:set name="segments" value="observingLog.logSegments" scope="request"/>
<s:iterator value="observingLog.logSegments" id="seg">
    <table class="log" id="rowID" width="100%" cellpadding="0">
        <caption><s:property value="segmentCaption"/></caption>
        <thead>
            <tr>
                <th width="1%">Views</th>
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
                        log</a>
                </td>
                <s:iterator value="[1].visibleTableInfo">
                    <s:set name="pname" value="property"/>
                    <td><s:property value="get(#pname)"/></td>
                </s:iterator>
            </tr>
            <tr>
                <td colspan="<ww:property value='[1].visibleTableInfo.size'/>">
                    <textarea style="width:100%;" onfocus="growText('<s:property value="observationID"/>','<s:property value="configID"/>');" id="<s:property value='configID'/>" wrap="physical" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
            </tr>
            </tbody>
        </s:iterator>
    </table>
</s:iterator>