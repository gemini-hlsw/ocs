<%@ include file="taglibs.jsp" %>

<div id="content">
    <p>Weather Information for Observing Log:
        <s:property value="planID"/>
    </p>

    <div id="winput">
        <fieldset>
            <legend>Enter New Weather Information</legend>

            <form method="post" action="updateWeather.action">
                <s:push value="weatherRow">
                    <input type="hidden" name="planID" value="<s:property value='planID'/>"/>
                    <input type="hidden" name="ID" value="<s:property value='ID'/>"/>
                    <label for="time" class="inbox">UTC Time</label><input type="text" name="time" readonly="readonly"
                                                                           class="inbox"
                                                                           value="<s:property value='time'/>"/><br/>
                    <label for="temperature" class="inbox">Temperature</label><input type="text" name="temperature"
                                                                                     class="inbox"
                                                                                     value="<s:property value='temperature'/>"/><br/>
                    <label for="windspeed" class="inbox">Wind Speed</label><input type="text" name="windspeed"
                                                                                  class="inbox"
                                                                                  value="<s:property value='windSpeed'/>"/><br/>
                    <label for="windirection" class="inbox">Wind Direction</label><input type="text" name="windirection"
                                                                                         class="inbox"
                                                                                         value="<s:property value='windDirection'/>"/><br/>
                    <label for="barometricpressure" class="inbox">Barometric Pressure</label><input type="text"
                                                                                                    name="barometricpressure"
                                                                                                    class="inbox"
                                                                                                    value="<s:property value='barometricPressure'/>"/><br/>
                    <label for="relativehumidity" class="inbox">Relative Humidity</label><input type="text"
                                                                                                name="relativehumidity"
                                                                                                class="inbox"
                                                                                                value="<s:property value='relativeHumidity'/>"/><br/>
                    <label for="dimm" class="inbox">DIMM</label><input type="text" name="dimm" class="inbox"
                                                                       value="<s:property value='dimm'/>"/><br/>
                    <label for="comment" class="inbox">Comment</label><textarea name="comment" class="inbox" rows="4">
                    <s:property value="comment"/>
                </textarea><br/>
                    <input type="submit" value="Update Weather" class="submit-button"/><br/>
                </s:push>
            </form>
        </fieldset>
    </div>


    <h3>Current Weather Entries</h3>
    <s:set name="wlog" value="weatherLog" scope="request"/>

</div>
