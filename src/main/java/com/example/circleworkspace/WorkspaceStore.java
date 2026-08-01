package com.example.circleworkspace;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.*;
import java.nio.file.*;
import static com.example.circleworkspace.Model.*;

public final class WorkspaceStore {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().enable(SerializationFeature.INDENT_OUTPUT).build();
    public WorkspaceData load(Path path) throws IOException { return mapper.readValue(path.toFile(), WorkspaceData.class); }
    public void save(Path path, WorkspaceData data) throws IOException { mapper.writeValue(path.toFile(), data); }
    public WorkspaceData loadResource(String name) throws IOException {
        try (InputStream in = WorkspaceStore.class.getResourceAsStream(name)) {
            if (in == null) throw new FileNotFoundException(name);
            return mapper.readValue(in, WorkspaceData.class);
        }
    }
}
