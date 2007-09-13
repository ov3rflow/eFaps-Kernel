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

package org.efaps.webapp.models;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.ui.Command;
import org.efaps.admin.ui.CommandAbstract;
import org.efaps.admin.ui.Menu;
import org.efaps.admin.ui.MenuAbstract;
import org.efaps.beans.ValueList;
import org.efaps.beans.valueparser.ValueParser;
import org.efaps.db.Instance;
import org.efaps.db.SearchQuery;

/**
 * @author tmo
 * @version $Id$
 */
public class MenuItemModel extends Model {

  // ///////////////////////////////////////////////////////////////////////////
  // instance variables

  private static final long serialVersionUID = 505704924081527139L;

  /** Url to the image of this menu item. */
  private final String image;

  /** Label of this menu item. */
  private final String label;

  /** Description of this menu item. */
  private final String description;

  private final String oid;

  private final int target;

  private final UUID uuid;

  private final String reference;

  /**
   * All childs of this menu item.
   */
  private final List<MenuItemModel> childs = new ArrayList<MenuItemModel>();

  /** Url of this menu item. */
  private String url;

  private int level = 0;

  private int previouslevel = 0;

  private IModel ancestor;

  private boolean header = false;

  private boolean selected = false;

  public MenuItemModel(final UUID _uuid) throws Exception {
    this(Menu.get(_uuid), null);
  }

  // ///////////////////////////////////////////////////////////////////////////
  // constructors / destructors

  public MenuItemModel(final UUID _uuid, final String _oid) throws Exception {
    this(Menu.get(_uuid), _oid);
  }

  protected MenuItemModel(final CommandAbstract _command, String _oid)
                                                                      throws Exception {
    this.image = _command.getIcon();
    this.reference = _command.getReference();
    this.target = _command.getTarget();
    this.uuid = _command.getUUID();
    this.description = "";
    this.oid = _oid;

    String label = DBProperties.getProperty(_command.getLabel());

    if (_oid != null) {
      SearchQuery query = new SearchQuery();
      query.setObject(_oid);
      ValueParser parser = new ValueParser(new StringReader(label));
      ValueList list = parser.ExpressionString();
      list.makeSelect(query);
      if (query.selectSize() > 0) {
        query.execute();
        if (query.next()) {
          label = list.makeString(query);
        }
        query.close();
      }

    }
    this.label = label;

    if (_command instanceof MenuAbstract) {
      for (CommandAbstract subCmd : ((MenuAbstract) _command).getCommands()) {
        if (subCmd.hasAccess()) {
          this.childs.add(new MenuItemModel(subCmd, _oid));
        }
      }
    }
  }

  public void setLevel(final int _level) {
    this.level = _level;
  }

  public int getLevel() {
    return this.level;
  }

  public int getTarget() {
    return this.target;
  }

  public String getOid() {
    return this.oid;
  }

  public UUID getUUID() {
    return this.uuid;

  }

  public String getImage() {
    return this.image;
  }

  public String getTypeImage() {
    String ret = null;
    if (this.oid != null) {
      ret = new Instance(this.oid).getType().getIcon();
    }
    return ret;
  }

  public void setURL(String _url) {
    this.url = _url;
  }

  public List<MenuItemModel> getChilds() {
    return this.childs;
  }

  public boolean hasChilds() {
    return !this.childs.isEmpty();
  }

  public String getLabel() {
    return this.label;
  }

  public CommandAbstract getCommand() {
    CommandAbstract cmd = Command.get(this.uuid);
    if (cmd == null) {
      cmd = Menu.get(this.uuid);
    }
    return cmd;
  }

  /**
   * This is the getter method for the instance variable {@link #reference}.
   *
   * @return value of instance variable {@link #reference}
   */

  public String getReference() {
    return this.reference;
  }

  /**
   * This is the getter method for the instance variable {@link #url}.
   *
   * @return value of instance variable {@link #url}
   */

  public String getUrl() {
    return this.url;
  }

  /**
   * This is the setter method for the instance variable {@link #url}.
   *
   * @param url
   *                the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * This is the getter method for the instance variable {@link #ancestor}.
   *
   * @return value of instance variable {@link #ancestor}
   */

  public IModel getAncestor() {
    return this.ancestor;
  }

  /**
   * This is the setter method for the instance variable {@link #ancestor}.
   *
   * @param ancestor
   *                the ancestor to set
   */
  public void setAncestor(IModel ancestor) {
    this.ancestor = ancestor;
  }

  /**
   * This is the getter method for the instance variable {@link #previouslevel}.
   *
   * @return value of instance variable {@link #previouslevel}
   */

  public int getPreviouslevel() {
    return this.previouslevel;
  }

  /**
   * This is the getter method for the instance variable {@link #description}.
   *
   * @return value of instance variable {@link #description}
   */

  public String getDescription() {
    return this.description;
  }

  /**
   * This is the setter method for the instance variable {@link #previouslevel}.
   *
   * @param previouslevel
   *                the previouslevel to set
   */
  public void setPreviouslevel(int previouslevel) {
    this.previouslevel = previouslevel;
  }

  /**
   * This is the getter method for the instance variable {@link #header}.
   *
   * @return value of instance variable {@link #header}
   */

  public boolean isHeader() {
    return this.header;
  }

  /**
   * This is the setter method for the instance variable {@link #header}.
   *
   * @param header
   *                the header to set
   */
  public void setHeader(boolean header) {
    this.header = header;
  }

  /**
   * This is the getter method for the instance variable {@link #selected}.
   *
   * @return value of instance variable {@link #selected}
   */

  public boolean isSelected() {
    return this.selected;
  }

  /**
   * This is the setter method for the instance variable {@link #selected}.
   *
   * @param selected
   *                the selected to set
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  // ///////////////////////////////////////////////////////////////////////////

}
