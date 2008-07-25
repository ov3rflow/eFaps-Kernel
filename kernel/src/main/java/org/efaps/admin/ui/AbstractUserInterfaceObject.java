/*
 * Copyright 2003-2008 The eFaps Team
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

package org.efaps.admin.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.efaps.admin.AbstractAdminObject;
import org.efaps.admin.EFapsClassNames;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.EventDefinition;
import org.efaps.admin.event.EventType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.user.AbstractUserObject;
import org.efaps.admin.user.Role;
import org.efaps.db.Context;
import org.efaps.db.SearchQuery;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.Cache;
import org.efaps.util.cache.CacheReloadException;
import org.efaps.util.cache.CacheReloadInterface;

/**
 * This Class is the Abstract Class for all UserInterfaces in eFaps.<br/> In
 * this Class only a few Methods are defined wich are commun to all Class inside
 * the UserInterface Package. With this Class all
 * <code>UserInterfaceObjects</code> can be initialized, the Access is checked
 * and the Triggers for the <code>UserInterfaceObjects</code> are handled.
 *
 * @author tmo
 * @author jmox
 * @version $Id$
 */
public abstract class AbstractUserInterfaceObject extends AbstractAdminObject {

  // ///////////////////////////////////////////////////////////////////////////
  // instance variables

  /**
   * The instance variable is an Access HashSet to store all users (person,
   * group or role) who have access to this user interface object.
   *
   * @see #getAccess
   */
  private final Set<AbstractUserObject> access =
      new HashSet<AbstractUserObject>();

  // ///////////////////////////////////////////////////////////////////////////
  // constructors / destructors

  /**
   * Constructor to set the id, the uuid and the name of the user interface
   * object.
   *
   * @param _id
   *                id to set
   * @param _uuid
   *                uuid to set (as String)
   * @param _name
   *                name to set
   */
  protected AbstractUserInterfaceObject(final long _id, final String _uuid,
                                        final String _name) {
    super(_id, _uuid, _name);
  }

  // ///////////////////////////////////////////////////////////////////////////

  /**
   * The instance method reads all needed information for this user interface
   * object. Here, only the properties are read from the database
   *
   * @param _context
   *                context for this request
   * @see #readFromDB4Properties
   * @see #readFromDB4Links
   * @see #readFromDB4Access
   */
  protected void readFromDB() throws CacheReloadException {
    readFromDB4Properties();
    readFromDB4Links();
    readFromDB4Access();
  }

