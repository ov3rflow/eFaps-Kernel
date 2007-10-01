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

package org.efaps.webapp.components.dojo;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.JavascriptResourceReference;

public class DojoReference {

  public static final ResourceReference CSS_TUNDRA =
      new CompressedResourceReference(DojoReference.class,
          "dijit/themes/tundra/tundra.css");

  public static final JavascriptResourceReference JS_DOJO =
      new JavascriptResourceReference(DojoReference.class, "dojo/dojo.js");

  public static final JavascriptResourceReference JS_EFAPSDOJO =
      new JavascriptResourceReference(DojoReference.class, "dojo/eFapsDojo.js");
}
