# 2.4.0 [Code 9] (2021/11/06)
- Fix: Ensure only one Notification per event is dispatched.
- Fix: Periodic Background Check reliability.
- Improve: Various Code Details.
- Add: Advanced Settings Area to Settings Page.
- Add: Advanced Setting: Delete All Website Entries.

# 2.3.0 [Code 8] (2021/11/05)
- Fix: Not resolving AAAA records correctly.
- Improve: DNS Resolution efficiency.
- Improve: Detect & indicate NX_DOMAIN failure, if present on all A/AAAA DNS Records' responses.
- Add: Laissez Faire Mode. If enabled, does not notify, if the website responds with one of the following codes: 201, 202, 204, 401, 403.

# 2.2.3 [Code 7] (2021/11/04)
- Various UX Improvements.

# 2.2.2 [Code 6] (2021/11/04)
- Fix: Failure indication, even if all destinations are reachable.
- Fix: Visit Website action.
- Fix: Item Order not respected after updating website entry.
- Add: Backup & Restore.
- Add: Check connectivity of common address records (A, AAAA) from a domain.
- Add: Check Onion Domains, when Orbot is running.

# 2.0.0 [Code 5] (2021/11/01)
- Grab Website favicons through DuckDuckGo, instead of Google.
- Do not show Notifications while App is in Foreground.
- Show only a single notification, if more than one website is not reachable. (Do not show 10 Notifications for 10 unreachable Websites.)
- Enable Drag & Drop to re-order Website entries.
- Make upgrading to newer versions easier.