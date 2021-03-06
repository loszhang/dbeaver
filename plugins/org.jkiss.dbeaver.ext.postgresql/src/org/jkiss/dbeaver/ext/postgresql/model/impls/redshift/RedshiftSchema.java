/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 */
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * RedshiftExternalSchema
 */
public class RedshiftSchema extends PostgreSchema {
    private static final Log log = Log.getLog(RedshiftSchema.class);

    public RedshiftSchema(PostgreDatabase database, String name, ResultSet dbResult) throws SQLException {
        super(database, name, dbResult);
    }

    public RedshiftSchema(PostgreDatabase database, String name, PostgreRole owner) {
        super(database, name, owner);
    }

    @Override
    protected String getTableColumnsQueryExtraParameters(PostgreSchema owner, PostgreTableBase forTable) {
        return ",format_encoding(a.attencodingtype::integer) AS \"encoding\"";
    }
}

