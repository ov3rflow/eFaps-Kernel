/*
 * Copyright 2003 - 2009 The eFaps Team
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

package org.efaps.admin.datamodel.attributetype;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.efaps.admin.datamodel.Dimension;
import org.efaps.util.EFapsException;

/**
 * @author The eFaps Team
 * @version $Id$
 */
public class DecimalWithUoMType extends AbstractWithUoMType
{
    /**
     * @see #getValue
     * @see #setValue
     */
    private BigDecimal value = new BigDecimal(0);

    /**
     * The localised string and the internal string value are equal. So the
     * internal value can be set directly with method {@link #setValue}.
     *
     * @param _value new value to set
     * @throws EFapsException on error
     */
    public void set(final Object[] _value) throws EFapsException
    {
        if (_value instanceof Object[]) {
            if ((_value[0] instanceof String) && (((String) _value[0]).length() > 0)) {
                this.value = DecimalType.parseLocalized((String) _value[0]);
            } else if (_value[0] instanceof BigDecimal) {
                this.value = (BigDecimal) _value[0];
            } else if (_value[0] instanceof Number) {
                this.value = new BigDecimal(((Number) _value[0]).toString());
            }
            if ((_value[1] instanceof String) && (((String) _value[1]).length() > 0)) {
                setUoM(Dimension.getUoM(Long.parseLong((String) _value[1])));
            } else if (_value[1] instanceof Number) {
                setUoM(Dimension.getUoM(((Number) _value[1]).longValue()));
            }
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    protected Double getValue()
    {
        return this.value.doubleValue();
    }

    /**
     *{@inheritDoc}
     */
    @Override
    protected void setValueStmt(final PreparedStatement _stmt, final int _index) throws SQLException
    {
        _stmt.setBigDecimal(_index, this.value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object readValue(final Object _object)
    {
        if (_object instanceof BigDecimal) {
            this.value = (BigDecimal) _object;
        } else if (_object != null) {
            this.value = new BigDecimal(_object.toString());
        }
        return this.value;
    }
}
