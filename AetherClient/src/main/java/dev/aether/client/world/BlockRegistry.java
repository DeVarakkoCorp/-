package dev.aether.client.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BlockRegistry {
    private final Map<Integer, BlockDefinition> definitions;

    private BlockRegistry(Map<Integer, BlockDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public BlockDefinition get(int id) {
        BlockDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Unregistered block id: " + id);
        return definition;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<Integer, BlockDefinition> definitions = new HashMap<>();
        private boolean built;

        private Builder() {
            definitions.put(BlockDefinition.AIR_ID, BlockDefinition.AIR);
        }

        public Builder register(BlockDefinition definition) {
            if (built) throw new IllegalStateException("Block registry builder is already used");
            Objects.requireNonNull(definition, "definition");
            if (definitions.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate block id: " + definition.id());
            }
            return this;
        }

        public BlockRegistry build() {
            if (built) throw new IllegalStateException("Block registry builder is already used");
            built = true;
            return new BlockRegistry(definitions);
        }
    }
}
