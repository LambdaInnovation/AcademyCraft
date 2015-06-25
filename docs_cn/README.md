# AcademyCraft 标准文档
## 代码结构和惯例说明

Author: WeAthFolD

本代码规范主要说明在AC中编程的代码惯例。本代码规范未涉及的部分，请参考[Google Java Style](http://google-styleguide.googlecode.com/svn/trunk/javaguide.html)或[Code Conventions for the Java TM Programming Language](http://www.oracle.com/technetwork/java/javase/documentation/codeconvtoc-136057.html)。
应该还存在很多没完善的内容，持续补完中。

包结构
===

根包： ```cn.academy```
通用包格式： ```cn.academy.<模块包>.[功能包1].[功能包2]. ...```

在根包下的一级包（模块包）列表如下（在1.0应该不会再增加）：

主要：

* __core__
	Mod主类和一些核心机制。这个模块中不存放任何的实现，只存放：
		
		* 通用模板类(e.g. ```ACBlock```, ```ACItem```, ```ACCommand```)
		* 通用Utils（e.g. ```Resources```, ```RayRenderer```，```ACSounds```）
		* 通用Helper（e.g. ```PlayerData```，```DataPart```）
		* 总之是通用的、内容无关的

* __energy__
	能源系统子模块。这里面存放的是所有和虚通量能源系统有关的实现。无线网事件、能源矩阵、节点、能源物品等都在这里。

* __knowledge__
	知识系统子模块。知识系统和相关UI在这里实现。

* __terminal__
	数据终端子模块。在这里可以找到数据终端的UI和添加新App的方法。

* __ability__
	能力系统子模块。这里是支撑整个能力系统的基础框架。能力开发机、主界面的能力UI、操作控制系统等都在这个包中。

* __vanilla__
	能力系实现模块。预计会有电气使、原子崩坏、空间移动、矢量操纵。每一个系单独开一个包。(e.g.```vanilla.electromaster```)

次要：

* __crafting__
	合成模块。和生存模式合成相关的内容都在这里。有一些合成的机器（e.g. 蚀刻机），合成表，以及**仅作**合成用的物品和方块。
* __misc__
	杂项模块，包含彩蛋内容。
* __support__
	第三方支持模块。将会在这里做能源的通用适配以及NEI支持等等。
* __test__
	测试模块。未定的和待定的新功能都暂时放到这里，以待迁移或者删除。

在每个模块包之下，按照功能类别对Class进行分类。例如： ```block```, ```tile```, ```client.render```。

类组织原则
===

在每个模块包内，应该存在一个名为ModuleXXX的主类。在这个主类中使用AR的```@RegInit```注解完成必要的初始化工作。

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
