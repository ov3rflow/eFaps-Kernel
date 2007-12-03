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
 * Revision:        $Rev:1510 $
 * Last Changed:    $Date:2007-10-18 09:35:40 -0500 (Thu, 18 Oct 2007) $
 * Last Changed By: $Author:jmox $
 */

package org.efaps.ui.wicket.components.table.header;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.behavior.StringHeaderContributor;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.util.string.CssUtils;
import org.apache.wicket.util.string.JavascriptUtils;

import org.efaps.db.Context;
import org.efaps.ui.wicket.components.dojo.DnDBehavior;
import org.efaps.ui.wicket.components.dojo.DojoReference;
import org.efaps.ui.wicket.components.modalwindow.ModalWindowContainer;
import org.efaps.ui.wicket.components.modalwindow.UpdateParentCallback;
import org.efaps.ui.wicket.components.table.TablePanel;
import org.efaps.ui.wicket.models.HeaderModel;
import org.efaps.ui.wicket.models.TableModel;
import org.efaps.ui.wicket.models.TableModel.UserAttributeKey;
import org.efaps.ui.wicket.pages.content.table.TablePage;
import org.efaps.ui.wicket.pages.error.ErrorPage;
import org.efaps.util.EFapsException;

/**
 * This class renders the Header of a Table
 *
 * @author jmox
 * @version $Id:TableHeaderPanel.java 1510 2007-10-18 14:35:40Z jmox $
 */
public class HeaderPanel extends Panel {

  private static final long serialVersionUID = 1L;

  private final ModalWindowContainer modal =
      new ModalWindowContainer("eFapsModal");

  public static final JavascriptResourceReference JAVASCRIPT =
      new JavascriptResourceReference(HeaderPanel.class, "HeaderPanel.js");

  private final Component tablepanel;

  private final String headerproperties;

  public HeaderPanel(final String _id, final TablePanel _tablePanel) {
    super(_id, _tablePanel.getModel());
    this.tablepanel = _tablePanel;
    final TableModel model = (TableModel) super.getModel();
    this.headerproperties = "eFapsTable" + model.getTableId();

    this.add(new AjaxStoreColumnWidthBehavior());
    this.add(new AjaxStoreColumnOrderBehavior());
    this.add(new AjaxReloadTableBehavior());
    this.add(new SimpleAttributeModifier("class", "eFapsTableHeader"));

    final DnDBehavior dndBehavior = DnDBehavior.getSourceBehavior();
    dndBehavior.setHorizontal(true);
    dndBehavior.setHandles(true);
    dndBehavior.setAppendJavaScript("storeColumnOrder(getColumnOrder("
        + this.headerproperties
        + "));\n reloadTable();\n");
    this.add(dndBehavior);

    final int browserWidth =
        ((WebClientInfo) getRequestCycle().getClientInfo()).getProperties()
            .getBrowserWidth();

    final RepeatingView cellRepeater = new RepeatingView("cellRepeater");
    add(cellRepeater);
    int i = model.getTableId();
    if (model.isShowCheckBoxes()) {
      final HeaderCellPanel cell =
          new HeaderCellPanel(cellRepeater.newChildId());
      cell.setOutputMarkupId(true);
      cellRepeater.add(cell);
      i++;
    }
    final List<String> widths = new ArrayList<String>();

    int fixed = 0;
    for (int j = 0; j < model.getHeaders().size(); j++) {
      final HeaderModel headermodel = model.getHeaders().get(j);

      final HeaderCellPanel cell =
          new HeaderCellPanel(cellRepeater.newChildId(), headermodel, model);

      if (headermodel.isFixedWidth()) {
        widths.add(".eFapsCellFixedWidth"
            + i
            + "{width: "
            + headermodel.getWidth()
            + "px;}\n");
        cell.add(new SimpleAttributeModifier("class",
            "eFapsTableHeaderCell eFapsCellFixedWidth" + i));
        fixed += headermodel.getWidth();
      } else {
        Integer width = 0;
        if (model.isUserSetWidth()) {
          width = headermodel.getWidth();
        } else {
          width =
              browserWidth / model.getWidthWeight() * headermodel.getWidth();
        }
        widths.add(".eFapsCellWidth"
            + i
            + "{width: "
            + width.toString()
            + "px;}\n");
        cell.add(new SimpleAttributeModifier("class",
            "eFapsTableHeaderCell eFapsCellWidth" + i));

        cell.add(DnDBehavior.getItemBehavior());
      }
      cell.setOutputMarkupId(true);
      cellRepeater.add(cell);

      if (j + 1 < model.getHeaders().size() && !headermodel.isFixedWidth()) {
        boolean add = false;
        for (int k = j + 1; k < model.getHeaders().size(); k++) {
          if (!model.getHeaders().get(k).isFixedWidth()) {
            add = true;
            break;
          }
        }
        if (add) {
          cellRepeater.add(new Seperator(cellRepeater.newChildId(), i,
              this.headerproperties));
        }
      }
      i++;
    }

    add(this.modal);
    this.modal.setPageMapName("modal");
    this.modal.setWindowClosedCallback(new UpdateParentCallback(this,
        this.modal, false));

    this.add(new StringHeaderContributor(getWidthStyle(widths)));

    this
        .add(new HeaderContributor(DojoReference.getHeaderContributerforDojo()));
    this
        .add(new HeaderContributor(HeaderContributor.forJavaScript(JAVASCRIPT)));
  }

