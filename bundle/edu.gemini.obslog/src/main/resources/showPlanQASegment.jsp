<%@ include file="taglibs.jsp" %>
<head>
    <title>Bulk Edit: Select Datasets</title>
    <!--<link rel="alternate" type="application/rss+xml" title="RSS" href="http://gsodb.gemini.edu:rss.xml"/>-->
</head>
<script language="javascript" type="text/javascript">
    function setCheckboxes() {
        var value = document.bulkedit.all.checked;
        var numelements  = document.bulkedit.elements.length;
        var item;
        for (var i=0; i<numelements; i++) {
            item = document.bulkedit.elements[i];
            item.checked = value;
        }
    }
</script>
<div id="content">
    <h3>Quality Assurance Log for: <s:property value="planID"/> (<a href="textExportQAPlan.action?planID=<s:property value='planID'/>">export</a> as text)</h3>

    <s:push value="observingLog.logSegments.get(0)">
    <form name="bulkedit" method="post" action="bulkQAEdit3.action?planID=<s:property value='planID'/>">
        <s:hidden name="bulkID"/>
        <p>To change the QA state of one or more datasets:</p>
        <div class="instructions">
        <ol>
            <li>Select the check box on one or more rows. Select all rows with the check box in the table heading.</li>
            <li>Select the new QA state for the datasets to <s:select name="QAState" list="QAStates"/>.</li>
            <li>Click this <input type="submit" name="Set" id="next" value="Set QA State"> button to make the change.</li>
        </ol>
        </div>

        <table class="log" id="rowID" width="100%" cellpadding="0">
            <caption><s:property value="segmentCaption"/></caption>
            <thead>
                <tr>
                    <th width="1%"><input type="checkbox" name="all" onClick="setCheckboxes()"></th>
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
                    <td rowspan="2" ><input type="checkbox" name="bulk" value="<s:property value='configID'/>"/></td>
                    <td rowspan="2" nowrap>
                        <a href='<s:url value="fetchProgram.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            </s:url>'>
                            prog</a>
                        <a href='<s:url value="fetchPlan.action" includeParams="none">
                            <s:param name="planID" value="planID"/>
                            </s:url>'>
                            log</a>
                        <a href='<s:url value="fetchPlanTADisplay.action" includeParams="none">
                            <s:param name="planID" value="planID"/>
                            </s:url>'>
                            TA</a>
                    </td>
                    <s:iterator value="[1].visibleTableInfo">
                        <s:set name="pname" value="property"/>
                        <td><s:property value="get(#pname)"/></td>
                    </s:iterator>
                </tr>
                <tr>
                    <td colspan="<s:property value='[1].visibleTableInfo.size'/>">
                        <textarea style="width:99%;" onfocus="growText('<s:property value="observationID"/>','<s:property value="configID"/>');" id="<s:property value='configID'/>" wrap="physical" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
                </tr>
                </tbody>
            </s:iterator>
        </table>
    </form>
    </s:push>

</div>