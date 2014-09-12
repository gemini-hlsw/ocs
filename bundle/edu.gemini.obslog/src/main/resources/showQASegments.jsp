<%@ include file="taglibs.jsp" %>
<head>
    <title>GOL: <s:property value="planID"/></title>
    <!--<link rel="alternate" type="application/rss+xml" title="RSS" href="http://gsodb.gemini.edu:rss.xml"/>-->
</head>



<div id="content">

<h3>Quality Assurance Log for: <s:property value="planID"/></h3>

<a href="bulkQAEdit1.action?<s:property value='bulkID'/>">Bulk Edit</a> the QA State of these <s:property value="datasetCount"/> datasets.

<s:include value="QASegment.jsp"/>

</div>