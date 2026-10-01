ALTER TABLE solar_sites
    ADD CONSTRAINT ck_solar_sites_capacity
        CHECK (capacity_kw > 0 AND capacity_kw < 'Infinity'::numeric);

ALTER TABLE batteries
    ADD CONSTRAINT ck_batteries_measurements
        CHECK (
            capacity_kwh > 0
            AND capacity_kwh < 'Infinity'::numeric
            AND current_charge_percent >= 0
            AND current_charge_percent <= 100
            AND current_stored_energy_kwh >= 0
            AND current_stored_energy_kwh < 'Infinity'::numeric
            AND current_stored_energy_kwh <= capacity_kwh
        );

ALTER TABLE energy_readings
    ADD CONSTRAINT ck_energy_readings_measurements
        CHECK (
            production_kwh >= 0 AND production_kwh < 'Infinity'::numeric
            AND consumption_kwh >= 0 AND consumption_kwh < 'Infinity'::numeric
            AND grid_import_kwh >= 0 AND grid_import_kwh < 'Infinity'::numeric
            AND grid_export_kwh >= 0 AND grid_export_kwh < 'Infinity'::numeric
        );

ALTER TABLE storage_readings
    ADD CONSTRAINT ck_storage_readings_measurements
        CHECK (
            charge_percent >= 0
            AND charge_percent <= 100
            AND stored_energy_kwh >= 0
            AND stored_energy_kwh < 'Infinity'::numeric
        );

ALTER TABLE system_settings
    ADD CONSTRAINT ck_system_settings_measurements
        CHECK (
            low_battery_threshold >= 0
            AND low_battery_threshold <= 100
            AND high_consumption_threshold_kwh >= 0
            AND high_consumption_threshold_kwh < 'Infinity'::numeric
            AND low_production_threshold_kwh >= 0
            AND low_production_threshold_kwh < 'Infinity'::numeric
            AND device_offline_threshold_minutes BETWEEN 1 AND 10080
        );
