//
// $
//

package edu.gemini.catalog.skycat.binding.adapter;

import edu.gemini.catalog.skycat.DefaultOutputAdapter;
import edu.gemini.catalog.skycat.SkycatOutputAdapter;
import edu.gemini.skycalc.DDMMSS;
import edu.gemini.skycalc.HHMMSS;
import edu.gemini.shared.util.immutable.Pair;
import edu.gemini.shared.util.immutable.Tuple2;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DefaultOutputAdapter} for use with the GSC2 @ STScI, which returns
 * its results in a different format than expected by the catalog output parser.
 */
@Deprecated
public final class Gsc2StsciOutputAdapter extends DefaultOutputAdapter {
    public static final String HEADER_LINE =
            "HSTGSID\tRa\tDec\tEpoch\tFpg\tJpg\tNpg\tUmag\tBmag\tVmag\tRmag\tImag\tc\tstatus\ta\tPA\te\tV\tM";

    public static final Factory FACTORY = new Factory() {
        public SkycatOutputAdapter create(BufferedReader rdr) {
            return new Gsc2StsciOutputAdapter(rdr);
        }
    };

    private enum State {
        header()  {

            public Tuple2<State,String> next(BufferedReader rdr) throws IOException {
                String line = rdr.readLine();
                if (line == null) return TERMINAL_TUP;

                // Ignore the line we just read.  The header is fixed.
                return new Pair<State,String>(separator, HEADER_LINE);
            }
        },

        separator {
            public Tuple2<State,String> next(BufferedReader rdr) {
                // A separator.
                return new Pair<State,String>(guidestar, "-");
            }
        },

        guidestar {
            // Relies on the exact output format that was current at the time
            // of writing....  It is fragile and will break when they change
            // their output again.
            public Tuple2<State,String> next(BufferedReader rdr) throws IOException {
                String res = rdr.readLine();
                if (res == null) return TERMINAL_TUP;

                String[] tokens = tokenize(res);

                // Strip out the GSCID, if present.  It will be tokens[1] at
                // this point.
                if (tokens.length == 42) {
                    String[] tmp = new String[41];
                    tmp[0] = tokens[0];
                    System.arraycopy(tokens, 2, tmp, 1, 40);
                    tokens = tmp;
                } else if (tokens.length != 41) {
                    // unexpected output
                    return TERMINAL_TUP;
                }

                StringBuilder buf = new StringBuilder();
                buf.append(tokens[0]).append('\t'); // HSTGSID

                // Append the Ra
                String raStr  = tokens[1] + ":" + tokens[2] + ":" + tokens[3];
                try {
                    buf.append(String.format("%.7f", HHMMSS.parse(raStr).getMagnitude()));
                    buf.append('\t');
                } catch (Exception ex) {
                    return TERMINAL_TUP;
                }

                // Append the Dec
                String decStr = tokens[4] + ":" + tokens[5] + ":" + tokens[6];
                if (decStr.startsWith("+")) decStr = decStr.substring(1);
                try {
                    buf.append(String.format("%.7f", DDMMSS.parse(decStr).getMagnitude()));
                    buf.append('\t');
                } catch (Exception ex) {
                    return TERMINAL_TUP;
                }

                // Append the Epoch
                buf.append(tokens[9]);

                // Append magnitudes
                // Fpg, Jpg, Npg, U, B, V, R, I
                int[] magIndices = {10, 13, 16, 19, 22, 25, 28, 31};
                for (int i : magIndices) {
                    buf.append('\t').append(tokens[i]);
                }

                // Append everything else.
                for (int i=34; i<tokens.length; ++i) {
                    buf.append('\t').append(tokens[i]);
                }

                return new Pair<State, String>(guidestar, buf.toString());
            }
        },

        terminal  {
            public Tuple2<State,String> next(BufferedReader rdr) {
                return TERMINAL_TUP;
            }
        };

        private static final Tuple2<State,String> TERMINAL_TUP =
                                 new Pair<State,String>(terminal, "");

        abstract Tuple2<State, String> next(BufferedReader rdr) throws IOException;

        // Get a list of tokens in the line (stripping out empty strings).
        private static String[] tokenize(String line) {
            List<String> tokens = new ArrayList<String>();
            String[] tokA = line.split("\\s");
            for (String tok : tokA) {
                if ("".equals(tok)) continue;
                tokens.add(tok);
            }
            return tokens.toArray(new String[tokens.size()]);
        }
    }

    private State state = State.header;

    /**
     * Constructs with the BufferedReader source of catalog data.
     */
    public Gsc2StsciOutputAdapter(BufferedReader rdr) {
        super(rdr);
    }

    public String readLine() throws IOException {
        Tuple2<State, String> tup = state.next(getReader());
        state = tup._1();
        if (state == State.terminal) return null;
        return tup._2();
    }}
