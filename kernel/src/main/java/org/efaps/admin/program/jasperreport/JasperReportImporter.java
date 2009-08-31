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

package org.efaps.admin.program.jasperreport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignParameter;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRProperties;
import net.sf.jasperreports.engine.xml.JRXmlDigester;
import net.sf.jasperreports.engine.xml.JRXmlDigesterFactory;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.xml.sax.SAXException;

import org.efaps.admin.EFapsClassNames;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.program.AbstractProgramImporter;
import org.efaps.db.Instance;
import org.efaps.db.SearchQuery;
import org.efaps.util.EFapsException;

/**
 * Class is used to import a JasperReport into the eFaps DataBase.
 *
 * @author The eFaps Team
 * @version $Id$
 */
public class JasperReportImporter extends AbstractProgramImporter
{
    /**
     * Design of the current report.
     */
    private JasperDesign jasperDesign;

    /**
     * @param _url  url to the file to be imported
     * @throws EFapsException   on error
     */
    public JasperReportImporter(final URL _url) throws EFapsException
    {
        super(_url);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readCode() throws EFapsException
    {
        JRProperties.setProperty(JRProperties.COMPILER_XML_VALIDATION, false);
        try {
            final JRXmlDigester digester = JRXmlDigesterFactory.createDigester();

            final JRXmlLoader loader = new JRXmlLoader(digester);

            final InputStream input = getUrl().openStream();

            // this property is set with files that can not be loaded, if a different file loader is used
           // but the validation is deactivated so they are not necessary
            digester.getParser().setProperty("http://java.sun.com/xml/jaxp/properties/schemaSource", new Object[] {});
            this.jasperDesign = loader.loadXML(input);
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final JRException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String evalProgramName()
    {
        return this.jasperDesign.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String evalRevision()
    {
        return "1";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UUID evalUUID()
    {
        UUID ret = null;
        final JRDesignParameter para = (JRDesignParameter) this.jasperDesign.getParametersMap().get("EFAPS_DEFINITION");
        if (para != null) {
            final String uuid = para.getPropertiesMap().getProperty("UUID");
            if (uuid != null) {
                ret = UUID.fromString(uuid);
            }
        }
        return ret;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public Instance searchInstance() throws EFapsException
    {
        Instance instance = null;
        final Type type = Type.get(EFapsClassNames.ADMIN_PROGRAM_JASPERREPORT);
        final SearchQuery query = new SearchQuery();
        query.setQueryTypes(type.getName());
        query.addWhereExprEqValue("Name", getProgramName());
        query.addSelect("OID");
        query.executeWithoutAccessCheck();
        if (query.next()) {
            instance = Instance.get((String) query.get("OID"));
        }
        query.close();
        return instance;
    }
}
