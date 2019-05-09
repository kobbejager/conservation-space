//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, v2.2.5-2
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2012.11.14 at 04:17:55 PM EET
//

package com.sirma.itt.seip.definition.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * <p>
 * Java class for uiParam complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="uiParam"&gt;
 *   &lt;simpleContent&gt;
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema&gt;string"&gt;
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/simpleContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "uiParam", propOrder = { "value" })
public class UiParam {

	@XmlValue
	protected String value;
	@XmlAttribute(name = "id", required = true)
	protected String id;
	@XmlAttribute(name = "type")
	protected String type; // NOSONAR
	@XmlAttribute(name = "name", required = true)
	protected String name;

	/**
	 * Gets the value of the value property.
	 *
	 * @return the value possible object is {@link String }
	 */
	public String getValue() {
		return value;
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
	 * Gets the value of the name property.
	 *
	 * @return the name possible object is {@link String }
	 */
	public String getName() {
		return name;
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
	 * Sets the value of the value property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Sets the value of the name property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setName(String value) {
		name = value;
	}

	/**
	 * Gets the type attribute of a <control-param> tag. This attribute is used to differentiate between control-params
	 * used for different purposes.
	 *
	 * @return the type as a String.
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
