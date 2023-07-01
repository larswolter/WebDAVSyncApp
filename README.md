# WebDAVSyncAPP
An application to sync files from a webdav server (Nextcloud for example). 
First usage is to sync some audiobooks 

## Syncing files with Webdav

The app shows local m4b Mediafiles and compares them with m4b files on a webdav server. It then allows to download non existing files from webdav to the Media Store.
Use the settings page to specify the webdav URL and credentials. You can also select if files that do not exist on the webdav should be removed locally.

## Planed features

* Adding multiple sync jobs, defining file type (currently only m4b audiobook) and locations
* Better comparison between local and remote, not just filename but hash/filesize/modified date
* Show more progress during download
* Automatic sync in the background
* Make a good looking UI
* Sharing files