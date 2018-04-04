package edu.gemini.qpt.core.util;

import java.util.EnumSet;
import java.util.Set;

import edu.gemini.qpt.core.Variant;
import edu.gemini.qpt.core.Variant.Flag;
import edu.gemini.qpt.shared.sp.Obs;

public class ObsWeighter {

	private final Obs obs;
	private final Set<Flag> flags;

	private static final EnumSet<Flag> AUTOMATIC_DEATH_FLAGS = EnumSet.of(
        Flag.CONFIG_UNAVAILABLE,
		Flag.MASK_IN_CABINET,
        Flag.LGS_UNAVAILABLE,
		Flag.INSTRUMENT_UNAVAILABLE,
		Flag.ELEVATION_CNS,
		Flag.BACKGROUND_CNS,
		Flag.IQ_UQUAL,
		Flag.WV_UQUAL,
		Flag.CC_UQUAL,
		Flag.MULTI_CNS
	);

	public ObsWeighter(final Variant variant, final Obs obs) {
		this.obs = obs;
		this.flags = variant.getFlags(obs);
	}


	/**
	 * Score is between 0 and 1.
	 */
	public double getScore() {

		// All kinds of flags automatically give you a zero.
		for (Flag f: flags)
			if (AUTOMATIC_DEATH_FLAGS.contains(f))
				return 0;

		// Initial score is 1 / science band ^ 2
		double score = 1 / obs.getProg().getBand();
		score *= score;

		// Decrease if doesn't set early
		if (!flags.contains(Flag.SETS_EARLY))
			score *= 0.75;

		// Decrease if over-qualified
		if (flags.contains(Flag.OVER_QUALIFIED))
			score *= 0.8;

		// Decrease lot if not started
		if (!flags.contains(Flag.IN_PROGRESS))
			score *= 0.5;

		return score;

	}

}
