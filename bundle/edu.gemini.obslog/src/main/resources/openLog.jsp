<%@ include file="taglibs.jsp" %>

<h3>
    Nights With Available Observing Logs
</h3>

<s:set name="tplans" value="plans" scope="request"/>

<div id="winput">
    <fieldset>
        <legend>
            Open a Nightly Plan/Observing Log
        </legend>

        <display:table name="tplans" class="log" requestURI="" uid="plan" pagesize="25">
            <display:column title="Action">
                <c:url var="openurl" value="fetchPlan.action">
                    <c:param name="planID" value="${plan.planID}"/>
                </c:url>
                <c:url var="exporturl" value="textExportPlan.action">
                    <c:param name="planID" value="${plan.planID}"/>
                </c:url>
                <c:url var="timeurl" value="fetchPlanTADisplay.action">
                    <c:param name="planID" value="${plan.planID}"/>
                </c:url>
                <c:url var="qaurl" value="fetchPlanQADisplay.action">
                    <c:param name="planID" value="${plan.planID}"/>
                </c:url>
                <a href='<c:out value="${openurl}"/>'>open</a>
                <a href='<c:out value="${exporturl}"/>'>export</a>
                <a href='<c:out value="${timeurl}"/>'>TA</a>
                <a href='<c:out value="${qaurl}"/>'>QA</a>
            </display:column>

            <display:column title="Observing Log/Nightly Record Title" sortable="true">
                <a href='<c:out value="${openurl}"/>'>
                    <c:out value="${plan.planID}"/>
                </a>
            </display:column>

            <display:column title="Last Modified (UTC)" property="lastmodified" sortable="true"/>

        </display:table>

        <c:url var="todayurl" value="fetchToday.action">
            <c:param name="planID" value="today"/>
        </c:url>
        <c:url var="yesterdayurl" value="fetchToday.action">
            <c:param name="planID" value="yesterday"/>
        </c:url>
        <c:url var="twodaysurl" value="fetchToday.action">
            <c:param name="planID" value="two_days_ago"/>
        </c:url>
        Open: <a href='<c:out value="${todayurl}"/>'>Today</a> <a href='<c:out value="${yesterdayurl}"/>'>Yesterday</a>
        <a href='<c:out value="${twodaysurl}"/>'>Two Days Ago</a>

    </fieldset>
</div>