/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.ext.rest.v2;

import com.google.common.io.ByteStreams;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.driver.network.http.IHttpResponse;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.SpecificTemplateStorage;
import de.dytanic.cloudnet.ext.rest.RestUtils;
import de.dytanic.cloudnet.http.HttpSession;
import de.dytanic.cloudnet.http.V2HttpHandler;
import de.dytanic.cloudnet.template.install.InstallInformation;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class V2HttpHandlerTemplate extends V2HttpHandler {

  public V2HttpHandlerTemplate(String requiredPermission) {
    super(requiredPermission, "GET", "POST", "DELETE");
  }

  @Override
  protected void handleBearerAuthorized(String path, IHttpContext context, HttpSession session) {
    if (context.request().method().equalsIgnoreCase("GET")) {
      if (path.contains("/file/")) {
        if (path.contains("/download")) {
          this.handleFileDownloadRequest(context);
        } else if (path.contains("/info")) {
          this.handleFileInfoRequest(context);
        } else if (path.contains("/exists")) {
          this.handleFileExistsRequest(context);
        }
      } else if (path.endsWith("/download")) {
        this.handleDownloadRequest(context);
      } else if (path.contains("/directory/")) {
        if (path.contains("/list")) {
          this.handleFileListRequest(context);
        }
      } else if (path.endsWith("/create")) {
        this.handleCreateRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("POST")) {
      if (path.endsWith("/deploy")) {
        this.handleDeployRequest(context);
      } else if (path.contains("/file/")) {
        if (path.contains("/create")) {
          this.handleFileWriteRequest(context, false);
        } else if (path.contains("/append")) {
          this.handleFileWriteRequest(context, true);
        }
      } else if (path.contains("/directory/")) {
        if (path.contains("/create")) {
          this.handleDirectoryCreateRequest(context);
        }
      } else if (path.endsWith("/install")) {
        this.handleInstallationRequest(context);
      }
    } else if (context.request().method().equalsIgnoreCase("DELETE")) {
      if (path.contains("/file") || path.contains("/directory")) {
        this.handleFileDeleteRequest(context);
      } else {
        this.handleTemplateDeleteRequest(context);
      }
    }
  }

  protected void handleDownloadRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var stream = storage.zipTemplateAsync().get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to zip template").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context, "application/zip; charset=UTF-8")
          .body(stream)
          .header("Content-Disposition", "attachment; filename="
            + template.toString().replace('/', '_') + ".zip")
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileDownloadRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var stream = storage.newInputStreamAsync(path).get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Missing file or path is directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        var fileName = this.guessFileName(path);
        this.ok(context, "application/octet-stream")
          .header("Content-Disposition",
            String.format("attachment%s", fileName == null ? "" : "; filename=" + fileName))
          .body(stream)
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileInfoRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var info = storage.fileInfoAsync(path).get();
      if (info == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unknown file or directory").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        this.ok(context)
          .body(this.success().append("info", info).toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      }
    });
  }

  protected void handleFileExistsRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.hasFileAsync(path).get();
      this.ok(context)
        .body(this.success().append("exists", status).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileListRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var dir = RestUtils.getFirst(context.request().queryParameters().get("directory"), "");
      var deep = Boolean.parseBoolean(RestUtils.getFirst(context.request().queryParameters().get("deep"), "false"));

      var files = storage.listFilesAsync(dir, deep).get();
      this.ok(context)
        .body(this.success().append("files", files).toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleCreateRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.createAsync().fireExceptionOnFailure().get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleDeployRequest(IHttpContext context) {
    var stream = context.request().bodyStream();
    if (stream == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing data in body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deployAsync(stream).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleFileDeleteRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.deleteFileAsync(path).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleTemplateDeleteRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      boolean status = storage.deleteAsync().get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleInstallationRequest(IHttpContext context) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var body = this.body(context.request());

      var versionType = body.get("type", ServiceVersionType.class);
      if (versionType == null) {
        versionType = this.getCloudNet().getServiceVersionProvider()
          .getServiceVersionType(body.getString("typeName", "")).orElse(null);
        if (versionType == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "No service type or type name provided").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      var version = body.get("version", ServiceVersion.class);
      if (version == null) {
        version = versionType.getVersion(body.getString("versionName", "")).orElse(null);
        if (version == null) {
          this.badRequest(context)
            .body(this.failure().append("reason", "Missing version or version name").toString())
            .context()
            .closeAfter(true)
            .cancelNext();
          return;
        }
      }

      var forceInstall = body.getBoolean("force", false);
      var cacheFiles = body.getBoolean("caches", version.isCacheFiles());

      var installInformation = InstallInformation.builder()
        .serviceVersion(version)
        .serviceVersionType(versionType)
        .cacheFiles(cacheFiles)
        .toTemplate(template)
        .build();

      if (this.getCloudNet().getServiceVersionProvider()
        .installServiceVersion(installInformation, forceInstall)) {
        this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
      } else {
        this.ok(context).body(this.failure().toString()).context().closeAfter(true).cancelNext();
      }
    });
  }

  protected void handleFileWriteRequest(IHttpContext context, boolean append) {
    var content = context.request().bodyStream();
    if (content == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing input from body").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      var task = append ? storage.appendOutputStreamAsync(path) : storage.newOutputStreamAsync(path);
      var stream = task.get();
      if (stream == null) {
        this.notFound(context)
          .body(this.failure().append("reason", "Unable to open file stream").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
      } else {
        try {
          ByteStreams.copy(content, stream);
          this.ok(context).body(this.success().toString()).context().closeAfter(true).cancelNext();
        } catch (IOException exception) {
          this.notifyException(context, exception);
        }
      }
    });
  }

  protected void handleDirectoryCreateRequest(IHttpContext context) {
    this.handleWithFileTemplateContext(context, (template, storage, path) -> {
      boolean status = storage.createDirectoryAsync(path).get();
      this.ok(context)
        .body(status ? this.success().toString() : this.failure().toString())
        .context()
        .closeAfter(true)
        .cancelNext();
    });
  }

  protected void handleWithTemplateContext(IHttpContext context,
    ThrowableBiConsumer<ServiceTemplate, SpecificTemplateStorage, Exception> handler) {
    var storage = context.request().pathParameters().get("storage");
    var prefix = context.request().pathParameters().get("prefix");
    var name = context.request().pathParameters().get("name");

    if (storage == null || prefix == null || name == null) {
      this.badRequest(context)
        .body(this.failure().append("reason", "Missing storage, prefix or name in path parameters").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    var template = ServiceTemplate.builder().prefix(prefix).name(name).storage(storage).build();
    var templateStorage = template.knownStorage();

    if (templateStorage == null) {
      this.ok(context)
        .body(this.failure().append("reason", "Unknown template storage").toString())
        .context()
        .closeAfter(true)
        .cancelNext();
      return;
    }

    try {
      handler.accept(template, templateStorage);
    } catch (Exception exception) {
      this.notifyException(context, exception);
    }
  }

  protected void handleWithFileTemplateContext(IHttpContext context,
    ThrowableTriConsumer<ServiceTemplate, SpecificTemplateStorage, String, Exception> handler) {
    this.handleWithTemplateContext(context, (template, storage) -> {
      var fileName = RestUtils.getFirst(context.request().queryParameters().get("path"), null);
      if (fileName == null) {
        this.badRequest(context)
          .body(this.failure().append("reason", "Missing file name in path").toString())
          .context()
          .closeAfter(true)
          .cancelNext();
        return;
      }

      handler.accept(template, storage, fileName);
    });
  }

  protected void notifyException(IHttpContext context, Exception exception) {
    LOGGER.fine("Exception handling template request", exception);
    this.response(context, HttpResponseCode.HTTP_INTERNAL_ERROR)
      .body(this.failure().append("reason", "Exception processing request").toString())
      .context()
      .closeAfter(true)
      .cancelNext();
  }

  protected IHttpResponse ok(@NotNull IHttpContext context, @NotNull String contentType) {
    return context.response()
      .statusCode(HttpResponseCode.HTTP_OK)
      .header("Content-Type", contentType)
      .header("Access-Control-Allow-Origin", this.accessControlConfiguration.getCorsPolicy());
  }

  protected @Nullable String guessFileName(String path) {
    var index = path.lastIndexOf('/');
    if (index == -1 || index + 1 == path.length()) {
      return null;
    } else {
      return path.substring(index);
    }
  }

  @FunctionalInterface
  protected interface ThrowableBiConsumer<T, U, E extends Throwable> {

    void accept(T t, U u) throws E;
  }

  @FunctionalInterface
  protected interface ThrowableTriConsumer<T, U, F, E extends Throwable> {

    void accept(T t, U u, F f) throws E;
  }
}
