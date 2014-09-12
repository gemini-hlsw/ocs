//
// $
//

package edu.gemini.catalog.skycat.table;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

/**
 *
 */
public final class SampleData {
    public static final Object[] TABLE_HEADER = new Object[] {
        "2MASS", "RAJ2000",	"DEJ2000", "Jmag",   "e_Jmag",
         "Hmag", "e_Hmag",  "Kmag",    "e_Kmag", "Qflg",
         "Rflg", "Bflg",    "Cflg",    "Xflg",   "Aflg",
    };

    public static final Object[][] TABLE_DATA = new Object[][] {
            {"23594336-0016235", 	359.930670,	-00.273210,	15.664,	 0.102,	15.563,	 0.133,	15.002,	 0.147,	"ABB",	222,	111,	"ccc",	0,	0},
            {"23593472-0015389", 	359.894697,	-00.260815,	14.775,	 0.034,	14.290,	 0.051,	14.207,	 0.068,	"AAA",	222,	111,	"000",	0,	0},
            {"23594033-0015048", 	359.918049,	-00.251347,	15.914,	 0.067,	15.192,	 0.072,	15.296,	 0.158,	"AAC",	222,	111,	"00c",	0,	0},
            {"23594410-0016122", 	359.933764,	-00.270082,	16.135,	 0.105,	15.718,	 0.154,	15.762,	 0.307,	"ABD",	222,	111,	"ccc",	0,	0},
            {"23595911-0017492", 	359.996332,	-00.297000,	16.032,	 0.086,	15.828,	 0.157,	15.548,	 0.241,	"ACD",	222,	111,	"000",	0,	0},
    };

    public static TableModel createTableModel() {
        return new DefaultTableModel(TABLE_DATA, TABLE_HEADER);
    }
}
