package marquez.api;

import static marquez.api.DatasetResource.Datasets;
import static marquez.common.models.ModelGenerator.newDatasetName;
import static marquez.common.models.ModelGenerator.newNamespaceName;
import static marquez.common.models.ModelGenerator.newRunId;
import static marquez.common.models.ModelGenerator.newTagName;
import static marquez.service.models.ModelGenerator.newDbTable;
import static marquez.service.models.ModelGenerator.newDbTableMeta;
import static marquez.service.models.ModelGenerator.newDbTableWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response;
import marquez.UnitTests;
import marquez.api.exceptions.DatasetNotFoundException;
import marquez.common.models.DatasetName;
import marquez.common.models.Field;
import marquez.common.models.NamespaceName;
import marquez.common.models.TagName;
import marquez.service.DatasetService;
import marquez.service.JobService;
import marquez.service.NamespaceService;
import marquez.service.TagService;
import marquez.service.models.Dataset;
import marquez.service.models.DbTable;
import marquez.service.models.DbTableMeta;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Category(UnitTests.class)
public class DatasetResourceTest {
  private static final NamespaceName NAMESPACE_NAME = newNamespaceName();

  private static final DatasetName DB_TABLE_NAME = newDatasetName();
  private static final DbTable DB_TABLE_0 = newDbTable();
  private static final DbTable DB_TABLE_1 = newDbTable();
  private static final DbTable DB_TABLE_2 = newDbTable();
  private static final ImmutableList<Dataset> DATASETS =
      ImmutableList.of(DB_TABLE_0, DB_TABLE_1, DB_TABLE_2);

  private static final TagName TAG_NAME = newTagName();
  private static final UUID RUN_ID = newRunId();

  @Rule public MockitoRule rule = MockitoJUnit.rule();

  @Mock private NamespaceService namespaceService;
  @Mock private DatasetService datasetService;
  @Mock private JobService jobService;
  @Mock private TagService tagService;
  private DatasetResource datasetResource;

  @Before
  public void setUp() {
    datasetResource =
        spy(new DatasetResource(namespaceService, datasetService, jobService, tagService));
  }

  @Test
  public void testCreateOrUpdate() throws Exception {
    final DbTableMeta dbTableMeta = newDbTableMeta();
    final DbTable dbTable = toDbTable(DB_TABLE_NAME, dbTableMeta);

    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(jobService.runExists(RUN_ID)).thenReturn(true);
    when(datasetService.createOrUpdate(NAMESPACE_NAME, DB_TABLE_NAME, dbTableMeta))
        .thenReturn(dbTable);

    final Response response =
        datasetResource.createOrUpdate(NAMESPACE_NAME, DB_TABLE_NAME, dbTableMeta);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((Dataset) response.getEntity()).isEqualTo(dbTable);
  }

  @Test
  public void testGet() throws Exception {
    final DbTable dbTable = newDbTableWith(DB_TABLE_NAME);

    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.get(NAMESPACE_NAME, DB_TABLE_NAME)).thenReturn(Optional.of(dbTable));

