-- Run in Supabase → SQL Editor before enabling account deletion in the app.
-- Verifies the calling user's password and deletes their auth.users row in one transaction.
-- Returns true on success, raises on bad password / no user.

create extension if not exists pgcrypto;

create or replace function public.delete_current_user(p_password text)
returns boolean
language plpgsql
security definer
set search_path = public, auth, extensions
as $$
declare
  v_uid uuid := auth.uid();
  v_hash text;
begin
  if v_uid is null then
    raise exception 'not_authenticated';
  end if;

  select encrypted_password into v_hash from auth.users where id = v_uid;
  if v_hash is null then
    raise exception 'user_not_found';
  end if;

  if v_hash <> crypt(p_password, v_hash) then
    raise exception 'invalid_password';
  end if;

  delete from auth.users where id = v_uid;
  return true;
end;
$$;

revoke all on function public.delete_current_user(text) from public;
grant execute on function public.delete_current_user(text) to authenticated;
