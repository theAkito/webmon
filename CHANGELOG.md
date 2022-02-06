# 2.8.0 [Code 13] (2022/02/06)
- Fix: Wrong WebsiteEntry order by itemPosition when exiting Main Search.
- Fix: Interval chooser displaying interval options only in minutes.
- Fix: Favicons blinking during Retrieval.
- Fix: Data Backup Import not applying Tag updates.
- Improve: Optionally let Data Backup Import force existing Website Entries to update accordingly.
- Improve: Sync Worker reliability.
- Improve: Re-Vamp Sync Worker Foreground Service.
- Improve: Deal with Forced Background Service in a user-friendly way.
- Improve: Warn user when attempting to add Website Entry with duplicate URL.
- Improve: Avoid potential data loss when creating new Website Entry. Thank you, @Naitim!
- Add: Custom Port Support for Onion Addresses.

# 2.7.0 [Code 12] (2021/12/04)
- Fix: SwipeRefresh Toggle.
- Fix: Re-Order Website Entries.
- Fix: Floating Action Button disappearing after deleting Entry.
- Improve: Website Favicon Retrieval Reliability & Efficiency.
- Improve: Re-Vamp Debug Log.
- Add: Advanced Setting: Floating Action Button as Menu Item.
- Add: Advanced Setting: Long SwipeRefresh Trigger Distance.
- Add: Android Permission Explanations.
- Add: Troubleshooting Info: Replace Add Button with Menu Entry.

# 2.6.0 [Code 11] (2021/11/20)
- Fix: Sharing Backups.
- Fix: Search Filter not working reliably all the time.
- Improve: Upgrade Project Icon.
- Add: Fetching Website icons is back! This time with 100% FOSS tools.
- Add: Force Background Service (optional). This way, the app never gets killed in the background.
- Add: Show App Log for Debug purposes.
- Add: Share App Log Dump.

# 2.5.0 [Code 10] (2021/11/13)
- Fix: Creating new Website Entry dispatches a "Website Unavailable" message.
- Fix: isOnionAddress Switch sometimes appearing when TOR is disabled.
- Improve: Pre-Fill URL field with "https://" Prefix when creating new Website Entry.
- Improve: Replace Toast with Snackbar for unavailable Website Messages.
- Improve: Remove redundant Backup & Restore Buttons in Menu.
- Improve: Do not inform about auto start on App startup.
- Improve: Pretty-Print Backup JSON.
- Add: Tag Cloud to Website Entry Editor.
- Add: Make Website Entries searchable by Tag.
- Add: Advanced Settings: Import & Export Data Backup.
- Add: Advanced Settings: Import & Export Settings Backup.
- Add: Advanced Settings: Share Data & Settings Backup.
- Add: Advanced Setting: Toggle Refresh on Swipe.
- Add: Advanced Setting: Delete All Website Entry Tags.

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