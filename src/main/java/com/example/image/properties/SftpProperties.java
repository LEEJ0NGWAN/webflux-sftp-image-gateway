package com.example.image.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@ConfigurationProperties("sftp")
public class SftpProperties {

    private int minSessionCount;
    private int maxChannelCount;

    private String remoteDirectory;
    private String remoteUsername;
    private String remotePassword;
    private String remoteAddress;

    private String privateKey;
    private byte[] privateKeyBytes;

    public void setPrivateKey(String privateKey) {

        this.privateKey = privateKey;
        this.privateKeyBytes = privateKey.getBytes();
    }

    public void setRemoteDirectory(String directory) {

        this.remoteDirectory =
        directory.endsWith("/")? directory: directory.concat("/");
    }
}
