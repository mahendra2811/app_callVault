-- Run in Supabase → SQL Editor before enabling FCM.
create table if not exists public.device_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users on delete cascade,
  fcm_token text not null,
  updated_at timestamptz default now(),
  unique (user_id, fcm_token)
);

alter table public.device_tokens enable row level security;

create policy "users manage own tokens" on public.device_tokens
  for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
