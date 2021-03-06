package fr.ms.lang.stringmaker.impl;

import fr.ms.lang.delegate.StringMakerFactory;

public class StringBufferFactory implements StringMakerFactory {

	public StringMaker newString() {
		return new StringBufferImpl();
	}

	public StringMaker newString(final int capacity) {
		return new StringBufferImpl(capacity);
	}

	public StringMaker newString(final String str) {
		return new StringBufferImpl(str);
	}

	public int getPriority() {
		return 3;
	}
}
