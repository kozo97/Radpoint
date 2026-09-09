package pl.pawelwieczorek.radpoint.application;

import java.util.List;

import pl.pawelwieczorek.radpoint.domain.SampleData;

public final class SampleDataService {

    private final SampleDataRepository repository;

    public SampleDataService(SampleDataRepository repository) {
        this.repository = repository;
    }

    public List<SampleData> findActive(String tenantId) {
        return repository.findActive(tenantId);
    }

    public SampleData add(String tenantId, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Value must not be blank.");
        }

        String normalizedValue = value.trim();

        if (normalizedValue.length() > 500) {
            throw new IllegalArgumentException(
                    "Value must not be longer than 500 characters."
            );
        }

        return repository.save(tenantId, normalizedValue);
    }

    public void delete(String tenantId, long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("ID must be greater than zero.");
        }

        boolean deleted = repository.softDelete(tenantId, id);

        if (!deleted) {
            throw new IllegalArgumentException(
                    "Active record not found for ID: " + id
            );
        }
    }
}
