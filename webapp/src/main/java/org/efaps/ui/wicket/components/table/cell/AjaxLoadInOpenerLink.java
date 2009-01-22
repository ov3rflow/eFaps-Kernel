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

package org.efaps.ui.wicket.components.table.cell;

import org.apache.wicket.IPageMap;
import org.apache.wicket.PageParameters;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import org.efaps.admin.ui.Menu;
import org.efaps.db.Instance;
import org.efaps.ui.wicket.models.cell.UITableCell;
import org.efaps.ui.wicket.pages.contentcontainer.ContentContainerPage;
import org.efaps.ui.wicket.pages.error.ErrorPage;
import org.efaps.ui.wicket.pages.main.MainPage;

/**
 * Class is used as link to load a page from an popup window inside the opener
 * window.
 *
 * @author jmox
 * @version $Id$
 *
 */
public class AjaxLoadInOpenerLink extends AjaxLink<UITableCell> {

  /**
   * Needed for serialization.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Constructor.
   *
   * @param _wicketId   wicket id for this component
   * @param _model      model for this component
   */
  public AjaxLoadInOpenerLink(final String _wicketId,
                              final IModel<UITableCell> _model) {
    super(_wicketId, _model);
  }
  /**
   * Method to load something inside the opener window.
   * @param _target   AjaxRequestTarget
   */
  @Override
  public void onClick(final AjaxRequestTarget _target) {
    Instance instance = null;
    final UITableCell cellmodel = super.getModelObject();
    if (cellmodel.getOid() != null) {
      instance = new Instance(cellmodel.getOid());
      Menu menu = null;
      try {
        menu = Menu.getTypeTreeMenu(instance.getType());
      } catch (final Exception e) {
        throw new RestartResponseException(new ErrorPage(e));
      }
      if (menu == null) {
        final Exception ex =
            new Exception("no tree menu defined for type "
                + instance.getType().getName());
        throw new RestartResponseException(new ErrorPage(ex));
      }
      final PageParameters parameters = new PageParameters();
      parameters.add("command", menu.getUUID().toString());
      parameters.add("oid", cellmodel.getOid());

      //the url must be in the pagemap of the frame inside the mainpage
      final IPageMap pageMap
            = Session.get().pageMapForName(MainPage.IFRAME_PAGEMAP_NAME, false);
      final String url =
               (String) urlFor(pageMap, ContentContainerPage.class, parameters);

      _target.prependJavascript(new StringBuilder()
                  .append("opener.eFapsFrameContent.location.href = '")
                  .append(url).append("'").toString());
    }

  }

}