# AcademyCraft 标准文档
# ability模块

ability模块描述的是AC的核心机制——超能力系统。超能力系统较为复杂，牵涉mod中几乎所有的系统，因而要说明的方面也会相对繁杂。

本文档中的所有类名均省略了```cn.academy.ability.```前缀。

API简介
---

该包属于API类型的包，对用户所暴露的接口（类）大致如下所示：

### Category
Category代表一个技能系。你应该在技能系里定义该技能系的技能分类、所具有的技能， 以及这些技能的相互依赖关系。
你需要通过
```java
CategoryManager.register(Category)
```
来注册一个技能系。也可以通过标记在域上的```@RegCategory```注解来注册。

### Controllable
Controllable是```Skill```的基类。它代表一个可以被能力操作键位所调用到的类。Skill和之后的特殊技能的子技能为了实现操作都会继承这个抽象类。

### Skill
Skill代表一个普通的操作型技能。它需要被添加到一个```Category```中才被认为是有效的。它可以在能力开发机中被学习。你需要在Skill类中定义它的学习
相关信息、它的logo和名称、以及它的操作等。

### SkillInstance
SkillInstance是描述一个技能的操作的类。具体的来说，它描述了当玩家按下按键、按键tick、松开按键时的行为。SkillInstance只会在client被创建。你应该
将SkillInstance和```SyncAction```配合使用，以影响服务端。 在它的内部同时实现了一个简单的CD（冷却时间）系统。 ```api.ctrl.instance.*```提供了一些常用的SkillInstance包装。

### SyncAction, ActionManager
Vio快来填坑

### AbilityData
一个玩家的基本能力数据，存放了玩家当前的能力系信息以及学习的技能信息。

### CPData
玩家的计算力数据。存放了玩家当前的计算力和超载信息。

### PresetData
玩家的预设数据。存放了玩家的所有预设的信息，以及玩家的当前预设。

### client.event.*
很多mod内部的操作交互通过MinecraftForge的事件系统进行。
在此包内创建了一系列和能力操作相关的事件。你可以通过侦听```MinecraftForge.EVENT_BUS```获取这些
事件的回调。

内部实现
---
* 能力开发机： ```.block.*```, ```developer.*```
* 超能力指令： ```.command.*```
* 界面： ```.client.ui.*```
* 技能树App： ```.api.app.*```

技能实现和脚本集成
---
由于技能操作部分数据很重，为了调试的方便强制采用ripple脚本编写数据。为此，我们已经在Skill内做了脚本值/函数读取的集成。

通常，一个技能所对应的SkillInstance和SyncAction应该分别写为内部类和静态内部类，并且每一个```Skill```在本类中存放一个静态单例。
这样，就可以在SyncAction和SkillInstance中访问到Skill的脚本集成了。详细的实现请参考源代码中已实现的技能。

SyncAction用法详述
---
Vio快来填坑~

TODO
---

* 实现BUFF（被动）技能
* 实现技能树App和各个等级的能力开发机
* 进一步进行SkillInstance和SyncAction的包装
* 实现特殊技能
