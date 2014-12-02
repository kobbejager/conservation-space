//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2013.02.15 at 03:17:22 PM EET
//

package com.sirma.itt.emf.definition.model.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for filterDefinition complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="filterDefinition">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>string">
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="mode" type="{http://www.w3.org/2001/XMLSchema}string" default="INCLUDE" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterDefinition", propOrder = { "value" })
public class FilterDefinition {

	/** The value. */
	@XmlValue
	protected String value;

	/** The id. */
	@XmlAttribute(name = "id", required = true)
	protected String id;

	/** The mode. */
	@XmlAttribute(name = "mode")
	protected String mode;

	/**
	 * Gets the value of the value property.
	 *
	 * @return the value possible object is {@link String }
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value of the value property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Gets the value of the id property.
	 *
	 * @return the id possible object is {@link String }
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the value of the id property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setId(String value) {
		id = value;
	}

	/**
	 * Gets the value of the mode property.
	 *
	 * @return the mode possible object is {@link String }
	 */
	public String getMode() {
		if (mode == null) {
			return "INCLUDE";
		} else {
			return mode;
		}
	}

	/**
	 * Sets the value of the mode property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setMode(String value) {
		mode = value;
	}

}
