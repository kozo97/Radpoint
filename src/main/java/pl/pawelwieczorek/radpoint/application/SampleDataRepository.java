package pl.pawelwieczorek.radpoint.application;

import java.util.List;

import pl.pawelwieczorek.radpoint.domain.SampleData;

public interface SampleDataRepository {

    List<SampleData> findActive(String tenantId);

    SampleData save(String tenantId, String value);

    boolean softDelete(String tenantId, long id);
}
