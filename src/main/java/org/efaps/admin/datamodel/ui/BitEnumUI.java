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

import java.io.Serializable;

import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class BitEnumUI
    implements IUIProvider, Serializable
{

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(final UIValue _uiValue)
        throws EFapsException
    {
        return _uiValue.getDbValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String validateValue(final UIValue _uiValue)
        throws EFapsException
    {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object transformObject(final UIValue _uiValue,
                                  final Object _object)
        throws EFapsException
    {
        if (_object instanceof Serializable) {
            _uiValue.setDbValue((Serializable) _object);
        }
        return _object;
    }
}
