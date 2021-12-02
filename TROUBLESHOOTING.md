# FAQ for Troubleshooting common known Issues

# Usability Issues

## The button for adding websites obstructs website entry options button.
This is an extreme edge case almost never happening. If this issue is however appearing on your device, please go to the Advanced Settings and enable the option "Replace Add Button with Menu Entry".
You can now add website entries through the main menu in the app's home (Three dots in the top right corner.) and the original button won't appear, again.

# Connection Issues
## My TOR/Onion address keeps sporadically showing up as unavailable, even though it is available.
[TOR](https://www.torproject.org/) is a technology with a lot of pitfalls, if not used correctly. For example, it can happen, that your phone cannot connect through [Orbot](https://github.com/guardianproject/orbot) all the time reliably to the Onion address you want to monitor. That is "normal" in the sense, that [TOR](https://www.torproject.org/) is not meant to be super stable or fast, but it has a much different purpose.
So, if you get a notification about your Onion service not being available and then it seems to be available again, then that is normal behaviour.

The connection issues to [TOR](https://www.torproject.org/) services are therefore not an issue with this app but a common side effect when using that technology.

To avoid such problems, you could bump up the monitor interval to, for example, once a day, so you only get notified once a day maximum, even if it isn't really down.
You could also pause the monitoring of all Onion addresses and manually check from time to time.