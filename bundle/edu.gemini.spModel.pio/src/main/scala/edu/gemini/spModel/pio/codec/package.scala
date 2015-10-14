package edu.gemini.spModel.pio

package object codec {

  implicit def toParamSetCodecOps[A: ParamSetCodec](a: A): CodecSyntax.ParamSetCodecOps[A] = 
    new CodecSyntax.ParamSetCodecOps(a)

  implicit def toParamSetOps(ps: ParamSet): CodecSyntax.ParamSetOps = 
    new CodecSyntax.ParamSetOps(ps)

}
