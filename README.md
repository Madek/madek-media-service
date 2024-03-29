Madek Media Service
===================


Building, Development & Testing this Project
--------------------------------------------


### Building

#### Without AOT

To build run `./bin/clj-uberjar`.

Run the uberjar:

    java -cp madek-media-service.jar clojure.main -m madek.media-service.main

#### With AOT


To build run `./bin/clj-uberjar-aot`.

Run the uberjar:

    java -jar madek-media-service.jar



### Development

#### Running main

    clj -m madek.media-service.main


#### Create some test files in the old store

    FactoryGirl.create :research_video_media_entry,  responsible_user_id: '653bf621-45c8-4a23-a15e-b29036aa9b10'


Notes
-----

### S3 Upload via Browser

TLDR: pre-signed URLs seems likely to work for AWS and other implementations

https://docs.min.io/docs/upload-files-from-browser-using-pre-signed-urls.html




### Meta Data Extraction with Exif

TLDR: seems to work, but not in every case, falls back to full download

> - Does ExitTool stop processing a file (or stream) when it reaches the required data? i.e. if the metadata is in the header (or the first bit of the file) will it find it without having to read the whole file?
> Yes, if possible.  See the -fast option.

https://exiftool.org/forum/index.php?topic=7579.0

https://exiftool.org/exiftool_pod.html#PIPING-EXAMPLES

curl -s http://a.domain.com/bigfile.jpg | exiftool -fast -
Extract information from an image over the internet using the cURL utility. The -fast option prevents exiftool from scanning for trailer information, so only the meta information header is transferred.



### SHA1 in the Browser

https://stackoverflow.com/questions/18586839/read-file-stream-using-javascript-in-web-browser?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa


