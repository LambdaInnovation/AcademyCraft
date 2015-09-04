# SPECIFICATION DOCUMENTATION OF ACADEMYCRAFT
# THE CORE MODULE

Author：WeAthFolD
Translator: GISDYT

All of the package paths in this documentation begin with ```cn.academy.core.```.

Content Introductions
---

The package core stores the central loader of AcademyCraft and some abstract wrapups. The core class of AcademyCraft is in package core. There are also some wrapups and tools which have nothing to do with content.

The following content mainly explains the existing tools and abstracts in the package core and the basic usage of them. Please refer to the comments of the code for details.



Basic Utils
---

* ```ACSounds``` is the helper method of playing sounds.
* ```ControlOverrider``` is a very useful tool to override the controls. This class can shield a native key mapping of MineCraft temporarily and restore it dynamically.
* ```ModuleCoreClient.keyHandler, dynKeyHandler``` is the univeral KeyHandler manager of AcademyCraft. You can use the first one to register the key which need to be affected by cfg and use the second one to register the key which needn't to be affected by cfg.




Template Classes
---

* ```ACItem``` is the base class of items. It can set the name and creative tab automatically.
* ```ACBlock``` is the base class of blocks. It can set the name and creative tab automatically.
* ```ACBlockContainer``` is a Container version of the base class of blocks.
* ```ACCommand``` is the base class of commands. It provides the formatting method of the localized strings of AcademyCraft.
* ```TileRecieverBase``` is the base class of Tile of the energy receiving blocks.
* ```TileInventory``` is the base class of the Tile with the inventory.
* ```EntityRayBase&RenderRayXXX``` is the ray and render ray. It provides the prewrap of many beam effects of AcademyCraft.


Register
---
* ```InstanceEjector``` is used to export the instances of Block/Item from JsonLoader to static domain.
* ```RegACKeyHandler``` is used to register key mappings.
* ```RegDataPart``` is used to register the DataPart.


util.DataPart
---

__DataPart__ is a component class which is writted to cope with the harsh additional data of entities of MineCraft.

The basic concepts of it: Each class ```DataPart``` is a data type which can be equipped with player statically(lastingly).
And there data synchronization between the client and the server of this data type to some extent. ```DataPart``` handles equipping/creating/read from NBT automatically. And it provides
a very convenient data synchronization method. If you need some additional data which is based on player, it is the best choise to use this.

DataPart is driven by ```util.PlayerData```. By calling 

```java
PlayerData.register("id", MyDataPart.class);
```

, you can register a DataPart.

Of cource, you can use AnnotationRegistry to register the DataPart, such as:

```java
@Registrant
@RegDataPart("CP")
public class CPData extends DataPart { ... }
```

DataPart will be constructed automatically and we guarentee that it is availability in the following process when the player enter the world after you register it. By calling ```PlayerData.get(player).getDataPart()```, you can get the DataPart.

DataPart is loaded from the data which was readed from NBT by server as a benchmark, and it will synchronized the data to client when it is loading.
So, please be sure to pay attention for the possibility of synchronization data when client is getting data. In the client and server, please use unified method ```fromNBT()``` and ```toNBT()``` to read and write the data.

In addition, DataPart has already had a serializer -- __INSTANCE__, so you can easily carry out the custon synchronization based on it by using the NetworkCall.

At last, we suggest that use a special method, ```public static get(EntityPlayer)``` to get the instance of DataPart which conrrespond to this player for each custom DataPart. By this way, you can reduce code amount in a large scale.


ValuePipeline
---

 **ValuePipeline** is mainly used to deal with the demand of a class of passive skills. You can take it as a modified and extrmely simplified event bus.

You can pipe a series of number(int, float, double) via ```ValuePipeline.pipeXXX(key, value, parameters...```
And you can use a String as key(channel) to distinguish different numberical content. ValuePipeline will ergodic the Subsriber of this key and value when you pipe a value into it.
And you can modify it. When pipe is finished, the return value you get is the value which was modified by all Subcriber.

You can use ```ValuePipeline.register(Object)``` to register a instance of Subcriber. All the method which is labeled by ```@SubscribePipeline(key)``` in it will be considered a pipeline method.
A pipeline should be like this:

```java
@SubscribePipeline("someValue")
public [type] pipelineMethod([type] input, [AnyType] par1, [AnyType] par2, ...);
```

That is to say, the first parameter must be in the same type with the return value and their type must be one of the (int, float , double). The parameter after it is the additional parameters which are introduced into pipe.
They should discribe the additional conditions when calculating this value(such as the instance of player who are using skills), and the less, the better.
The count and type of the parameters after it should match the count and type of pipe accurately. If the match fails, there will be a warning be printed to the console.

You can visit the universal pipeline of AcademyCraft by ```AcademyCraft.pipeline```.

**The hint of style: Please try to ensure that the parameters of pipe is simple. generally speaking, just one EntityPlayer parameter is enough. All the code should try to ensure that this single player parameter is specifical and avoid complex.**

Hint UI
---
We implement a cool hint UI, like this:
![](https://raw.githubusercontent.com/LambdaInnovation/AcademyCraft/master/blob/ui0.jpg)
The method to visit it is NotifyUI. At present, it is implemented by the method of listening events and add display in the NotifyUI initiative. If there are more contents need notify, you will consider to open the interface.
