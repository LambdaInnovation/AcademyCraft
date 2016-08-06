![](https://raw.githubusercontent.com/LambdaInnovation/AcademyCraft/master/blob/logo.png)  

[![Build Status](http://jenkins.mcbox.cc/job/AcademyCraft/badge/icon)](http://jenkins.mcbox.cc/job/AcademyCraft/)

A Minecraft mod about superability, including its aquiring, upgrading and using. The inspiration of AcademyCraft comes from [A Certain Scientific Railgun (とある科学の超電磁砲)](https://en.wikipedia.org/wiki/A_Certain_Scientific_Railgun) but the mod content is not limited of the background.

Visit [AcademyCraft's Website](http://ac.li-dev.cn/) to get the latest release and 
know more about it.

Developement
===========

Before either build the mod or setup the workspace, you must first build [LambdaLib][lambdalib] 
with the required version as specified in `build.properties` locally (using `gradlew install`).

## Build

Simply use `gradlew build`. If you encounter `Unknown constant: 18` error, `gradlew build` again and it should be fine.

## Workspace

An IDEA workspace with everything required can be set up using `gradlew idea`. You must have scala plugin installed.

Eclipse workspace is not tested.

Issue Submission
============

Any issue should be prefixed with the version associated. For example, `[1.0.0] Some unknown bug`.

## Bug

Please provide the following information if you are to submit a bug:

* A brief description of the bug
* A repeatible way to reproduce the bug, if you can find any
* Crash report and the latest client/server log

Localization
============

Any localization help is greatly appreciated! Please submit the PR to `dev` branch. Proper
credits for the translator will be given in the next version's release. Note that contents include
[Lang files][langdir] and [Tutorial texts][tutdir].

Misc
====

## License

Licensed under [GPLv3](http://www.gnu.org/licenses/gpl.html).

## Modpack permission

Yes. >)

## Regarding Toaru Magic Index

Many people have been asking questions about whether or how much the mod will be related to
the original story _A Certain Magic Index_. Our answer is that although AC is based on the 
_Railgun_, which is a spinoff of _Index_, the mod will only focus on the science side of 
the story, and thus just loosely related to _Index_.

The mod is dedicated to build an interesting experience evolved around the idea of **superability**,
that's really everything.

[langdir]: src/main/resources/assets/academy/lang
[tutdir]: src/main/resources/assets/academy/tutorials
[lambdalib]: https://github.com/LambdaInnovation/LambdaLib
