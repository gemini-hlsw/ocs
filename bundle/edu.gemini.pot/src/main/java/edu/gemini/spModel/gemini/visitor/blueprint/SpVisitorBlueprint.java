package edu.gemini.spModel.gemini.visitor.blueprint;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.shared.util.immutable.ImOption;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.gemini.visitor.VisitorConfig;
import edu.gemini.spModel.gemini.visitor.VisitorConfig$;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;
import edu.gemini.spModel.pio.ParamSet;
import edu.gemini.spModel.pio.Pio;
import edu.gemini.spModel.pio.PioFactory;
import edu.gemini.spModel.template.SpBlueprint;

import java.util.Objects;

public final class SpVisitorBlueprint extends SpBlueprint {
    public static final String PARAM_SET_NAME    = "visitorBlueprint";
    public static final String NAME_PARAM_NAME   = "name";
    public static final String CONFIG_PARAM_NAME = "visitorConfig";

    public final String name;
    public final Option<VisitorConfig> visitorConfig;

    public SpVisitorBlueprint(String name, scala.Option<VisitorConfig> visitorConfig) {
        this(name, ImOption.fromScalaOpt(visitorConfig));
    }

    public SpVisitorBlueprint(String name, Option<VisitorConfig> visitorConfig) {
        if (name == null) throw new NullPointerException("'name' parameter cannot be null");
        if (visitorConfig == null) throw new NullPointerException("'visitorConfig' parameter cannot be null");

        this.name          = name;
        this.visitorConfig = visitorConfig;
    }

    public SpVisitorBlueprint(ParamSet paramSet) {
        this.name          = Pio.getValue(paramSet, NAME_PARAM_NAME, "Unknown");
        this.visitorConfig = ImOption.apply(Pio.getValue(paramSet, CONFIG_PARAM_NAME))
                              .flatMap(VisitorConfig$.MODULE$::findByNameJava);
    }

    public String paramSetName() { return PARAM_SET_NAME; }
    public SPComponentType instrumentType() { return VisitorInstrument.SP_TYPE; }

    public scala.Option<VisitorConfig> scalaVisitorConfig() {
        return ImOption.toScalaOpt(visitorConfig);
    }

    @Override
    public String toString() {
        return String.format("Visitor %s", name);
    }

    public ParamSet toParamSet(PioFactory factory) {
        ParamSet paramSet = factory.createParamSet(PARAM_SET_NAME);
        Pio.addParam(factory, paramSet, NAME_PARAM_NAME, name);
        visitorConfig.foreach(c -> Pio.addParam(factory,paramSet, CONFIG_PARAM_NAME, c.name()));

        return paramSet;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SpVisitorBlueprint that = (SpVisitorBlueprint) o;
        return name.equals(that.name) && visitorConfig.equals(that.visitorConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, visitorConfig);
    }
}
