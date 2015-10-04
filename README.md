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

IndustrialCraft [2.2.717](http://jenkins.ic2.player.to/job/IC2_experimental/717/).

Redstone Flux API [1.7.10R1.0.2](https://github.com/CoFH/RedstoneFlux-API).

Thermal Expansion [4.0.3B1-218](http://minecraft.curseforge.com/mc-mods/69163-thermalexpansion/files/2246924).

Not Enough Items [1.0.3.74](http://chickenbones.net/Pages/links.html).

### Build

The mod requires Java 8 to compile. Officially we use [RetorLamba](https://github.com/evant/gradle-retrolambda) to compile the mod, and any use of stream API is seriously forbiddened.

Use ``gradle clean install`` to build locally.

## Documents

We are working on filling up the documents of AC codes. You can check out the Chinese version of API document at [HERE](https://github.com/LambdaInnovation/AcademyCraft/tree/master/docs_cn "Chinese Documents"). Pull requests are welcomed.

[llib]: https://github.com/LambdaInnovation/LambdaLib
