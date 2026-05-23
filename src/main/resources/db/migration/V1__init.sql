create table if not exists app_bootstrap (
    id integer primary key,
    created_at timestamptz not null default now()
);

insert into app_bootstrap (id)
values (1)
on conflict (id) do nothing;
