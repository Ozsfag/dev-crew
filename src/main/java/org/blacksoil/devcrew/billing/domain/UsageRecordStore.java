package org.blacksoil.devcrew.billing.domain;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Port-интерфейс хранилища записей об использовании. */
public interface UsageRecordStore {

  UsageRecordModel save(UsageRecordModel record);

  List<UsageRecordModel> findByOrgIdAndMonth(UUID orgId, YearMonth month);
}
