package io.github.joaodallagnol.backend.storage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class StorageScopeConverter implements AttributeConverter<StorageScope, String> {

    @Override
    public String convertToDatabaseColumn(StorageScope attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public StorageScope convertToEntityAttribute(String dbData) {
        return dbData == null ? null : StorageScope.fromValue(dbData);
    }
}
