//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB)
// Reference Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source
// schema.
// Generated on: 2013.02.16 at 02:48:56 PM EET
//

package com.sirma.itt.emf.definition.model.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for regionDefinition complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="regionDefinition">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="control" type="{}controlDefinition" minOccurs="0"/>
 *         &lt;element name="condition" type="{}conditionDefinition" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="fields" type="{}complexFieldsDefinition" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="order" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="displayType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="label" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="tooltip" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "regionDefinition", propOrder = { "control", "condition", "fields" })
public class RegionDefinition {

	/** The control. */
	protected ControlDefinition control;

	/** The condition. */
	protected List<ConditionDefinition> condition;

	/** The fields. */
	protected ComplexFieldsDefinition fields;

	/** The order. */
	@XmlAttribute(name = "order")
	protected BigInteger order;

	/** The id. */
	@XmlAttribute(name = "id", required = true)
	protected String id;

	/** The display type. */
	@XmlAttribute(name = "displayType")
	protected String displayType;

	/** The label. */
	@XmlAttribute(name = "label")
	protected String label;

	/** The tooltip. */
	@XmlAttribute(name = "tooltip")
	protected String tooltip;

	/**
	 * Gets the value of the control property.
	 *
	 * @return possible object is {@link ControlDefinition }
	 */
	public ControlDefinition getControl() {
		return control;
	}

	/**
	 * Sets the value of the control property.
	 *
	 * @param value
	 *            allowed object is {@link ControlDefinition }
	 */
	public void setControl(ControlDefinition value) {
		control = value;
	}

	/**
	 * Gets the value of the condition property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the condition property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getCondition().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 *
	 * @return the condition {@link ConditionDefinition }
	 */
	public List<ConditionDefinition> getCondition() {
		if (condition == null) {
			condition = new ArrayList<ConditionDefinition>();
		}
		return condition;
	}

	/**
	 * Gets the value of the fields property.
	 *
	 * @return possible object is {@link ComplexFieldsDefinition }
	 */
	public ComplexFieldsDefinition getFields() {
		return fields;
	}

	/**
	 * Sets the value of the fields property.
	 *
	 * @param value
	 *            allowed object is {@link ComplexFieldsDefinition }
	 */
	public void setFields(ComplexFieldsDefinition value) {
		fields = value;
	}

	/**
	 * Gets the value of the order property.
	 *
	 * @return possible object is {@link BigInteger }
	 */
	public BigInteger getOrder() {
		return order;
	}

	/**
	 * Sets the value of the order property.
	 *
	 * @param value
	 *            allowed object is {@link BigInteger }
	 */
	public void setOrder(BigInteger value) {
		order = value;
	}

	/**
	 * Gets the value of the id property.
	 *
	 * @return possible object is {@link String }
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
	 * Gets the value of the displayType property.
	 *
	 * @return possible object is {@link String }
	 */
	public String getDisplayType() {
		return displayType;
	}

	/**
	 * Sets the value of the displayType property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setDisplayType(String value) {
		displayType = value;
	}

	/**
	 * Gets the value of the label property.
	 *
	 * @return possible object is {@link String }
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Sets the value of the label property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setLabel(String value) {
		label = value;
	}

	/**
	 * Gets the value of the tooltip property.
	 *
	 * @return possible object is {@link String }
	 */
	public String getTooltip() {
		return tooltip;
	}

	/**
	 * Sets the value of the tooltip property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setTooltip(String value) {
		tooltip = value;
	}

}
