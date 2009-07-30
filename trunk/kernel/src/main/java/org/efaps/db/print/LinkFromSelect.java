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

package org.efaps.db.print;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.Type;
import org.efaps.db.AbstractPrintQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.transaction.ConnectionResource;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
/**
 * Select Part for <code>linkfrom[TYPERNAME#ATTRIBUTENAME]</code>.
 */
public class LinkFromSelect extends AbstractPrintQuery
{
    /**
     * Name of the Attribute the link to is based on.
     */
    private final String attrName;

    /**
     * Type the {@link #attrName} belongs to.
     */
    private final Type type;

    /**
     * Did this query return a result.
     */
    private boolean hasResult;

    /**
     * @param _linkFrom linkfrom element of the query
     */
    public LinkFromSelect(final String _linkFrom)
    {
        final String[] linkfrom = _linkFrom.split("#");
        this.type = Type.get(linkfrom[0]);
        this.attrName = linkfrom[1];
        final OneSelect onsel = new OneSelect(this, _linkFrom);
        addOneSelect(onsel);
        onsel.setFromSelect(this);
        onsel.getSelectParts().add(new ISelectPart() {

            public Type getType()
            {
                return LinkFromSelect.this.type;
            }

            public int join(final OneSelect _oneselect, final StringBuilder _fromBldr, final int _relIndex)
            {
                // TODO Auto-generated method stub
                return 0;
            }
        });
    }

    /**
     * Getter method for instance variable {@link #hasResult}.
     *
     * @return value of instance variable {@link #hasResult}
     */
    public boolean hasResult()
    {
        return this.hasResult;
    }

    /**
     * Execute the from select for the given instance.
     * @param _instance instance
     * @throws EFapsException on error
     * @return true if statement didi return values, else false
     */
    public boolean execute(final Instance _instance) throws EFapsException
    {
        this.hasResult = executeOneCompleteStmt(createSQLStatement(_instance), getAllSelects());
        return this.hasResult;
    }

    /**
     * Method to create on Statement out of the different parts.
     * @param _instance instance
     * @return StringBuilder containing the sql statement
     */
    private StringBuilder createSQLStatement(final Instance _instance)
    {
        final Attribute attr = this.type.getAttribute(this.attrName);
        final StringBuilder selBldr = new StringBuilder();
        selBldr.append("select T0.ID, T0.").append(attr.getSqlColNames().get(0));

        final StringBuilder fromBldr = new StringBuilder();
        fromBldr.append(" from ").append(this.type.getMainTable().getSqlTable()).append(" T0");

        // on a from  select only on table is the base
        getAllSelects().get(0).append2SQLFrom(fromBldr);

        int colIndex = 3;
        for (final OneSelect oneSel : getAllSelects()) {
            colIndex += oneSel.append2SQLSelect(selBldr, colIndex);
        }

        final StringBuilder whereBldr = new StringBuilder();
        whereBldr.append(" where T0.").append(attr.getSqlColNames().get(0)).append("=")
            .append(_instance.getId());

        // in a subquery the type must also be set
        if (this.type.getMainTable().getSqlColType() != null) {
            whereBldr.append(" and T0.").append(this.type.getMainTable().getSqlColType()).append("=")
            .append(this.type.getId());
        }

        selBldr.append(fromBldr).append(whereBldr);

        if (LOG.isDebugEnabled()) {
            LOG.debug(selBldr.toString());
        }
        return selBldr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean executeOneCompleteStmt(final StringBuilder _complStmt, final List<OneSelect> _oneSelects)
        throws EFapsException
    {
        boolean ret = false;
        ConnectionResource con = null;
        try {
            con = Context.getThreadContext().getConnectionResource();

            if (AbstractPrintQuery.LOG.isDebugEnabled()) {
                AbstractPrintQuery.LOG.debug(_complStmt.toString());
            }

            final Statement stmt = con.getConnection().createStatement();

            final ResultSet rs = stmt.executeQuery(_complStmt.toString());

            while (rs.next()) {
                for (final OneSelect onesel : _oneSelects) {
                    onesel.addObject(rs);
                }
                ret = true;
            }

            rs.close();
            stmt.close();
            con.commit();
        } catch (final EFapsException e) {
            if (con != null) {
                con.abort();
            }
            throw e;
        } catch (final Throwable e) {
            if (con != null) {
                con.abort();
            }
            // TODO: exception eintragen!
            throw new EFapsException(getClass(), "executeOneCompleteStmt.Throwable");
        }
        return ret;
    }

    /**
     * Getter method for instance variable {@link #oneSelect}.
     *
     * @return value of instance variable {@link #oneSelect}
     */
    public OneSelect getMainOneSelect()
    {
        return getAllSelects().get(0);
    }

    /**
     * {@inheritDoc}
     */
    public Type getType()
    {
        return this.type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Instance getCurrentInstance()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Instance> getInstanceList()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Type getMainType()
    {
        // TODO Auto-generated method stub
        return null;
    }
}