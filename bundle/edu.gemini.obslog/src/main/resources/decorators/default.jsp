<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict //EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ include file="../taglibs.jsp"%>

<html xmlns="http://www.w3.org/1999/xhtml" lang="en" >
<head>
    <title><decorator:title default="Gemini Observing Log"/></title>
    <meta http-equiv="content-type" content="text/html; charset=iso-8859-1" />
    <c:set var="ctx" value="${pageContext.request.contextPath}" scope="request" />
    <link href="<c:out value="${ctx}"/>/css/screen.css" type="text/css" rel="stylesheet"/>
    <link href="<c:out value="${ctx}"/>/favicon.ico" rel="SHORTCUT ICON"/>
    <script type="text/javascript" src="<c:out value="${ctx}"/>/scripts/MyJS.js"></script>
    <script type="text/javascript" src="<c:out value="${ctx}"/>/scripts/MyFade.js"></script>
    <decorator:head />
</head>


<div id="header">
<h1>Gemini Observing Log</h1>
<ul id="navlist">
	<li>
		<a href="${ctx}/index.jsp">Welcome</a>
	</li>
        <li>
                <a href="${ctx}/openLog.action">Open Observing Log</a>
        </li>
        <li>
                <a href="${ctx}/help/index.html">Help</a>
        </li>
</ul>
</div>

<body>

<div id="content">
    <%@ include file="../messages.jsp" %>
    <decorator:body />
</div>

</body>
<script type="text/javascript">window.status = "Loading: <decorator:title default="Gemini Electronic Observing Logs" />...";</script>

</html>
