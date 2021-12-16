# FAQ for Troubleshooting common known Issues

# Usability Issues

## The button for adding websites obstructs website entry options button.
This is an extreme edge case almost never happening. If this issue is however appearing on your device, please go to the Advanced Settings and enable the option "Replace Add Button with Menu Entry".
You can now add website entries through the main menu in the app's home (Three dots in the top right corner.) and the original button won't appear, again.

# Reliability Issues

## Even when enabling the Forced Background Service in the Advanced Settings, the app/service sometimes still gets killed.
On most modern phones, it's enough to not have have any service running to keep the app working. Then, there are older or otherwise special phones, which kill apps frequently, for whatever reason. These are the cases where you can turn on the Forced Background Service and your app will keep running, as long as the permanent notification is visible. However, there is a small percentage of really old, powerless or special phones, which have so little resources available, that they might even kill the app and its service, when the Forced Background Service option is enabled.
If you have a phone from a Chinese manufacturer, like e.g. Xiaomi, with stock ROM installed, then you still have a chance to fix this issue. [See this example workaround](https://stackoverflow.com/a/39575519/7061105).

However, if your phone is truly very old and/or powerless, there is little you can do about this issue. It could perhaps be theoretically possible to make it somehow work with a lot of work and effort, but there is little reason to invest time and resources into making this app work on such devices. Additionally, it does not make much sense to drain the battery and weak CPU/RAM on such phones, any further.

This is why the app explicitly does not support such devices, i.e. it's welcome if it is run, however fixes won't be provided for any issues arising from such a stituation.
## My TOR/Onion address keeps sporadically showing up as unavailable, even though it is available.
[TOR](https://www.torproject.org/) is a technology with a lot of pitfalls, if not used correctly. For example, it can happen, that your phone cannot connect through [Orbot](https://github.com/guardianproject/orbot) all the time reliably to the Onion address you want to monitor. That is "normal" in the sense, that [TOR](https://www.torproject.org/) is not meant to be super stable or fast, but it has a much different purpose.
So, if you get a notification about your Onion service not being available and then it seems to be available again, then that is normal behaviour.

The connection issues to [TOR](https://www.torproject.org/) services are therefore not an issue with this app but a common side effect when using that technology.

To avoid such problems, you could bump up the monitor interval to, for example, once a day, so you only get notified once a day maximum, even if it isn't really down.
You could also pause the monitoring of all Onion addresses and manually check from time to time.