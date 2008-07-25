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

package org.efaps.eclipse;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.efaps.admin.runlevel.RunLevel;
import org.efaps.db.Context;
import org.efaps.init.StartupDatabaseConnection;
import org.efaps.init.StartupException;
import org.efaps.update.Install;
import org.efaps.util.EFapsException;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 *
 * @author tmo
 */
public class EfapsPlugin extends AbstractUIPlugin
{
  public enum LogLevel  {
    DEBUG(SWT.COLOR_BLUE),
    ERROR(SWT.COLOR_DARK_RED),
    INFO(SWT.COLOR_BLACK),
    TRACE(SWT.COLOR_DARK_RED),
    WARN(SWT.COLOR_DARK_MAGENTA);

    final int color;

    LogLevel(final int _color)  {
      this.color = _color;
    }
  }

  /**
   * eFaps plug-in id.
   */
  public static final String PLUGIN_ID = "EclipsePlugin";

  /**
   * Shared eFaps plug-in instance.
   */
  private static EfapsPlugin plugin;

  /**
   * eFaps plug-in console.
   */
  private MessageConsole console = null;

  /**
   * Map of all console streams depending on the log level (and the different
   * colors for each console).
   *
   * @see LogLevel
   */
  private final Map<LogLevel,MessageConsoleStream> streams = new HashMap<LogLevel,MessageConsoleStream>();


  private boolean initialized = false;

  private final  ResourceBundle bundle = ResourceBundle.getBundle("plugin");

  /**
   * The constructor
   * @throws Exception
   * @throws IOException
   */
  public EfapsPlugin()
  {
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(final BundleContext context)
      throws Exception
  {
    super.start(context);
    plugin = this;

    this.console = new MessageConsole(translate(null, "plugin.console"), null);
    this.console.activate();
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[]{this.console});
    for (final LogLevel logLevel : LogLevel.values())  {
      MessageConsoleStream stream = console.newMessageStream();
      stream.setActivateOnWrite(true);
      stream.setColor(Display.getCurrent().getSystemColor(logLevel.color));
      this.streams.put(logLevel, stream);
    }
  }

  /**
   * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(final BundleContext context)
      throws Exception
  {
    ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[]{ this.console });
    this.console = null;
    this.streams.clear();
    plugin = null;
    super.stop(context);
  }


  /*private boolean displayConsoleView()
  {
    try
    {
      IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if( activeWorkbenchWindow != null )
      {
        IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
        if( activePage != null )
          activePage.showView(IConsoleConstants.ID_CONSOLE_VIEW, null, IWorkbenchPage.VIEW_VISIBLE);
      }

    } catch (PartInitException partEx) {
      return false;
    }

    return true;
  }*/
  /**
   *
   * @return <i>true</i> if connection to database is initialized, otherwise
   *         <i>false</i>
   */
  public boolean connect()
  {
    logInfo("connect.logInit");

    final String type = this.getPluginPreferences().getString("dbtype");
    logInfo("connect.logDatabaseType", type);

    final String factory = this.getPluginPreferences().getString("dbfactory");
    logInfo("connect.logFactory", factory);

    // prepare connection properties
    final String propsStr = this.getPluginPreferences().getString("dbproperties");
    final Map<String,String> props = new HashMap<String,String>();
    for (final String group : propsStr.split("\n")) {
      final int index = group.indexOf('=');
      final String key = (index > 0)
                         ? group.substring(0, index).trim()
                         : group.trim();
      final String value = (index > 0)
                           ? group.substring(index + 1).trim()
                           : "";
      props.put(key, value);
    }

    try {
      StartupDatabaseConnection.startup(type,
                                        factory,
                                        props,
                                        "org.objectweb.jotm.Current");
      this.initialized = true;
    } catch (StartupException e) {
      logError("connect.logException", e);
    }

    if (this.initialized)  {
      logInfo("connect.logInitialized");
    }

    return this.initialized;
  }

  public void disconnect()
  {
    println(LogLevel.INFO, "disconnect from eFaps");
  }

  /**
   * Reloads the internal eFaps cache.
   *
   * @return <i>true</i> if cache is reloaded; otherwise <i>false</i>
   */
  public boolean reloadCache()
  {
    boolean reloaded = false;
    logInfo("reloadCache.logStart");

    boolean finished = false;
    try {
      if (startTransaction())  {
        RunLevel.init("shell");
        RunLevel.execute();
        Context.commit();
        logInfo("reloadCache.logEnded");
        reloaded = true;
      } else  {
        logError("reloadCache.logFailed");
      }
      finished = true;
    } catch (Exception e) {
      logError("reloadCache.logException", e);
    } finally  {
      if (!finished)  {
        try {
          Context.rollback();
        } catch (EFapsException e) {
          e.printStackTrace();
        }
      }
    }
    return reloaded;
  }

  public boolean isInitialized()
  {
    return this.initialized;
  }

