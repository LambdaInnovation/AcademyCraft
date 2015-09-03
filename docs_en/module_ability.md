# THE SPECIFICATION DOCUMENTATION OF ACADEMYCRAFT
# ABILITY MODULE

Ability Module discribes the central machanism of AcademyCraft, the Superability System. The Superability System is complex. It involves almost all of the system in AcademyCraft. Therefore, it will be relatively complex to explain.

All the names of Class in this documentation omitted the prefix ```cn.academy.ability.```.

Brief Introduction of APIs
---

This package is a package of API. The the Interfaces(Classes) for users are as follows:

### Category
Category means a skill department. You should define the skill classifications of this skill department, the skills of this department and the dependency relationships of those skills.
You need
```java
CategoryManager.register(Category)
```
to register a skill department. You can also use the annotation ```@RegCategory``` on the domain to register.

### Controllable
Controllable is the Base Class of ```Skill```. It means a Class which can be called by a ability operation key. Skill and others special after it extend this Abstract Class to implement the operations.

### Skill
Skill means a simple operatable skill. It must be added in a ```Category``` to be effective. It can be learned in the Ability Developer. You need define its learning information, its logo, its name and its operation.

### SkillInstance
Skill Instance is a Class which discribes a skill operation. More specifically, it discribes the action when player press/tick/release a key. SkillInstance would only be created in client. You should use SkillInstance and ```SyncAction``` together to affect the server. We implement a simple CD(Cooling-off Period) system. ```api.ctrl.instance.*``` provides some usual Packaged Class of SkillInstance.

### SyncAction, ActionManager
Please be quickly to complete this, Vio.

### AbilityData
This is a basic ability data of a player. It stores the ability department information and the skills which player learned of the player in this time.

### CPData
This is the Calculation Force of player. It stores the information about Calculation Force and Overload of the player.

### PresetData
This is the pre-sets data of player. It stores all of the pre-set information and the current pre-set of the player.

### client.event.*
There are a lot of internal operations of mod conduct by the Event System of MinecraftForge.
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
由于技能操作部分数据很重，为了调试的方便**强制采用**ripple脚本编写数据。为此，我们已经在Skill内做了脚本值/函数读取的集成。

通常，一个技能所对应的SkillInstance和SyncAction应该分别写为内部类和静态内部类，并且每一个```Skill```在本类中存放一个静态单例。

这样，就可以在SyncAction和SkillInstance中访问到Skill的脚本集成了。详细的实现请参考源代码中已实现的技能。（电气使和原子崩坏）


被动技能
---

被动技能同样用Skill类解决。通过设置```canControl=false```让这个技能不在预设设置里出现。然后就可以用一般的```@SubscribePipeline```
写法来写数值逻辑了。Skill类会在构造器里自动往AC的全局pipeline注册，所以不用手动注册。

详见.vanilla.generic包的几个技能。


特殊技能
---
特殊技能通过SpecialSkill配合SubSkill类来实现。在特殊技能执行的阶段，会有一个SpecialSkillAction持续活跃。你可以通过这个SpecialSkillAction来操纵
特殊技能对操纵的重载情况。具体的实现方法详见javadoc。

SyncAction用法详述
---
Vio快来填坑~

调试指令
---
* /preset 和玩家操作预设有关的指令。
* /aim 玩家能力相关的综合指令。可以设置能力系、能力等级等等。

TODO
---

* 实现BUFF（被动）技能
* 实现技能树App和各个等级的能力开发机
* 进一步进行SkillInstance和SyncAction的包装
* 实现特殊技能
