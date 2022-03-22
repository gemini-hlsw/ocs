// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class VisitorSupportTest extends InstrumentSupportTestBase<VisitorInstrument> {

    public VisitorSupportTest() {
       super(VisitorInstrument.SP_TYPE);
    }

    @Test public void testMaroonX() throws Exception {

        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.MaroonX$.MODULE$);
        setInstrument(visitor);

        assertEquals(
            "Expecting `Fixed`",
            "Fixed",
            getParam(getSouthResults(), TccNames.COSYS).orElse("not found")
        );
    }

    @Test public void testOther() throws Exception {

        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Alopeke$.MODULE$);
        setInstrument(visitor);

        assertEquals(
            "Expecting `FK5/J2000`",
            "FK5/J2000",
             getParam(getSouthResults(), TccNames.COSYS).orElse("not found")
        );
    }
}
