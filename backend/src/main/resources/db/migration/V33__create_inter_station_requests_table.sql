-- Flyway Migration V33: Create inter_station_requests table for inter-station coordination

CREATE TABLE IF NOT EXISTS inter_station_requests (
    id UUID PRIMARY KEY,
    case_id UUID NOT NULL REFERENCES cases(id) ON DELETE CASCADE,
    victim_id UUID REFERENCES victims(id) ON DELETE SET NULL,
    source_station_id UUID NOT NULL REFERENCES police_stations(id),
    destination_station_id UUID NOT NULL REFERENCES police_stations(id),
    requested_by_officer_id UUID NOT NULL REFERENCES police_officers(id),
    reason TEXT NOT NULL,
    share_case_reference BOOLEAN DEFAULT TRUE,
    share_victim_details BOOLEAN DEFAULT TRUE,
    share_incident_summary BOOLEAN DEFAULT TRUE,
    share_evidence BOOLEAN DEFAULT FALSE,
    share_witness_info BOOLEAN DEFAULT FALSE,
    shared_data_snapshot TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    email_delivery_status VARCHAR(50) DEFAULT 'PENDING',
    email_delivery_error TEXT,
    provider_message_id VARCHAR(255),
    response_notes TEXT,
    responded_by_officer_id UUID REFERENCES police_officers(id),
    responded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inter_station_case_id ON inter_station_requests(case_id);
CREATE INDEX IF NOT EXISTS idx_inter_station_victim_id ON inter_station_requests(victim_id);
CREATE INDEX IF NOT EXISTS idx_inter_station_source_station ON inter_station_requests(source_station_id);
CREATE INDEX IF NOT EXISTS idx_inter_station_dest_station ON inter_station_requests(destination_station_id);
CREATE INDEX IF NOT EXISTS idx_inter_station_status ON inter_station_requests(status);
CREATE INDEX IF NOT EXISTS idx_inter_station_created_at ON inter_station_requests(created_at);
