package de.dytanic.cloudnet.ext.storage.ftp.storage;

import com.jcraft.jsch.ChannelSftp;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.ext.storage.ftp.client.FTPCredentials;
import de.dytanic.cloudnet.ext.storage.ftp.client.SFTPClient;
import de.dytanic.cloudnet.template.ITemplateStorage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

// todo: add queue
public class SFTPTemplateStorage implements ITemplateStorage {

    private final String name;
    private final SFTPClient ftpClient;
    private final String baseDirectory;

    public SFTPTemplateStorage(String name, FTPCredentials credentials) {
        this.ftpClient = new SFTPClient();
        this.name = name;

        String baseDirectory = credentials.getBaseDirectory();

        this.baseDirectory = baseDirectory.endsWith("/") ? baseDirectory.substring(0, baseDirectory.length() - 1) : baseDirectory;

        this.connect(credentials);
    }

    private void connect(FTPCredentials credentials) {
        this.connect(credentials.getAddress().getHost(), credentials.getAddress().getPort(), credentials.getUsername(), credentials.getPassword());
    }

    private void connect(String host, int port, String username, String password) {
        if (this.ftpClient.isConnected()) {
            this.ftpClient.close();
        }

        this.ftpClient.connect(host, port, username, password);
    }

    @Override
    public boolean deploy(byte[] zipInput, ServiceTemplate target) {
        return this.ftpClient.uploadDirectory(new ByteArrayInputStream(zipInput), this.getPath(target));
    }

    @Override
    public boolean deploy(File directory, ServiceTemplate target, Predicate<File> fileFilter) {
        return this.ftpClient.uploadDirectory(directory.toPath(), this.getPath(target), path -> fileFilter.test(path.toFile()));
    }

    @Override
    public boolean deploy(Path[] paths, ServiceTemplate target) {
        for (Path path : paths) {
            if (!this.ftpClient.uploadFile(path, this.getPath(target) + "/" + path)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deploy(File[] files, ServiceTemplate target) {
        return this.deploy(Arrays.stream(files).map(File::toPath).toArray(Path[]::new), target);
    }

    @Override
    public boolean copy(ServiceTemplate template, File directory) {
        directory.mkdirs();
        return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
    }

    @Override
    public boolean copy(ServiceTemplate template, Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return this.ftpClient.downloadDirectory(this.getPath(template), directory.toString());
    }

    @Override
    public boolean copy(ServiceTemplate template, File[] directories) {
        Path tempDirectory = Paths.get(System.getProperty("cloudnet.tempDir.ftpCache", "temp/ftpCache"));
        try {
            Files.createDirectories(tempDirectory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        if (!this.ftpClient.downloadDirectory(this.getPath(template), tempDirectory.toString())) {
            FileUtils.delete(tempDirectory.toFile());
            return false;
        }
        for (File directory : directories) {
            try {
                FileUtils.copyFilesToDirectory(tempDirectory.toFile(), directory);
            } catch (IOException exception) {
                exception.printStackTrace();
                FileUtils.delete(tempDirectory.toFile());
                return false;
            }
        }
        FileUtils.delete(tempDirectory.toFile());
        return true;
    }

    @Override
    public boolean copy(ServiceTemplate template, Path[] directories) {
        return this.copy(template, Arrays.stream(directories).map(Path::toFile).toArray(File[]::new));
    }

    @Override
    public byte[] toZipByteArray(ServiceTemplate template) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        this.ftpClient.zipDirectory(this.getPath(template), outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public boolean delete(ServiceTemplate template) {
        return this.ftpClient.deleteDirectory(this.getPath(template));
    }

    @Override
    public boolean create(ServiceTemplate template) {
        this.ftpClient.createDirectories(this.getPath(template));
        return true;
    }

    @Override
    public boolean has(ServiceTemplate template) {
        return this.ftpClient.existsDirectory(this.getPath(template));
    }

    @Override
    public OutputStream appendOutputStream(ServiceTemplate template, String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Override
    public OutputStream newOutputStream(ServiceTemplate template, String path) {
        return this.ftpClient.openOutputStream(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean createFile(ServiceTemplate template, String path) {
        return this.ftpClient.createFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean createDirectory(ServiceTemplate template, String path) {
        this.ftpClient.createDirectories(this.getPath(template) + "/" + path);
        return true;
    }

    @Override
    public boolean hasFile(ServiceTemplate template, String path) {
        return this.ftpClient.existsFile(this.getPath(template) + "/" + path);
    }

    @Override
    public boolean deleteFile(ServiceTemplate template, String path) {
        return this.ftpClient.deleteFile(this.getPath(template) + "/" + path);
    }

    @Override
    public String[] listFiles(ServiceTemplate template, String dir) {
        List<String> files = new ArrayList<>();
        //todo this method is called by ServiceVersionProvider.installServiceVersion with non-existing directories
        Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(this.getPath(template) + "/" + dir);
        if (entries != null) {
            for (ChannelSftp.LsEntry listFile : entries) {
                if (listFile.getAttrs().isDir()) {
                    files.addAll(Arrays.asList(this.listFiles(template, listFile.getLongname())));
                }
            }
        }
        return files.toArray(new String[0]);
    }

    @Override
    public Collection<ServiceTemplate> getTemplates() {
        Collection<ChannelSftp.LsEntry> entries = this.ftpClient.listFiles(this.baseDirectory);
        if (entries == null)
            return Collections.emptyList();

        Collection<ServiceTemplate> templates = new ArrayList<>(entries.size());

        for (ChannelSftp.LsEntry entry : entries) {
            String prefix = entry.getFilename();

            Collection<ChannelSftp.LsEntry> prefixEntries = this.ftpClient.listFiles(this.baseDirectory + "/" + prefix);
            if (prefixEntries != null) {
                for (ChannelSftp.LsEntry nameEntry : prefixEntries) {
                    String name = nameEntry.getFilename();

                    templates.add(new ServiceTemplate(prefix, name, getName()));
                }
            }
        }

        return templates;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void close() {
        if (this.ftpClient != null) {
            this.ftpClient.close();
        }
    }

    private String getPath(ServiceTemplate template) {
        return this.baseDirectory + "/" + template.getPrefix() + "/" + template.getName();
    }

}
