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

package org.efaps.admin.datamodel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.event.EventDefinition;
import org.efaps.admin.event.EventType;
import org.efaps.ci.CIAdminDataModel;
import org.efaps.ci.CIAdminUser;
import org.efaps.db.Context;
import org.efaps.db.databases.information.ColumnInformation;
import org.efaps.db.query.CachedResult;
import org.efaps.db.transaction.ConnectionResource;
import org.efaps.db.wrapper.SQLInsert;
import org.efaps.db.wrapper.SQLUpdate;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.Cache;
import org.efaps.util.cache.CacheReloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the class for the attribute description. The type description holds
 * information about creation of a new instance of a attribute
 * with default values.
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class Attribute
    extends AbstractDataModelObject
{

    /**
     * ENUM used to access the different attribute types.
     */
    public enum AttributeTypeDef {
        /** Attribute type Link. */
        ATTRTYPE_LINK("440f472f-7be2-41d3-baec-4a2f0e4e5b31"),
        /** Attribute type Link with Ranges. */
        ATTRTYPE_LINK_WITH_RANGES("9d6b2e3e-68ce-4509-a5f0-eae42323a696"),
        /** Attribute type PersonLink. */
        ATTRTYPE_PERSON_LINK("7b8f98de-1967-44e0-b174-027349868a61"),
        /** Attribute type Creator Link. */
        ATTRTYPE_CREATOR_LINK("76122fe9-8fde-4dd4-a229-e48af0fb4083"),
        /** Attribute type Modifier Link. */
        ATTRTYPE_MODIFIER_LINK("447a7c87-8395-48c4-b2ed-d4e96d46332c"),
        /** Attribute type Multi Line Array. */
        ATTRTYPE_MULTILINEARRAY("adb13c3d-9506-4da2-8d75-b54c76779c6c"),
        /** Attribute type Status. */
        ATTRTYPE_STATUS("0161bcdb-45e9-4839-a709-3a1c56f8a76a");

        /**
         * Stored the UUID for the given type.
         */
        private final UUID uuid;

        /**
         * Private Constructor.
         *
         * @param _uuid UUID to set
         */
        private AttributeTypeDef(final String _uuid)
        {
            this.uuid = UUID.fromString(_uuid);
        }

        /**
         * @return the uuid
         */
        public UUID getUuid()
        {
            return this.uuid;
        }
    }

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Attribute.class);

    /**
     * Stores all instances of attribute.
     *
     * @see #get
     */
    private static AttributeCache CACHE = new AttributeCache();

    /**
     * This is the instance variable for the table, where attribute is stored.
     *
     * @see #getTable
     */
    private final SQLTable sqlTable;

    /**
     * Instance variable for the link to another type.
     *
     * @see #getLink
     * @see #setLink
     */
    private Type link = null;

    /**
     * Instance variable for the parent type.
     *
     * @see #getParent
     * @see #setParent
     */
    private Type parent = null;

    /**
     * This instance variable stores the sql column name.
     *
     * @see #getSqlColName
     * @see #setSqlColName
     */
    private final ArrayList<String> sqlColNames = new ArrayList<String>();

    /**
     * The instance variable stores the attribute type for this attribute.
     *
     * @see #getAttributeType
     */
    private final AttributeType attributeType;

    /**
     * The collection instance variables holds all unique keys, for which this
     * attribute belongs to.
     *
     * @see #getUniqueKeys
     * @see #setUniqueKeys
     */
    private Collection<UniqueKey> uniqueKeys = null;

    /**
     * The String holds the default value as string for this Attribute.
     *
     * @see #getDefaultValue
     */
    private final String defaultValue;

    /**
     * Is the attribute required? This means at minimum one part of the
     * attribute is not allowed to be a null value.
     *
     * @see #isRequired
     */
    private final boolean required;

    /**
     * The parent this attribute belongs to.
     */
    private AttributeSet parentSet;

    /**
     * Size of the attribute (for string). Precision of the attribute (for
     * decimal).
     */
    private final int size;

    /**
     * Scale of the attribute (for decimal).
     */
    private final int scale;

    /**
     * UUID of the dimension belonging to this attribute.
     */
    private final String dimensionUUID;

    /**
     * Holds the Attributes this Attribute depend on.
     * A TreeMap is used to have a fixed position of each attribute.
     * (Needed e.g for printquery)
     */
    private Map<String, Attribute> dependencies;

    /**
     * This is the constructor for class {@link Attribute}. Every instance of
     * class {@link Attribute} must have a name (parameter <i>_name</i>) and an
     * identifier (parameter <i>_id</i>).
     *
     * @param _id               id of the attribute
     * @param _name             name of the instance
     * @param _sqlColNames      name of the SQL columns
     * @param _sqlTable         table of this attribute
     * @param _attributeType    type of this attribute
     * @param _defaultValue     default value for this attribute
     * @param _dimensionUUID    UUID of the Dimension
     * @throws EFapsException on error while retreiving column informationfrom database
     */
    protected Attribute(final long _id,
                        final String _name,
                        final String _sqlColNames,
                        final SQLTable _sqlTable,
                        final AttributeType _attributeType,
                        final String _defaultValue,
                        final String _dimensionUUID)
        throws EFapsException
    {
        super(_id, null, _name);
        this.sqlTable = _sqlTable;
        this.attributeType = _attributeType;
        this.defaultValue = (_defaultValue != null) ? _defaultValue.trim() : null;
        this.dimensionUUID = (_dimensionUUID != null) ? _dimensionUUID.trim() : null;
        // add SQL columns and evaluate if attribute is required
        boolean req = false;
        int sizeTemp = 0;
        int scaleTemp = 0;
        final StringTokenizer tok = new StringTokenizer(_sqlColNames.trim(), ",");
        while (tok.hasMoreTokens()) {
            final String colName = tok.nextToken().trim();
            getSqlColNames().add(colName);
            final ColumnInformation columInfo = this.sqlTable.getTableInformation().getColInfo(colName);
            if (columInfo == null) {
                throw new EFapsException(Attribute.class, "Attribute", _id, _name, _sqlTable, colName);
            }
            req |= !columInfo.isNullable();
            sizeTemp = columInfo.getSize();
            scaleTemp = columInfo.getScale();
        }
        this.size = sizeTemp;
        this.scale = scaleTemp;
        this.required = req;
    }

    /**
     * This is the constructor for class {@link Attribute}. Every instance of
     * class {@link Attribute} must have a name (parameter <i>_name</i>) and an
     * identifier (parameter <i>_id</i>).<br/>
     * This constructor is used for the copy method (clone of an attribute
     * instance).
     *
     * @see #copy
     * @param _id               id of the attribute
     * @param _name             name of the instance
     * @param _sqlTable         table of this attribute
     * @param _attributeType    typer of this attribute
     * @param _defaultValue     default value for this attribute
     * @param _dimensionUUID    uuid of the dimension belnging to this attribute
     * @param _required         is it required
     * @param _size             Size
     * @param _scale            Scale
     */
    private Attribute(final long _id,
                      final String _name,
                      final SQLTable _sqlTable,
                      final AttributeType _attributeType,
                      final String _defaultValue,
                      final String _dimensionUUID,
                      final boolean _required,
                      final int _size,
                      final int _scale)
    {
        super(_id, null, _name);
        this.sqlTable = _sqlTable;
        this.attributeType = _attributeType;
        this.defaultValue = (_defaultValue != null) ? _defaultValue.trim() : null;
        this.required = _required;
        this.size = _size;
        this.scale = _scale;
        this.dimensionUUID = _dimensionUUID;
    }

    /**
     * This method returns <i>true</i> if a link exists. This is made with a
     * test of the return value of method {@link #getLink} on null.
     *
     * @return <i>true</i> if this attribute has a link, otherwise <i>false</i>
     */
    public boolean hasLink()
    {
        boolean ret = false;
        if (getLink() != null) {
            ret = true;
        }
        return ret;
    }

    /**
     * A unique key can added to this attribute instance. If no unique key is
     * added before, the instance variable {@link #uniqueKeys} is initialised.
     *
     * @param _uniqueKey unique key to add to this attribute
     * @see #uniqueKeys
     */
    public void addUniqueKey(final UniqueKey _uniqueKey)
    {
        if (getUniqueKeys() == null) {
            setUniqueKeys(new HashSet<UniqueKey>());
        }
        getUniqueKeys().add(_uniqueKey);
    }

    /**
     * The method makes a clone of the current attribute instance.
     *
     * @return clone of current attribute instance
     */
    public Attribute copy()
    {
        final Attribute ret = new Attribute(getId(), getName(), this.sqlTable, this.attributeType, this.defaultValue,
                                            this.dimensionUUID, this.required, this.size, this.scale);
        ret.getSqlColNames().addAll(getSqlColNames());
        ret.setLink(getLink());
        ret.setUniqueKeys(getUniqueKeys());
        ret.getProperties().putAll(getProperties());
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvent(final EventType _eventtype,
                         final EventDefinition _eventdef)
    {
        super.addEvent(_eventtype, _eventdef);
        for (final Type child : this.parent.getChildTypes()) {
            final Attribute childAttr = child.getAttribute(getName());
            if (childAttr != null) {
                childAttr.addEvent(_eventtype, _eventdef);
            }
        }
    }

    /**
     * This is the getter method for instance variable {@link #sqlTable}.
     *
     * @return value of instance variable {@link #sqlTable}
     * @see #sqlTable
     */
    public SQLTable getTable()
    {
        return this.sqlTable;
    }

    /**
     * This is the setter method for instance variable {@link #link}.
     *
     * @param _link new instance of class {@link Type} to set for link
     * @see #link
     * @see #getLink
     */
    protected void setLink(final Type _link)
    {
        this.link = _link;
    }

    /**
     * This is the getter method for instance variable {@link #link}.
     *
     * @return value of instance variable {@link #link}
     * @see #link
     * @see #setLink
     */
    public Type getLink()
    {
        return this.link;
    }

    /**
     * Getter method for the instance variable {@link #dependencies}.
     *
     * @return value of instance variable {@link #dependencies}
     */
    public Map<String, Attribute> getDependencies()
    {
        if (this.dependencies == null) {
            this.dependencies = new TreeMap<String, Attribute>();
            // in case of a rate attribute the dependencies to the currencies must be given
            if (getProperties().containsKey("CurrencyAttribute4Rate")) {
                this.dependencies.put("CurrencyAttribute4Rate",
                                getParent().getAttribute(getProperties().get("CurrencyAttribute4Rate")));
                this.dependencies.put("TargetCurrencyAttribute4Rate",
                               getParent().getAttribute(getProperties().get("TargetCurrencyAttribute4Rate")));
            }
        }
        return this.dependencies;
    }

    /**
     * This is the setter method for instance variable {@link #parent}.
     *
     * @param _parent new instance of class {@link Type} to set for parent
     * @see #parent
     * @see #getParent
     */
    public void setParent(final Type _parent)
    {
        this.parent = _parent;
    }

    /**
     * This is the getter method for instance variable {@link #parent}.
     *
     * @return value of instance variable {@link #parent}
     * @see #parent
     * @see #setParent
     */
    public Type getParent()
    {
        return this.parent;
    }

    /**
     * This is the getter method for instance variable {@link #parentSet}.
     *
     * @return value of instance variable {@link #parentSet}
     *
     */
    public AttributeSet getParentSet()
    {
        return this.parentSet;
    }

    /**
     * This is the setter method for instance variable {@link #parentSet}.
     *
     * @param _parentSet new instance of class {@link AttributeSet} to set
     */
    private void setParentSet(final AttributeSet _parentSet)
    {
        this.parentSet = _parentSet;
    }

    /**
     * This is the getter method for instance variable {@link #sqlColNames}.
     *
     * @return value of instance variable {@link #sqlColNames}
     * @see #sqlColNames
     */
    public ArrayList<String> getSqlColNames()
    {
        return this.sqlColNames;
    }

    /**
     * This is the getter method for instance variable {@link #attributeType}.
     *
     * @return value of instance variable {@link #attributeType}
     * @see #attributeType
     */
    public AttributeType getAttributeType()
    {
        return this.attributeType;
    }

    /**
     * This is the getter method for instance variable {@link #uniqueKeys}.
     *
     * @return value of instance variable {@link #uniqueKeys}
     * @see #uniqueKeys
     */
    public Collection<UniqueKey> getUniqueKeys()
    {
        return this.uniqueKeys;
    }

    /**
     * This is the setter method for instance variable {@link #uniqueKeys}.
     *
     * @param _uniqueKeys new value for instance variable {@link #uniqueKeys}
     * @see #uniqueKeys
     * @see #getUniqueKeys
     */
    private void setUniqueKeys(final Collection<UniqueKey> _uniqueKeys)
    {
        this.uniqueKeys = _uniqueKeys;
    }

    /**
     * This is the getter method for instance variable {@link #defaultValue}.
     *
     * @return value of instance variable {@link #defaultValue}
     * @see #defaultValue
     */
    public String getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * This is the getter method for instance variable {@link #required}.
     *
     * @return value of instance variable {@link #required}
     * @see #required
     */
    public boolean isRequired()
    {
        return this.required;
    }

    /**
     * Getter method for instance variable {@link #size}.
     *
     * @return value of instance variable {@link #size}
     */
    public int getSize()
    {
        return this.size;
    }

    /**
     * Getter method for instance variable {@link #scale}.
     *
     * @return value of instance variable {@link #scale}
     */
    public int getScale()
    {
        return this.scale;
    }

    /**
     * Method to get the dimension related to this attribute.
     * @return Dimension
     */
    public Dimension getDimension()
    {
        return Dimension.get(UUID.fromString(this.dimensionUUID));
    }

    /**
     * Has this attribute an UoM.
     * @return true id dimensionUUId!=null, else false
     */
    public boolean hasUoM()
    {
        return (this.dimensionUUID != null);
    }

    /**
     * Prepares for given <code>_values</code> depending on this attribute the
     * <code>_insert</code> into the database.
     *
     * @param _insert   SQL insert statement for related {@link #sqlTable}
     * @param _values   values to insert
     * @throws SQLException if values could not be inserted
     */
    public void prepareDBInsert(final SQLInsert _insert,
                                final Object... _values)
        throws SQLException
    {
        this.attributeType.getDbAttrType().prepareInsert(_insert, this, _values);
    }

    /**
     * Prepares for given <code>_values</code> depending on this attribute the
     * <code>_update</code> into the database.
     *
     * @param _update   SQL update statement for related {@link #sqlTable}
     * @param _values   values to update
     * @throws SQLException if values could not be inserted
     */
    public void prepareDBUpdate(final SQLUpdate _update,
                                final Object... _values)
        throws SQLException
    {
        this.attributeType.getDbAttrType().prepareUpdate(_update, this, _values);
    }

    /**
     *
     * @param _rs       cached result set
     * @param _indexes  indexes within the cached result for the value
     * @return found value
     * @throws Exception if values could not be read from the cached result
     */
    public Object readDBValue(final CachedResult _rs,
                              final List<Integer> _indexes)
        throws Exception
    {
        return this.attributeType.getDbAttrType().readValue(this, _rs, _indexes);
    }

    /**
     *
     * @param _objectList   object list from the database
     * @return found value
     * @throws EFapsException if values could not be read from the
     *                        <code>_objectList</code>
     */
    public Object readDBValue(final List<Object> _objectList)
        throws EFapsException
    {
        return this.attributeType.getDbAttrType().readValue(this, _objectList);
    }

    /**
     * Method to initialize this Cache.
     * @param _class clas that called this method
     * @throws CacheReloadException on error
     */
    public static void initialize(final Class<?> _class)
        throws CacheReloadException
    {
        Attribute.CACHE.initialize(_class);
    }

    /**
     * Method to initialize the Cache of this CacheObjectInterface.
     * @throws CacheReloadException on error
     */
    public static void initialize() throws CacheReloadException
    {
        Attribute.initialize(Attribute.class);
    }

    /**
     * Returns for given parameter <i>_id</i> the instance of class
     * {@link Attribute}.
     *
     * @param _id id to search in the cache
     * @return instance of class {@link Attribute}
     * @throws CacheReloadException on error
     * @see #getCache
     */
    public static Attribute get(final long _id)
        throws CacheReloadException
    {
        return Attribute.CACHE.get(_id);
    }

    /**
     * Returns for given parameter <i>_name</i> the instance of class
     * {@link Attribute}.
     *
     * @param _name name to search in the cache
     * @return instance of class {@link Attribute}
     * @throws CacheReloadException on error
     * @see #getCache
     */
    public static Attribute get(final String _name)
        throws CacheReloadException
    {
        return Attribute.CACHE.get(_name);
    }

    /**
     * The instance method returns the string representation of this attribute.
     * The string representation of this attribute is the name of the type plus
     * slash plus name of this attribute.
     *
     * @return String representation
     */
    @Override
    public String toString()
    {
        return new ToStringBuilder(this).appendSuper(super.toString())
            .append("attribute name", getParent().getName() + "/" + getName())
            .append("attributetype", getAttributeType().toString())
            .append("required", this.required).toString();
    }

    /**
     * Class used as cache.
     *
     */
    protected static final class AttributeCache
        extends Cache<Attribute>
    {

        /**
         * This is the sql select statement to select all types from the
         * database.
         */
        private static final String SQL_SELECT = "select ID,"
                                                    + "NAME,"
                                                    + "TYPEID,"
                                                    + "DMTABLE,"
                                                    + "DMTYPE,"
                                                    + "DMATTRIBUTETYPE,"
                                                    + "DMTYPELINK,"
                                                    + "PARENTSET,"
                                                    + "SQLCOLUMN,"
                                                    + "DEFAULTVAL, "
                                                    + "DIMENSION "
                                                 + "from V_ADMINATTRIBUTE";

        /**
         * @see org.efaps.util.cache.Cache#readCache(java.util.Map, java.util.Map, java.util.Map)
         * @param _newCache4Id      cache for id
         * @param _newCache4Name    cache for name
         * @param _newCache4UUID    cache for uuid
         * @throws CacheReloadException on error
         */
        @Override
        protected void readCache(final Map<Long, Attribute> _newCache4Id,
                                 final Map<String, Attribute> _newCache4Name,
                                 final Map<UUID, Attribute> _newCache4UUID)
            throws CacheReloadException
        {
            ConnectionResource con = null;
            try {
                con = Context.getThreadContext().getConnectionResource();

                Statement stmt = null;
                try {
                    stmt = con.getConnection().createStatement();
                    final Map<Long, AttributeSet> id2Set = new HashMap<Long, AttributeSet>();
                    final Map<Attribute, Long> attribute2setId = new HashMap<Attribute, Long>();
                    final ResultSet rs = stmt.executeQuery(Attribute.AttributeCache.SQL_SELECT);
                    while (rs.next()) {
                        final long id = rs.getLong(1);
                        final String name = rs.getString(2).trim();
                        final long typeAttrId = rs.getLong(3);
                        final long tableId = rs.getLong(4);
                        final long typeId = rs.getLong(5);
                        final long attrTypeId = rs.getLong(6);
                        final long typeLinkId = rs.getLong(7);
                        final long parentSetId = rs.getLong(8);
                        final String sqlCol = rs.getString(9);
                        final String defaultval = rs.getString(10);
                        final String dimensionUUID = rs.getString(11);
                        final Type type = Type.get(typeId);

                        Attribute.LOG.debug("read attribute '" + type.getName() + "/" + name + "' (id = " + id + ")");

                        final Type typeAttr = Type.get(typeAttrId);

                        if (typeAttr.getUUID().equals(CIAdminDataModel.AttributeSet.uuid)) {
                            final AttributeSet set = new AttributeSet(id, type, name, AttributeType.get(attrTypeId),
                                            sqlCol, tableId, typeLinkId);
                            id2Set.put(id, set);
                        } else {
                            final Attribute attr = new Attribute(id, name, sqlCol, SQLTable.get(tableId),
                                                                 AttributeType.get(attrTypeId), defaultval,
                                                                 dimensionUUID);
                            attr.setParent(type);
                            final UUID uuid = attr.getAttributeType().getUUID();
                            if (uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_LINK.getUuid())
                                         || uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_LINK_WITH_RANGES.getUuid())
                                         || uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_STATUS.getUuid())) {
                                final Type linkType = Type.get(typeLinkId);
                                attr.setLink(linkType);
                                linkType.addLink(attr);
                            // in case of a PersonLink, CreatorLink or ModifierLink a link to Admin_User_Person
                            // must be set
                            } else if (uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_CREATOR_LINK.getUuid())
                                            || uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_MODIFIER_LINK.getUuid())
                                            || uuid.equals(Attribute.AttributeTypeDef.ATTRTYPE_PERSON_LINK.getUuid())) {
                                final Type linkType = CIAdminUser.Person.getType();
                                attr.setLink(linkType);
                                linkType.addLink(attr);
                            }

                            attr.readFromDB4Properties();

                            if (typeAttr.getUUID().equals(CIAdminDataModel.AttributeSetAttribute.uuid)) {
                                attribute2setId.put(attr, parentSetId);
                            } else {
                                type.addAttribute(attr, false);
                            }
                            _newCache4Id.put(attr.getId(), attr);
                            _newCache4Name.put(attr.getParent().getName() + "/" + attr.getName(), attr);
                        }
                    }
                    rs.close();
                    // make connection between set and attributes
                    for (final Entry<Attribute, Long> entry : attribute2setId.entrySet()) {
                        final AttributeSet parentset = id2Set.get(entry.getValue());
                        final Attribute childAttr = entry.getKey();
                        parentset.addAttribute(childAttr, false);
                        childAttr.setParentSet(parentset);
                    }
                } finally {
                    if (stmt != null) {
                        stmt.close();
                    }
                }
                con.commit();
            } catch (final SQLException e) {
                throw new CacheReloadException("could not read attributes", e);
            } catch (final EFapsException e) {
                throw new CacheReloadException("could not read attributes", e);
            } finally {
                if ((con != null) && con.isOpened()) {
                    try {
                        con.abort();
                    } catch (final EFapsException e) {
                        throw new CacheReloadException("could not read attributes", e);
                    }
                }
            }
        }
    }
}