package ca.yorku.eecs3311.coursepilot.infrastructure.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds completion tracking to existing local H2 databases without deleting saved plans. */
@Component
public class StudyPlanSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public StudyPlanSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("ALTER TABLE study_plan_items ADD COLUMN IF NOT EXISTS completed BOOLEAN DEFAULT FALSE NOT NULL");
        jdbc.execute("ALTER TABLE study_plan_items ADD COLUMN IF NOT EXISTS actual_minutes INT DEFAULT 0 NOT NULL");
    }
}
