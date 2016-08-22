# About
The Communote content replacement plugin is a plugin for [Communote](https://github.com/Communote/communote-server) which adds the ability to modify the content of notes with the 
help of regular expression based search and replace operations. These manipulations are performed while the notes are rendered and are thus not persisted.

# Compatibility
The plugin can be used with a Communote standalone installation and the Communote SaaS platform.

The following table shows which Communote server versions are supported by a specific version of the plugin. A server version which 
is not listed cannot be used with the plugin.

| Plugin Version  | Supported Server Version |
| ------------- | ------------- |
| 1.2  | 3.4  |

# Installation
To install the plugin get a release from the [Releases](https://github.com/Communote/communote-plugin-content-replacement/releases) section and deploy it to your Communote installation 
as described in the [Installation Documentation](http://communote.github.io/doc/install_extensions.html).

# Usage
After installing the plugin a new page named 'Content Replacement' will be available in the administration section of Communote under 'Extensions'. There you can view, add and remove 
replacement definitions.

A replacement definition has a name, a condition which should be matched and a replacement for the match. The name is only for you and can be used to give the definition a descriptive 
identifier. The condition is a regular expression which will be applied to the content of each note. We use the Java regular expression flavor. The available constructs are documented 
[here](https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html). The actual replacement can be anything but you should take care that after applying the replacement the 
HTML of the note is still valid. Within the replacement you can use references to matched groups of the expression (i.e. $0 for the whole match, $1 for the first group and so on). 
The last element of the form to add a replacement definition is a checkbox which when checked modifies how the replacement is applied. If this option is activated the replacement 
is not used to replace the match instead it is appended to the content of the note.

## Example
The following example will replace every occurrence of ```[gh-x]``` where x is a number with a link to the GitHub issue with that number of the 
[Communote server](https://github.com/Communote/communote-server) repository. The link is enriched with a small octocat logo (:octocat:) which was previously uploaded as an attachment 
to a note in a Communote topic that is readable by all users.

* Name: Communote GitHub issues
* Condition: (^|[\s>;])\[(gh-)([0-9]+)\]
* Replacement:

  ```
  $1<a href="https://github.com/Communote/communote-server/issues/$3" target="_blank">$2$3<img style="border:0px;width:16px;height:16px;display:inline" src="http://example.com/communote/microblog/global/image/attachment.do?id=30" /></a>
  ```


# Building
To build the plugin you have to clone or download the source and setup your development environment as described in our [Developer Documentation](http://communote.github.io/doc/dev_preparation.html). 
Afterwards you can just run ```mvn``` in the checkout directory. The JAR file will be created in the target subdirectory.

# License
The plugin is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).