    final Response response = datasetResource.get(NAMESPACE_NAME, DB_TABLE_NAME);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat((Dataset) response.getEntity()).isEqualTo(dbTable);
  }

  @Test
  public void testGet_notFound() throws Exception {
    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.get(NAMESPACE_NAME, DB_TABLE_NAME)).thenReturn(Optional.empty());

    assertThatExceptionOfType(DatasetNotFoundException.class)
        .isThrownBy(() -> datasetResource.get(NAMESPACE_NAME, DB_TABLE_NAME))
        .withMessageContaining(DB_TABLE_NAME.getValue());
  }

  @Test
  public void testList() throws Exception {
    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.getAll(NAMESPACE_NAME, 4, 0)).thenReturn(DATASETS);

    final Response response = datasetResource.list(NAMESPACE_NAME, 4, 0);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(((Datasets) response.getEntity()).getValue())
        .containsOnly(DB_TABLE_0, DB_TABLE_1, DB_TABLE_2);
  }

  @Test
  public void testList_empty() throws Exception {
    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.getAll(NAMESPACE_NAME, 4, 0)).thenReturn(ImmutableList.of());

    final Response response = datasetResource.list(NAMESPACE_NAME, 4, 0);
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(((Datasets) response.getEntity()).getValue()).isEmpty();
  }

  @Test
  public void testTag_dataset() throws Exception {
    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.exists(NAMESPACE_NAME, DB_TABLE_NAME)).thenReturn(true);
    when(tagService.exists(TAG_NAME)).thenReturn(true);

    final DbTable dbTable = tagDatasetWith(TAG_NAME, newDbTableWith(DB_TABLE_NAME));
    doReturn(Response.ok(dbTable).build()).when(datasetResource).get(NAMESPACE_NAME, DB_TABLE_NAME);

    final Response response = datasetResource.tag(NAMESPACE_NAME, DB_TABLE_NAME, TAG_NAME);
    assertThat(response.getStatus()).isEqualTo(200);

    final Dataset dataset = (Dataset) response.getEntity();
    assertThat(dataset).isEqualTo(dbTable);
    assertThat(dataset.getTags()).contains(TAG_NAME);
  }

  @Test
  public void testTag_datasetField() throws Exception {
    when(namespaceService.exists(NAMESPACE_NAME)).thenReturn(true);
    when(datasetService.exists(NAMESPACE_NAME, DB_TABLE_NAME)).thenReturn(true);
    when(tagService.exists(TAG_NAME)).thenReturn(true);

    final DbTable dbTable = tagAllFieldsWith(TAG_NAME, newDbTableWith(DB_TABLE_NAME));
    doReturn(Response.ok(dbTable).build()).when(datasetResource).get(NAMESPACE_NAME, DB_TABLE_NAME);

    final Response response = datasetResource.tag(NAMESPACE_NAME, DB_TABLE_NAME, TAG_NAME);
    assertThat(response.getStatus()).isEqualTo(200);

    final Dataset dataset = (Dataset) response.getEntity();
    assertThat(dataset).isEqualTo(dbTable);
    for (final Field field : dataset.getFields()) {
      assertThat(field.getTags()).contains(TAG_NAME);
    }
  }

  private static DbTable toDbTable(final DatasetName dbTableName, final DbTableMeta dbTableMeta) {
    final Instant now = Instant.now();
    return new DbTable(
        dbTableName,
        dbTableMeta.getPhysicalName(),
        now,
        now,
        dbTableMeta.getSourceName(),
        dbTableMeta.getFields(),
        dbTableMeta.getTags(),
        null,
        dbTableMeta.getDescription().orElse(null));
  }

  private static DbTable tagDatasetWith(final TagName tagName, final DbTable dbTable) {
    final ImmutableSet<TagName> tags =
        ImmutableSet.<TagName>builder().addAll(dbTable.getTags()).add(tagName).build();
    return new DbTable(
        dbTable.getName(),
        dbTable.getPhysicalName(),
        dbTable.getCreatedAt(),
        dbTable.getUpdatedAt(),
        dbTable.getSourceName(),
        dbTable.getFields(),
        tags,
        dbTable.getLastModifiedAt().orElse(null),
        dbTable.getDescription().orElse(null));
  }

  private static DbTable tagAllFieldsWith(final TagName tagName, final DbTable dbTable) {
    final ImmutableList.Builder<Field> fields = ImmutableList.builder();
    for (final Field field : dbTable.getFields()) {
      final ImmutableSet<TagName> tags =
          ImmutableSet.<TagName>builder().addAll(field.getTags()).add(tagName).build();
      fields.add(
          new Field(field.getName(), field.getType(), tags, field.getDescription().orElse(null)));
    }
    return new DbTable(
        dbTable.getName(),
        dbTable.getPhysicalName(),
        dbTable.getCreatedAt(),
        dbTable.getUpdatedAt(),
        dbTable.getSourceName(),
        fields.build(),
        dbTable.getTags(),
        dbTable.getLastModifiedAt().orElse(null),
        dbTable.getDescription().orElse(null));
  }
}