  /**
   * The instance method reads the access for this user interface object.
   *
   * @param _context
   *                for this request
   * @todo use SearchQuery
   */
  private void readFromDB4Access() throws CacheReloadException {
    Statement stmt = null;
    try {
      stmt = Context.getThreadContext().getConnection().createStatement();
      final ResultSet resultset =
          stmt.executeQuery("select "
              + "T_UIACCESS.USERABSTRACT "
              + "from T_UIACCESS "
              + "where T_UIACCESS.UIABSTRACT="
              + getId());
      while (resultset.next()) {
        final long userId = resultset.getLong(1);
        final AbstractUserObject userObject =
            AbstractUserObject.getUserObject(userId);
        if (userObject == null) {
          throw new Exception("user " + userId + " does not exists!");
        } else {
          getAccess().add(userObject);
        }
      }
      resultset.close();
    } catch (Exception e) {
      throw new CacheReloadException("could not read access for "
          + "'"
          + getName()
          + "'", e);
    }
    finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
        }
      }
    }
  }

  /**
   * Check, if the user of the context has access to this user interface object.<br>
   * The Check is made in the following order: <br>
   * <ol>
   * <li>If no access Uuser or role is assigned to this user interface object,
   * all user have access and the return is <i>true</i> => go on with Step 3</li>
   * <li>else check if the context person is assigned to one of the user
   * objects.</li>
   * <li> if Step 1 or Step 2 have <i>true</i> and the context an Event of the
   * Type <code>TriggerEvent.ACCESSCHECK</code>, the return of the trigger
   * initiated program is returned</li>
   * </ol>
   *
   * @return <i>true</i> if context user has access, otherwise <i>false</i> is
   *         returned
   * @throws EFapsException
   */
  public boolean hasAccess() throws EFapsException {
    boolean ret = false;
    if (getAccess().isEmpty()) {
      ret = true;
    } else {
      for (AbstractUserObject userObject : getAccess()) {
        if (userObject.isAssigned()) {
          ret = true;
          break;
        }
      }
    }
    if (ret && super.hasEvents(EventType.UI_ACCESSCHECK)) {
      ret = false;
      final List<EventDefinition> events =
          super.getEvents(EventType.UI_ACCESSCHECK);

      final Parameter parameter = new Parameter();
      parameter.put(ParameterValues.UIOBJECT, this);
      for (EventDefinition event : events) {
        final Return retIn = event.execute(parameter);
        ret = retIn.get(ReturnValues.TRUE) != null;

      }
    }
    return ret;
  }

  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Getter method for the HashSet instance variable {@link #access}.
   *
   * @return value of the HashSet instance variable {@link #access}
   * @see #access
   * @see #add(Role)
   */
  protected Set<AbstractUserObject> getAccess() {
    return this.access;
  }

  // ///////////////////////////////////////////////////////////////////////////

  /**
   * Initialise the cache of Userinterfaces.
   *
   * @param _context
   *                eFaps context for this request
   */
  public static void initialise() throws CacheReloadException {
    Image.getCache().initialise();
    Command.getCache().initialise();
    Menu.getCache().initialise();
    Search.getCache().initialise();
    Form.getCache().initialise();
    Table.getCache().initialise();
    Image.getCache().readFromDB();
    Command.getCache().readFromDB();
    Menu.getCache().readFromDB();
    Search.getCache().readFromDB();
    Form.getCache().readFromDB();
    Table.getCache().readFromDB();
  }

  /**
   * Inner Class to store the UserInterfaces in aCache
   *
   * @param <UIObj>
   */
  static protected class UserInterfaceObjectCache<UIObj extends AbstractUserInterfaceObject>
      extends Cache<UIObj> {

    // /////////////////////////////////////////////////////////////////////////
    // instance variables

    private final Class<UIObj> callerClass;

    protected UserInterfaceObjectCache(final Class<UIObj> _callerClass) {
      super(new CacheReloadInterface() {

        public int priority() {
          return 1200;
        };

        public void reloadCache() throws CacheReloadException {
          UIObj.initialise();
        };
      });
      this.callerClass = _callerClass;
    }

    /**
     * All cached user interface objects are read into the cache.
     *
     * @see #initialise
     */
    protected void readFromDB() throws CacheReloadException {
      for (UIObj uiObj : getCache4Id().values()) {
        uiObj.readFromDB();
      }
    }

    /**
     * Initialise the cache of a specific user interface object type. Initialise
     * means, that all all objects of this user interface type are read from the
     * database and stored in the cache. If the eFaps admin type itself is not
     * defined, that initialiase does nothing (this could happen in the create
     * phase).<br/> After initialise, the user interface object itself is read
     * with method {@link #readFromDB}.
     *
     * @param _context
     *                eFaps context for this request
     * @see #readFromDB
     */
    protected void initialise() throws CacheReloadException
    {
      final Class<UIObj> uiObjClass = getCallerClass();
      try {
        if (Type.get(getEFapsClassName()) != null) {
          final SearchQuery query = new SearchQuery();
          query.setQueryTypes(Type.get(getEFapsClassName()).getName());
          query.addSelect("ID");
          query.addSelect("Name");
          query.addSelect("UUID");
          query.executeWithoutAccessCheck();
          while (query.next()) {
            final long id = (Long) query.get("ID");
            final String name = (String) query.get("Name");
            final String uuid = (String) query.get("UUID");
            final Constructor<UIObj> uiObj =
                uiObjClass.getConstructor(Long.class, String.class,
                    String.class);
            final UIObj uiObj2 = uiObj.newInstance(id, uuid, name);
            add(uiObj2);
          }
        }
      } catch (NoSuchMethodException e) {
        throw new CacheReloadException("class '"
            + uiObjClass.getName()
            + "' does not implement contructor (Long, String, String)", e);
      } catch (InstantiationException e) {
        throw new CacheReloadException("could not instantiate class '"
            + uiObjClass.getName()
            + "'", e);
      } catch (IllegalAccessException e) {
        throw new CacheReloadException("could not access class '"
            + uiObjClass.getName()
            + "'", e);
      } catch (InvocationTargetException e) {
        throw new CacheReloadException(
            "could not invoce constructor of class '"
                + uiObjClass.getName()
                + "'", e);
      } catch (EFapsException e) {
        throw new CacheReloadException("could not initialise cache", e);
      }
    }

    /**
     * read an <code>UserInterfaceObject</code> from the Database
     *
     * @see #read(SearchQuery)
     * @param _id
     *                ID of the <code>UserInterfaceObject</code> to search for
     * @return <code>UserInterfaceObject</code>
     * @throws EFapsException
     */
    protected UIObj read(final long _id) throws EFapsException {
      try {
        final SearchQuery query = new SearchQuery();
        query.setQueryTypes(Type.get(getEFapsClassName()).getName());
        query.addWhereExprEqValue("ID", _id);
        query.addSelect("ID");
        query.addSelect("Name");
        return read(query);
      } catch (Throwable e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.Throwable4Id", e, _id);
      }
    }

    /**
     * read an <code>UserInterfaceObject</code> from the Database
     *
     * @see #read(SearchQuery)
     * @param _name
     *                Name of the <code>UserInterfaceObject</code> to search
     *                for
     * @return<code>UserInterfaceObject</code>
     * @throws EFapsException
     */
    protected UIObj read(final String _name) throws EFapsException {
      try {
        final SearchQuery query = new SearchQuery();
        query.setQueryTypes(Type.get(getEFapsClassName()).getName());
        query.addWhereExprEqValue("Name", _name);
        query.addSelect("ID");
        query.addSelect("Name");
        return read(query);
      } catch (Throwable e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.Throwable4Name", e, _name);
      }
    }

    /**
     * get the <code>EFapsClassName</code> of this
     * <code>UserInterfaceObject</code>
     *
     * @return <code>EFapsClassName</code>
     * @throws EFapsException
     */
    private EFapsClassNames getEFapsClassName() throws EFapsException {
      final Class<UIObj> uiObjClass = getCallerClass();
      try {
        return ((EFapsClassNames) uiObjClass.getField("EFAPS_CLASSNAME").get(null));
      } catch (NoSuchFieldException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "getEFapsClassName.EFapsClassNameNotExist", e, uiObjClass.getName());
      } catch (IllegalAccessException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "getEFapsClassName.EFapsClassNameNotAccessable", e, uiObjClass
                .getName());
      }
    }

    /**
     * This Method is called to return a <code>UserInterfaceObject</code>
     *
     * @see #read(long)
     * @see #read(String)
     * @param _query
     *                <code>SearchQuery</code> to be called
     * @return <code>UserInterfaceObject</code>
     * @throws EFapsException
     */
    private UIObj read(final SearchQuery _query) throws EFapsException {
      UIObj uiObj = null;
      final Class<UIObj> uiObjClass = getCallerClass();
      try {
        _query.executeWithoutAccessCheck();
        if (_query.next()) {
          final long id = (Long) _query.get("ID");
          final String name = (String) _query.get("Name");
          uiObj =
              uiObjClass.getConstructor(Long.class, String.class).newInstance(
                  id, name);
          add(uiObj);
          uiObj.readFromDB();
        }
      } catch (NoSuchMethodException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.ConstructorNotFound", e, uiObjClass.getName());
      } catch (SecurityException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.ConstructorNotAccessable", e, uiObjClass.getName());
      } catch (IllegalArgumentException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.ConstructorWithCorrectArgumentsNotExists", e, uiObjClass
                .getName());
      } catch (InstantiationException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.ClassIsNotClass", e, uiObjClass.getName());
      } catch (IllegalAccessException e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.ConstructorNotPublic", e, uiObjClass.getName());
      } catch (InvocationTargetException e) {
        final Throwable t = e.getCause();
        if (t instanceof EFapsException) {
          throw (EFapsException) t;
        } else {
          throw new EFapsException(UserInterfaceObjectCache.class,
              "read.UIObjectNotInstanceable", t, uiObjClass.getName());
        }
      } catch (Throwable e) {
        throw new EFapsException(UserInterfaceObjectCache.class,
            "read.Throwable", e);
      }
      finally {
        try {
          _query.close();
        } catch (Exception e) {
        }
      }
      return uiObj;
    }

    // /////////////////////////////////////////////////////////////////////////
    // getter and setter methods

    /**
     * get the CallerClass
     *
     * @see #callerClasscallerClass
     */
    private Class<UIObj> getCallerClass() {
      return this.callerClass;
    }

  }
}
