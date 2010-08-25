/*
 * Copyright 2003 - 2010 The eFaps Team
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


package org.efaps.db.search.compare;

import org.efaps.db.search.QAttribute;
import org.efaps.db.search.value.AbstractQValue;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class QIn
    extends AbstractQAttrCompare
{
    /**
     * Constructor setting attribute and value.
     * @param _attribute Attribute to be checked for greater
     * @param _value     value as criteria
     */
    public QIn(final QAttribute _attribute,
               final AbstractQValue _value)
    {
        super(_attribute, _value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QIn appendSQL(final StringBuilder _sql)
        throws EFapsException
    {
        getAttribute().appendSQL(_sql);
        _sql.append(" IN (");
        getValue().appendSQL(_sql);
        _sql.append(")");
        return this;
    }
}