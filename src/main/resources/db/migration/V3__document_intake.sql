create table if not exists document_intake_session (
    id uuid primary key,
    owner_id varchar(120) not null,
    expires_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null
);

create index if not exists idx_document_intake_session_owner_id on document_intake_session (owner_id);
create index if not exists idx_document_intake_session_expires_at on document_intake_session (expires_at);

create table if not exists document_intake_document (
    id uuid primary key,
    session_id uuid not null references document_intake_session(id) on delete cascade,
    document_type varchar(40) not null,
    processing_status varchar(40) not null,
    original_file_name varchar(255) not null,
    mime_type varchar(120) not null,
    file_size bigint not null check (file_size >= 0),
    storage_key varchar(255),
    failure_code varchar(80),
    failure_message varchar(255),
    uploaded_at timestamptz not null,
    processed_at timestamptz,
    deleted_at timestamptz,
    constraint uk_document_intake_document_type unique (session_id, document_type),
    constraint uk_document_intake_storage_key unique (storage_key)
);

create index if not exists idx_document_intake_document_session_id on document_intake_document (session_id);
create index if not exists idx_document_intake_document_uploaded_at on document_intake_document (uploaded_at);

create table if not exists document_intake_extracted_field (
    id uuid primary key,
    session_id uuid not null references document_intake_session(id) on delete cascade,
    document_id uuid not null references document_intake_document(id) on delete cascade,
    field_key varchar(80) not null,
    raw_value text not null,
    reviewed_value text,
    source_page integer,
    source_text text,
    confidence numeric(5, 4) not null check (confidence >= 0 and confidence <= 1),
    review_status varchar(40) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_document_intake_field_key unique (document_id, field_key)
);

create index if not exists idx_document_intake_extracted_field_session_id on document_intake_extracted_field (session_id);
create index if not exists idx_document_intake_extracted_field_document_id on document_intake_extracted_field (document_id);

create table if not exists document_intake_warning (
    id uuid primary key,
    session_id uuid not null references document_intake_session(id) on delete cascade,
    document_id uuid references document_intake_document(id) on delete cascade,
    warning_type varchar(40) not null,
    message varchar(255) not null,
    related_field_keys varchar(255) not null,
    created_at timestamptz not null
);

create index if not exists idx_document_intake_warning_session_id on document_intake_warning (session_id);
