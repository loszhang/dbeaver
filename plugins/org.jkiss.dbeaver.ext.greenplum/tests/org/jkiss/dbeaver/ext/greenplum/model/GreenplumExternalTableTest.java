package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GreenplumExternalTableTest {
    @Mock
    DBRProgressMonitor monitor;

    @Mock
    PostgreSchema mockSchema;

    @Mock
    PostgreDatabase mockDatabase;

    @Mock
    JDBCResultSet mockResults;

    @Mock
    PostgreDataSource mockDataSource;

    @Mock
    PostgreSchema.TableCache mockTableCache;

    private final String exampleDatabaseName = "sampleDatabase";
    private final String exampleSchemaName = "sampleSchema";
    private final String exampleTableName = "sampleTable";
    private final String exampleUriLocation = "gpfdist://filehost:8081/*.txt";
    private final String exampleEncoding = "UTF8";
    private final String exampleFormatOptions = "DELIMITER ','";
    private final String exampleFormatType = "c";
    private final int exampleRejectLimit = 100_000;
    private final String exampleRejectLimitType = "r";
    private final String exampleExecLocation = "ALL_SEGMENTS";

    @Before
    public void setup() throws SQLException {
        Mockito.when(mockSchema.getDatabase()).thenReturn(mockDatabase);
        Mockito.when(mockSchema.getDataSource()).thenReturn(mockDataSource);
        Mockito.when(mockDatabase.getName()).thenReturn(exampleDatabaseName);
        Mockito.when(mockSchema.getName()).thenReturn(exampleSchemaName);
        Mockito.when(mockSchema.getTableCache()).thenReturn(mockTableCache);
        Mockito.when(mockDataSource.isServerVersionAtLeast(Matchers.anyInt(), Matchers.anyInt())).thenReturn(false);

        Mockito.when(mockResults.getString("relname")).thenReturn(exampleTableName);
        Mockito.when(mockResults.getString("fmttype")).thenReturn(exampleFormatType);
        Mockito.when(mockResults.getString("urilocation")).thenReturn(exampleUriLocation);
        Mockito.when(mockResults.getString("fmtopts")).thenReturn(exampleFormatOptions);
        Mockito.when(mockResults.getString("encoding")).thenReturn(exampleEncoding);
        Mockito.when(mockResults.getString("execlocation")).thenReturn(exampleExecLocation);
    }

    @Test
    public void onCreation_readsASingleUriLocationFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("urilocation")).thenReturn("SOME_EXTERNAL_LOCATION");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals(Collections.singletonList("SOME_EXTERNAL_LOCATION"), table.getUriLocations());
    }

    @Test
    public void onCreation_readsMultipleUriLocaitonsFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("urilocation"))
                .thenReturn("SOME_EXTERNAL_LOCATION,ANOTHER_EXTERNAL_LOCATION");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals(Arrays.asList("SOME_EXTERNAL_LOCATION", "ANOTHER_EXTERNAL_LOCATION"),
                table.getUriLocations());
    }

    @Test
    public void onCreation_readsExecLocationFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("execlocation")).thenReturn("ALL_SEGMENTS");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "ALL_SEGMENTS";
        Assert.assertEquals(expectedUriLocation, table.getExecLocation());
    }

    @Test
    public void onCreation_readsFormatTypeFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("fmttype")).thenReturn("t");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals(GreenplumExternalTable.FormatType.t, table.getFormatType());
    }

    @Test
    public void onCreation_readsFormatOptionsFromDbResult() throws SQLException {
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("delimiter ',' null '' escape '\"' quote '\"' header");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "delimiter ',' null '' escape '\"' quote '\"' header";
        Assert.assertEquals(expectedUriLocation, table.getFormatOptions());
    }

    @Test
    public void onCreation_readsEncodingFromDBResult() throws SQLException {
        Mockito.when(mockResults.getString("encoding")).thenReturn("UTF8");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        String expectedUriLocation = "UTF8";
        Assert.assertEquals(expectedUriLocation, table.getEncoding());
    }

    @Test
    public void onCreation_readsRejectLimitTypeFromDBResult() throws SQLException {
        Mockito.when(mockResults.getString("rejectlimittype")).thenReturn("r");
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        Assert.assertEquals(GreenplumExternalTable.RejectLimitType.r, table.getRejectLimitType());
    }

    @Test
    public void onCreation_readsRejectLimitFromDBResult() throws SQLException {
        Mockito.when(mockResults.getInt("rejectlimit")).thenReturn(50_000);
        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        int expectedUriLocation = 50_000;
        Assert.assertEquals(expectedUriLocation, table.getRejectLimit());
    }

    @Test
    public void generateDDL_whenTableHasASingleColumn_returnsDDLStringForASingleColumn()
            throws DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasNoEncodingSet_returnsDDLStringWithNoEncoding()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("encoding")).thenReturn(null);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDLWithNoEncodingSet =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )";

        Assert.assertEquals(expectedDDLWithNoEncodingSet, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasMultiColumns_returnsDDLStringForMultiColumns()
            throws DBException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        PostgreTableColumn mockPostgreTableColumn2 = mockDbColumn("column2", "int2", 2);
        List<PostgreTableColumn> tableColumns = Arrays.asList(mockPostgreTableColumn, mockPostgreTableColumn2);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDLWithMultiColumns =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4,\n\tcolumn2 int2\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDLWithMultiColumns, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasASegmentRejectLimit_returnsDDLStringWithSegmentRejectLimit()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getInt("rejectlimit")).thenReturn(exampleRejectLimit);
        Mockito.when(mockResults.getString("rejectlimittype")).thenReturn(exampleRejectLimitType);

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'\n" +
                        "SEGMENT REJECT LIMIT 100000 ROWS";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenExecLocationIsMasterOnly_returnsDDLStringWithAMasterOnlyExecLocation()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("execlocation")).thenReturn("MASTER_ONLY");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON MASTER\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasMultipleUriLocations_returnsDDLStringForASingleColumn()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("urilocation"))
                .thenReturn("gpfdist://filehost:8081/*.txt,gpfdist://filehost:8081/*.gz");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt',\n" +
                        "\t'gpfdist://filehost:8081/*.gz'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CSV' ( DELIMITER ',' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    @Test
    public void generateDDL_whenTableHasACustomFormatType_returnsDDLStringWithACustomFormat()
            throws DBException, SQLException {
        PostgreTableColumn mockPostgreTableColumn = mockDbColumn("column1", "int4", 1);
        List<PostgreTableColumn> tableColumns = Collections.singletonList(mockPostgreTableColumn);

        Mockito.when(mockResults.getString("fmttype")).thenReturn("b");
        Mockito.when(mockResults.getString("fmtopts")).thenReturn("FORMATTER 'formatter_export_s'");

        GreenplumExternalTable table = new GreenplumExternalTable(mockSchema, mockResults);
        addMockColumnsToTableCache(tableColumns, table);

        String expectedDDL =
                "CREATE EXTERNAL TABLE sampleDatabase.sampleSchema.sampleTable (\n\tcolumn1 int4\n)\n" +
                        "LOCATION (\n" +
                        "\t'gpfdist://filehost:8081/*.txt'\n" +
                        ") ON ALL\n" +
                        "FORMAT 'CUSTOM' ( FORMATTER='formatter_export_s' )\n" +
                        "ENCODING 'UTF8'";

        Assert.assertEquals(expectedDDL, table.generateDDL(monitor));
    }

    private PostgreTableColumn mockDbColumn(String columnName, String columnType, int ordinalPosition) {
        PostgreTableColumn mockPostgreTableColumn = Mockito.mock(PostgreTableColumn.class);
        Mockito.when(mockPostgreTableColumn.getName()).thenReturn(columnName);
        Mockito.when(mockPostgreTableColumn.getTypeName()).thenReturn(columnType);
        Mockito.when(mockPostgreTableColumn.getOrdinalPosition()).thenReturn(ordinalPosition);
        return mockPostgreTableColumn;
    }

    private void addMockColumnsToTableCache(List<PostgreTableColumn> tableColumns, GreenplumExternalTable table)
            throws DBException {
        Mockito.when(mockTableCache.getChildren(monitor, mockSchema, table)).thenReturn(tableColumns);
    }
}