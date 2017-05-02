//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.6
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2013.09.25 at 07:28:31 PM EEST
//

package com.sirma.itt.seip.definition.jaxb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for transitionDefinition complex type.
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="transitionDefinition"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="condition" type="{}conditionDefinition" maxOccurs="unbounded" minOccurs="0"/&gt;
 *         &lt;element name="fields" type="{}complexFieldsDefinition" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" use="required" type="{}idType" /&gt;
 *       &lt;attribute name="displayType" type="{}displayTypeType" default="editable" /&gt;
 *       &lt;attribute name="label" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="tooltip" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="eventId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="nextPrimaryState" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="nextSecondaryState" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="defaultTransition" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;attribute name="immediate" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" /&gt;
 *       &lt;attribute name="purpose" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="order" type="{http://www.w3.org/2001/XMLSchema}integer" /&gt;
 *       &lt;attribute name="confirmation" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="disabledReason" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "transitionDefinition", propOrder = { "condition", "fields" })
public class TransitionDefinition {

	/** The condition. */
	protected List<ConditionDefinition> condition;

	/** The fields. */
	protected ComplexFieldsDefinition fields;

	/** The id. */
	@XmlAttribute(name = "id", required = true)
	protected String id;

	/** The display type. */
	@XmlAttribute(name = "displayType")
	protected String displayType;

	/** The label. */
	@XmlAttribute(name = "label", required = true)
	protected String label;

	/** The tooltip. */
	@XmlAttribute(name = "tooltip")
	protected String tooltip;

	/** The event id. */
	@XmlAttribute(name = "eventId", required = true)
	protected String eventId;

	/** The next primary state. */
	@XmlAttribute(name = "nextPrimaryState")
	protected String nextPrimaryState;

	/** The next secondary state. */
	@XmlAttribute(name = "nextSecondaryState")
	protected String nextSecondaryState;

	/** The default transition. */
	@XmlAttribute(name = "defaultTransition")
	protected Boolean defaultTransition;

	/** The immediate. */
	@XmlAttribute(name = "immediate")
	protected Boolean immediate;

	/** The purpose. */
	@XmlAttribute(name = "purpose")
	protected String purpose;

	/** The order. */
	@XmlAttribute(name = "order")
	protected BigInteger order;

	/** The confirmation. */
	@XmlAttribute(name = "confirmation")
	protected String confirmation;

	/** The disabled reason. */
	@XmlAttribute(name = "disabledReason")
	protected String disabledReason;

	/**
	 * Gets the value of the condition property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the condition property.
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
			condition = new ArrayList<>();
		}
		return condition;
	}

	/**
	 * Gets the value of the fields property.
	 *
	 * @return the fields possible object is {@link ComplexFieldsDefinition }
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
	 * Gets the value of the displayType property.
	 *
	 * @return the display type possible object is {@link String }
	 */
	public String getDisplayType() {
		if (displayType == null) {
			return "editable";
		} else {
			return displayType;
		}
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
	 * @return the label possible object is {@link String }
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
	 * @return the tooltip possible object is {@link String }
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

	/**
	 * Gets the value of the eventId property.
	 *
	 * @return the event id possible object is {@link String }
	 */
	public String getEventId() {
		return eventId;
	}

	/**
	 * Sets the value of the eventId property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setEventId(String value) {
		eventId = value;
	}

	/**
	 * Gets the value of the nextPrimaryState property.
	 *
	 * @return the next primary state possible object is {@link String }
	 */
	public String getNextPrimaryState() {
		return nextPrimaryState;
	}

	/**
	 * Sets the value of the nextPrimaryState property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setNextPrimaryState(String value) {
		nextPrimaryState = value;
	}

	/**
	 * Gets the value of the nextSecondaryState property.
	 *
	 * @return the next secondary state possible object is {@link String }
	 */
	public String getNextSecondaryState() {
		return nextSecondaryState;
	}

	/**
	 * Sets the value of the nextSecondaryState property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setNextSecondaryState(String value) {
		nextSecondaryState = value;
	}

	/**
	 * Gets the value of the defaultTransition property.
	 *
	 * @return true, if is default transition possible object is {@link Boolean }
	 */
	public boolean isDefaultTransition() {
		if (defaultTransition == null) {
			return false;
		} else {
			return defaultTransition;
		}
	}

	/**
	 * Sets the value of the defaultTransition property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 */
	public void setDefaultTransition(Boolean value) {
		defaultTransition = value;
	}

	/**
	 * Gets the value of the immediate property.
	 *
	 * @return true, if is immediate possible object is {@link Boolean }
	 */
	public boolean isImmediate() {
		if (immediate == null) {
			return false;
		} else {
			return immediate;
		}
	}

	/**
	 * Sets the value of the immediate property.
	 *
	 * @param value
	 *            allowed object is {@link Boolean }
	 */
	public void setImmediate(Boolean value) {
		immediate = value;
	}

	/**
	 * Gets the value of the purpose property.
	 *
	 * @return the purpose possible object is {@link String }
	 */
	public String getPurpose() {
		return purpose;
	}

	/**
	 * Sets the value of the purpose property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setPurpose(String value) {
		purpose = value;
	}

	/**
	 * Gets the value of the order property.
	 *
	 * @return the order possible object is {@link BigInteger }
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
	 * Gets the value of the confirmation property.
	 *
	 * @return the confirmation possible object is {@link String }
	 */
	public String getConfirmation() {
		return confirmation;
	}

	/**
	 * Sets the value of the confirmation property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setConfirmation(String value) {
		confirmation = value;
	}

	/**
	 * Gets the value of the disabledReason property.
	 *
	 * @return the disabled reason possible object is {@link String }
	 */
	public String getDisabledReason() {
		return disabledReason;
	}

	/**
	 * Sets the value of the disabledReason property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 */
	public void setDisabledReason(String value) {
		disabledReason = value;
	}

}
