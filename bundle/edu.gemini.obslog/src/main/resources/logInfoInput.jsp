<%@ include file="./taglibs.jsp"%>

<div id="content">
   <p>Observing Log Information for Observing Log: <s:property value="planID"/></p>

	<div id="winput">
		<fieldset>
		<legend>Enter Observing Information</legend>
		<form method="Post" action="storeLogInfo.action" name="Main">
			<h2>Observer Information</h2>

			<label for="nightobs" class="inbox">Night Observers</label><input type="text" name="logInformation.nightObservers" value="<s:property value='logInformation.nightObservers'/>" class="inbox"/><br/>
			<label for="ssa" class="inbox">SSAs</label><input type="text" name="logInformation.Ssas"  value="<s:property value='logInformation.Ssas'/>" class="inbox"/><br/>
			<label for="dataproc" class="inbox">DataProc Observer</label><input type="text" name="logInformation.dataproc" value="<s:property value='logInformation.dataproc'/>" class="inbox"/><br/>
			<label for="dayobs" class="inbox">Daytime Observers</label><input type="text" name="logInformation.dayobserver" value="<s:property value='logInformation.dayobserver'/>" class="inbox"/><br/>
			<label for="comment" class="inbox">Comment</label><textarea name="logInformation.nightComment" class="inbox" rows="6"><s:property value='logInformation.nightComment'/></textarea><br/>
			<br/>
			<h2>Software Information</h2>

			<label for="fileprefix" class="inbox">File Prefix</label><input type="text" name="logInformation.filePrefix" value="<s:property value='logInformation.filePrefix'/>" class="inbox"/><br/>
			<label for="ccversion" class="inbox">CC Version</label><input type="text" name="logInformation.CCVersion" value="<s:property value='logInformation.CCVersion'/>" class="inbox"/><br/>
			<label for="dcversion" class="inbox">DC Version</label><input type="text" name="logInformation.DCVersion" value="<s:property value='logInformation.DCVersion'/>" class="inbox"/><br/>
			<label for="comment" class="inbox">Software Comment</label><textarea class="inbox" name="logInformation.softwareComment" rows="6"><s:property value='logInformation.softwareComment'/></textarea><br/>
                        <input type="hidden" name="planID" value="<s:property value='planID'/>" />
			<input type="submit" value="Update" class="submit-button"/><br/>
		</form>
		</fieldset>
	</div>

</div>

