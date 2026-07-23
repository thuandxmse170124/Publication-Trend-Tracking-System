package com.publication_trend_tracking_system.sever_web_app.entity;

import jakarta.persistence.*;
import lombok.*;

// Single-row table (id is always 1) holding the global on/off switch for the weekly background
// sync scheduler. Deliberately separate from ApiSource.status, which already gates both manual
// and scheduled sync per-source (see SyncServiceImpl) — reusing it here would also disable
// admin-triggered "Sync (Query)"/"Sync All", not just the cron job.
@Entity
@Table(name = "scheduler_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchedulerSettings {

    @Id
    private Integer id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;
}
