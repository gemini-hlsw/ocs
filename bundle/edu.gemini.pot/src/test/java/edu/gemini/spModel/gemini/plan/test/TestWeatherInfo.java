package edu.gemini.spModel.gemini.plan.test;

import edu.gemini.spModel.gemini.plan.WeatherInfo;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.xml.PioXmlFactory;
import junit.framework.TestCase;

public class TestWeatherInfo extends TestCase {

    public void testParamSets() {
        WeatherInfo weatherInfo = new WeatherInfo(1, "12 deg", "13 kph", "WNW",
                "30.06 in", "77%", "?", "15%", "some comment 1");
        PioXmlFactory fact = new PioXmlFactory();
        ParamSet paramSet = weatherInfo.getParamSet(fact, "test");
        WeatherInfo copy = new WeatherInfo();
        copy.setParamSet(paramSet);
        assertEquals(weatherInfo, copy);
    }
}
