/*
 * Copyright 2006 The eFaps Team
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.AttributeType;
import org.efaps.admin.datamodel.AttributeTypeInterface;
import org.efaps.admin.datamodel.SQLTable;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.TriggerEvent;
import org.efaps.admin.event.EventDefinition;
import org.efaps.admin.ui.Field;
import org.efaps.db.transaction.ConnectionResource;
import org.efaps.util.EFapsException;
import org.efaps.admin.event.TriggerKeys4Values;

/**
 *
 */
public class Insert extends Update {

  /**
   * Logging instance used in this class.
   */
  private static final Log LOG = LogFactory.getLog(Insert.class);

  /////////////////////////////////////////////////////////////////////////////

  /**
   * @param _context  context for this request
   * @param _type     type instance to insert
   */
  public Insert(Context _context, Type _type) throws EFapsException  {
    super(_context, _type, null);

    addCreateUpdateAttributes(_context);
    addTables();
  }

  /**
   * @todo wrong exception!
   */
  public Insert(Context _context, String _type) throws Exception  {
    super(_context, Type.get(_type), null);

    addCreateUpdateAttributes(_context);
    addTables();
  }

  /**
   * Add all tables of the type to the expressions, because for the type
   * an insert must be made for all tables!!!
   */
  private void addTables()  {
    for (SQLTable table : getType().getTables())  {
      if (getExpr4Tables().get(table) == null)  {
        getExpr4Tables().put(table, new HashMap < String, AttributeTypeInterface >());
      }
    }

  }

  /**
   * Add all attributes of the type which must be always updated.
   *
   * @param _context  context for this request
   * @param _type     data model type
   * @throws EFapsException from called method
   */
  private void addCreateUpdateAttributes(Context _context) throws EFapsException  {
    Iterator iter = getType().getAttributes().entrySet().iterator();
    while (iter.hasNext())  {
      Map.Entry entry = (Map.Entry) iter.next();
      Attribute attr = (Attribute) entry.getValue();
      AttributeType attrType = attr.getAttributeType();
      if (attrType.isCreateUpdate())  {
        add(_context, attr, null, false);
      }
    }
  }

  /**
   * @param _context  context for this request
   */
  public void execute(Context _context) throws EFapsException  {
    ConnectionResource con = null;
    try  {
      executeTrigger(_context, TriggerEvent.INSERT_PRE);

      con = _context.getConnectionResource();

      if (test4Unique(_context))  {
        throw new EFapsException(getClass(), "execute.UniqueKeyError");
      }

      SQLTable mainTable = getType().getMainTable();
      setInstance(new Instance(_context, getInstance().getType(), getNewId(con, mainTable)));
      executeOneStatement(_context, con, mainTable, getExpr4Tables().get(mainTable), getId());

      for (Map.Entry<SQLTable, Map<String,AttributeTypeInterface>> entry
                                              : getExpr4Tables().entrySet())  {
        SQLTable table = entry.getKey();
        if ((table != mainTable) && !table.isReadOnly())  {
          executeOneStatement(_context, con, table, entry.getValue(), getId());
        }
      }

      con.commit();

      executeTrigger(_context, TriggerEvent.INSERT_POST);
    } catch (EFapsException e)  {
      if (con != null)  {
        con.abort();
      }
      throw e;
    } catch (Throwable e)  {
      if (con != null)  {
        con.abort();
      }
      throw new EFapsException(getClass(), "execute.Throwable");
    }
  }

  /**
   * The method gets all triggers for the given trigger event and executes
   * them in the given order. If no triggers are defined, nothing is done.
   *
   * @param _context      eFaps context for this request
   * @param _triggerEvent trigger events to execute
   * @throws EFapsException from trigger execution
   */
  protected void executeTrigger(final Context _context, TriggerEvent _triggerEvent) throws EFapsException  {
    List < EventDefinition > triggers = getInstance().getType().getTrigger(_triggerEvent);
    if (triggers != null)  {
      Map < TriggerKeys4Values, Map > map = new HashMap < TriggerKeys4Values, Map >();
      map.put(TriggerKeys4Values.NEW_VALUES, this.values);
      for (EventDefinition evenDef : triggers)  {
        evenDef.execute(_context, getInstance(), map);
      }
    }
  }


  /**
   * @param _context  context for this request
   */
  private void executeOneStatement(final Context _context,
      final ConnectionResource _con, final SQLTable _table,
      final Map _expressions, final String _id) throws EFapsException  {

    PreparedStatement stmt = null;
    try {
      stmt = createOneStatement(_context, _con, _table, _expressions, _id);
      int rows = stmt.executeUpdate();
      if (rows==0)  {
        throw new EFapsException(getClass(), "executeOneStatement.NotInserted",
            _table.getName()
        );
      }
    } catch (EFapsException e)  {
      throw e;
    } catch (Exception e)  {
      throw new EFapsException(getClass(), "executeOneStatement.Exception", e,
          _table.getName()
      );
    } finally  {
      try  {
        stmt.close();
      } catch (Exception e)  {
      }
    }
  }

  /**
   * @param _context  context for this request
   */
  private PreparedStatement createOneStatement(final Context _context,
      final ConnectionResource _con, final SQLTable _table,
      final Map _expressions, final String _id) throws SQLException  {

    List<AttributeTypeInterface> list = new ArrayList<AttributeTypeInterface>();
    StringBuffer cmd = new StringBuffer();
    StringBuffer val = new StringBuffer();
    cmd.append("insert into ").append(_table.getSqlTable()).append("(").append(_table.getSqlColId());
    Iterator iter = _expressions.entrySet().iterator();
    boolean command = false;
    while (iter.hasNext())  {
      Map.Entry entry = (Map.Entry)iter.next();

      cmd.append(",").append(entry.getKey());
      val.append(",");

      AttributeTypeInterface attr = (AttributeTypeInterface)entry.getValue();
      if (!attr.prepareUpdate(val))  {
        list.add(attr);
      }
    }
    if (_table.getSqlColType()!=null)  {
      cmd.append(",").append(_table.getSqlColType());
      val.append(",?");
    }
    cmd.append(") values (").append(_id).append("").append(val).append(")");

    if (LOG.isTraceEnabled())  {
      LOG.trace(cmd.toString());
    }

    PreparedStatement stmt = _con.getConnection().prepareStatement(cmd.toString());
    for (int i=0, j=1; i<list.size(); i++, j++)  {
      AttributeTypeInterface attr = (AttributeTypeInterface)list.get(i);
      attr.update(_context, stmt, j);
    }
    if (_table.getSqlColType()!=null)  {
      stmt.setLong(list.size()+1, getType().getId());
    }
    return stmt;
  }

  private long getNewId(final ConnectionResource _con,
      final SQLTable _table) throws EFapsException  {

    long ret = 0;
    Statement stmt = null;
    try  {
      stmt = _con.getConnection().createStatement();
      ResultSet rs = stmt.executeQuery(_table.getSqlNewIdSelect());
      if (!rs.next())  {
        throw new EFapsException(getClass(), "getNewId.NoIdFound",
            _table.getName(), _table.getSqlNewIdSelect()
        );
      }
      ret = rs.getLong(1);
// if max selection!
if (ret == 0)  {
  ret = 1;
}
    } catch (EFapsException e)  {
      throw e;
    } catch (Exception e)  {
      throw new EFapsException(getClass(), "getNewId.Exception", e,
          _table.getName(), _table.getSqlNewIdSelect()
      );
    } finally  {
      try  {
        stmt.close();
      } catch (SQLException e)  {
      }
    }
    return ret;
  }
}