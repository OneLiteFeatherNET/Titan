-- Append-only: a finished run is inserted once and never changed. mode and end_reason hold the
-- enum names, never ordinals, so reordering the constants cannot shift stored data.
create table jumprun_run (
    id          bigint generated always as identity primary key,
    player_uuid uuid         not null,
    player_name varchar(32)  not null,
    mode        varchar(16)  not null,
    score       integer      not null check (score >= 0),
    end_reason  varchar(16)  not null,
    finished_at timestamptz  not null
);

create index jumprun_run_player_mode on jumprun_run (player_uuid, mode, score desc);
create index jumprun_run_mode_player_best on jumprun_run (mode, player_uuid, score desc, finished_at);
