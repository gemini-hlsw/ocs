<%@ include file="taglibs.jsp" %>
<head>
<title>GOL: <s:property value="planID"/></title>
<!--<link rel="alternate" type="application/rss+xml" title="RSS" href="http://gsodb.gemini.edu:rss.xml"/>-->
</head>

<h3>Observing Log for: <s:property value="planID"/>&nbsp;(<a href="textExportPlan.action?planID=<s:property value='planID'/>">export</a> as text) or (<a href="textSplitExportPlan.action?planID=<s:property value='planID'/>">split export</a> as text) </h3>

<s:set name="obsLog" value="observingLog" scope="request" />
<div class="obsinfo">
    <form action="showLogInfo.action?">
        <fieldset>
            <legend>Observing Log Information</legend>
	    <table>
		<tbody>
                    <tr>
                        <s:push value="observingLog.logInformation">
			<td><dl><dt>Night Observers</dt><dd><s:property value="nightObservers"/></dd></dl></td>
			<td><dl><dt>SSAs</dt><dd><s:property value="ssas"/></dd></dl></td>
			<td><dl><dt>Dataproc Observer</dt><dd><s:property value="dataproc"/></dd></dl></td>
                        <td><dl><dt>Daytime Observers</dt><dd><s:property value="dayobserver"/></dd></dl></td>
			<td><dl><dt>File Prefix</dt><dd><s:property value="filePrefix"/></dd></dl></td>
                        <td><dl><dt>CC Software Version</dt><dd><s:property value="CCVersion"/></dd></dl></td>
			<td><dl><dt>DC Software Version</dt><dd><s:property value="DCVersion"/></dd></dl></td>
                        </s:push>
		    </tr>
		</tbody>
	</table>
        <input type="hidden" name="planID" value="<s:property value='planID'/>"/>
	<input type="submit" value="Modify" class="submit-button" />
	</fieldset>
    </form>
</div>

</p>

<s:iterator value="observingLog.logSegments" id="seg">
    <table class="log" id="rowID">
        <caption><s:property value='segmentCaption'/></caption>
        <thead>
            <tr>
                <th>Views</th>
                <th>Observation ID</th>
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
                    <a href='<s:url value="fetchObservation.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            </s:url>'>
                    obs</a>
                    <a href='<s:url value="fetchProgram.action" includeParams="none">
                            <s:param name="observationID" value="observationID"/>
                            </s:url>'>
                    prog</a><br>
                    <a href='<s:url value="fetchPlanTADisplay.action" includeParams="none">
                            <s:param  name="planID" value="planID"/>
                            </s:url>'>
                    TA</a>
                    <a href='<s:url value="fetchPlanQADisplay.action" includeParams="none">
                            <s:param  name="planID" value="planID"/>
                            </s:url>'>
                    QA</a>
                </td>

                <td rowspan="2" nowrap><s:property value="observationID"/></td>
                <s:iterator value="visibleTableInfo">
                    <s:set name="pname" value="property"/>
                    <td><s:property value="get(#pname)"/></td>
                </s:iterator>
            </tr>
            <tr>
                <td colspan="<s:property value='visibleTableInfo.size'/>" ><textarea style="width:99%;" onfocus="growText('<s:property value="observationID"/>','<s:property value="configID"/>');" id="<s:property value='configID'/>" wrap="physical" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
            </tr>

            </tbody>
        </s:iterator>
    </table>
</s:iterator>


<!-- Weather Segment -->

<table class="log" id="rowID">
  <s:push value="observingLog.weatherSegment">
  <caption><s:property value='segmentCaption'/></caption>
  <thead>
      <tr>
          <th>UTC Time</th>
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
          <td rowspan="2" nowrap><s:property value="time"/></td>
          <s:iterator value="visibleTableInfo">
              <s:set name="pname" value="property"/>
              <td><s:property value="get(#pname)"/></td>
          </s:iterator>
      </tr>
      <tr>
         <td colspan="<s:property value='visibleTableInfo.size'/>" ><textarea style="width:99%;" onfocus="growText('<s:property value="planID"/>','<s:property value="ID"/>');" id="<s:property value='ID'/>" wrap="physical" rows="<s:property value='commentRows'/>"><s:property value="comment"/></textarea></td>
      </tr>
      </tbody>
  </s:iterator>
  </s:push>
</table>

