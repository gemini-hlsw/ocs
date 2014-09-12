<%@ include file="taglibs.jsp" %>


<div id="winput">
    <br/>
    <s:form action="'fetchObservation.action?observationID=${observationID}'">
        <s:textfield label="'Show one observation'" name="'observationID'"/>
        <s:submit value="'Submit'" cssClass="'submit-button'"/>
    </s:form>
</div>
