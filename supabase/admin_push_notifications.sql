-- Run this in Supabase SQL Editor.
-- It stores admin FCM tokens and creates notification rows whenever inventory becomes low.

create table if not exists public.admin_devices (
  token text primary key,
  device_name text default '',
  updated_at timestamptz default now()
);

alter table public.admin_devices enable row level security;

drop policy if exists "admin devices are readable" on public.admin_devices;
create policy "admin devices are readable"
on public.admin_devices for select
using (true);

drop policy if exists "admin devices can upsert token" on public.admin_devices;
create policy "admin devices can upsert token"
on public.admin_devices for insert
with check (true);

drop policy if exists "admin devices can update token" on public.admin_devices;
create policy "admin devices can update token"
on public.admin_devices for update
using (true)
with check (true);

create or replace function public.create_low_stock_notification()
returns trigger
language plpgsql
as $$
begin
  if new.quantity <= new.min_quantity and old.quantity is distinct from new.quantity then
    insert into public.notifications(machine_id, machine_name, message, is_read)
    values (
      coalesce(new.machine_id, 'M01'),
      'May ' || coalesce(new.machine_id, 'M01'),
      case
        when new.quantity <= 0 then new.product_name || ' da het hang'
        else new.product_name || ' sap het hang, con ' || new.quantity || ' chai'
      end,
      false
    );
  end if;

  return new;
end;
$$;

drop trigger if exists trg_low_stock_notification on public.machine_inventory;
create trigger trg_low_stock_notification
after update of quantity on public.machine_inventory
for each row
execute function public.create_low_stock_notification();

-- Next step in Supabase Dashboard:
-- Database > Webhooks > Create webhook
-- Table: notifications
-- Events: Insert
-- Type: Supabase Edge Function
-- Function URL: https://<project-ref>.supabase.co/functions/v1/send-admin-push