  public final ModalWindowContainer getModal() {
    return this.modal;
  }

  private String getScript() {

    final String ret =
        JavascriptUtils.SCRIPT_OPEN_TAG
            + "  var "
            + this.headerproperties
            + " = new headerProperties();\n  "
            + this.headerproperties
            + ".headerID = \""
            + this.getMarkupId()
            + "\";\n  "
            + this.headerproperties
            + ".bodyID = \""
            + this.tablepanel.getMarkupId()
            + "\";\n  "
            + this.headerproperties
            + ".modelID = "
            + ((TableModel) super.getModel()).getTableId()
            + ";\n  "
            + this.headerproperties
            + ".storeColumnWidths = "
            + ((AjaxStoreColumnWidthBehavior) this.getBehaviors(
                AjaxStoreColumnWidthBehavior.class).get(0)).getJavaScript()
            + "  addOnResizeEvent(function (){positionTableColumns("
            + this.headerproperties
            + ");});\n"
            + "  dojo.addOnLoad(function (){positionTableColumns("
            + this.headerproperties
            + ");});\n"

            + ((AjaxStoreColumnOrderBehavior) this.getBehaviors(
                AjaxStoreColumnOrderBehavior.class).get(0)).getJavaScript()
            + ((AjaxReloadTableBehavior) this.getBehaviors(
                AjaxReloadTableBehavior.class).get(0)).getJavaScript()
            + JavascriptUtils.SCRIPT_CLOSE_TAG;

    return ret;
  }

  private String getWidthStyle(final List<String> _widths) {

    final StringBuilder ret = new StringBuilder();

    ret.append(CssUtils.INLINE_OPEN_TAG).append(".eFapsCSSId").append(
        ((TableModel) super.getModel()).getTableId()).append("{}\n");
    for (String width : _widths) {
      ret.append(width);
    }
    ret.append(CssUtils.INLINE_CLOSE_TAG);
    return ret.toString();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.apache.wicket.Component#onBeforeRender()
   */
  @Override
  protected void onBeforeRender() {
    this.add(new StringHeaderContributor(getScript()));
    super.onBeforeRender();

  }

  public class AjaxStoreColumnWidthBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    /**
     * String used as Variablename in the Javascript
     */
    public final static String COLUMNW_PARAMETERNAME = "eFapsColumnWidths";

    public String getJavaScript() {
      final StringBuilder ret = new StringBuilder();
      ret.append("function(_widths){\n    ").append(
          generateCallbackScript("wicketAjaxPost('"
              + getCallbackUrl(false)
              + "','"
              + COLUMNW_PARAMETERNAME
              + "=' + _widths")).append("\n" + "  }\n");
      return ret.toString();
    }

    @Override
    protected void respond(final AjaxRequestTarget _target) {
      final String widths =
          this.getComponent().getRequest().getParameter(COLUMNW_PARAMETERNAME);
      try {
        Context.getThreadContext().setUserAttribute(
            ((TableModel) this.getComponent().getModel())
                .getUserAttributeKey(UserAttributeKey.COLUMNWIDTH), widths);
        ((TableModel) this.getComponent().getModel()).resetModel();
      } catch (EFapsException e) {
        throw new RestartResponseException(new ErrorPage(e));
      }
    }
  }

  public class AjaxStoreColumnOrderBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    /**
     * String used as Variablename in the Javascript
     */
    public final static String COLUMNORDER_PARAMETERNAME = "eFapsColumnOrder";

    public String getJavaScript() {
      final StringBuilder ret = new StringBuilder();
      ret.append("  function storeColumnOrder(_columnOrder){\n    ").append(
          generateCallbackScript("wicketAjaxPost('"
              + getCallbackUrl(false)
              + "','"
              + COLUMNORDER_PARAMETERNAME
              + "=' + _columnOrder")).append("\n" + "  }\n");
      return ret.toString();
    }

    @Override
    protected void respond(final AjaxRequestTarget _target) {
      final String order =
          this.getComponent().getRequest().getParameter(
              COLUMNORDER_PARAMETERNAME);

      ((TableModel) this.getComponent().getModel()).setColumnOrder(order);

    }
  }

  public class AjaxReloadTableBehavior extends AbstractDefaultAjaxBehavior {

    private static final long serialVersionUID = 1L;

    public String getJavaScript() {
      final StringBuilder ret = new StringBuilder();
      ret.append("  function reloadTable(){\n    ").append(getCallbackScript())
          .append("\n  }\n");
      return ret.toString();
    }

    @Override
    protected void respond(final AjaxRequestTarget _target) {
      final TableModel model = (TableModel) this.getComponent().getModel();
      model.resetModel();
      this.getComponent().setResponsePage(new TablePage(model));

    }
  }
}
