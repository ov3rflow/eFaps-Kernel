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

package org.efaps.ui.wicket.resources;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;

/**
 * TODO description
 *
 * @author jmox
 * @version $Id$
 */
public class StaticHeaderContributor extends HeaderContributor {

  private static final long serialVersionUID = 1L;

  private final EFapsContentReference reference;

  public enum Type {
    CSS,
    JS;
  }

  private StaticHeaderContributor.Type type;

  public StaticHeaderContributor(final IHeaderContributor _headerContributor,
                                 final EFapsContentReference _reference) {
    super(_headerContributor);
    this.reference = _reference;
  }

  public static final StaticHeaderContributor forCss(
                                                     final EFapsContentReference _reference) {
    final StaticHeaderContributor ret =
        new StaticHeaderContributor(new IHeaderContributor() {

          private static final long serialVersionUID = 1L;

          public void renderHead(IHeaderResponse response) {
            response.renderCSSReference(_reference.getCSSUrl());
          }
        }, _reference);
    ret.setType(Type.CSS);
    return ret;
  }

  /**
   * This is the getter method for the instance variable {@link #reference}.
   *
   * @return value of instance variable {@link #reference}
   */
  public EFapsContentReference getReference() {
    return this.reference;
  }

  public static final StaticHeaderContributor forJavaScript(
                                                            final EFapsContentReference _reference) {

    final StaticHeaderContributor ret =
        new StaticHeaderContributor(new IHeaderContributor() {

          private static final long serialVersionUID = 1L;

          public void renderHead(IHeaderResponse response) {
            response.renderJavascriptReference(_reference.getCSSUrl());
          }
        }, _reference);
    ret.setType(Type.JS);
    return ret;
  }

  /**
   * This is the getter method for the instance variable {@link #type}.
   *
   * @return value of instance variable {@link #type}
   */
  public StaticHeaderContributor.Type getType() {
    return this.type;
  }

  /**
   * This is the setter method for the instance variable {@link #type}.
   *
   * @param type
   *                the type to set
   */
  public void setType(StaticHeaderContributor.Type type) {
    this.type = type;
  }

}
