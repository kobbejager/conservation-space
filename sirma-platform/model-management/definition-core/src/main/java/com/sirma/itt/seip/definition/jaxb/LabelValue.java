//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2014.12.13 at 09:54:45 AM EET
//

package com.sirma.itt.seip.definition.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for labelValue complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="labelValue"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="lang" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "labelValue", propOrder = { "value" })
public class LabelValue {

	/** The value. */
	@XmlValue
	protected String value;

	/** The lang. */
	@XmlAttribute(name = "lang", required = true)
	protected String lang;

	/**
	 * Gets the value of the value property.
	 *
	 * @return possible object is {@link String }
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
	 * Gets the value of the lang property.
	 *
	 * @return possible object is {@link String }
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * Sets the value of the lang property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setLang(String value) {
		lang = value;
	}

}
