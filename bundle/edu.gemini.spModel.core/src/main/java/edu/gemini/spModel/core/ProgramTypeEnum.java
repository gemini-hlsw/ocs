package edu.gemini.spModel.core;

/**
 * A program type enum companion to the Scala ProgramType, meant to facilitate
 * use from Java. The values here parallel those in ProgramType, and are
 * obtainable from a ProgramType via the typeEnum method.
 */
public enum ProgramTypeEnum {
    C,
    CAL,
    DD,
    DS,
    ENG,
    FT,
    LP,
    Q,
    SV
    ;
}
