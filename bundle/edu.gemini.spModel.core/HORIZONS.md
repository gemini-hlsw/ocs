## Horizons Lookups

Ok, so [JPL HORIZONS](http://ssd.jpl.nasa.gov/?horizons) is an amazing tool for scientists but it's not designed for automation. And for this reason we have quite a bit of hacking surrounding it. This page intends to catalog our knowledge thus far.

HORIZONS lets you search for solar system objects which fall into three categories. It is best to think of it as **three databases** with a single extremely quirky UI. Keeping these categories separated is a major challenge.

The primary tasks we discuss here are:
- How to find an object **by name**.
- How to determine its **unique id**.
- How to look it up again.

Although we use the web interface to fetch data, the **telnet interface** is useful for experimenting.

```
telnet horizons.jpl.nasa.gov 6775
```

### Case 1 of 3: Comets

When **searching for a comet by name** the search string must be formatted as `COMNAM=foo*;CAP` indicating a substring search (not just a prefix, as you might guess), limiting results to the current apparition. The query `COMNAM=hu*;CAP` yields multiple results, which look like this:

```
*******************************************************************************
JPL/DASTCOM3               Small-body Search Results       2015-Oct-01 10:30:06

 SUB-STRING name search [SPACE sensitive]:                               
    COMNAM = hu*; CAP < 2457357.5;

 Matching small-bodies: 

    Record #  Epoch-yr  Primary Desig  Name                      COMNAM                   CAP     
    --------  --------  -------------  ------------------------- ------------------------ ------  
     900652     2015    67P             Churyumov-Gerasimenko     SCHUSTER                      
     900883     2015    106P            Schuster                  MCNAUGHT-HUGHES               
     900940     2013    130P            McNaught-Hughes           HUG-BELL                      
     901000     2014    178P            Hug-Bell                  PECHULE                       
     901436     1881    C/1880 Y1       Pechule                   HUBBLE                        
     901593     1936    C/1937 P1       Hubble                    HUMASON                       
     901669     1959    C/1960 M1       Humason                   WILSON-HUBBARD                
     901671     1961    C/1961 O1       Wilson-Hubbard            HUMASON                       
     901672     1962    C/1961 R1       Humason                   HUCHRA                        
     901717     1973    C/1973 H1       Huchra                    SCHUSTER                      
     901733     1975    C/1976 D2       Schuster                  CHURYUMOV-SOLODOVNIKOV        
     901794     1986    C/1986 N1       Churyumov-Solodovnikov    MCNAUGHT-HUGHES               
     901842     1991    C/1990 M1       McNaught-Hughes           ZHU-BALAM                     
     901977     1997    C/1997 L1       Zhu-Balam                 PANSTARRS                     

 (14 matches. To SELECT, enter record # (integer), followed by semi-colon.)
*******************************************************************************
```

The **primary designation** is provided in its own column. This is the unique identifier that will permit reproduceable lookups in the future, so this is the value we need to hang onto. Critically the record number is **not** unique. These are recycled and reassigned and should never be used.

The query `COMNAM=hubble;CAP` yields a single result, which looks like this:

```
*******************************************************************************
JPL/HORIZONS                 Hubble (C/1937 P1)            2015-Oct-01 10:32:44
Rec #:901593                    Soln.date: -                # obs: 58 (85 days)
 
FK5/J2000.0 helio. ecliptic osc. elements (au, days, deg., period=Julian yrs): 
 
  EPOCH=  2428480.5 ! 1936-Nov-08.0000000 (CT)     RMSW= n.a.                  
   EC= .972499             QR= 1.953657            TP= 2428486.7456            
   OM= 97.7957             W= 147.4924             IN= 11.5806                 

...
```

Here the primary designation `C/1937 P1` is given **in parentheses following the common name** in the header line.

The query `COMNAM=halley*;CAP` also gives a single result.
 
```
*******************************************************************************
JPL/HORIZONS                      1P/Halley                2015-Oct-01 11:12:16
Rec #:900033 (+COV)   Soln.date: 2001-Aug-02_13:51:39   # obs: 7428 (1835-1994)
 
FK5/J2000.0 helio. ecliptic osc. elements (au, days, deg., period=Julian yrs): 

...
```

But here the designation is given **before the common name, separated by a slash**. There may be yet other formats. Who knows!!?! (╯°□°）╯︵ ┻━┻)

---

When **looking up a comet by unique id** the search string must be formatted as `DES=foo;CAP` indicating an exact designation lookup, limiting results to the current apparition. For primary designations the result will be unique. So `DES=1P;CAP` will always yield Halley's comet:

```
*******************************************************************************
JPL/HORIZONS                      1P/Halley                2015-Oct-01 10:40:52
Rec #:900033 (+COV)   Soln.date: 2001-Aug-02_13:51:39   # obs: 7428 (1835-1994)
 
FK5/J2000.0 helio. ecliptic osc. elements (au, days, deg., period=Julian yrs): 
 
  EPOCH=  2449400.5 ! 1994-Feb-17.0000000 (CT)     RMSW= n.a.                  
   EC= .9671429084623044   QR= .5859781115169086   TP= 2446467.3953170511      
   OM= 58.42008097656843   W= 111.3324851045177    IN= 162.2626905791606       

...
```

### Case 2 of 3: Asteroids

When **searching for an asteroid by name** the search string must be formatted as `ASTNAM=foo*` indicating a substring search. The query `ASTNAM=her*` yields multiple results, which look like this:

```
*******************************************************************************
JPL/DASTCOM3           Small-body Index Search Results     2015-Oct-01 10:44:58

 Asteroids-only index search:

 ASTNAM = HER*;

 Matching small-bodies: 

    Record #  Primary Desig  >MATCH NAME<
    --------  -------------  -------------------------
        103   (undefined)    Hera
        121   (undefined)    Hermione
        135   (undefined)    Hertha
        206   (undefined)    Hersilia
        214   (undefined)    Aschera
        295   (undefined)    Theresia
        331   (undefined)    Etheridgea
        346   1892 P         Hermentaria
        458   1900 FK        Hercynia
        516   1903 MG        Amherstia
        532   1904 NY        Herculina
        546   1904 PA        Herodias
        567   1905 QP        Eleutheria
        568   1905 QS        Cheruskia

...
```

There are two cases to consider here:
- For some asteroids like Hera the primary designation is undefined, and in thes cases the **record number** is the unique identifier.
- For others the **primary designation** is the unique identifier that must be remembered.

There also two cases for lookups that yield a single result.

The query `ASTNAM=sedna` yields a single record with a primary designation given in parentheses following the common name.

```
*******************************************************************************
JPL/HORIZONS               90377 Sedna (2003 VB12)         2015-Oct-01 10:49:31
Rec #: 90377 (+COV)   Soln.date: 2015-Jan-08_06:07:56     # obs: 90 (1990-2014)
 
FK5/J2000.0 helio. ecliptic osc. elements (au, days, deg., period=Julian yrs): 
 
  EPOCH=  2453738.5 ! 2006-Jan-03.00 (CT)          Residual RMS= .39565        
   EC= .8458266195292785   QR= 76.02742907162026   TP= 2479549.2466984675      
   OM= 144.4870784415725   W=  311.5699312261542   IN= 11.93062491439996       

...
``` 

The query `ASTNAM=vesta` yields a single record with no primary designation. In this case the record number is given before the common name and again on the second line.

```
*******************************************************************************
JPL/HORIZONS                       4 Vesta                 2015-Oct-01 10:50:46
Rec #:     4 (+COV)   Soln.date: 2013-Jul-16_08:46:23   # obs: 7107 (1827-2013)
 
FK5/J2000.0 helio. ecliptic osc. elements (au, days, deg., period=Julian yrs): 
 
  EPOCH=  2451544.5 ! 2000-Jan-01.00 (CT)          Residual RMS= .30082        
   EC= .09002258422539088  QR= 2.148943570760723   TP= 2451614.8711416456      
   OM= 103.9514346862076   W=  149.5867755441192   IN= 7.133936541718016       

...
```
---

When **looking up an asteroid by unique id** there are two cases.
- For objects **without** a primary designation, the query string is simply the record number followed by a semicolon: `123;`
- For objects **with** a primary designation, the query string looks like `DES=2003 VB12`.

### Case 3 of 3: Major Bodies

Major bodies are defined as planets, natural satellites, spacecraft, and some "special cases" (per the HORIZONS documentation). When **searching for a major body by name** the query string must have the format `foo`.

The query `mar` yields multiple results:

```
*******************************************************************************
 Multiple major-bodies match string "MAR"

  ID#      Name                               Designation  IAU/aliases/other   
  -------  ---------------------------------- -----------  ------------------- 
        4  Mars Barycenter                                                      
      499  Mars                                                                 
      723  Margaret                           2003U3       UXXIII               
       -2  Mariner 2 (spacecraft)                                               
       -3  Mars Orbiter Mission (MOM) (spacec              Mangalyaan ISRO      
      -41  Mars Express (spacecraft)                       MEX                  

...
```

The **id number** is the unique identifier for these objects.

A single result looks like this:

```
Horizons> charon 
*******************************************************************************
 Revised: Aug 11, 2015          Charon / (Pluto)                            901 

 Fit to all available observations.

 SATELLITE GENERAL PHYSICAL PROPERTIES (PLU042, IAU2009, and derived values):
  GM (km^3/s^2)           = 102.271      Density (g cm^-3)      = 1.65
  Radius (km)             = 605          Geometric Albedo       = 0.372 +- .012

...
```

Here the **id number** is given in the right margin of the header line.

If there are **no matches** then the query **falls through to small body search** and the results will be misleading ... they are not the right object type. So code **must** detect and discard this case.

```
*******************************************************************************
JPL/DASTCOM3           Small-body Index Search Results     2015-Oct-01 11:06:28

 Comet AND asteroid index search:

   NAME = HALLEY;

 Matching small-bodies: 

    Record #  Epoch-yr  Primary Desig  >MATCH NAME<
    --------  --------  -------------  -------------------------
       2688             1982 HG1       Halley
     900001     -239    1P             Halley
     900002     -163    1P             Halley
     900003      -86    1P             Halley
     900004      -11    1P             Halley
     900005       66    1P             Halley

...
```

---

When **looking up a major body by unique id** the query is simply the id. For example `606` always returns Titan.

```
*******************************************************************************
 Revised: Mar 02, 2015              Titan / (Saturn)                        606
                         http://ssd.jpl.nasa.gov/?sat_phys_par
                           http://ssd.jpl.nasa.gov/?sat_elem

 SATELLITE PHYSICAL PROPERTIES:
  Mean Radius (km)       = 2575.5   +-  2.0  Density (g/cm^3) =  1.880 +- 0.004
  Mass (10^22 g)         = 13455.3           Geometric Albedo =  0.2 

...
```



