// This software is Copyright(c) 2010 Association of Universities for
// Research in Astronomy, Inc.  This software was prepared by the
// Association of Universities for Research in Astronomy, Inc. (AURA)
// acting as operator of the Gemini Observatory under a cooperative
// agreement with the National Science Foundation. This software may 
// only be used or copied as described in the license set out in the 
// file LICENSE.TXT included with the distribution package.

package edu.gemini.itc.shared;

/**
 * The Morphology3D abstract class specifies the operations that all
 * of the Morphologies should implement.  While right now it is minimal
 * later on it can grow.
 */
public abstract class Morphology3D implements VisitableMorphology {
    public abstract double get2DSquareIntegral(double xMin, double xMax, double yMin, double yMax);
}
