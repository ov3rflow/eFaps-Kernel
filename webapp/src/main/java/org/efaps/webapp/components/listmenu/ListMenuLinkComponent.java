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

package org.efaps.webapp.components.listmenu;

import org.apache.wicket.PageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.InlineFrame;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import org.efaps.admin.ui.CommandAbstract;
import org.efaps.webapp.EFapsSession;
import org.efaps.webapp.models.MenuItemModel;
import org.efaps.webapp.pages.WebFormPage;
import org.efaps.webapp.pages.WebTablePage;

/**
 * @author jmo
 * @version $Id$
 */
public class ListMenuLinkComponent extends WebMarkupContainer {

  private static final long serialVersionUID = 1L;

  private final String menukey;

  private String defaultStyleClass;

  private boolean isInit;

  public ListMenuLinkComponent(final String _id, final String _menukey,
                               final IModel _model) {
    super(_id, _model);
    this.menukey = _menukey;
    this.isInit = true;
    this.add(new AjaxClickBehaviour());
  }

  @Override
  protected void onComponentTag(ComponentTag tag) {
    super.onComponentTag(tag);
    MenuItemModel model = (MenuItemModel) super.getModel();

    if (model.hasChilds() && (this.findParent(ListItem.class) != null)) {
      this.defaultStyleClass = "eFapsListMenuHeader";
      if (this.isInit) {
        ((EFapsSession) this.getSession()).setIntoCache(this.menukey,
            this);
        tag.put("class", "eFapsListMenuSelected");

        ListMenuPanel parentListMenuPanel =
            (ListMenuPanel) this.findParent(ListMenuPanel.class);
        parentListMenuPanel.setHeaderComponent(this);
        this.isInit = false;
      } else {
        if (this.equals(((EFapsSession) this.getSession())
            .getFromCache(this.menukey))) {
          tag.put("class", "eFapsListMenuSelected");
        } else {

          tag.put("class", "eFapsListMenuHeader");
        }
      }
    } else {
      ListMenuPanel listmenupanel =
          (ListMenuPanel) this.findParent(ListMenuPanel.class);
      int padding =
          model.getLevel() * listmenupanel.getPadding()
              + listmenupanel.getPaddingAdd();

      tag.put("style", "padding-left:" + padding + "px;");
      tag.put("class", "eFapsListMenuItem");
      this.defaultStyleClass = "eFapsListMenuItem";
    }

  }

  public String getDefaultStyleClass() {
    return this.defaultStyleClass;
  }

  public String getCallbackScript() {
    return ((AjaxClickBehaviour) super.getBehaviors().get(0))
        .getCallbackScript();
  }

  private class AjaxClickBehaviour extends AjaxEventBehavior {

    private static final long serialVersionUID = 1L;

    public AjaxClickBehaviour() {
      super("onclick");
    }

    public String getCallbackScript() {
      return super.getCallbackScript().toString();

    }

    @Override
    protected void onEvent(final AjaxRequestTarget _target) {
      MenuItemModel model = (MenuItemModel) this.getComponent().getModel();

      CommandAbstract cmd = model.getCommand();
      PageParameters para = new PageParameters();
      para.add("oid", model.getOid());
      para.add("command", cmd.getUUID().toString());

      InlineFrame page;
      if (cmd.getTargetTable() != null) {
        page =
            new InlineFrame("eFapsContentContainerFrame", PageMap
                .forName("content"), WebTablePage.class, para);
      } else {
        page =
            new InlineFrame("eFapsContentContainerFrame", PageMap
                .forName("content"), WebFormPage.class, para);
      }
      InlineFrame component =
          (InlineFrame) getPage()
              .get(
                  "eFapsSplitContainer:containerrechts:aktParent:eFapsContentContainerFrame");
      page.setOutputMarkupId(true);

      component.replaceWith(page);
      _target.addComponent(page.getParent());
      ListMenuUpdate.setSelectedItem(menukey, this.getComponent(), _target);

    }
  }
}
