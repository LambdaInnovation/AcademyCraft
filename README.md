![](https://raw.githubusercontent.com/LambdaInnovation/AcademyCraft/master/blob/logo_resized.png)  

[![Build Status](http://ci.sumcraft.net:8080/job/LI-AcademyCraft/badge/icon)](http://ci.sumcraft.net:8080/job/LI-AcademyCraft/)

A Minecraft mod about superability, including its aquiring, upgrading and using. The inspiration of AcademyCraft comes from [A Certain Scientific Railgun (とある科学の超電磁砲)](https://en.wikipedia.org/wiki/A_Certain_Scientific_Railgun) but the mod content is not limited of the background.

Visit [AcademyCraft's Website](http://ac.li-dev.cn/) to get the latest release and 
know more about it.

Developement
===========

A java8 dev environment with scala compatibility is required. Please refer to buildscript for Forge version and dependencies.

to build the mod you must first build [LambdaLib][lambdalib] locally (use `gradlew install`).

If you encounter `Unknown constant: 18` error, `gradlew build` again and it should be fine.

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