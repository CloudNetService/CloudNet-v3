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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter.defaults.service;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.service.ICloudService;
import eu.cloudnetservice.cloudnet.ext.report.paste.emitter.ReportDataEmitter;

public class ServiceInfoSnapshotEmitter implements ReportDataEmitter<ICloudService> {

  @Override
  public void emitData(StringBuilder builder, ICloudService service) {
    builder
      .append(" - ServiceInfoSnapshot ").append(service.getServiceId().name()).append(" - \n")
      .append(JsonDocument.newDocument(service.serviceInfo()).toPrettyJson()).append("\n")
      .append(" - ServiceInfoSnapshot END - \n\n");
  }
}
