alter table market_price_snapshot
    add column sample_count integer,
    add column lawd_cd varchar(5),
    add column deal_ymd_from varchar(6),
    add column deal_ymd_to varchar(6);
