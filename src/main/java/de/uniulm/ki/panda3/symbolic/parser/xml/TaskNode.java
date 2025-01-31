// PANDA 3 -- a domain-independent planner for classical and hierarchical planning
// Copyright (C) 2014-2018 the original author or authors.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package de.uniulm.ki.panda3.symbolic.parser.xml;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java-Klasse für anonymous complex type.
 * <p>
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * <p>
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}variableDeclaration" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="taskSchema" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="displayName" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" default="" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "variableDeclaration"
})
@XmlRootElement(name = "taskNode")
public class TaskNode {

    protected List<VariableDeclaration> variableDeclaration;
    @XmlAttribute(name = "name", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String name;
    @XmlAttribute(name = "taskSchema", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object taskSchema;
    @XmlAttribute(name = "displayName")
    @XmlSchemaType(name = "anySimpleType")
    protected String displayName;

    /**
     * Gets the value of the variableDeclaration property.
     * <p>
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the variableDeclaration property.
     * <p>
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getVariableDeclaration().add(newItem);
     * </pre>
     * <p>
     * <p>
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link VariableDeclaration }
     */
    public List<VariableDeclaration> getVariableDeclaration() {
        if (variableDeclaration == null) {
            variableDeclaration = new ArrayList<VariableDeclaration>();
        }
        return this.variableDeclaration;
    }

    /**
     * Ruft den Wert der name-Eigenschaft ab.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Legt den Wert der name-Eigenschaft fest.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Ruft den Wert der taskSchema-Eigenschaft ab.
     *
     * @return possible object is
     * {@link Object }
     */
    public Object getTaskSchema() {
        return taskSchema;
    }

    /**
     * Legt den Wert der taskSchema-Eigenschaft fest.
     *
     * @param value allowed object is
     *              {@link Object }
     */
    public void setTaskSchema(Object value) {
        this.taskSchema = value;
    }

    /**
     * Ruft den Wert der displayName-Eigenschaft ab.
     *
     * @return possible object is
     * {@link String }
     */
    public String getDisplayName() {
        if (displayName == null) {
            return "";
        } else {
            return displayName;
        }
    }

    /**
     * Legt den Wert der displayName-Eigenschaft fest.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDisplayName(String value) {
        this.displayName = value;
    }

}
