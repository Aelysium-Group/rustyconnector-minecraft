package group.aelysium.rustyconnector.plugin.common.config;

import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.errors.Error;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Namespace("rustyconnector")
@Config("/metadata/aes.private")
public class PrivateKeyConfig {
    @AllContents()
    private byte[] key;

    public AES cryptor() {
        String privateKey = System.getenv("RUSTYCONNECTOR_PRIVATEKEY");
        if(privateKey != null) return AES.from(Base64.getDecoder().decode(privateKey));

        return AES.from(Base64.getDecoder().decode(this.key));
    }

    public static PrivateKeyConfig New() throws IOException {
        // This logic only cares about generating the config if it doesn't exist.
        File file = new File(DeclarativeYAML.basePath("rustyconnector") + "/metadata/aes.private");
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                try(FileWriter writer = new FileWriter(file)) {
                    writer.write(new String(Base64.getEncoder().encode(AES.createKey()), StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
        return DeclarativeYAML.From(PrivateKeyConfig.class);
    }
    public static Optional<PrivateKeyConfig> Load() throws IOException {
        File file = new File(DeclarativeYAML.basePath("rustyconnector") + "/metadata/aes.private");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                return Optional.empty();
            }
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }

        return Optional.of(DeclarativeYAML.From(PrivateKeyConfig.class));
    }
}
