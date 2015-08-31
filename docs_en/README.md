# Documentation Of The Specification Of AcademyCraft
## The Structure Of The Source Code And The Convention

Author: WeAthFolD

This documentation of the specification mainly explained the source code specification when we develop the AcademyCraft.
Please consult the [Google Java Style](http://google-styleguide.googlecode.com/svn/trunk/javaguide.html) and the [Code Conventions for the Java TM Programming Language](http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html) when this specification don't get involved.

The Structure Of Packages
===

The root package： ```cn.academy```
The universal format of the name of package： ```cn.academy.<模块包>.[功能包1].[功能包2]. ...```

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
	This is the implementation module of department of ability. We expect there will be electro_master, melt_downer, teleporter and vector_controller. (e.g.```vanilla.electromaster```)

The secondary things：

* __crafting__
	This is the module of synthesising. There is the content about recipe in survival mode. There are some machine about synthesising(e.g.Etcher), the sythesising recipe and the Items and Blocks which is **only** used as synthesising.
* __misc__
	This is the miscellaneous module. It cantains 
* __support__
	This is the module which can support the party products. We will develop the universal adapter about the energy and NEI support.
* __test__
	This is the module about testing. The function which is not sure whether to be added to the game will be put here and waiting to be moved or be removed.

Under each module package, we classify each Class by function category. Such as ```block```, ```tile``` and ```client.render```.

The Organizing Principle Of Classes
===

在每个模块包内，应该存在一个名为ModuleXXX的主类。在这个主类中使用AR的```@RegInit```注解完成必要的初始化工作。
Under each module package. There should be a major Class and it should be named "ModuleXXX". In this Class, we use a AR annotation, ```@RegInit``` to finish some 

不过，在AC中提倡使用分散实现和注册的方法。应该有效的利用AR提供的功能实现子系统的分别注册，而非在一个统一的类中加载。
Item和Block是个例外。它们由于经常被全局访问，应该在每个模块主类中被注册。其他一些需要提供全局访问的实例，也应该在每个模块主类中被注册。

代码原则
===

* 暴露的接口处提供充分的注释和说明
* 将接口和实现明确分开
* 高度模块化和分别注册
* 善用事件系统进行去耦
* 纯装饰物品和方块尽量使用JsonLoader进行加载

惯例
===

* 尽量使用core包提供的包装和抽象，少造轮子，造轮子之前检查有没有重复
* 客户端的很多渲染和声音相关资源可以从```Resource```类获取。
* Item的基类是```ACItem```
* Block的基类是```ACBlock```和```ACBlockContainer```
* Command的基类是```ACCommand```
* 带物品栏的方块```TileEntity```基类是```TileInventory```

分支规则
===

目前可以无脑推送到```master```。在接下来应该会引入更严谨的git协作组织规则。

Ripple脚本集成
===

AC中有很多数据很重的实现部分。为了将数据和实现解耦，我们使用了LIUtils提供的Ripple脚本语言。任何数据量很大又经常修改的内容，都应该考虑使用脚本来实现。能力实现部分强制使用Ripple编写数据。

在脚本摆放计算值或者脚本所必须遵守的命名范例如下：

* 脚本文件存放在assets/academy/scripts
* 必须在"ac"命名空间之内。
* 可以直接在第一级子命名空间摆放内容。 例如 ac { developer { } }
* 在上一条使用过程中出现名称冲突的情况，考虑直接更名。
* 对能力系的脚本摆放有特殊的要求。格式如下：
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
* 杂项数值可以放置在"generic.r"中。
* 能力系通用数值放置在"ability.r"中。
* 每个能力系技能相关数值单独新建一个脚本。（"electro_master.r", "melt_downer.r"）
* 如果有很多数值同属于一个分类，考虑为那个分类单独新建一个脚本文件。

在调用和加载脚本时：

* 在AcademtCraft#preInit方法内加载所有脚本文件。
* 使用AcademyCraft#getValue,getDouble,getFunction获取方法。也可以使用Academy.script.xxx。

其它文档
===

每一个模块会在同目录下提供一个单独的md文档，来索引它所具有的基本功能，以及提供该模块的阅读/使用/开发思路和基本用法。

文档索引：

* 补完中……