  /**
   *
   * @param _file   file used to update
   * @return
   */
  public boolean update(final String _file)
  {
    boolean updated = false;

    final File file = new File(_file);

    logInfo("update.logStart", file.getName());

    final Install install = new Install();

    boolean read = false;
    try {
      install.addFile(file.toURL(), "install-xml");
      read = true;
    } catch (MalformedURLException e) {
      logError("update.logException", e);
    }

    if (read)  {
      boolean finished = false;
      try {
        if (startTransaction())  {
          install.updateLatest();
          Context.commit();
          logInfo("update.logEnd", file.getName());
          updated = true;
        }
        finished = true;
      } catch (Exception e) {
        logError("update.logException", e);
      } finally  {
        if (!finished)  {
          try {
            Context.rollback();
          } catch (EFapsException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return updated;
  }

  /**
   * Starts a new transaction. A eFaps user must be defined, that a transaction
   * could be started. If no user is defined, the transaction is not started
   * (and <i>false</i> is returned).
   *
   * @return <i>true</i> if transaction could be started, otherwise
   *         <i>false</i>
   * @throws EFapsException
   */
  protected boolean startTransaction()
      throws EFapsException
  {
    boolean started = false;
    final String user = this.getPluginPreferences().getString("name");
    if ((user == null) || (user.length() == 0))  {
      println(getClass(), LogLevel.ERROR, "startTransaction.logNoUser");
    } else  {
      Context.begin(user);
      started = true;
    }
    return started;
  }

  /**
   * Returns the shared instance
   *
   * @return the shared instance
   */
  public static EfapsPlugin getDefault()
  {
    return plugin;
  }

  /**
   * Translates given key within given class. if no class is defined, the key
   * itself is only used. The translated key is then formated (with the message
   * formatted {@link MessageFormat}). If the key could not be translated, the
   * key itself surrounded with <code>!!!</code> is returned.
   *
   * @param _clazz      class for which the translation is done
   * @param _key       key of the class for which the translation is done
   * @param _arguments  arguments for the message formatter
   * @return formatted and translated text
   * @see MessageFormat   message formatter
   * @see #bundle         resource bundle used to translate the key
   */
  public String translate(final Class<?> _clazz,
                          final String _key,
                          final Object... _arguments)
  {
    final String key = (_clazz == null)
                       ? _key
                       : new StringBuilder().append(_clazz.getName()).append('.').append(_key).toString();
    String ret;
    try  {
      ret = this.bundle.getString(key);
      ret = MessageFormat.format(ret, _arguments);
    } catch (MissingResourceException e)  {
      ret = new StringBuilder().append("!!!").append(_key).append("!!!").toString();
    }
    return ret;
  }


  /**
   * Shows an information message dialog.
   *
   * @param _event  event for which the information message must be shown
   * @param _clazz  class for which the information must be shown
   * @param _text   key of the text property
   * @throws ExecutionException
   */
  public void showInfo(final ExecutionEvent _event,
                       final Class<?> _clazz,
                       final String _text)
      throws ExecutionException
  {
    final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(_event);
    MessageDialog.openInformation(window.getShell(),
                                  translate(null, "name"),
                                  translate(_clazz, _text));

  }

  /**
   * Shows an error message dialog.
   *
   * @param _event  event for which the information message must be shown
   * @param _clazz  class for which the error must be shown
   * @param _text   key of the text property
   * @throws ExecutionException
   */
  public void showError(final ExecutionEvent _event,
                        final Class<?> _clazz,
                        final String _text)
      throws ExecutionException
  {
    final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(_event);
    MessageDialog.openError(window.getShell(),
                            translate(null, "name"),
                            translate(_clazz, _text));

  }


  public void logInfo(final String _key,
                      final Object... _arguments)
  {
    println(getClass(), LogLevel.INFO, _key, _arguments);
  }

  public void logError(final String _key,
                       final Object... _arguments)
  {
    println(getClass(), LogLevel.ERROR, _key, _arguments);
  }

  /**
   *
   * @param _clazz
   * @param _logLevel
   * @param _key
   * @param _arguments
   */
  public void println(final Class<?> _clazz,
                      final LogLevel _logLevel,
                      final String _key,
                      final Object... _arguments)
  {
    if ((_arguments != null) && (_arguments.length > 0) && (_arguments[0] instanceof Throwable))  {
      final Throwable e = (Throwable) _arguments[0];
      final Object[] args = new Object[_arguments.length];
      args[0] = e.toString();
      for (int idx = 1; idx < _arguments.length; idx++)  {
        args[idx] = _arguments[idx];
      }
      println(_logLevel, translate(_clazz, _key, args), e);
    } else  {
      println(_logLevel, translate(_clazz, _key, _arguments), null);
    }
  }

  /**
   * Prints a text to the console stream
   *
   * @param _logLevel   log level (used to add to the output)
   * @param _text       text to print
   * @see #println(LogLevel, String, Throwable)
   */
  public void println(final LogLevel _logLevel,
                      final String _text)
  {
    println(_logLevel, _text, null);
  }

  /**
   * Prints a text and stack trace of related exception.
   *
   * @param _logLevel   log level (used to add to the output)
   * @param _text       text to print
   * @param _e          exception with stack trace (or null if no exception is
   *                    defined)
   */
  public void println(final LogLevel _logLevel,
                      final String _text,
                      final Throwable _e)
  {
    showConsole();
    final MessageConsoleStream stream = this.streams.get(_logLevel);
    final StringBuilder text = new StringBuilder()
          .append('[').append(_logLevel.name()).append("] ").append(_text);

    if (_e != null)  {
      final StringWriter sw = new StringWriter();
      _e.printStackTrace(new PrintWriter(sw));
      text.append('\n')
          .append(sw.toString());
    }
    stream.println(text.toString());
    try {
      stream.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Shows the eFaps plugin console.
   *
   * @see #console    eFaps plugin console
   */
  public void showConsole()
  {
    ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this.console);
  }
}
