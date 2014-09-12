package edu.gemini.util;

import java.io.Serializable;

import java.util.Iterator;

public class RemoteIteratorWrapper<T extends Serializable> implements RemoteIterator<T> {

	private final Iterator<T> it;
	
	public RemoteIteratorWrapper(final Iterator<T> it) {
		this.it = it;
	}

	public boolean hasNext()  {
		return it.hasNext();
	}

	public T next()  {
		return it.next();
	}

	public void remove()  {
		it.remove();
	}

}
