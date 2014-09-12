package edu.gemini.jca;

import edu.gemini.jca.impl.ProcessVariableImpl;

import java.lang.reflect.InvocationTargetException;

public class ProcessVariableFactory {

	public enum EnumMapMode {
		EPICS_STRING,
		EPICS_SHORT,
		EPICS_INTEGER
	}

	/**
	 * Creates a process variable that maps Java type T to its equivalent EPICS
	 * type. Currently only Java primitives (and String) and their wrappers are
	 * supported.
	 * @param <T>
	 * @param channel
	 * @return
	 */
	public <T> ProcessVariable<T> create(String channel) {
		return new ProcessVariableImpl.Identity<T>(channel);
	}

	/**
	 * Creates a process variable that maps Java type enum type T to an EPICS
	 * type based on the passed EnumMapMode.
	 * @param <T>
	 * @param channel
	 * @param mapMode
	 * @return
	 */
	public <T extends Enum<T>> ProcessVariable<T> create(String channel, Class<T> type, EnumMapMode mapMode) {
		switch (mapMode) {
		case EPICS_SHORT:
			return new ProcessVariableImpl.EnumShort<T>(channel, values(type));

		case EPICS_STRING:
			return new ProcessVariableImpl.EnumString<T>(channel, type);

		case EPICS_INTEGER:
			return new ProcessVariableImpl.EnumInt<T>(channel, values(type));

		default:
			throw new IllegalArgumentException("Unknown mapMode " + mapMode);
		}
	}

	/**
	 * Creates a process variable that maps between arbitrary EPICS type ET and
	 * Java type JT.
	 * @param <JT>
	 * @param <ET>
	 * @param channel fully qualified channel name
	 * @param adapter an adapter to convert between Java and EPICS
	 * @return
	 */
	public <JT, ET> ProcessVariable<JT> create(String channel, final ProcessVariableAdapter<JT, ET> adapter) {
		return new ProcessVariableImpl<JT, ET>(channel) {
			@Override
			protected ET toEpics(JT o) {
				return adapter.toEpics(o);
			}
			@Override
			protected JT fromEpics(ET o) {
				return adapter.fromEpics(o);
			}
		};
	}

	@SuppressWarnings("unchecked")
	private <T extends Enum> T[] values(Class<T> type) {
		try {
			return  (T[]) type.getMethod("values").invoke(null);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private ProcessVariableFactory() {
	}

	public static ProcessVariableFactory newInstance() {
		return new ProcessVariableFactory();
	}

}

