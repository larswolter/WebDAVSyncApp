# m4bApp
An application to manage m4b Audiobooks. First function implemented is to synchronize files from an webdav server (Nextcloud for example)

## Syncing files with Webdav

The app shows local m4b Mediafiles and compares them with m4b files on a webdav server. It then allows to download non existing files from webdav to the Media Store.
Use the settings page to specify the webdav URL and credentials. You can also select if files that do not exist on the webdav should be removed locally.

## Planed features

* better comparison between local and remote, not just filename but hash/filesize/modified date
* show some more infos for files, embedded image/size/modified
* show more progress during download
* automatic sync in the background
* Make a good looking UI
* play the files, but i think we are better of to do this with other players, like [Voice](https://github.com/PaulWoitaschek/Voice)
