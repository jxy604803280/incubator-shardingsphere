/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
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
 * </p>
 */

package io.shardingjdbc.core.parsing.integrate;

import com.google.common.base.Preconditions;
import io.shardingjdbc.core.constant.DatabaseType;
import io.shardingjdbc.core.parsing.SQLParsingEngine;
import io.shardingjdbc.core.parsing.parser.jaxb.ParserAssert;
import io.shardingjdbc.core.parsing.parser.jaxb.ParserAsserts;
import io.shardingjdbc.core.parsing.parser.jaxb.helper.ParserAssertHelper;
import io.shardingjdbc.core.parsing.parser.jaxb.helper.ParserJAXBHelper;
import io.shardingjdbc.core.parsing.parser.sql.SQLStatement;
import io.shardingjdbc.core.parsing.parser.sql.dql.select.SelectStatement;
import io.shardingjdbc.core.util.SQLPlaceholderUtil;
import io.shardingjdbc.test.sql.SQLCasesLoader;
import lombok.RequiredArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor
@RunWith(Parameterized.class)
public final class IntegrateSupportedSQLParsingTest extends AbstractBaseIntegrateSQLParsingTest {
    
    private final String testCaseName;
    
    private final DatabaseType databaseType;
    
    private final ParserAssert assertObj;
    
    @Parameters(name = "{0}In{1}")
    public static Collection<Object[]> getTestParameters() throws JAXBException {
        Collection<Object[]> result = new LinkedList<>();
        URL url = IntegrateSupportedSQLParsingTest.class.getClassLoader().getResource("parser/");
        Preconditions.checkNotNull(url, "Cannot found parser test cases.");
        File[] files = new File(url.getPath()).listFiles();
        Preconditions.checkNotNull(files, "Cannot found parser test cases.");
        for (File each : files) {
            result.addAll(getTestParameters(each));
        }
        return result;
    }
    
    private static Collection<Object[]> getTestParameters(final File file) throws JAXBException {
        List<Object[]> result = new LinkedList<>();
        for (ParserAssert each : ((ParserAsserts) JAXBContext.newInstance(ParserAsserts.class).createUnmarshaller().unmarshal(file)).getParserAsserts()) {
            result.addAll(getTestParameters(each));
        }
        return result;
    }
    
    private static List<Object[]> getTestParameters(final ParserAssert assertObj) {
        List<Object[]> result = new LinkedList<>();
        for (DatabaseType each : getDatabaseTypes(SQLCasesLoader.getInstance().getDatabaseTypes(assertObj.getSqlCaseId()))) {
            result.add(getTestParameters(assertObj, each));
        }
        return result;
    }
    
    private static Object[] getTestParameters(final ParserAssert assertObj, final DatabaseType databaseType) {
        final Object[] result = new Object[3];
        result[0] = assertObj.getSqlCaseId();
        result[1] = databaseType;
        result[2] = assertObj;
        return result;
    }
    
    @Test
    public void assertLiteralSQL() {
        assertSQLStatement(new SQLParsingEngine(databaseType, SQLPlaceholderUtil.replaceStatement(
                SQLCasesLoader.getInstance().getSQL(testCaseName), ParserJAXBHelper.getParameters(assertObj.getParameters())), getShardingRule()).parse(), false);
    }
    
    @Test
    public void assertPlaceholderSQL() {
        for (DatabaseType each : getDatabaseTypes(SQLCasesLoader.getInstance().getDatabaseTypes(testCaseName))) {
            assertSQLStatement(new SQLParsingEngine(each, SQLPlaceholderUtil.replacePreparedStatement(SQLCasesLoader.getInstance().getSQL(testCaseName)), getShardingRule()).parse(), true);
        }
    }
    
    private void assertSQLStatement(final SQLStatement actual, final boolean isPreparedStatement) {
        ParserAssertHelper.assertTables(assertObj.getTables(), actual.getTables());
        ParserAssertHelper.assertConditions(assertObj.getConditions(), actual.getConditions(), isPreparedStatement);
        ParserAssertHelper.assertSqlTokens(assertObj.getSqlTokens(), actual.getSqlTokens(), isPreparedStatement);
        if (actual instanceof SelectStatement) {
            SelectStatement selectStatement = (SelectStatement) actual;
            SelectStatement expectedSqlStatement = ParserJAXBHelper.getSelectStatement(assertObj);
            ParserAssertHelper.assertOrderBy(expectedSqlStatement.getOrderByItems(), selectStatement.getOrderByItems());
            ParserAssertHelper.assertGroupBy(expectedSqlStatement.getGroupByItems(), selectStatement.getGroupByItems());
            ParserAssertHelper.assertAggregationSelectItem(expectedSqlStatement.getAggregationSelectItems(), selectStatement.getAggregationSelectItems());
            ParserAssertHelper.assertLimit(assertObj.getLimit(), selectStatement.getLimit(), isPreparedStatement);
        }
    }
}
