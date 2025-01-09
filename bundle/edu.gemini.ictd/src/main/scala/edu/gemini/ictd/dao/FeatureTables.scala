package edu.gemini.ictd.dao

import shapeless.tag
import shapeless.tag.@@

/** Collection of tagged AvailabilityTable grouped by instrument feature
  * (filters, gratings, etc.).  The availability information is used to
  * determine the availability of corresponding Science Program enumerations.
  */
final case class FeatureTables(
  circular:   CircularTable,
  filter:     FilterTable,
  grating:    GratingTable,
  ifu:        IfuTable,
  longslit:   LongslitTable,
  nsLongslit: NSLongslitTable
)


object FeatureTables {

  val empty: FeatureTables = {
    def emptyTable[T]: AvailabilityTable @@ T =
      tag[T][AvailabilityTable](Map.empty)

    FeatureTables(
      emptyTable[CircularTag  ],
      emptyTable[FilterTag    ],
      emptyTable[GratingTag   ],
      emptyTable[IfuTag       ],
      emptyTable[LongslitTag  ],
      emptyTable[NSLongslitTag]
    )
  }

}
