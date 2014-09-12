<!--
  This is the definition of a source that is used within the Phase 1
  (and Phase 2) documents.

  Authors: Kim Gillies (kgillies@gemini.edu), Shane Walker (swalker@gemini.edu)
  Create: 8 Feb 1998

  $Id: target.mod 392 2004-05-14 16:50:12Z brighton $
-->

<!--========================================================================-->

<!--A target has a name and can be one of five types.
    namedSystem - only has a name.  Information will be found in a catalog.
    conicSystem - the position will be calculated from the orbital elements.
    hmsdegSystem - one of the coordinate systems that use hours of time
                   for the first coordinate and degrees of arc for the second.
    degdegSystem - one of the coordinate systems that use degrees of
                   arc for both coordinates.
    nonSidSystem - a simple way of specifying the approximate position of a
                   non-sidereal object at a specific time and date.

    Each target has a name and an optional PCDATA element containing
    brightness information.
-->
<!ELEMENT targetName (#PCDATA)>
<!ELEMENT targetBrightness (#PCDATA)>

<!--  *****     *****     *****     ******     *****     *****     *****
    Targets are shared data for all the observatory obslist entities.
    Targets should have an "ID" that can be referenced in observations.
-->
<!-- VC: Target ID values must be unique in the proposal.  Authoring tools
         should name their target id values as "targetX" where X is a 
         number starting with 1 (e.g. target1, target2) -->
<!-- VC: A proposal with duplicate target ID values is invalid. -->
<!ELEMENT target (targetName, targetBrightness?, 
                  (namedSystem |
                   conicSystem | 
                  hmsdegSystem | 
                  degdegSystem |
                  nonSidSystem ))>
<!ATTLIST target
          id ID #REQUIRED
          type ( science | guide | wfs | oiwfs ) "science"
>

<!-- A namedSystem is an object with a name that can be 
     looked up in a catalog. It has a type of major or minor -->
<!ELEMENT namedSystem EMPTY>
<!ATTLIST namedSystem type ( major | 
                             minor |
                             planetarySatellite ) "major"
>

<!-- A conicSystem is represented by its orbital elements.  Its
     type can be minor planet, comet, or planetary satellite. 
     All values are text.  Correctness is up to programs, not the DTD. 
     The elements can be specified in three variants since major planets, 
     minor planets, and comets respectively tend to use different options
     (communication of Patrick Wallace). 
         element set     1              2              3

         epoch           t0             t0             T
         orbinc          i              i              i
         anode           Omega          Omega          Omega
         perih           curly pi       omega          omega
         aorq            a              a              q
         e               e              e              e
         aorl            L              M              -
         dm              n              -              -

     where:

         t0           is the epoch of the elements (MJD, TT)
         T              "    epoch of perihelion (MJD, TT)
         i              "    inclination (radians)
         Omega          "    longitude of the ascending node (degrees)
         curly pi       "    longitude of perihelion (degrees)
         omega          "    argument of perihelion (degrees)
         a              "    mean distance (AU)
         q              "    perihelion distance (AU)
         e              "    eccentricity
         L              "    longitude (degrees)
         M              "    mean anomaly (degrees)
         n              "    daily motion (degrees)
-->
<!ELEMENT conicSystem (epoch, 
                       inclination,
                       anode, 
                       perihelion, 
                       aorq, 
                       e, 
                       LorM?, 
                       n?)>
<!ATTLIST conicSystem type ( major |
                             minor | 
                             comet ) "comet"
>

<!-- The epoch of the orbital elements or epoch of perihelion (t0, T) -->
<!ELEMENT epoch (#PCDATA)>
<!ATTLIST epoch units CDATA #FIXED "years">

<!-- The inclination of the orbit (i) -->
<!ELEMENT inclination (#PCDATA)>
<!ATTLIST inclination units CDATA #FIXED "degrees">

<!-- The longitude of the ascending node -->
<!ELEMENT anode (#PCDATA)>
<!ATTLIST anode units CDATA #FIXED "degrees">

<!-- The argument of perihelion (Omega)-->
<!ELEMENT perihelion (#PCDATA)>
<!ATTLIST perihelion units CDATA #FIXED "degrees">

<!-- The mean distance (a) or perihelion distance (q) -->
<!ELEMENT aorq (#PCDATA)>
<!ATTLIST aorq units CDATA #FIXED "au">

<!-- Orbital eccentricity --> 
<!ELEMENT e (#PCDATA)>

<!-- The longitude (L) or mean anomaly (M) -->
<!ELEMENT LorM (#PCDATA)>
<!ATTLIST LorM units CDATA #FIXED "degrees">

<!-- The mean daily motion -->
<!ELEMENT n (#PCDATA)>
<!ATTLIST n units CDATA #FIXED "degrees-day">

<!-- c1 and c2 are the two coordinates in the other systems. -->
<!ELEMENT c1 (#PCDATA)>
<!ELEMENT c2 (#PCDATA)>

<!-- Target data is optional data that can provide additional data for
     targets specified with radec or altaz systems. -->
<!ELEMENT targetData (epoch?, 
                      pm1?, 
                      pm2?, 
                      rv?, 
                      parallax?, 
                      differentialTracking?)> 

<!-- Proper motion of C1 and c2 -->
<!ELEMENT pm1 (#PCDATA)>
<!ATTLIST pm1 
          units ( sec-year | arcsec-year ) "arcsec-year"
>
<!ELEMENT pm2 (#PCDATA)>
<!ATTLIST pm2 units CDATA #FIXED "arcsec-year">
<!-- Radial velocity of the target -->
<!ELEMENT rv (#PCDATA)>
<!ATTLIST rv units CDATA #FIXED "km-sec">
<!-- Parallax of the target -->
<!ELEMENT parallax (#PCDATA)>
<!ATTLIST parallax units CDATA #FIXED "arcsec">
<!-- TAI at which the tracking rates result in no tracking errors. -->
<!ELEMENT differentialTracking (taizDate, taizTime, c1Rate, c2Rate)>
<!-- Date and time when rate1 and rate2 are valid -->
<!ELEMENT taizDate (#PCDATA)>
<!ELEMENT taizTime (#PCDATA)>
<!-- Differential tracking rates in C1 and C2 -->
<!ELEMENT c1Rate (#PCDATA)>
<!ELEMENT c2Rate (#PCDATA)>

<!-- The radecSystem has its c1 position in hours, minutes, seconds.  Its
     C2 is in degrees, arc minutes, and arc seconds.  -->
<!ELEMENT hmsdegSystem (c1, c2, targetData?)>
<!ATTLIST hmsdegSystem type (J2000 | 
                             B1950 | 
                             apparent |
                             polarMount |
                             JNNNN |
                             BNNNN) "J2000"
>

<!-- The nonSidSystem has its c1 position in hours, minutes, seconds.  Its
     C2 is in degrees, minutes, and seconds.  A date says when the
     C1 C2 are accurate. -->
<!ELEMENT nonSidSystem (c1, c2, taizDate, taizTime?)>
<!ATTLIST nonSidSystem type (J2000 | 
                             B1950 | 
                             apparent) "J2000"
>

<!-- The altaz System has its c1 and c2 positions in degrees, 
     minutes, seconds. -->
<!ELEMENT degdegSystem (c1, c2, targetData?)>
<!ATTLIST degdegSystem type (galactic |
                             altaz |
                             altazMount) "altaz"
>
