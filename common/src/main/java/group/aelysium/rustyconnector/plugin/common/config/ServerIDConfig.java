package group.aelysium.rustyconnector.plugin.common.config;

import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Namespace("rustyconnector")
@Config("/metadata/server.id")
public class ServerIDConfig {
    @AllContents()
    private byte[] id;
    
    public String id() {
        String envId = System.getenv("RUSTYCONNECTOR_SERVERID");
        if (envId != null && !envId.isEmpty()) return envId;

        return new String(id, StandardCharsets.UTF_8);
    }

    public static ServerIDConfig Load(String id) throws IOException {
        // This logic only cares about generating the config if it doesn't exist.
        File file = new File(DeclarativeYAML.basePath("rustyconnector") + "/metadata/server.id");
        try {
            if(!file.exists()) {
                File parent = file.getParentFile();
                if (!parent.exists()) parent.mkdirs();

                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(id);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return DeclarativeYAML.From(ServerIDConfig.class);
    }
    public static ServerIDConfig Read() throws IOException {
        File file = new File(DeclarativeYAML.basePath("rustyconnector") + "/metadata/server.id");
        if (!file.exists()) return null;
        return DeclarativeYAML.From(ServerIDConfig.class);
    }
}
