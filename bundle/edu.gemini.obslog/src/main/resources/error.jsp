<%@ include file="taglibs.jsp" %>

<head><title>Doh! Errors</title></head>

<s:actionerror/>
<s:if test="hasErrors()">
  An Error has occurred in this applications.
  <br />
  <font color="red">
    <s:iterator value="actionErrors">
      <s:property/><br />
    </s:iterator>
    <s:iterator value="actionMessage">
      <s:property/><br />
    </s:iterator>
  </font>
</s:if>

An Error has occurred in this application.










