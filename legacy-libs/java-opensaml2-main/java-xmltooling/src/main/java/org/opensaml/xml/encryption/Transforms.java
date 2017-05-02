/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.xml.encryption;

import java.util.List;

import javax.xml.namespace.QName;

import org.opensaml.xml.signature.Transform;
import org.opensaml.xml.util.XMLConstants;
import org.opensaml.xml.validation.ValidatingXMLObject;

/**
 * XMLObject representing XML Encryption, version 20021210, Transforms element.
 */
public interface Transforms extends ValidatingXMLObject {
    
    /** Element local name. */
    public static final String DEFAULT_ELEMENT_LOCAL_NAME = "Transforms";
    
    /** Default element name. */
    public static final QName DEFAULT_ELEMENT_NAME = new QName(XMLConstants.XMLENC_NS, DEFAULT_ELEMENT_LOCAL_NAME, 
            XMLConstants.XMLENC_PREFIX);
    
    /** Local name of the XSI type. */
    public static final String TYPE_LOCAL_NAME = "TransformsType"; 
        
    /** QName of the XSI type. */
    public static final QName TYPE_NAME = new QName(XMLConstants.XMLENC_NS, TYPE_LOCAL_NAME, 
            XMLConstants.XMLENC_PREFIX);
    
    /**
     * Get the list of Transform child elements.
     * 
     * @return a List of Transform child elements
     */
    public List<Transform> getTransforms();

}
