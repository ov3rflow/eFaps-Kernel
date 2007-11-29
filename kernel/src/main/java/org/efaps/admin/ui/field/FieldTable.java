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

package org.efaps.admin.ui.field;

import org.efaps.admin.ui.Table;

public class FieldTable extends Field {

  /**
   * The static variable defines the class name in eFaps.
   */
  public final static EFapsClassName EFAPS_CLASSNAME =
      EFapsClassName.FIELDTABLE;

  /**
   * The instance variable stores the target user interface table object which
   * is shown by the this field.
   *
   * @see #getTargetTable
   * @see #setTargetTable
   */
  private Table targetTable = null;

  public FieldTable(final long _id, final String _uuid, final String _name) {
    super(_id, _uuid, _name);
  }

  @Override
  protected void setLinkProperty(final EFapsClassName _linkType,
                                 final long _toId,
                                 final EFapsClassName _toType,
                                 final String _toName) throws Exception {
    switch (_linkType) {
      case LINK_TARGET_TABLE:
        this.targetTable = Table.get(_toId);
        break;
      default:
        super.setLinkProperty(_linkType, _toId, _toType, _toName);
        break;
    }
  }

  /**
   * This is the getter method for the instance variable {@link #targetTable}.
   *
   * @return value of instance variable {@link #targetTable}
   */
  public Table getTargetTable() {
    return this.targetTable;
  }

}
