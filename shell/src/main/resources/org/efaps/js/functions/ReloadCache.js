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
 * Author:          tmo
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

importClass(Packages.org.efaps.db.Context);
importClass(Packages.org.efaps.util.cache.Cache);
importClass(Packages.org.efaps.admin.runlevel.RunLevel);

/**
 * Reloades the complete eFaps cache.
 */
function reloadCache()  {
  try  {
    Shell.transactionManager.begin();
    var context = new Context.newThreadContext(
                                    Shell.transactionManager.getTransaction());
    Cache.reloadCache();
    Shell.transactionManager.rollback();
    context.close();

  } catch (e)  {
    print(e);
  }
}

function initRunLevel(_runLevel)  {
  try  {
    Shell.transactionManager.begin();
    var context = new Context.newThreadContext(
                                    Shell.transactionManager.getTransaction());
    RunLevel.init(_runLevel);
    Shell.transactionManager.rollback();
    context.close();

  } catch (e)  {
    print(e);
  }
}