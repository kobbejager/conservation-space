package com.sirma.itt.seip.convert;

import java.util.Objects;

import com.sirma.itt.seip.exception.EmfConfigurationException;
import com.sirma.itt.seip.runtime.boot.Startup;
import com.sirma.itt.seip.runtime.boot.StartupPhase;

/**
 * Utility class that provides a singleton/static access to {@link TypeConverter} functionality.
 *
 * @author BBonev
 */
public class TypeConverterUtil {

	/**
	 * Instantiates a new type converter util.
	 */
	private TypeConverterUtil() {
		// disable instance creation
	}

	/** The type converter. */
	private static TypeConverter typeConverter;

	/**
	 * Gets the converter. If no converter implementation is found then the method will throw a
	 * {@link EmfConfigurationException}
	 *
	 * @return the converter
	 */
	public static TypeConverter getConverter() {
		if (typeConverter == null) {
			throw new EmfConfigurationException("TypeConverter implementation not found!");
		}
		return typeConverter;
	}

	/**
	 * Setter method for typeConverter.
	 *
	 * @param typeConverter
	 *            the typeConverter to set
	 */
	@Startup(phase = StartupPhase.DEPLOYMENT, name = "typeConverterInit")
	public static void setTypeConverter(TypeConverter typeConverter) {
		Objects.requireNonNull(typeConverter);
		TypeConverterUtil.typeConverter = typeConverter;
	}

}
