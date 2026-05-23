create table if not exists house_check_request (
    id uuid primary key,
    owner_id varchar(120) not null,
    address_road varchar(255) not null,
    address_lot varchar(255),
    contract_type varchar(40) not null,
    housing_type varchar(40) not null,
    deposit_amount bigint not null check (deposit_amount >= 0),
    monthly_rent_amount bigint not null check (monthly_rent_amount >= 0),
    contract_planned_date date not null,
    occupancy_planned_date date not null,
    landlord_name varchar(255) not null,
    analysis_status varchar(30) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_house_check_request_owner_id on house_check_request (owner_id);
create index if not exists idx_house_check_request_created_at on house_check_request (created_at);

create table if not exists house_check_document (
    id uuid primary key,
    house_check_id uuid not null references house_check_request(id) on delete cascade,
    document_type varchar(40) not null,
    original_file_name varchar(255) not null,
    mime_type varchar(120) not null,
    file_size bigint not null check (file_size >= 0),
    storage_key varchar(255) not null,
    issued_date date,
    uploaded_at timestamptz not null,
    constraint uk_house_check_document_type unique (house_check_id, document_type),
    constraint uk_house_check_document_storage_key unique (storage_key)
);

create index if not exists idx_house_check_document_house_check_id on house_check_document (house_check_id);
create index if not exists idx_house_check_document_uploaded_at on house_check_document (uploaded_at);

create table if not exists registry_manual_finding (
    id uuid primary key,
    house_check_id uuid not null unique references house_check_request(id) on delete cascade,
    current_owner_name varchar(255) not null,
    owner_matches_landlord boolean not null,
    has_trust_registration boolean not null,
    has_seizure boolean not null,
    has_provisional_seizure boolean not null,
    has_provisional_disposition boolean not null,
    has_auction_proceeding boolean not null,
    has_lease_registration boolean not null,
    has_mortgage boolean not null,
    senior_debt_amount bigint check (senior_debt_amount is null or senior_debt_amount >= 0),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists building_ledger_manual_finding (
    id uuid primary key,
    house_check_id uuid not null unique references house_check_request(id) on delete cascade,
    usage varchar(255) not null,
    is_residential_use_confirmed boolean not null,
    is_violation_building boolean not null,
    is_unit_confirmed boolean not null,
    is_contract_area_consistent boolean not null,
    approval_date date,
    housing_type_observed varchar(40),
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create table if not exists market_price_snapshot (
    id uuid primary key,
    house_check_id uuid not null unique references house_check_request(id) on delete cascade,
    estimated_market_value bigint check (estimated_market_value is null or estimated_market_value >= 0),
    estimated_jeonse_value bigint check (estimated_jeonse_value is null or estimated_jeonse_value >= 0),
    source_label varchar(255) not null,
    reference_date date not null,
    source_kind varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_market_price_snapshot_reference_date on market_price_snapshot (reference_date);

create table if not exists house_risk_report (
    id uuid primary key,
    house_check_id uuid not null unique references house_check_request(id) on delete cascade,
    risk_level varchar(20) not null,
    summary text not null,
    registry_summary text not null,
    building_summary text not null,
    deposit_summary text not null,
    jeonse_ratio numeric(7, 2),
    total_exposure_ratio numeric(7, 2),
    valuation_status varchar(30) not null,
    valuation_note text,
    recovery_status varchar(30) not null,
    recovery_note text,
    estimated_auction_value bigint,
    recoverable_deposit_amount bigint,
    shortfall_amount bigint,
    generated_at timestamptz not null
);

create index if not exists idx_house_risk_report_generated_at on house_risk_report (generated_at);
create index if not exists idx_house_risk_report_risk_level on house_risk_report (risk_level);

create table if not exists house_risk_reason (
    id uuid primary key,
    report_id uuid not null references house_risk_report(id) on delete cascade,
    reason_code varchar(80) not null,
    risk_level varchar(20) not null,
    title varchar(255) not null,
    detail text not null,
    source_type varchar(40) not null,
    display_order integer not null
);

create index if not exists idx_house_risk_reason_report_id on house_risk_reason (report_id);
