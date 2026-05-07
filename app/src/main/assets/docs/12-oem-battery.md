# OEM battery setup

Stock Android respects callNest's background sync schedule. Several Chinese OEM skins do not. If your reminders never fire or sync stops after a day, follow the steps for your phone.

## Xiaomi / MIUI

1. **Settings → Apps → Manage apps → callNest**.
2. Tap **Battery saver** → choose **No restrictions**.
3. Open **Autostart** in the same screen and enable it.
4. Lock callNest in the recents tray (swipe down, tap the lock icon).

## Realme / OPPO / ColorOS

1. **Settings → Battery → App battery management → callNest**.
2. Toggle **Allow background activity** and **Allow auto-launch**.

## Vivo / FunTouch / OriginOS

1. **iManager → App manager → Autostart manager** → enable callNest.
2. **Settings → Battery → High background power consumption** → allow callNest.

## OnePlus / OxygenOS 11+

1. **Settings → Apps → callNest → Battery**.
2. Switch from **Optimised** to **Don't optimise**.

## Samsung / OneUI

1. **Settings → Apps → callNest → Battery**.
2. Set **Unrestricted**.
3. **Settings → Device care → Battery → Background usage limits** → remove callNest from any deep-sleep list.

## Generic Android 13+

- **Settings → Apps → Special access → Alarms & reminders** → enable callNest. This is required for follow-up reminders to be exact.
