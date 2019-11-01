package de.dytanic.cloudnet.command.commands;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.command.ICommandSender;
import de.dytanic.cloudnet.common.Properties;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.template.ITemplateStorage;
import de.dytanic.cloudnet.template.TemplateStorageUtil;
import de.dytanic.cloudnet.template.install.ServiceVersion;
import de.dytanic.cloudnet.template.install.ServiceVersionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class CommandTemplate extends CommandDefault {

    public CommandTemplate() {
        super("template", "t");
    }

    @Override
    public void execute(ICommandSender sender, String command, String[] args, String commandLine, Properties properties) {
        if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("list")) {
            String storageName = args.length == 2 ? args[1] : "local";
            ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, storageName);
            if (storage == null) {
                sender.sendMessage(LanguageManager.getMessage("command-template-storage-not-found").replace("%storage%", storageName));
                return;
            }
            List<String> messages = new ArrayList<>();
            messages.add(LanguageManager.getMessage("command-template-list-templates").replace("%storage%", storageName));
            for (ServiceTemplate template : storage.getTemplates()) {
                messages.add("  " + template.getTemplatePath());
            }
            sender.sendMessage(messages.toArray(new String[0]));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("versions")) {
            List<String> messages = new ArrayList<>();
            messages.add(LanguageManager.getMessage("command-template-list-versions"));
            for (ServiceVersionType versionType : CloudNet.getInstance().getServiceVersionProvider().getServiceVersionTypes().values()) {
                messages.add("  " + versionType.getName() + ":");
                for (ServiceVersion version : versionType.getVersions()) {
                    messages.add("    " + version.getName());
                }
            }
            sender.sendMessage(messages.toArray(new String[0]));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("install")) {
            this.handleWithTemplateAndStorage(sender, args[1], (template, storage) -> {
                Optional<ServiceVersionType> optionalVersionType = CloudNet.getInstance().getServiceVersionProvider().getServiceVersionType(args[2]);
                if (!optionalVersionType.isPresent()) {
                    sender.sendMessage(LanguageManager.getMessage("command-template-invalid-version-type").replace("%versionType%", args[2]));
                    return;
                }
                ServiceVersionType versionType = optionalVersionType.get();
                Optional<ServiceVersion> optionalVersion = versionType.getVersion(args[3]);
                if (!optionalVersion.isPresent()) {
                    sender.sendMessage(LanguageManager.getMessage("command-template-invalid-version").replace("%versionType%", args[2]).replace("%version%", args[3]));
                    return;
                }
                ServiceVersion version = optionalVersion.get();

                sender.sendMessage(LanguageManager.getMessage("command-template-install-try")
                        .replace("%version%", versionType.getName() + "-" + version.getName())
                        .replace("%template%", template.toString())
                );
                CloudNet.getInstance().getServiceVersionProvider().installServiceVersion(versionType, version, storage, template);
                sender.sendMessage(LanguageManager.getMessage("command-template-install-success")
                        .replace("%version%", versionType.getName() + "-" + version.getName())
                        .replace("%template%", template.toString())
                );
            });

        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            this.handleWithTemplateAndStorage(sender, args[1], (template, storage) -> {
                if (!storage.has(template)) {
                    sender.sendMessage(LanguageManager.getMessage("command-template-delete-template-not-found")
                            .replace("%template%", template.getTemplatePath())
                            .replace("%storage%", template.getStorage())
                    );
                    return;
                }
                storage.delete(template);
                sender.sendMessage(LanguageManager.getMessage("command-template-delete-success")
                        .replace("%template%", template.getTemplatePath())
                        .replace("%storage%", template.getStorage())
                );
            });
        } else if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            this.handleWithTemplateAndStorage(sender, args[1], (template, storage) -> {
                if (storage.has(template)) {
                    sender.sendMessage(LanguageManager.getMessage("command-template-create-template-already-exists")
                            .replace("%template%", template.getTemplatePath())
                            .replace("%storage%", template.getStorage())
                    );
                    return;
                }
                ServiceEnvironmentType environment;
                try {
                    environment = ServiceEnvironmentType.valueOf(args[2].toUpperCase());
                } catch (Exception exception) {
                    sender.sendMessage(LanguageManager.getMessage("command-template-environment-not-found")
                            .replace("%env%", args[2])
                            .replace("%availableEnvs%", Arrays.toString(ServiceEnvironmentType.values()))
                    );
                    return;
                }
                try {
                    if (TemplateStorageUtil.createAndPrepareTemplate(storage, template.getPrefix(), template.getName(), environment)) {
                        sender.sendMessage(LanguageManager.getMessage("command-template-create-success")
                                .replace("%template%", template.getTemplatePath())
                                .replace("%storage%", template.getStorage())
                        );
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    sender.sendMessage(LanguageManager.getMessage("command-template-create-failed")
                            .replace("%template%", template.getTemplatePath())
                            .replace("%storage%", template.getStorage())
                    );
                }
            });
        } else {
            sender.sendMessage(
                    "template install <[storage:]prefix/name> <versionType> <version> | example: template install Lobby/default paperspigot 1.13.2",
                    "template versions",
                    "template list [storage]",
                    "template delete <[storage:]prefix/name>",
                    "template create <[storage:]prefix/name> <" + Arrays.toString(ServiceEnvironmentType.values()) + ">"
            );
        }
    }

    private void handleWithTemplateAndStorage(ICommandSender sender, String templateString, BiConsumer<ServiceTemplate, ITemplateStorage> consumer) {
        ServiceTemplate template = ServiceTemplate.parse(templateString);
        if (template == null) {
            sender.sendMessage(LanguageManager.getMessage("command-template-invalid-template"));
            return;
        }
        ITemplateStorage storage = CloudNet.getInstance().getServicesRegistry().getService(ITemplateStorage.class, template.getStorage());
        if (storage == null) {
            sender.sendMessage(LanguageManager.getMessage("command-template-storage-not-found").replace("%storage%", template.getStorage()));
            return;
        }

        consumer.accept(template, storage);
    }
}
