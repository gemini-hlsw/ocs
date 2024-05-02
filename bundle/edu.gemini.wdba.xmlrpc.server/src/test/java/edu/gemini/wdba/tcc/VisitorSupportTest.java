// Copyright (c) 2016-2018 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.target.obsComp.PwfsGuideProbe;

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
        verifyInstrumentConfig(getSouthResults(), "MAROONX");
    }

    @Test public void testMaroonX_P1() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.MaroonX$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getSouthResults(), "MAROONX_P1");
    }

    @Test public void testMaroonX_P2() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.MaroonX$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getSouthResults(), "MAROONX_P2");
    }

    @Test public void testAlopeke() throws Exception {

        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Alopeke$.MODULE$);
        setInstrument(visitor);
        assertEquals(
            "Expecting `FK5/J2000`",
            "FK5/J2000",
             getParam(getSouthResults(), TccNames.COSYS).orElse("not found")
        );
        verifyInstrumentConfig(getSouthResults(), "ALOPEKE2");
    }

    @Test public void testAlopeke_P1() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Alopeke$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getSouthResults(), "ALOPEKE2_P1");
    }

    @Test public void testAlopeke_P2() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Alopeke$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getSouthResults(), "ALOPEKE2_P2");
    }

    @Test public void testZorro() throws Exception {

        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Zorro$.MODULE$);
        setInstrument(visitor);
        verifyInstrumentConfig(getSouthResults(), "ZORRO2");
    }

    @Test public void testZorro_P1() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Zorro$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs1);
        verifyInstrumentConfig(getSouthResults(), "ZORRO2");
    }

    @Test public void testZorro_P2() throws Exception {
        final VisitorInstrument visitor = getInstrument();
        visitor.setVisitorConfig(VisitorConfig.Zorro$.MODULE$);
        setInstrument(visitor);
        addGuideStar(PwfsGuideProbe.pwfs2);
        verifyInstrumentConfig(getSouthResults(), "ZORRO2");
    }
}
