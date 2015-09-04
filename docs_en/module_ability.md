# THE SPECIFICATION DOCUMENTATION OF ACADEMYCRAFT
# ABILITY MODULE

Ability Module discribes the central machanism of AcademyCraft--the Superability System. The Superability System is really complex. It involves almost all of the systems in AcademyCraft. Therefore, it will be relatively complex to explain.

All the names of Class in this documentation omits the prefix ```cn.academy.ability.```.

Brief Introduction of APIs
---

This package is a API package. The the Interfaces(Classes) for users are as follows:

### Category
An ESPer owns an Ability Category at any time. You should define the skill classifications of this skill category, the skills of this category and the dependency relationships of those skills.
You should call
```java
CategoryManager.register(Category)
```
to register a category. You can also use the annotation ```@RegCategory``` in the domain to register.

### Controllable
Controllable is the Base Class of ```Skill```. It represents a Class which can be called by a ability operation keys. Skill and others special controlling method extends this abstract class to implement the operations.

### Skill
Skill means a simple operatable skill. It must be added in a ```Category``` to be effective. It can be learned in the Ability Developer. You need define its learning information, its icon, its name and its operation.

### SkillInstance
Skill Instance is a Class which discribes a skill operation. More specifically, it discribes the action when player press/tick/release a key. SkillInstance would only be created in client. You should use SkillInstance and ```SyncAction``` together to affect the server. We implement a simple CD(Cooldown) system. ```api.ctrl.instance.*``` provides some usual Wrapped Class of SkillInstance.

### SyncAction, ActionManager
Please complete this quickly, Vio.

### AbilityData
This is a basic ability data of a player. It stores the ability category information and the skills which player learned of the player in this time.

### CPData
This is the Calculation Point of player. It stores the information about Calculation Point and Overload of the player.

### PresetData
This is the preset data of player. It stores all of the preset information and the current preset of the player.

### client.event.*
There are a lot of internal operations of mod conducted by the Event System of MinecraftForge.
In this package, we created a lot of events about ability operations. You can get the callback of those events by listening ```MinecraftForge.EVENT_BUS```.

Internal Implementations
---
* The Ability Developer: ```.block.*```, ```developer.*```
* The Commands of Superability: ```.command.*```
* The UI: ```.client.ui.*```
* The App of the Skill Tree: ```.api.app.*```

the Implementation of Skills and Integration of the Script
---
In order to debug easily, we use Ripple Script to write datas compulsively because the skill operations involves lots of data tweaking. To this end, we have integrated the machanism about reading the value/function of script in class Skill.

Usually, the class SkillInstance and SyncAction should be defined as internal class and static internal class respectively. And each ```Skill``` should store a static single-instance of them.

If you do like that, then you can visit the integrated script in SyncAction and SkillInstance. Please refer to the functions which has been implemented in the source code for details. (e.g.electro_master and melt_downer)


the Passive Skills
---

You can also use the Class Skill to implement the passive skill. By setting ```canControl=false```, you can make this skill disappear in the preset UI. And then, you can use simple annotation, ```@SubscribePipeline``` on the methods to write the number logic. The Class Skill will regiter it to the universal pipeline of AcademyCraft in the constructor, so you needn't to register it by yourself.

Please refer to a few skills in the package .vanilla.generic for details.


Special Skills
---
You can use the class SpecialSkill and SubSkill to implement a special skill. There will be a sustained active SpecialSkillAction when the special skills are executing. You can control the controlling overriding of the special skills by this SpecialSkillAction. Please refer to JavaDoc for details.

Usage of SyncAction
---
Please complete this quickly, Vio.

Debug Commands
---
* /preset is the command about the pre-sets of the player.
* /aim is the omnibus command of player's ability. It can set the ability category, the ability level and so on.

TODO
---

* Implement BUFF(passive) skills
* Implement the App of the skill tree and the ability developer of each level
* Do more wrapping for SkillInstance and SyncAction
* Implement the special skills

Translator: GISDYT
