[![Build Status](http://ci.sumcraft.net:8080/job/LI-AcademyCraft/badge/icon)](http://ci.sumcraft.net:8080/job/LI-AcademyCraft/)

﻿![](https://raw.githubusercontent.com/LambdaInnovation/AcademyCraft/master/blob/logo_resized.png)  

## Introduction

AcademyCraft is a Minecraft mod about superability, including its aquiring, upgrading and using. The inspiration of the mod comes from [A Certain Scientific Railgun (とある科学の超電磁砲)](https://en.wikipedia.org/wiki/A_Certain_Scientific_Railgun) but the mod content is not limited of the background.

AcademyCraft is licensed under the [GPL license](http://www.gnu.org/licenses/gpl.html "gpl license")


## Downloads&Installation

Visit our [Website](http://ac.li-dev.cn/) to get the latest release.

## Dev Setup

### Requirements

* JDK8 and 1.8 project compilance level
* MinecraftForge 1.7.10-10.13.4.1448.
* [LambdaLib][llib] ___dev___ branch

### Supported Mods (Compile-Time Dependencies)

* IndustrialCraft [2.2.717](http://jenkins.ic2.player.to/job/IC2_experimental/717/).
* Mine Tweaker [3.0.9C](http://minetweaker.powerofbytes.com/download/MineTweaker3-Dev-1.7.10-3.0.9C.jar)
* Not Enough Items [1.0.3.74](http://chickenbones.net/Pages/links.html).
* Redstone Flux API [1.7.10R1.0.2](https://github.com/CoFH/RedstoneFlux-API).
* Thermal Expansion [4.0.3B1-218](http://minecraft.curseforge.com/mc-mods/69163-thermalexpansion/files/2246924).

### Build

Use ``gradle clean install`` to build locally.

## Localization

We would love the help if you are willing to submit a translation of your language. We are officially maintaining `zh_CN`, `en_US` and `zh_TW` language file. Just send a pull request for typo fixes and new translation, and we'll merge it after verifying. Please submit the PR to `master` branch, which reflects the lastest release version. Proper credits will be given in the next release :w:

You can find .lang files [HERE](src/main/resources/assets/academy/lang).

NOTE: The `zh_TW` language file is machine-translated and is somewhat inaccurate. Feel free to send a patch.

## Documents

Code refs and documents are under reconstruction. If you are trying to write plugins or sth else and is confused about the code, submit a issue and we'll see what we can do.

[llib]: https://github.com/LambdaInnovation/LambdaLib
