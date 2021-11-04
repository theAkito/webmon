<h1 style="text-align: center;">Webmon</h1>

<h1 align="center"><img width="120" height="120" src="fastlane/metadata/android/en-US/images/icon.png" alt="logo"/></h1>

<p style="text-align: center;">Monitor web services and get notified, if a service becomes unavailable.</p>

<br>
<br>

[![Language](https://img.shields.io/badge/project-language-blue?style=plastic)](https://kotlinlang.org/)

[![Upstream](https://img.shields.io/badge/project-upstream-yellow?style=plastic)](https://gitlab.com/manimaran/website-monitor)

![GitHub](https://img.shields.io/github/license/theAkito/webmon?style=plastic)
![Liberapay patrons](https://img.shields.io/liberapay/patrons/Akito?style=plastic)

<p align="center">
<a href="https://f-droid.org/packages/ooo.akito.webmon/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" width="200px"></a><a href="https://play.google.com/store/apps/details?id=ooo.akito.webmon"><img src="https://raw.githubusercontent.com/manimaran96/Spell4Wiki/master/files/assets/images/badges/google_play.png" width="200px"></a>
</p>

[**Release/Demo App**](https://github.com/theAkito/webmon/releases)


## App Features
- Simple UI.
- No login required.
- Get notified when site becomes unavailable.
- Check website status according to chosen interval.
- Pause/Resume Monitoring for a particular site.
- Custom Monitoring option. Useful for continually checking website status every 1 second or 5 minutes. The Second/Minute interval can be manually assigned.
- Tap a website entry to quickly refresh that particular one.
- Ability to notify only on Server issue. If enabled, does not notify, if the app has no internet connection.
- Import & Export of Website entry list backups.
- Check connectivity of common address records (A, AAAA) from a domain.
- Check Onion Domains, when [Orbot](https://github.com/guardianproject/orbot) is running.

## Screenshots 

| Main | Settings | Monitor Intervals | Custom Monitoring Setup |
|:-:|:-:|:-:|:-:|
| ![First](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png?raw=true) | ![Sec](fastlane/metadata/android/en-US/images/phoneScreenshots/2.png?raw=true) | ![Third](fastlane/metadata/android/en-US/images/phoneScreenshots/3.png?raw=true) | ![Fourth](fastlane/metadata/android/en-US/images/phoneScreenshots/4.png?raw=true) |

| Backup & Restore | Monitor A/AAAA DNS Records | Monitor Onion Sites | Search |
|:-:|:-:|:-:|:-:|
| ![Fifth](fastlane/metadata/android/en-US/images/phoneScreenshots/5.png?raw=true) | ![Sixth](fastlane/metadata/android/en-US/images/phoneScreenshots/6.png?raw=true) | ![Seventh](fastlane/metadata/android/en-US/images/phoneScreenshots/7.png?raw=true) | ![Eighth](fastlane/metadata/android/en-US/images/phoneScreenshots/8.png?raw=true) |

## TODO
* [Add Dark Mode](https://gitlab.com/manimaran/website-monitor/-/issues/3).
* [Use Notification workaround to ensure App never gets killed, when in background.](https://gitlab.com/manimaran/website-monitor/-/issues/14)
* ~~[Support Tor Proxy.](https://gitlab.com/manimaran/website-monitor/-/issues/2)~~
* ~~[Check common address records (A, AAAA).](https://gitlab.com/manimaran/website-monitor/-/issues/11)~~
* ~~Export Website Entries to JSON as Backup.~~
* ~~Enable Import of Backups.~~
* ~~Publish to F-Droid.~~
* ~~Merge Notifications into single one, if more than one Website is down.~~

## Note

This is a fork of the project [WebSite Monitor](https://gitlab.com/manimaran/website-monitor) made by [Manimaran](https://gitlab.com/manimaran).

This fork is [approved and kindly supported](https://gitlab.com/manimaran/website-monitor/-/merge_requests/4#note_724151423) by the maintainer of the original project.

## License

<img src="https://raw.githubusercontent.com/manimaran96/Spell4Wiki/master/files/assets/images/badges/gplv3.svg" width="100px"></img>

Copyright (C) 2021  Akito <the@akito.ooo>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
