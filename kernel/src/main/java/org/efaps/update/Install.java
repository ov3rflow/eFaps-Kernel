/*
 * Copyright 2003-2007 The eFaps Team
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

package org.efaps.update;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.jexl.JexlContext;
import org.apache.commons.jexl.JexlHelper;
import org.apache.commons.lang.builder.ToStringBuilder;

import org.efaps.update.access.AccessSetUpdate;
import org.efaps.update.access.AccessTypeUpdate;
import org.efaps.update.common.SystemAttributeUpdate;
import org.efaps.update.datamodel.SQLTableUpdate;
import org.efaps.update.datamodel.TypeUpdate;
import org.efaps.update.integration.WebDAVUpdate;
import org.efaps.update.program.JavaUpdate;
import org.efaps.update.ui.CommandUpdate;
import org.efaps.update.ui.FormUpdate;
import org.efaps.update.ui.ImageUpdate;
import org.efaps.update.ui.MenuUpdate;
import org.efaps.update.ui.SearchUpdate;
import org.efaps.update.ui.TableUpdate;
import org.efaps.update.user.JAASSystemUpdate;
import org.efaps.update.user.RoleUpdate;
import org.efaps.util.EFapsException;

/**
 * @author tmo
 * @version $Id$
 * @todo description
 */
public class Install {

  // ///////////////////////////////////////////////////////////////////////////
  // static variables

  /**
   * List of all update classes. The order is also used for the install order.
   *
   * @see #install
   */
  private final List<Class<? extends AbstractUpdate>> UPDATE_CLASSES =
      new ArrayList<Class<? extends AbstractUpdate>>();
  {
    if (this.UPDATE_CLASSES.size() == 0) {
      this.UPDATE_CLASSES.add(RoleUpdate.class);
      this.UPDATE_CLASSES.add(SQLTableUpdate.class);
      this.UPDATE_CLASSES.add(TypeUpdate.class);
      this.UPDATE_CLASSES.add(JAASSystemUpdate.class);
      this.UPDATE_CLASSES.add(AccessTypeUpdate.class);
      this.UPDATE_CLASSES.add(AccessSetUpdate.class);
      this.UPDATE_CLASSES.add(ImageUpdate.class);
      this.UPDATE_CLASSES.add(FormUpdate.class);
      this.UPDATE_CLASSES.add(TableUpdate.class);
      this.UPDATE_CLASSES.add(SearchUpdate.class);
      this.UPDATE_CLASSES.add(MenuUpdate.class);
      this.UPDATE_CLASSES.add(CommandUpdate.class);
      this.UPDATE_CLASSES.add(WebDAVUpdate.class);
      this.UPDATE_CLASSES.add(JavaUpdate.class);
      this.UPDATE_CLASSES.add(SystemAttributeUpdate.class);
    }
  }

  // ///////////////////////////////////////////////////////////////////////////
  // instance variables

  /**
   * All defined file urls which are updated.
   *
   * @see #addURL
   */
  private final Map<String, URL> urls = new TreeMap<String, URL>();

  /**
   * Flag to store that the cache is initialised.
   *
   * @see #initialise
   * @see #addURL
   */
  private boolean initialised = false;

  /**
   * Cache with all update instances (loaded from the list of {@link #urls}).
   *
   * @see #initialise
   * @see #install
   */
  private final Map<Class<? extends AbstractUpdate>, List<AbstractUpdate>> cache =
      new HashMap<Class<? extends AbstractUpdate>, List<AbstractUpdate>>();

  // ///////////////////////////////////////////////////////////////////////////
  // instance methods

  /**
   * Installs the xml update scripts of the schema definitions for this version
   * defined in {@link #number}.
   */
  @SuppressWarnings("unchecked")
  public void install(final Long _number) throws EFapsException, Exception {

    // initialse cache
    initialise();

    // initiliase JexlContext (used to evalute version)
    JexlContext jexlContext = JexlHelper.createContext();
    if (_number != null) {
      jexlContext.getVars().put("version", _number);
    }

    // make update
    for (Class<? extends AbstractUpdate> updateClass : this.UPDATE_CLASSES) {
      for (AbstractUpdate update : this.cache.get(updateClass)) {
        update.updateInDB(jexlContext);
      }
    }
  }

  /**
   * @see #initialised
   */
  public void initialise() throws Exception {
    if (!this.initialised) {
      this.initialised = true;
      this.cache.clear();

      for (Class<? extends AbstractUpdate> updateClass : this.UPDATE_CLASSES) {
        List<AbstractUpdate> list = new ArrayList<AbstractUpdate>();
        this.cache.put(updateClass, list);
        Method method = updateClass.getMethod("readXMLFile", URL.class);
        for (URL url : this.urls.values()) {
          Object obj = method.invoke(null, url);
          if (obj != null) {
            list.add((AbstractUpdate) obj);
          }
        }
      }
    }
  }

  /**
   * Appends a new file defined through an url. The initialised flag is
   * automatically reseted.
   *
   * @param _url
   *          file to append
   * @see #urls
   * @see #initialised
   */
  public void addURL(final URL _url) {
    this.urls.put(_url.toString(), _url);
    this.initialised = false;
  }

  /**
   * this is the getter method for instance variable {@link #urls}
   *
   * @return instance variable {@link #urls}
   * @see #urls
   */
  public Map<String, URL> getURLs() {
    return this.urls;
  }

  // ///////////////////////////////////////////////////////////////////////////
  // instance getter and setter methods

  /**
   * Returns a string representation with values of all instance variables.
   *
   * @return string representation of this Application
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this).append("urls", this.urls).toString();
  }
}
