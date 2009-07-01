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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.efaps.db.query.CachedResult;

/**
 * @author The eFaps Team
 * @version $Id$
 */
public class StringType extends AbstractType
{

    // ///////////////////////////////////////////////////////////////////////////

    /**
     * @see #getValue
     * @see #setValue
     */
    private  String value = null;


    /**
     * @see org.efaps.admin.datamodel.attributetype.AbstractLinkType#update(java.lang.Object, java.sql.PreparedStatement, int)
     * @param _object   object
     * @param _stmt     SQL statement to update the value
     * @param _index    index in the SQL statement to update the value
     * @return number of indexes used in the method, if the return value is null an error should be thrown
     * @throws SQLException on error
     */
    public int update(final Object _object, final PreparedStatement _stmt, final int _indexes)
                    throws SQLException
    {
        _stmt.setString(_indexes,  this.value);
        return 1;
    }

    public Object readValue(final CachedResult _rs, final List<Integer> _indexes)
    {

        this.value = (_rs.getString(_indexes.get(0).intValue()));
        if ( this.value != null) {
            this.value =  this.value.trim();
        }
        return  this.value;
    }

    /**
     * The localised string and the internal string value are equal. So the
     * internal value can be set directly with method {@link #setValue}.
     *
     * @param _context context for this request
     * @param _value new value to set
     */
    public void set(final Object[] _value)
    {
        if (_value[0] instanceof String) {
            this.value = ((String) _value[0]);
        } else if (_value[0] != null) {
            this.value = (_value[0].toString());
        }
    }

    /**
     * Getter method for instance variable {@link #value}.
     *
     * @return value of instance variable {@link #value}
     */
    protected String getValue()
    {
        return this.value;
    }

    /**
     * Setter method for instance variable {@link #value}.
     *
     * @param value value for instance variable {@link #value}
     */
    protected void setValue(final String value)
    {
        this.value = value;
    }

    @Override
    public String toString()
    {
        return "" + this.value;
    }
}