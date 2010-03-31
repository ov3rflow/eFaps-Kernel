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


package org.efaps.db;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.datamodel.SQLTable;
import org.efaps.admin.datamodel.Type;
import org.efaps.db.search.And;
import org.efaps.db.search.Equal;
import org.efaps.db.search.NumberValue;
import org.efaps.db.search.QueryAttribute;
import org.efaps.db.search.Where;
import org.efaps.db.transaction.ConnectionResource;
import org.efaps.db.wrapper.SQLSelect;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class Query
{
    /**
     * Logging instance used in this class.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(PrintQuery.class);

    /**
     * Base type this query is searching on.
     */
    private final Type baseType;

    /**
     * The where criteria for this search.
     */
    private Where where;

    /**
     * Should the child types be also be included in this search?
     */
    private boolean includeChildTypes = true;

    /**
     * Map to store the table to index relation.
     */
    private final Map<SQLTable, Integer> sqlTable2Index = new HashMap<SQLTable, Integer>();


    /**
     * Must this query be executed company depended.
     * (if the type is company dependend)
     */
    private boolean companyDepended = true;


    /**
     * Constructor setting the type by his UUID.
     * @param _typeUUI UUID of the Type the query is based on
     */
    public Query(final UUID _typeUUI)
    {
        this(Type.get(_typeUUI));
    }

    /**
     * Constructor setting the type.
     * @param _type TYpe the query is based on
     */
    public Query(final Type _type)
    {
        this.baseType = _type;
    }

    /**
     * Getter method for the instance variable {@link #companyDepended}.
     *
     * @return value of instance variable {@link #companyDepended}
     */
    public boolean isCompanyDepended()
    {
        return this.companyDepended;
    }


    /**
     * Setter method for instance variable {@link #companyDepended}.
     *
     * @param _companyDepended value for instance variable {@link #companyDepended}
     * @return this
     */

    public Query setCompanyDepended(final boolean _companyDepended)
    {
        this.companyDepended = _companyDepended;
        return this;
    }


    /**
     * Getter method for the instance variable {@link #includeChildTypes}.
     *
     * @return value of instance variable {@link #includeChildTypes}
     */
    public boolean isIncludeChildTypes()
    {
        return this.includeChildTypes;
    }


    /**
     * Setter method for instance variable {@link #includeChildTypes}.
     *
     * @param _includeChildTypes value for instance variable {@link #includeChildTypes}
     * @return this
     */

    public Query setIncludeChildTypes(final boolean _includeChildTypes)
    {
        this.includeChildTypes = _includeChildTypes;
        return this;
    }


    /**
     * Getter method for the instance variable {@link #baseType}.
     *
     * @return value of instance variable {@link #baseType}
     */
    public Type getBaseType()
    {
        return this.baseType;
    }

    /**
     * Getter method for the instance variable {@link #sqlTable2Index}.
     *
     * @return value of instance variable {@link #sqlTable2Index}
     */
    public Map<SQLTable, Integer> getSqlTable2Index()
    {
        return this.sqlTable2Index;
    }

    /**
     * setter method for the instance variable {@link #where}.
     * @param _where value for instance variable {@link #where}
     * @return this
     */
    public Query setWhere(final Where _where)
    {
        this.where = _where;
        return this;
    }


    /**
     * Execute the Query.
     * @return this
     * @throws EFapsException
     * TODO Accesscheck
     */
    public Query execute()
        throws EFapsException
    {
        executeWithoutAccessCheck();
        return this;
    }

    /**
     * The instance method executes the query without an access check.
     *
     * @return true if the query contains values, else false
     * @throws EFapsException on error
     */
    public Query executeWithoutAccessCheck()
        throws EFapsException
    {

        prepareQuery();
        executeOneCompleteStmt(createSQLStatement());
        return this;
    }

    /**
     * Prepare the Query for execution.
     * @throws EFapsException on error
     */
    private void prepareQuery()
        throws EFapsException
    {
        this.sqlTable2Index.put(this.baseType.getMainTable(), 0);
        if (this.baseType.getMainTable().getSqlColType() != null) {
            final Equal eqPart = new Equal(new QueryAttribute(this.baseType.getTypeAttribute()),
                                           new NumberValue(this.baseType.getId()));
            if (this.includeChildTypes && !this.baseType.getChildTypes().isEmpty()) {
                for (final Type type : this.baseType.getChildTypes()) {
                    eqPart.addValue(new NumberValue(type.getId()));
                }
            }
            if (this.where == null) {
                this.where = new Where(eqPart);
            } else {
                this.where.setPart(new And(this.where.getPart(), eqPart));
            }
        }
        if (this.companyDepended && this.baseType.isCompanyDepended()) {
            if (Context.getThreadContext().getCompany() == null) {
                throw new EFapsException(Query.class, "noCompany");
            }
            final Equal eqPart = new Equal(new QueryAttribute(this.baseType.getCompanyAttribute()),
                                           new NumberValue(Context.getThreadContext().getCompany().getId()));
            if (this.where == null) {
                this.where = new Where(eqPart);
            } else {
                this.where.setPart(new And(this.where.getPart(), eqPart));
            }
        }
        this.where.prepare(this);
    }

    /**
     * Create the SQL statement.
     * @return StringBuilder containing the statement
     * @throws EFapsException on error
     */
    private StringBuilder createSQLStatement()
        throws EFapsException
    {
        final SQLSelect select = new SQLSelect()
            .column(0, "ID")
            .from(getBaseType().getMainTable().getSqlTable(), 0);

        // if the main table has a column for the type it is selected also
        int colIndex = 2;
        if (getBaseType().getMainTable().getSqlColType() != null) {
            select.column(0, getBaseType().getMainTable().getSqlColType());
            colIndex++;
        }

        final StringBuilder cmd = new StringBuilder()
                .append(select.getSQL()).append(this.where.getSQL());

        if (Query.LOG.isDebugEnabled()) {
            Query.LOG.debug(cmd.toString());
        }
        return cmd;
    }


    /**
     * Execute the actual statement against the database.
     * @param _complStmt        Statment to be executed
     * @return true if executed with success
     * @throws EFapsException on error
     */
    protected boolean executeOneCompleteStmt(final StringBuilder _complStmt)
        throws EFapsException
    {
        final boolean ret = false;
        ConnectionResource con = null;
        try {
            con = Context.getThreadContext().getConnectionResource();

            if (Query.LOG.isDebugEnabled()) {
                Query.LOG.debug(_complStmt.toString());
            }

            final Statement stmt = con.getConnection().createStatement();

            final ResultSet rs = stmt.executeQuery(_complStmt.toString());
            new ArrayList<Instance>();
            new HashMap<Instance, Integer>();
            int i = 0;
            while (rs.next()) {
                i++;
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
            throw new EFapsException(getClass(), "executeOneCompleteStmt.Throwable", e);
        }
        return ret;
    }

}
