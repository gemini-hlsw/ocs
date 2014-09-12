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

    <h3>Quality Assurance Log for: <s:property value="planID"/></h3>
    <a href="textExportQAPlan.action?planID=<s:property value='planID'/>">Export</a> this QA log.

    <s:push value="observingLog.logSegments.get(0)">
    <form name="bulkedit" method="post" action="bulkQAEdit3.action">
        <s:hidden name="bulkID"/>

            <input type="submit" name="Set" id="next" value="Set >>">

    </form>
    </s:push>

</div>>