# Documentation Of The Specification Of AcademyCraft
## The Structure Of The Source Code And The Convention

Author: WeAthFolD

This documentation of the specification mainly explained the source code specification when we develop the AcademyCraft.
Please consult the [Google Java Style](http://google-styleguide.googlecode.com/svn/trunk/javaguide.html) and the [Code Conventions for the Java TM Programming Language](http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html) when this specification don't get involved.

The Structures Of Packages
===

The root package： ```cn.academy```
The universal format of the name of package： ```cn.academy.<Module>.[Function1].[Function2]. ...```

The list of the single-stage sub package of the root package, the package of module: (We will not change it in version 1.0)

The major things：

* __core__
	The Main Class of this Mod and some of core mechanism. There is no implement in it. There are only:
		
		* Universal Template Class(e.g. ```ACBlock```, ```ACItem```, ```ACCommand```)
		* Universal Utils（e.g. ```Resources```, ```RayRenderer```，```ACSounds```）
		* Universal Helper（e.g. ```PlayerData```，```DataPart```）
		* Altogether, there are some universal and unrelated to content things.

* __energy__
	This is the sub module of the energy system. There is all the information about the implements of the IF. And there are also the Event of the wireless-network, the matrix of energy and the entity of energy.

* __knowledge__
	This is the sub module of the knowledge system. There are the implements and the User-Interface of the knowledge system.

* __terminal__
	This is the sub module of the data terminal. You can find the User-Interface of the data terminal and the Method of adding new App.

* __ability__
	This is the sub module of the ability system. It's the basic structure which support the whole ability system. The Ability-Developer, the ability User-Interface of the main interface and the operating controller system are all in this package.

* __vanilla__
	This is the implementation module of department of ability. We expect there will be electro_master, melt_downer, teleporter. (e.g.```vanilla.electromaster```)

The secondary things：

* __crafting__
	This is the module of synthesising. There is the content about recipe in survival mode. There are some machine about synthesising(e.g.Etcher), the sythesising recipe and the Items and Blocks which is **only** used as synthesising.
* __misc__
	This is the miscellaneous module. It cantains Easter Egg.
* __support__
	This is the module which can support the party products. We will develop the universal adapter about the energy and NEI support.
* __test__
	This is the module about testing. The function which is not sure whether to be added to the game will be put here and waiting to be moved or be removed.

Under each module package, we classify each Class by function category. Such as ```block```, ```tile``` and ```client.render```.

The Organizing Principles Of Classes
===

Under each module package. There should be a major Class and it should be named "ModuleXXX". In this Class, we use a AR annotation, ```@RegInit``` to finish some necessary initialization.

But in AcademyCraft, we advocate distributed implement and distributed register. You should effective use the function AR provide to implement the distributed registry of the sub system, instead of load them in a uniform Class. But Item and Block is exceptional. You should registering them in the major Class of each each module, because they are usually globally visited. Others instance which need to provide globally visiting should also be registering in the major Class of each module.

The Principles Of The Source Code
===

* Provide plenary notes and discribes in the public interface.
* Separate the interface and the implement clearly.
* Highly modular and registering respectively.
* Use Event System and decoupling.
* Use JsonLoader to load the Item/Block only for decorative as much as possible.

Conventions
===

* Use the Wrapper Class and Abstract Class as much as possible and make wheels as less as possible. Check repeat things before make wheels.
* You can get a lot of resource about render and voice from the Class ```Resource```.
* The Base Class of Item is ```ACItem```.
* The Base Class of Block is ```ACBlock``` and ```ACBlockContainer```.
* The Base Class of Command is ```ACCommand```.
* The Base Class of the Block with Item Bar, ```TileEntity``` is ```TileInventory```.

The Rules Of Branches
===

At this time we just need to push them to ```master``` branch directly. But we should introduce some more rigorous rules to organize the git cooperating.

Integrated Script Of Ripple
===

There are a lot of implement with very heavy data. In order to decoupling the data and the implement, we use the script language named Ripple provided by LIUtils. Any content with large data and will be usually modified should be implemented by the script. We are forced to use the Ripple to write data for the implement of the ability module. 

You should follow the following naming conventions when you put the value or script in the script:

* The script file should be put in assets/academy/scripts.
* You have to put things in the namespace named "ac".
* You can put the content in the first sub namespace directly. Such as ac { developer { } }.
* Consider change the name directly when the last rule has a naming conflict.
* There are some special requirements of putting the scripts of the ablity departments. Format is as follows:
```
ac {
	<category_name> {
		<skill_name> {
			<prop> { 1 }
			<prop> { 2 }
			<funct>(par1, par2, ...) { ... }
		}
	}
}
```
* The miscellaneous value can be put in "generic.r".
* The universal value of ability department be put in "ability.r".
* Create a new script for the value about each department of ability. (e.g."electro_master.r", "melt_downer.r")
* If there are a lot of value belongs to the same category, consider to create a new script file for that category.

When you call and load the scripts:

* Load all of the script files in the method AcademyCraft@preInit.
* Use AcademyCraft#getValue,getDouble,getFunction to get methods. You can also use Academy.script.xxx.

Others Documentations
===

Each module will provide a single md documentation to index all of its basic functions and provide the reading/using/developing thingking and basic usage of this module.

The index of documentations:

* Working in progress.
