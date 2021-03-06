/*
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

package marquez.api.mappers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.emptyList;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.NonNull;
import marquez.api.models.DatasetRequest;
import marquez.api.models.DatasetResponse;
import marquez.api.models.DatasetsResponse;
import marquez.api.models.DbTableRequest;
import marquez.api.models.DbTableResponse;
import marquez.api.models.JobRequest;
import marquez.api.models.JobResponse;
import marquez.api.models.JobsResponse;
import marquez.api.models.NamespaceRequest;
import marquez.api.models.NamespaceResponse;
import marquez.api.models.NamespacesResponse;
import marquez.api.models.RunRequest;
import marquez.api.models.RunResponse;
import marquez.api.models.RunsResponse;
import marquez.api.models.SourceRequest;
import marquez.api.models.SourceResponse;
import marquez.api.models.SourcesResponse;
import marquez.api.models.StreamRequest;
import marquez.api.models.StreamResponse;
import marquez.api.models.TagResponse;
import marquez.api.models.TagsResponse;
import marquez.common.Utils;
import marquez.common.models.DatasetName;
import marquez.common.models.JobType;
import marquez.common.models.NamespaceName;
import marquez.common.models.OwnerName;
import marquez.common.models.SourceName;
import marquez.common.models.SourceType;
import marquez.service.models.Dataset;
import marquez.service.models.DatasetId;
import marquez.service.models.DatasetMeta;
import marquez.service.models.DbTable;
import marquez.service.models.DbTableMeta;
import marquez.service.models.Job;
import marquez.service.models.JobMeta;
import marquez.service.models.Namespace;
import marquez.service.models.NamespaceMeta;
import marquez.service.models.Run;
import marquez.service.models.RunMeta;
import marquez.service.models.Source;
import marquez.service.models.SourceMeta;
import marquez.service.models.Stream;
import marquez.service.models.StreamMeta;
import marquez.service.models.Tag;

public final class Mapper {
  private Mapper() {}

  public static NamespaceMeta toNamespaceMeta(@NonNull final NamespaceRequest request) {
    return new NamespaceMeta(
        OwnerName.of(request.getOwnerName()), request.getDescription().orElse(null));
  }

  public static NamespaceResponse toNamespaceResponse(@NonNull final Namespace namespace) {
    return new NamespaceResponse(
        namespace.getName().getValue(),
        ISO_INSTANT.format(namespace.getCreatedAt()),
        ISO_INSTANT.format(namespace.getUpdatedAt()),
        namespace.getOwnerName().getValue(),
        namespace.getDescription().orElse(null));
  }

  public static List<NamespaceResponse> toNamespaceResponses(
      @NonNull final List<Namespace> namespaces) {
    return namespaces.stream().map(Mapper::toNamespaceResponse).collect(toImmutableList());
  }

  public static NamespacesResponse toNamespacesResponse(@NonNull final List<Namespace> namespaces) {
    return new NamespacesResponse(toNamespaceResponses(namespaces));
  }

  public static SourceMeta toSourceMeta(@NonNull final SourceRequest request) {
    return new SourceMeta(
        SourceType.valueOf(request.getType()),
        URI.create(request.getConnectionUrl()),
        request.getDescription().orElse(null));
  }

  public static SourceResponse toSourceResponse(@NonNull final Source source) {
    return new SourceResponse(
        source.getType().toString(),
        source.getName().getValue(),
        ISO_INSTANT.format(source.getCreatedAt()),
        ISO_INSTANT.format(source.getUpdatedAt()),
        source.getConnectionUrl().toASCIIString(),
        source.getDescription().orElse(null));
  }

  public static List<SourceResponse> toSourceResponses(@NonNull final List<Source> sources) {
    return sources.stream().map(Mapper::toSourceResponse).collect(toImmutableList());
  }

  public static SourcesResponse toSourcesResponse(@NonNull final List<Source> sources) {
    return new SourcesResponse(toSourceResponses(sources));
  }

  public static DatasetMeta toDatasetMeta(@NonNull final DatasetRequest request) {
    if (request instanceof DbTableRequest) {
      return toDbTableMeta(request);
    } else if (request instanceof StreamRequest) {
      return toStreamMeta(request);
    }
    throw new IllegalArgumentException();
  }

  public static DatasetMeta toDbTableMeta(@NonNull final DatasetRequest request) {
    return new DbTableMeta(
        DatasetName.of(request.getPhysicalName()),
        SourceName.of(request.getSourceName()),
        request.getFields(),
        request.getTags(),
        request.getDescription().orElse(null),
        request.getRunId().map(UUID::fromString).orElse(null));
  }

  public static DatasetMeta toStreamMeta(@NonNull final DatasetRequest request) {
    return new StreamMeta(
        DatasetName.of(request.getPhysicalName()),
        SourceName.of(request.getSourceName()),
        Utils.toUrl(((StreamRequest) request).getSchemaLocation()),
        request.getFields(),
        request.getTags(),
        request.getDescription().orElse(null),
        request.getRunId().map(UUID::fromString).orElse(null));
  }

  public static DatasetResponse toDatasetResponse(@NonNull final Dataset dataset) {
    if (dataset instanceof DbTable) {
      return toDbTableResponse(dataset);
    } else if (dataset instanceof Stream) {
      return toStreamResponse(dataset);
    }
    throw new IllegalArgumentException();
  }

  private static DatasetResponse toDbTableResponse(@NonNull final Dataset dataset) {
    return new DbTableResponse(
        dataset.getId(),
        dataset.getId().getNamespace(),
        dataset.getName(),
        dataset.getPhysicalName().getValue(),
        ISO_INSTANT.format(dataset.getCreatedAt()),
        ISO_INSTANT.format(dataset.getUpdatedAt()),
        dataset.getSourceName().getValue(),
        dataset.getFields(),
        dataset.getTags(),
        dataset.getLastModifiedAt().map(ISO_INSTANT::format).orElse(null),
        dataset.getDescription().orElse(null));
  }

  private static DatasetResponse toStreamResponse(@NonNull final Dataset dataset) {
    return new StreamResponse(
        dataset.getId(),
        dataset.getId().getNamespace(),
        dataset.getName(),
        dataset.getPhysicalName().getValue(),
        ISO_INSTANT.format(dataset.getCreatedAt()),
        ISO_INSTANT.format(dataset.getUpdatedAt()),
        dataset.getSourceName().getValue(),
        ((Stream) dataset).getSchemaLocation().toString(),
        dataset.getFields(),
        dataset.getTags(),
        dataset.getLastModifiedAt().map(ISO_INSTANT::format).orElse(null),
        dataset.getDescription().orElse(null));
  }

  public static List<DatasetResponse> toDatasetResponses(@NonNull final List<Dataset> datasets) {
    return datasets.stream().map(Mapper::toDatasetResponse).collect(toImmutableList());
  }

  public static DatasetsResponse toDatasetsResponse(@NonNull final List<Dataset> datasets) {
    return new DatasetsResponse(toDatasetResponses(datasets));
  }

  private static List<DatasetId> toIds(NamespaceName namespace, Collection<String> names) {
    return names == null
        ? null
        : names.stream()
            .map((n) -> new DatasetId(namespace, DatasetName.of(n)))
            .collect(toImmutableList());
  }

  public static JobMeta toJobMeta(NamespaceName namespace, @NonNull final JobRequest request) {
    List<DatasetId> inputs = null;
    List<DatasetId> outputs = null;
    if (request.getInputIds() != null || request.getOutputIds() != null) {
      // new style inputs/outputs with namespace
      inputs = request.getInputIds();
      outputs = request.getOutputIds();
    } else if (request.getInputs() != null || request.getOutputs() != null) {
      // old style inputs/outputs, defaults to job namespace
      inputs = toIds(namespace, request.getInputs());
      outputs = toIds(namespace, request.getOutputs());
    }
    if (inputs == null) {
      inputs = emptyList();
    }
    if (outputs == null) {
      outputs = emptyList();
    }
    return new JobMeta(
        JobType.valueOf(request.getType()),
        inputs,
        outputs,
        request.getLocation().map(Utils::toUrl).orElse(null),
        request.getContext(),
        request.getDescription().orElse(null));
  }

  public static JobResponse toJobResponse(@NonNull final Job job) {
    return new JobResponse(
        job.getId(),
        job.getId().getNamespace(),
        job.getType().toString(),
        job.getName(),
        ISO_INSTANT.format(job.getCreatedAt()),
        ISO_INSTANT.format(job.getUpdatedAt()),
        job.getInputs(),
        job.getOutputs(),
        job.getLocation().map(URL::toString).orElse(null),
        job.getContext(),
        job.getDescription().orElse(null),
        job.getLatestRun().map(Mapper::toRunResponse).orElse(null));
  }

  public static List<JobResponse> toJobResponses(@NonNull final List<Job> jobs) {
    return jobs.stream().map(Mapper::toJobResponse).collect(toImmutableList());
  }

  public static JobsResponse toJobsResponse(@NonNull final List<Job> jobs) {
    return new JobsResponse(toJobResponses(jobs));
  }

  public static RunMeta toRunMeta(@NonNull final RunRequest request) {
    return new RunMeta(
        request.getNominalStartTime().map(Instant::parse).orElse(null),
        request.getNominalEndTime().map(Instant::parse).orElse(null),
        request.getArgs());
  }

  public static RunResponse toRunResponse(@NonNull final Run run) {
    Optional<Long> duration =
        run.getEndedAt()
            .flatMap((e) -> run.getStartedAt().map((s) -> s.until(e, ChronoUnit.MILLIS)));
    return new RunResponse(
        run.getId().toString(),
        ISO_INSTANT.format(run.getCreatedAt()),
        ISO_INSTANT.format(run.getUpdatedAt()),
        run.getNominalStartTime().map(ISO_INSTANT::format).orElse(null),
        run.getNominalEndTime().map(ISO_INSTANT::format).orElse(null),
        run.getStartedAt().map(ISO_INSTANT::format).orElse(null),
        run.getEndedAt().map(ISO_INSTANT::format).orElse(null),
        duration.orElse(null),
        run.getState().toString(),
        run.getArgs());
  }

  public static List<RunResponse> toRunResponses(@NonNull final List<Run> runs) {
    return runs.stream().map(Mapper::toRunResponse).collect(toImmutableList());
  }

  public static RunsResponse toRunsResponse(@NonNull final List<Run> runs) {
    return new RunsResponse(toRunResponses(runs));
  }

  public static TagResponse toTagResponse(@NonNull final Tag tag) {
    return new TagResponse(tag.getName(), tag.getDescription().orElse(null));
  }

  public static List<TagResponse> toTagResponses(@NonNull final List<Tag> tags) {
    return tags.stream().map(Mapper::toTagResponse).collect(toImmutableList());
  }

  public static TagsResponse toTagsResponse(@NonNull final List<Tag> tags) {
    return new TagsResponse(toTagResponses(tags));
  }
}
