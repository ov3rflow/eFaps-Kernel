/*
 * Copyright 2003 - 2013 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.admin.datamodel.ui;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.util.EFapsException;

/**
 * Class to represent a Date for the user interface.
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class TypeUI
    extends AbstractUI
{

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReadOnlyHtml(final FieldValue _fieldValue)
        throws EFapsException
    {
        String ret = null;
        if (_fieldValue.getValue() instanceof Type) {
            final Type type = (Type) _fieldValue.getValue();
            ret = type.getLabel();
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(final FieldValue _fieldValue,
                       final FieldValue _fieldValue2)
    {
        String value = null;
        String value2 = null;
        if (_fieldValue.getValue() instanceof Type && _fieldValue2.getValue() instanceof Type) {
            value = DBProperties.getProperty(((Type) _fieldValue.getValue()).getName() + ".Label");
            value2 = DBProperties.getProperty(((Type) _fieldValue2.getValue()).getName() + ".Label");
        } else if (_fieldValue.getValue() instanceof String && _fieldValue2.getValue() instanceof String) {
            value = (String) _fieldValue.getValue();
            value2 = (String) _fieldValue2.getValue();
        }
        return value.compareTo(value2);
    }
}
