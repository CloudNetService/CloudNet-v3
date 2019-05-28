package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ModuleConfiguration {

  protected boolean runtimeModule;

  protected String
      group,
      name,
      version,
      main,
      description,
      author,
      website;

  //protected ModuleUpdateServiceConfiguration updateServiceConfiguration;

  protected ModuleRepository[] repos;

  protected ModuleDependency[] dependencies;

  protected JsonDocument properties;

  public String getMainClass() {
    return this.main;
  }

}